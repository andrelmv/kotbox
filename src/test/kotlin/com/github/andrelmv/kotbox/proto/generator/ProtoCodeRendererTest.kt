package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.proto.generator.model.ProtoEnumModel
import com.github.andrelmv.kotbox.proto.generator.model.ProtoField
import com.github.andrelmv.kotbox.proto.generator.model.ProtoFieldType
import com.github.andrelmv.kotbox.proto.generator.model.ProtoMessage
import com.github.andrelmv.kotbox.proto.generator.model.ProtoModifier
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ProtoCodeRendererTest {
    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    @Test
    fun `test renders syntax header`() {
        val model = message("Empty")
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.startsWith("""syntax = "proto3";"""))
    }

    @Test
    fun `test renders java package options when provided`() {
        val model = message("Empty")
        val output = ProtoCodeRenderer.render(model, javaPackage = "com.example")
        assertTrue(output.contains("""option java_package = "com.example";"""))
        assertTrue(output.contains("option java_multiple_files = true;"))
    }

    @Test
    fun `test does not render java package options when blank`() {
        val model = message("Empty")
        val output = ProtoCodeRenderer.render(model, javaPackage = "")
        assertFalse(output.contains("option java_package"))
    }

    // -------------------------------------------------------------------------
    // Scalar fields
    // -------------------------------------------------------------------------

    @Test
    fun `test renders scalar string field`() {
        val model =
            message(
                "User",
                scalarField("name", "string", number = 1),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("string name = 1;"))
    }

    @Test
    fun `test renders scalar int32 field`() {
        val model =
            message(
                "User",
                scalarField("age", "int32", number = 1),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("int32 age = 1;"))
    }

    @Test
    fun `test renders optional scalar field`() {
        val model =
            message(
                "User",
                scalarField("nickname", "string", number = 1, modifier = ProtoModifier.OPTIONAL),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("optional string nickname = 1;"))
    }

    @Test
    fun `test renders multiple scalar fields with correct numbers`() {
        val model =
            message(
                "User",
                scalarField("name", "string", number = 1),
                scalarField("age", "int32", number = 2),
                scalarField("active", "bool", number = 3),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("string name = 1;"))
        assertTrue(output.contains("int32 age = 2;"))
        assertTrue(output.contains("bool active = 3;"))
    }

    // -------------------------------------------------------------------------
    // Well-known type imports
    // -------------------------------------------------------------------------

    @Test
    fun `test emits timestamp proto import for google protobuf Timestamp field`() {
        val model = message("Event", scalarField("occurredAt", "google.protobuf.Timestamp", number = 1))
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("""import "google/protobuf/timestamp.proto";"""))
    }

    @Test
    fun `test emits any proto import for google protobuf Any field`() {
        val model = message("Wrapper", scalarField("payload", "google.protobuf.Any", number = 1))
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("""import "google/protobuf/any.proto";"""))
    }

    @Test
    fun `test emits no import for plain scalar fields`() {
        val model = message("User", scalarField("name", "string", number = 1))
        val output = ProtoCodeRenderer.render(model)
        assertFalse(output.contains("import"))
    }

    @Test
    fun `test import appears before message block`() {
        val model = message("Event", scalarField("occurredAt", "google.protobuf.Timestamp", number = 1))
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.indexOf("import") < output.indexOf("message Event"))
    }

    @Test
    fun `test import appears before java package options`() {
        val model = message("Event", scalarField("occurredAt", "google.protobuf.Timestamp", number = 1))
        val output = ProtoCodeRenderer.render(model, javaPackage = "com.example")
        assertTrue(output.indexOf("import") < output.indexOf("option java_package"))
    }

    @Test
    fun `test deduplicates imports when multiple fields use the same well-known type`() {
        val model =
            message(
                "Event",
                scalarField("startedAt", "google.protobuf.Timestamp", number = 1),
                scalarField("endedAt", "google.protobuf.Timestamp", number = 2),
            )
        val output = ProtoCodeRenderer.render(model)
        assertEquals(1, output.split("""import "google/protobuf/timestamp.proto";""").size - 1)
    }

    @Test
    fun `test emits timestamp import for repeated Instant field`() {
        val model =
            message(
                "Event",
                repeatedField("timestamps", "google.protobuf.Timestamp", number = 1),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("""import "google/protobuf/timestamp.proto";"""))
    }

    // -------------------------------------------------------------------------
    // camelCase -> snake_case
    // -------------------------------------------------------------------------

    @Test
    fun `test converts camelCase field name to snake_case`() {
        val model =
            message(
                "User",
                scalarField("firstName", "string", number = 1),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("string first_name = 1;"))
    }

    // -------------------------------------------------------------------------
    // Repeated fields
    // -------------------------------------------------------------------------

    @Test
    fun `test renders repeated scalar field`() {
        val model =
            message(
                "User",
                repeatedField("tags", "string", number = 1),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("repeated string tags = 1;"))
    }

    @Test
    fun `test renders repeated custom type field with nested message`() {
        val nested =
            message(
                "Item",
                messageRefField("label", "string", number = 1),
            )
        val model =
            message(
                "Cart",
                repeatedField("items", "Item", number = 1, nested = nested),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("message Item {"))
        assertTrue(output.contains("repeated Item items = 1;"))
    }

    // -------------------------------------------------------------------------
    // Map fields
    // -------------------------------------------------------------------------

    @Test
    fun `test renders map of scalar key and scalar value`() {
        val model =
            message(
                "User",
                mapField("scores", "string", "int32", number = 1),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("map<string, int32> scores = 1;"))
    }

    @Test
    fun `test renders map of scalar key and custom type value with nested message`() {
        val nested =
            message(
                "Address",
                messageRefField("street", "string", number = 1),
            )
        val model =
            message(
                "User",
                mapField("addresses", "string", "Address", number = 1, nestedValue = nested),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("message Address {"))
        assertTrue(output.contains("map<string, Address> addresses = 1;"))
    }

    // -------------------------------------------------------------------------
    // Nested messages
    // -------------------------------------------------------------------------

    @Test
    fun `test renders nested message before parent message`() {
        val nested =
            message(
                "Address",
                scalarField("street", "string", number = 1),
            )
        val model =
            message(
                "User",
                scalarField("address", "Address", number = 1, nested = nested),
            )
        val output = ProtoCodeRenderer.render(model)
        val addressIdx = output.indexOf("message Address")
        val userIdx = output.indexOf("message User")
        assertTrue(addressIdx < userIdx)
    }

    @Test
    fun `test renders three-level nested hierarchy`() {
        val coords =
            message(
                "Coordinates",
                messageRefField("lat", "double", number = 1),
            )
        val address =
            message(
                "Address",
                messageRefField("coordinates", "Coordinates", number = 1, nested = coords),
            )
        val model =
            message(
                "User",
                messageRefField("address", "Address", number = 1, nested = address),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("message Coordinates {"))
        assertTrue(output.contains("message Address {"))
        assertTrue(output.contains("message User {"))
        val coordsIdx = output.indexOf("message Coordinates")
        val addressIdx = output.indexOf("message Address")
        val userIdx = output.indexOf("message User")
        assertTrue(coordsIdx < addressIdx)
        assertTrue(addressIdx < userIdx)
    }

    @Test
    fun `test deduplicates nested messages appearing in multiple fields`() {
        val nested = message("Tag", scalarField("value", "string", number = 1))
        val model =
            message(
                "User",
                scalarField("primaryTag", "Tag", number = 1, nested = nested),
                scalarField("secondaryTag", "Tag", number = 2, nested = nested),
            )
        val output = ProtoCodeRenderer.render(model)
        assertEquals(1, output.split("message Tag {").size - 1)
    }

    // -------------------------------------------------------------------------
    // Enums
    // -------------------------------------------------------------------------

    @Test
    fun `test renders enum before message that references it`() {
        val enum = ProtoEnumModel("Score", linkedSetOf("HIGH", "LOW", "MEDIUM"))
        val model =
            message(
                "User",
                enumField("score", "Score", number = 1, enum = enum),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("enum Score {"))
        assertTrue(output.contains("HIGH = 0;"))
        assertTrue(output.contains("LOW = 1;"))
        assertTrue(output.contains("MEDIUM = 2;"))
        val enumIdx = output.indexOf("enum Score")
        val messageIdx = output.indexOf("message User")
        assertTrue(enumIdx < messageIdx)
    }

    @Test
    fun `test enum entries are uppercased`() {
        val enum = ProtoEnumModel("Status", linkedSetOf("active", "inactive"))
        val model =
            message(
                "User",
                enumField("status", "Status", number = 1, enum = enum),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("ACTIVE = 0;"))
        assertTrue(output.contains("INACTIVE = 1;"))
    }

    @Test
    fun `test renders enum definition for repeated enum field`() {
        val enum = ProtoEnumModel("Status", linkedSetOf("ACTIVE", "INACTIVE"))
        val model =
            message(
                "User",
                ProtoField(
                    name = "statuses",
                    number = 1,
                    fieldType = ProtoFieldType.Repeated(elementProto = "Status"),
                    nestedEnum = enum,
                    nestedMessage = null,
                ),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("enum Status {"))
        assertTrue(output.contains("repeated Status statuses = 1;"))
        val enumIdx = output.indexOf("enum Status")
        val messageIdx = output.indexOf("message User")
        assertTrue(enumIdx < messageIdx)
    }

    @Test
    fun `test deduplicates enums appearing in multiple fields`() {
        val enum = ProtoEnumModel("Score", linkedSetOf("HIGH", "LOW"))
        val model =
            message(
                "User",
                enumField("score1", "Score", number = 1, enum = enum),
                enumField("score2", "Score", number = 2, enum = enum),
            )
        val output = ProtoCodeRenderer.render(model)
        assertEquals(1, output.split("enum Score {").size - 1)
    }

    // -------------------------------------------------------------------------
    // Unresolved fields
    // -------------------------------------------------------------------------

    @Test
    fun `test renders TODO comment for unresolved field`() {
        val model =
            message(
                "User",
                ProtoField(
                    name = "unknown",
                    number = 1,
                    fieldType = ProtoFieldType.Scalar(protoType = "SomeExternalType", modifier = ProtoModifier.NONE),
                    unresolved = true,
                    nestedMessage = null,
                    nestedEnum = null,
                ),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("// TODO: unresolved type"))
    }

    // -------------------------------------------------------------------------
    // Full output shape
    // -------------------------------------------------------------------------

    @Test
    fun `test output ends with newline`() {
        val model = message("User", scalarField("name", "string", number = 1))
        assertTrue(ProtoCodeRenderer.render(model).endsWith("\n"))
    }

    @Test
    fun `test message block is correctly wrapped`() {
        val model = message("User", scalarField("name", "string", number = 1))
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("message User {"))
        assertTrue(output.contains("}"))
    }

    @Test
    fun `test renders optional MessageRef field`() {
        val nested = message("Address", scalarField("street", "string", number = 1))
        val model =
            message(
                "User",
                messageRefField("address", "Address", number = 1, modifier = ProtoModifier.OPTIONAL, nested = nested),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("optional Address address = 1;"))
    }

    @Test
    fun `test renders optional EnumRef field`() {
        val enum = ProtoEnumModel("Score", linkedSetOf("HIGH", "LOW"))
        val model =
            message(
                "User",
                ProtoField(
                    name = "score",
                    number = 1,
                    fieldType = ProtoFieldType.EnumRef(typeName = "Score", modifier = ProtoModifier.OPTIONAL),
                    nestedEnum = enum,
                    nestedMessage = null,
                ),
            )
        val output = ProtoCodeRenderer.render(model)
        assertTrue(output.contains("optional Score score = 1;"))
    }

    private fun message(
        name: String,
        vararg fields: ProtoField,
    ) = ProtoMessage(name = name, fields = fields.toList())

    private fun scalarField(
        name: String,
        protoType: String,
        number: Int,
        modifier: ProtoModifier = ProtoModifier.NONE,
        nested: ProtoMessage? = null,
    ) = ProtoField(
        name = name,
        number = number,
        fieldType = ProtoFieldType.Scalar(protoType = protoType, modifier = modifier),
        nestedMessage = nested,
        nestedEnum = null,
    )

    private fun repeatedField(
        name: String,
        elementProto: String,
        number: Int,
        nested: ProtoMessage? = null,
    ) = ProtoField(
        name = name,
        number = number,
        fieldType = ProtoFieldType.Repeated(elementProto = elementProto),
        nestedMessage = nested,
        nestedEnum = null,
    )

    private fun mapField(
        name: String,
        keyProto: String,
        valueProto: String,
        number: Int,
        nestedValue: ProtoMessage? = null,
    ) = ProtoField(
        name = name,
        number = number,
        fieldType = ProtoFieldType.Map(keyProto = keyProto, valueProto = valueProto),
        nestedMessage = nestedValue,
        nestedEnum = null,
    )

    private fun enumField(
        name: String,
        protoType: String,
        number: Int,
        enum: ProtoEnumModel,
    ) = ProtoField(
        name = name,
        number = number,
        fieldType = ProtoFieldType.EnumRef(typeName = protoType, modifier = ProtoModifier.NONE),
        nestedEnum = enum,
        nestedMessage = null,
    )

    private fun messageRefField(
        name: String,
        protoType: String,
        number: Int,
        modifier: ProtoModifier = ProtoModifier.NONE,
        nested: ProtoMessage? = null,
    ) = ProtoField(
        name = name,
        number = number,
        fieldType = ProtoFieldType.MessageRef(typeName = protoType, modifier = modifier),
        nestedMessage = nested,
        nestedEnum = null,
    )
}
