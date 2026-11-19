package com.example.ui.screens.profile

private val MULTIPLE_SPACES_REGEX = Regex("\\s+")

/**
 * Normalizes a user display name by trimming whitespace and leading/trailing commas
 * and collapsing repeated internal spaces without disturbing valid characters.
 */
fun sanitizeProfileName(raw: String): String {
    return raw
        .trim { it.isWhitespace() || it == ',' }
        .replace(MULTIPLE_SPACES_REGEX, " ")
        .trim { it.isWhitespace() || it == ',' }
}
