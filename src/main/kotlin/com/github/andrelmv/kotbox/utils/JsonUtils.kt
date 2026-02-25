package com.github.andrelmv.kotbox.utils

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

private val prettyGson by lazy { GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create() }
private val compactGson by lazy { GsonBuilder().disableHtmlEscaping().create() }

internal fun formatJson(json: String): String = prettyGson.toJson(JsonParser.parseString(json))

internal fun compactJson(json: String): String = compactGson.toJson(JsonParser.parseString(json))
