package com.github.andrelmv.kotbox.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonUtilsTest {
    @Test
    fun `test formatJson should convert compact JSON to pretty-printed format`() {
        val compactJson = """{"name":"André","age":30,"city":"Porto"}"""
        val result = formatJson(compactJson)

        val expected = """{
  "name": "André",
  "age": 30,
  "city": "Porto"
}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test formatJson should handle already formatted JSON`() {
        val prettyJson = """{
  "name": "André",
  "age": 30
}"""
        val result = formatJson(prettyJson)

        val expected = """{
  "name": "André",
  "age": 30
}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test formatJson should handle arrays`() {
        val compactJson = """["item1","item2","item3"]"""
        val result = formatJson(compactJson)

        val expected = """[
  "item1",
  "item2",
  "item3"
]"""

        assertEquals(expected, result)
    }

    @Test
    fun `test formatJson should handle nested objects`() {
        val compactJson = """{"user":{"name":"André","address":{"city":"Porto"}}}"""
        val result = formatJson(compactJson)

        val expected = """{
  "user": {
    "name": "André",
    "address": {
      "city": "Porto"
    }
  }
}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test formatJson should handle empty objects`() {
        val compactJson = """{}"""
        val result = formatJson(compactJson)

        assertEquals("{}", result)
    }

    @Test
    fun `test formatJson should handle empty arrays`() {
        val compactJson = """[]"""
        val result = formatJson(compactJson)

        assertEquals("[]", result)
    }

    @Test
    fun `test formatJson should not escape HTML characters`() {
        val compactJson = """{"html":"<div>test</div>","url":"http://example.com"}"""
        val result = formatJson(compactJson)

        val expected = """{
  "html": "<div>test</div>",
  "url": "http://example.com"
}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test compactJson should convert pretty-printed JSON to compact format`() {
        val prettyJson = """{
  "name": "André",
  "age": 30,
  "city": "Porto"
}"""
        val result = compactJson(prettyJson)

        val expected = """{"name":"André","age":30,"city":"Porto"}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test compactJson should handle already compact JSON`() {
        val compactJsonInput = """{"name":"André","age":30}"""
        val result = compactJson(compactJsonInput)

        val expected = """{"name":"André","age":30}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test compactJson should handle arrays`() {
        val prettyJson = """[
  "item1",
  "item2",
  "item3"
]"""
        val result = compactJson(prettyJson)

        val expected = """["item1","item2","item3"]"""

        assertEquals(expected, result)
    }

    @Test
    fun `test compactJson should handle nested objects`() {
        val prettyJson = """{
  "user": {
    "name": "André",
    "address": {
      "city": "Porto"
    }
  }
}"""
        val result = compactJson(prettyJson)

        val expected = """{"user":{"name":"André","address":{"city":"Porto"}}}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test compactJson should not escape HTML characters`() {
        val prettyJson = """{
  "html": "<div>test</div>",
  "url": "http://example.com"
}"""
        val result = compactJson(prettyJson)

        val expected = """{"html":"<div>test</div>","url":"http://example.com"}"""

        assertEquals(expected, result)
    }

    @Test
    fun `test formatJson and compactJson should be reversible for objects`() {
        val original = """{"name":"André","age":30,"city":"Porto"}"""
        val formatted = formatJson(original)
        val compacted = compactJson(formatted)

        assertEquals(original, compacted)
    }

    @Test
    fun `test formatJson should handle special characters`() {
        val compactJson = """{"text":"Quote: \" Backslash: \\ Newline: \n Tab: \t"}"""
        val result = formatJson(compactJson)

        // Verify it's valid JSON and contains the special characters
        assert(result.contains("Quote:"))
        assert(result.contains("Backslash:"))
        assert(result.contains("Newline:"))
        assert(result.contains("Tab:"))
    }

    @Test
    fun `test compactJson should handle numbers and booleans`() {
        val prettyJson = """{
           "integer": 42,
  "float": 3.14,
  "boolean": true
}"""
        val result = compactJson(prettyJson)

        val expected = """{"integer":42,"float":3.14,"boolean":true}"""

        assertEquals(expected, result)
    }
}
