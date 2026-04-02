package com.github.andrelmv.kotbox.services.password

object PasswordGeneratorService {
    private val LOWERCASE = ('a'..'z').toList()
    private val UPPERCASE = ('A'..'Z').toList()
    private val DIGITS = ('0'..'9').toList()
    private val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?".toList()
    private const val EMPTY_STR = ""

    private val ADJECTIVES =
        listOf(
            "brave",
            "bright",
            "calm",
            "clean",
            "cool",
            "dark",
            "deep",
            "fast",
            "fierce",
            "fresh",
            "glad",
            "grand",
            "green",
            "happy",
            "hard",
            "heavy",
            "high",
            "holy",
            "huge",
            "icy",
            "kind",
            "large",
            "lazy",
            "light",
            "little",
            "lively",
            "long",
            "loud",
            "lucky",
            "mighty",
            "nice",
            "noble",
            "odd",
            "old",
            "open",
            "plain",
            "proud",
            "pure",
            "quick",
            "quiet",
            "rare",
            "rich",
            "rough",
            "round",
            "safe",
            "sharp",
            "shiny",
            "short",
            "silent",
            "simple",
            "sleek",
            "slim",
            "slow",
            "small",
            "smart",
            "soft",
            "solid",
            "strong",
            "sunny",
            "swift",
            "tall",
            "tame",
            "thin",
            "tiny",
            "warm",
            "wild",
            "wise",
            "witty",
            "young",
            "bold",
            "busy",
            "clear",
            "crisp",
            "cute",
            "damp",
            "dull",
            "early",
            "easy",
            "elite",
            "empty",
            "exact",
            "fair",
            "fancy",
            "fine",
            "firm",
            "flat",
            "fond",
            "free",
            "full",
            "funny",
            "fuzzy",
            "giant",
            "good",
            "great",
            "grey",
            "grim",
            "gross",
            "handy",
            "harsh",
            "hot",
            "ideal",
            "inner",
            "joint",
            "keen",
        )

    private val NOUNS =
        listOf(
            "anchor",
            "arrow",
            "badge",
            "beam",
            "blade",
            "blast",
            "blaze",
            "block",
            "bloom",
            "bolt",
            "bridge",
            "brush",
            "burst",
            "cane",
            "cape",
            "card",
            "chain",
            "cliff",
            "cloud",
            "coast",
            "comet",
            "coral",
            "craft",
            "crest",
            "crown",
            "curve",
            "dawn",
            "delta",
            "dome",
            "draft",
            "drift",
            "drive",
            "dune",
            "eagle",
            "echo",
            "edge",
            "ember",
            "field",
            "flame",
            "flare",
            "flash",
            "fleet",
            "flint",
            "flood",
            "floor",
            "flow",
            "foam",
            "focus",
            "forge",
            "fort",
            "frost",
            "gale",
            "gate",
            "gem",
            "glade",
            "glow",
            "grove",
            "guard",
            "guide",
            "gulf",
            "hammer",
            "haven",
            "hawk",
            "hill",
            "horn",
            "hull",
            "hunt",
            "image",
            "isle",
            "jade",
            "jewel",
            "keep",
            "lance",
            "ledge",
            "light",
            "link",
            "lion",
            "loft",
            "loop",
            "lure",
            "mark",
            "marsh",
            "mast",
            "maze",
            "mesa",
            "mist",
            "moon",
            "mount",
            "oak",
            "orbit",
            "path",
            "peak",
            "pine",
            "plain",
            "plume",
            "pool",
            "port",
            "prism",
            "pulse",
            "quest",
            "ridge",
            "rift",
            "rune",
            "sail",
            "shard",
            "shore",
            "signal",
            "sky",
            "slate",
            "slope",
            "spark",
            "sphere",
            "spike",
            "spire",
            "spring",
            "star",
            "stem",
            "stone",
            "storm",
            "stream",
            "surge",
            "swift",
            "tide",
            "torch",
            "tower",
            "trace",
            "trail",
            "tree",
            "vale",
            "vault",
            "veil",
            "vibe",
            "vine",
            "vista",
            "void",
            "wave",
            "wind",
            "wolf",
            "wood",
            "world",
            "zone",
        )

    fun generate(config: PasswordConfig): String =
        when (config.type) {
            PasswordType.RANDOM -> generateRandom(config)
            PasswordType.MEMORABLE -> generateMemorable(config)
            PasswordType.PIN -> generatePin(config)
        }

    fun generateBulk(
        config: PasswordConfig,
        count: Int,
    ): List<String> = (1..count).map { generate(config) }

    private fun generateRandom(config: PasswordConfig): String {
        val pool =
            buildList {
                addAll(LOWERCASE)
                addAll(UPPERCASE)
                if (config.includeNumbers) addAll(DIGITS)
                if (config.includeSymbols) addAll(SYMBOLS)
            }

        if (pool.isEmpty()) return EMPTY_STR

        val password = CharArray(config.length) { pool.random() }

        // Guarantee at least one char from each required group so the password
        // always satisfies the selected options regardless of random luck.
        var idx = 0
        if (config.includeNumbers && password.none { it in DIGITS }) {
            password[idx++] = DIGITS.random()
        }
        if (config.includeSymbols && password.none { it in SYMBOLS }) {
            password[idx % config.length] = SYMBOLS.random()
        }

        return password.toList().shuffled().joinToString(EMPTY_STR)
    }

    private fun generateMemorable(config: PasswordConfig): String {
        // The length slider controls word count (clamped to 2–8 words).
        val wordCount = (config.length / 8).coerceIn(2, 8)

        val wordPool = ADJECTIVES + NOUNS
        val words = (1..wordCount).map { wordPool.random() }
        val separator = "-"

        val password = StringBuilder(words.joinToString(separator))

        if (config.includeNumbers) {
            password.append(separator)
            password.append((10..999).random())
        }
        if (config.includeSymbols) {
            password.append(SYMBOLS.random())
        }

        val result = password.toString()
        return if (result.length > config.length) result.take(config.length) else result
    }

    private fun generatePin(config: PasswordConfig): String = (1..config.length).map { DIGITS.random() }.joinToString(EMPTY_STR)
}
