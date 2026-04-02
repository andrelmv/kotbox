package com.github.andrelmv.kotbox.wrap

/**
 * Describes a coroutine builder that can be used to wrap a selected
 * block of code in the editor.
 *
 * To add a new wrapper, create an instance of this data class and
 * register it in [CoroutineWrapperRegistry].
 *
 * @param id             unique identifier, used internally and in plugin.xml
 * @param displayName    text shown in the Alt+Enter popup and in the menu
 * @param wrapTemplate   template for the generated code; %s is replaced by the selection
 * @param requiredImports fully-qualified imports to add (may be empty)
 * @param actionId        ID of the associated AnAction for keyboard shortcut binding
 */
data class WrapperDescriptor(
    val id: String,
    val displayName: String,
    val wrapTemplate: String,
    val requiredImports: List<String>,
    val actionId: String,
)
