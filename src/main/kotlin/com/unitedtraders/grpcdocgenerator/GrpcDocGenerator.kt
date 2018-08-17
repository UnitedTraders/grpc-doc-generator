package com.unitedtraders.grpcdocgenerator

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.compiler.PluginProtos
import com.salesforce.jprotoc.Generator
import com.salesforce.jprotoc.ProtocPlugin
import java.io.FileWriter
import java.io.PrintWriter
import java.util.stream.Stream

class GrpcDocGenerator : Generator() {
    private val logOutput: PrintWriter = PrintWriter(FileWriter("grpcdoc.log"), true)

    override fun generate(req: PluginProtos.CodeGeneratorRequest?): Stream<PluginProtos.CodeGeneratorResponse.File> {
        return req!!.protoFileList.flatMap { generateFile(it) }.stream()
    }

    private fun generateFile(fileProto: DescriptorProtos.FileDescriptorProto?): List<PluginProtos.CodeGeneratorResponse.File> {
        return fileProto!!.messageTypeList
                    .mapIndexed { index, descriptorProto -> generateMessage(descriptorProto, index, fileProto.sourceCodeInfo) }
                    .flatten() +
                fileProto.serviceList
                        .mapIndexed { index, serviceProto -> generateService(serviceProto, index, fileProto.sourceCodeInfo) }
                        .flatten()
    }

    private fun generateService(service: DescriptorProtos.ServiceDescriptorProto,
                                index: Int,
                                sourceCodeInfo: DescriptorProtos.SourceCodeInfo): List<PluginProtos.CodeGeneratorResponse.File> {
        // find message comment location
        val path = listOf(6, index) // 6 is service in FileDescriptorProto message
        val loc = sourceCodeInfo.locationList.firstOrNull { it.pathList == path }

        // generate service context
        val serviceName = service.name
        val context = ServiceContext(serviceName, cleanComments(loc?.leadingComments), emptyList())
        val content = applyTemplate("ServiceStub.mustache", context)

        return listOf<PluginProtos.CodeGeneratorResponse.File>(
                PluginProtos.CodeGeneratorResponse.File.newBuilder()
                        .setName("service-${serviceName}.adoc")
                        .setContent(content)
                        .build()
        )
    }

    private fun generateMessage(msgProto: DescriptorProtos.DescriptorProto?, index: Int,
                                sourceCodeInfo: DescriptorProtos.SourceCodeInfo):
            List<PluginProtos.CodeGeneratorResponse.File> {
        // debug print
        sourceCodeInfo.locationList.forEach { logOutput.println("${it.pathList} - ${it.leadingComments}") }

        // find message comment location
        val path = listOf(4, index) // 4 is message in FileDescriptorProto message
        val loc = sourceCodeInfo.locationList.firstOrNull { it.pathList == path }

        // generate fields
        val fields = msgProto!!.fieldList
                .mapIndexed { fieldIndex, field -> createFieldContext(field, fieldIndex, sourceCodeInfo, path) }
                .toList()

        // generate method context
        val messageName = msgProto.name
        val context = MessageContext(messageName, cleanComments(loc?.leadingComments), fields)
        val content = applyTemplate("MessageStub.mustache", context)

        return listOf<PluginProtos.CodeGeneratorResponse.File>(
                PluginProtos.CodeGeneratorResponse.File.newBuilder()
                        .setName("message-${messageName}.adoc")
                        .setContent(content)
                        .build()
        )
    }

    private fun getFieldType(field: DescriptorProtos.FieldDescriptorProto): String {
        return when (field.type) {
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE -> "double"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT -> "float"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64 -> "int64"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64 -> "unit64"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32 -> "int32"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64 -> "fixed64"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32 -> "fixed32"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL -> "bool"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING -> "string"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP -> "<<group-${field.typeName}>>"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE -> "<<message-${field.typeName}>>"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES -> "bytes"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32 -> "uint32"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM -> "<<enum-${field.typeName}>>"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32 -> "sfixed32"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64 -> "sfixed64"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32 -> "sint32"
            DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64 -> "sint64"
            else -> "Unknown"
        }
    }

    private fun cleanComments(comment: String?): String? {
        return comment
                ?.replace("\n", " ")
                ?.replace("@mandatory", "")
    }

    private fun parseFlags(comment: String?): FieldData {
        val cleared = cleanComments(comment)
        if (comment == null) {
            return FieldData(cleared, FieldFlags(false))
        }
        return FieldData(
                cleared,
                FieldFlags(
                    comment.contains("@mandatory", true)
               )
        )
    }

    private fun createFieldContext(field: DescriptorProtos.FieldDescriptorProto, index: Int,
                                   sourceCodeInfo: DescriptorProtos.SourceCodeInfo,
                                   messagePath: List<Int>): FieldContext {
        val path = messagePath + listOf(2, index)
        val loc = sourceCodeInfo.locationList.firstOrNull { it.pathList == path }
        val comment = loc?.leadingComments ?: loc?.trailingComments

        val (cleared, flags) = parseFlags(comment)

        return FieldContext(field.name, getFieldType(field), cleared, flags)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ProtocPlugin.generate(GrpcDocGenerator())
        }
    }
}

data class FieldFlags(val mandatory: Boolean)

data class FieldContext(val name: String, val type: String, val description: String?, val flags: FieldFlags)

data class MessageContext(val name: String, val description: String?, val fields: List<FieldContext>)

data class FieldData(val comment: String?, val flags: FieldFlags)

data class MethodContext(val name: String)

data class ServiceContext(val name: String, val description: String?, val methods: List<MethodContext>)
