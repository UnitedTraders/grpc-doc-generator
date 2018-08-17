# GRPC doc snippets generator

This project is a plugin for [protobuf-maven-plugin](https://www.xolstice.org/protobuf-maven-plugin/) generating 
AsciiDoc documentation snippets from comments in .proto files.

Inspired by [Spring REST Docs](https://spring.io/projects/spring-restdocs).

## Usage

Add plugin to protobuf-maven-plugin config
```$xml
<plugin>
    <groupId>org.xolstice.maven.plugins</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>0.5.1</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.directory}/generated-snippets/protobuf</outputDirectory>
                <protocPlugins>
                    <protocPlugin>
                        <id>grpcdoc</id>
                        <groupId>com.unitedtraders.grpc</groupId>
                        <artifactId>grpc-doc-generator</artifactId>
                        <version>${project.version}</version>
                        <mainClass>com.unitedtraders.grpcdocgenerator.GrpcDocGenerator</mainClass>
                    </protocPlugin>
                </protocPlugins>
            </configuration>
        </execution>
    </executions>
</plugin>

```

Run compile for project.

Check `${project.build.directory}/generated-snippets/protobuf` for files like `message*.adoc`.

Now you can include snippets into AsciiDoc documentation.

## Additional syntax

Plugin supports additional tags in comment body.

### Field tags

`@mandatory` adds mandatory flag to field.

## Samples

Input:
```proto
syntax = "proto3";

option java_package = "test";

// Test multiline
// comment for message
message TestMessage {
    // Optional Int32 parameter
    int32 optional_int32 = 1;

    // Mandatory Int32 parameter
    // @mandatory
    int32 mangatory_int32 = 2;

    // External message reference
    ExternalMessage external = 3;
}

message ExternalMessage {
    string other_field = 1;
}
```

For message `TestMessage` gives:
```asciidoc
[[message-.TestMessage]]
.`TestMessage` -  Test multiline  comment for message
|===
|Field |Type |Description |Options

|optional_int32 |int32 | Optional Int32 parameter  |

|mangatory_int32 |int32 | Mandatory Int32 parameter    | yes

|external |<<message-.ExternalMessage>> | External message reference  |

|===
```
