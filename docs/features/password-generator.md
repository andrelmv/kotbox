# Password Generator

The **Password Generator** is a tool for creating secure passwords directly inside IntelliJ IDEA, with support for different password styles, customizable options, and bulk generation.

---

## How to Access

1. Go to **View** → **Tool Windows** → **Kotlin Toolbox**
2. Click the menu icon and select **Password Generator**

---

## Password Types

### Random

Generates a cryptographically random password drawn from a pool of lowercase letters, uppercase letters, and optionally digits and symbols.

Example: `q8DtnHkTPJV!kBRNBEX`

### Memorable

Generates a human-readable passphrase built from common words joined by hyphens. The length slider controls the number of words. Optionally appends a number and/or a symbol at the end.

Example: `brave-storm-eagle-247!`

!!! tip "Memorable passwords"
    Memorable passwords trade some randomness for recallability. Use them when you need to type the password manually or share it verbally.

### PIN

Generates a numeric PIN of the configured length. Numbers and Symbols options are ignored in this mode.

Example: `847291`

---

## Options

| Option                  | Description                                                                                                   |
|-------------------------|---------------------------------------------------------------------------------------------------------------|
| **Characters (slider)** | Controls password length (4–64). For Memorable type, controls the number of words (2–8).                      |
| **Numbers**             | Include digits in Random passwords; append a number to Memorable passwords. Disabled for PIN.                 |
| **Symbols**             | Include symbols (`!@#$%^&*` …) in Random passwords; append a symbol to Memorable passwords. Disabled for PIN. |

---

## Single Password

The generated password is displayed in the output field. Two actions are available:

- **Copy icon** (right of the field) — copies the password to the clipboard
- **Regenerate** — generates a new password with the current settings

The password is automatically regenerated whenever you change the type, length, or any option.

---

## Bulk Generation

Generate multiple passwords at once:

1. Set the desired **Quantity** (1–1000)
2. Click **Generate**
3. The output editor will list one password per line, ready to copy

!!! info "Bulk passwords use the same settings"
    All passwords in a bulk run share the same type, length, and options configured in the single-password section.

---

## Tips

!!! success "Recommendations"
    - Use **Random** with Numbers and Symbols enabled for the strongest passwords
    - Use **Memorable** when the password must be typed from memory
    - Use **PIN** only for systems that exclusively accept numeric codes
    - Use **Bulk** to quickly pre-generate a set of one-time passwords or test credentials

!!! warning "Keep in mind"
    - All generation is done locally — no password ever leaves your machine
    - Generated passwords are not stored — copy or save them before closing the tool

---

<div style="text-align: center;" markdown="1">

**Want to explore other features?**

[JWT Encoder/Decoder](jwt-encoder-decoder.md){ .md-button }
[Kotlin Inlay Hints](kotlin-inlay-hints.md){ .md-button }

</div>
