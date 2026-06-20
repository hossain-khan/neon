package dev.hossain.neon.engine.highlightjs.internal

import dev.hossain.neon.core.TokenFontStyle
import dev.hossain.neon.core.TokenFontWeight
import kotlin.math.roundToInt

internal object ThemeParser {
    fun parse(cssText: String): Map<String, TokenStyleData> {
        if (cssText.isBlank()) return emptyMap()
        val rules = CssParser(cssText).parseStylesheet()
        if (rules.isEmpty()) return emptyMap()

        val result = mutableMapOf<String, TokenStyleData>()
        for (rule in rules) {
            val style = parseDeclarations(rule.declarations) ?: continue
            for (selector in rule.selectors) {
                applyHljsSelector(result, selector, style)
            }
        }
        return result
    }

    private fun applyHljsSelector(
        result: MutableMap<String, TokenStyleData>,
        selector: String,
        style: TokenStyleData,
    ) {
        val trimmed = selector.trim()
        if (trimmed.isEmpty()) return

        if ("::" in trimmed) return
        if (PSEUDO_CLASS_REGEX.containsMatchIn(trimmed)) return

        if (trimmed.any { it.isWhitespace() }) return
        if ('>' in trimmed || '+' in trimmed || '~' in trimmed) return

        if (!trimmed.startsWith('.')) return
        val match = HLJS_SELECTOR_REGEX.matchEntire(trimmed) ?: return
        val raw = match.value.trimStart('.')

        result[raw] = mergeTokenStyle(result[raw], style)

        val primary = raw.substringBefore('.')
        if (primary != raw && !result.containsKey(primary)) {
            result[primary] = mergeTokenStyle(result[primary], style)
        }
    }

    private val HLJS_SELECTOR_REGEX = Regex("""\.hljs(?:-[\w-]+)?(?:\.[\w][\w-]*)*""")
    private val PSEUDO_CLASS_REGEX = Regex(""":(?!:)[a-zA-Z]""")
    private val PROP_PATTERN = Regex("""([\w-]+)\s*:\s*([^;]+)""")
    private val WHITESPACE_REGEX = Regex("\\s+")

    private data class CssRule(
        val selectors: List<String>,
        val declarations: String,
    )

    private class CssParser(private val src: String) {
        private var pos: Int = 0
        private val len: Int = src.length

        fun parseStylesheet(): List<CssRule> {
            val rules = mutableListOf<CssRule>()
            skipTrivia()
            while (pos < len) {
                when {
                    startsWithCommentHere() -> skipComment()
                    src[pos] == '@' -> skipAtRule()
                    src[pos] == '}' -> break
                    else -> {
                        val rule = readRule() ?: break
                        rules.add(rule)
                    }
                }
                skipTrivia()
            }
            return rules
        }

        private fun readRule(): CssRule? {
            val selectorsRaw = readUntilOpenBrace() ?: return null
            if (pos >= len || src[pos] != '{') return null
            pos++
            val declarations = readDeclarations()
            if (pos < len && src[pos] == '}') pos++
            val selectors = splitTopLevelByComma(selectorsRaw)
            return CssRule(selectors, declarations)
        }

        private fun readUntilOpenBrace(): String? {
            val sb = StringBuilder()
            while (pos < len) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                val c = src[pos]
                if (c == '{') return sb.toString()
                if (c == '}') return null
                sb.append(c)
                pos++
            }
            return null
        }

