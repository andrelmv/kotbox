package com.github.andrelmv.kotbox.services.password

enum class PasswordType(
    val label: String,
) {
    RANDOM("Random"),
    MEMORABLE("Memorable"),
    PIN("PIN"),
}

data class PasswordConfig(
    val type: PasswordType = PasswordType.RANDOM,
    val length: Int = 20,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
)