        private fun readDeclarations(): String {
            val sb = StringBuilder()
            while (pos < len) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                val c = src[pos]
                if (c == '}') break
                if (c == '{') {
                    skipBalancedBlock()
                    continue
                }
                sb.append(c)
                pos++
            }
            return sb.toString()
        }

        private fun skipAtRule() {
            pos++
            while (pos < len) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                val c = src[pos]
                if (c == ';') {
                    pos++
                    return
                }
                if (c == '{') {
                    skipBalancedBlock()
                    return
                }
                pos++
            }
        }

        private fun skipBalancedBlock() {
            if (pos >= len || src[pos] != '{') return
            pos++
            var depth = 1
            while (pos < len && depth > 0) {
                if (startsWithCommentHere()) {
                    skipComment()
                    continue
                }
                when (src[pos]) {
                    '{' -> { depth++; pos++ }
                    '}' -> { depth--; pos++ }
                    else -> pos++
                }
            }
        }

        private fun startsWithCommentHere(): Boolean = pos + 1 < len && src[pos] == '/' && src[pos + 1] == '*'

        private fun skipComment() {
            pos += 2
            while (pos + 1 < len) {
                if (src[pos] == '*' && src[pos + 1] == '/') {
                    pos += 2
                    return
                }
                pos++
            }
            pos = len
        }

        private fun skipTrivia() {
            while (pos < len && src[pos].isWhitespace()) pos++
        }

        private fun splitTopLevelByComma(raw: String): List<String> {
            val out = mutableListOf<String>()
            val sb = StringBuilder()
            var paren = 0
            for (c in raw) {
                when {
                    c == '(' -> { paren++; sb.append(c) }
                    c == ')' -> { if (paren > 0) paren--; sb.append(c) }
                    c == ',' && paren == 0 -> {
                        out.add(sb.toString())
                        sb.clear()
                    }
                    else -> sb.append(c)
                }
            }
            if (sb.isNotEmpty()) out.add(sb.toString())
            return out
        }
    }

    private fun mergeTokenStyle(
        existing: TokenStyleData?,
        incoming: TokenStyleData,
    ): TokenStyleData {
        if (existing == null) return incoming
        return TokenStyleData(
            color = incoming.color ?: existing.color,
            fontWeight = incoming.fontWeight ?: existing.fontWeight,
            fontStyle = incoming.fontStyle ?: existing.fontStyle,
            background = incoming.background ?: existing.background,
            isUnderline = incoming.isUnderline || existing.isUnderline
        )
    }

    private fun parseDeclarations(declarations: String): TokenStyleData? {
        var color: String? = null
        var fontWeight: TokenFontWeight? = null
        var fontStyle: TokenFontStyle? = null
        var background: String? = null

        PROP_PATTERN.findAll(declarations).forEach { match ->
            val prop = match.groupValues[1].trim()
            val rawValue = match.groupValues[2].trim()
            val value = if (rawValue.endsWith("!important", ignoreCase = true)) {
                rawValue.substring(0, rawValue.length - "!important".length).trim()
            } else {
                rawValue
            }
            when (prop) {
                "color" -> color = parseColor(value)
                "background", "background-color" -> background = parseColor(value)
                "font-weight" -> {
                    val numericWeight = value.toIntOrNull()
                    when {
                        value == "bold" || value == "bolder" || (numericWeight != null && numericWeight >= 600) -> {
                            fontWeight = TokenFontWeight.BOLD
                        }
                        value == "normal" || value == "lighter" || (numericWeight != null && numericWeight < 600) -> {
                            fontWeight = TokenFontWeight.NORMAL
                        }
                    }
                }
                "font-style" -> {
                    if (value == "italic" || value == "oblique") fontStyle = TokenFontStyle.ITALIC
                }
            }
        }

        if (color == null && fontWeight == null && fontStyle == null && background == null) return null

        return TokenStyleData(
            color = color,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            background = background
        )
    }

    private val namedColors = mapOf(
        "black" to "#000000",
        "white" to "#ffffff",
        "red" to "#ff0000",
        "green" to "#008000",
        "blue" to "#0000ff",
        "yellow" to "#ffff00",
        "orange" to "#ffa500",
        "purple" to "#800080",
        "gray" to "#808080",
        "grey" to "#808080",
        "silver" to "#c0c0c0",
        "navy" to "#000080",
        "teal" to "#008080",
        "maroon" to "#800000",
        "olive" to "#808000",
        "lime" to "#00ff00",
        "aqua" to "#00ffff",
        "cyan" to "#00ffff",
        "fuchsia" to "#ff00ff",
        "magenta" to "#ff00ff",
        "gold" to "#ffd700",
    )

    private fun parseColor(value: String): String? {
        val trimmed = value.trim()
        return when {
            trimmed.startsWith("#") -> parseHexColor(trimmed)
            trimmed.startsWith("rgb") -> parseRgbColor(trimmed)
            else -> namedColors[trimmed.lowercase()]
        }
    }

    private fun parseHexColor(hex: String): String? {
        val cleaned = hex.trimStart('#')
        return when (cleaned.length) {
            3 -> {
                val r = cleaned[0].toString().repeat(2)
                val g = cleaned[1].toString().repeat(2)
                val b = cleaned[2].toString().repeat(2)
                "#ff$r$g$b"
            }
            4 -> {
                val r = cleaned[0].toString().repeat(2)
                val g = cleaned[1].toString().repeat(2)
                val b = cleaned[2].toString().repeat(2)
                val a = cleaned[3].toString().repeat(2)
                "#$a$r$g$b"
            }
            6 -> "#ff$cleaned"
            8 -> {
                val rr = cleaned.substring(0, 2)
                val gg = cleaned.substring(2, 4)
                val bb = cleaned.substring(4, 6)
                val aa = cleaned.substring(6, 8)
                "#$aa$rr$gg$bb"
            }
            else -> null
        }
    }

    private fun parseRgbColor(value: String): String? {
        val inner = value.substringAfter("(").substringBefore(")").trim()
        return if (inner.contains(",")) {
            parseCommaSeparatedRgb(inner)
        } else {
            parseSpaceSeparatedRgb(inner)
        }
    }

    private fun parseCommaSeparatedRgb(inner: String): String? {
        val parts = inner.split(",").map { it.trim() }
        return when (parts.size) {
            3 -> colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = null)
            4 -> colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = parts[3])
            else -> null
        }
    }

    private fun parseSpaceSeparatedRgb(inner: String): String? {
        return if (inner.contains("/")) {
            val slashIdx = inner.indexOf("/")
            val colorPart = inner.substring(0, slashIdx).trim()
            val alphaPart = inner.substring(slashIdx + 1).trim()
            val parts = colorPart.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
            if (parts.size != 3) return null
            colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = alphaPart)
        } else {
            val parts = inner.split(WHITESPACE_REGEX).filter { it.isNotEmpty() }
            if (parts.size != 3) return null
            colorFromRgbStrings(parts[0], parts[1], parts[2], alpha = null)
        }
    }

    private fun colorFromRgbStrings(
        r: String,
        g: String,
        b: String,
        alpha: String?,
    ): String? {
        val red = parseRgbComponent(r) ?: return null
        val green = parseRgbComponent(g) ?: return null
        val blue = parseRgbComponent(b) ?: return null
        val a = if (alpha != null) parseAlphaComponent(alpha) ?: return null else 255
        return "#${a.toString(16).padStart(2, '0')}${red.toString(16).padStart(2, '0')}${green.toString(16).padStart(2, '0')}${blue.toString(16).padStart(2, '0')}"
    }

    private fun parseRgbComponent(value: String): Int? {
        if (value.endsWith("%")) {
            val pct = value.dropLast(1).toFloatOrNull() ?: return null
            return (pct / 100f * 255).toInt().coerceIn(0, 255)
        }
        return value.toIntOrNull()?.coerceIn(0, 255)
    }

    private fun parseAlphaComponent(value: String): Int? {
        if (value.endsWith("%")) {
            val pct = value.dropLast(1).toFloatOrNull() ?: return null
            if (!pct.isFinite()) return null
            return (pct / 100f * 255).toInt().coerceIn(0, 255)
        }
        val f = value.toFloatOrNull() ?: return null
        if (!f.isFinite()) return null
        return if (f <= 1.0f) {
            (f * 255).roundToInt().coerceIn(0, 255)
        } else {
            value.toIntOrNull()?.coerceIn(0, 255)
        }
    }
}
