package dev.hossain.neon.engine.highlightjs.internal

import dev.hossain.neon.core.HighlightToken
import dev.hossain.neon.core.TokenFontStyle
import dev.hossain.neon.core.TokenFontWeight

internal object HtmlParser {
    fun parse(html: String, colorMap: Map<String, TokenStyleData>): List<HighlightToken> {
        val tokens = mutableListOf<HighlightToken>()
        var index = 0
        val length = html.length

        val styleStack = mutableListOf<TokenStyleData>()

        fun resolveCurrentStyle(): TokenStyleData {
            var color: String? = null
            var background: String? = null
            var fontWeight: TokenFontWeight? = null
            var fontStyle: TokenFontStyle? = null
            var isUnderline = false
            for (style in styleStack) {
                if (style.color != null) color = style.color
                if (style.background != null) background = style.background
                if (style.fontWeight != null) fontWeight = style.fontWeight
                if (style.fontStyle != null) fontStyle = style.fontStyle
                if (style.isUnderline) isUnderline = true
            }
            return TokenStyleData(color, background, fontWeight, fontStyle, isUnderline)
        }

        fun appendText(text: String) {
            if (text.isEmpty()) return
            val currentStyle = resolveCurrentStyle()
            
            var start = 0
            while (start < text.length) {
                val nextNewline = text.indexOf('\n', start)
                if (nextNewline == -1) {
                    val segment = text.substring(start)
                    tokens.add(
                        HighlightToken(
                            text = segment,
                            color = currentStyle.color,
                            background = currentStyle.background,
                            fontWeight = currentStyle.fontWeight,
                            fontStyle = currentStyle.fontStyle,
                            isUnderline = currentStyle.isUnderline
                        )
                    )
                    break
                }
                
                if (nextNewline > start) {
                    val segment = text.substring(start, nextNewline)
                    tokens.add(
                        HighlightToken(
                            text = segment,
                            color = currentStyle.color,
                            background = currentStyle.background,
                            fontWeight = currentStyle.fontWeight,
                            fontStyle = currentStyle.fontStyle,
                            isUnderline = currentStyle.isUnderline
                        )
                    )
                }
                tokens.add(HighlightToken(text = "\n"))
                start = nextNewline + 1
            }
        }

        fun walkNodes(parentTag: String?) {
            while (index < length) {
                val c = html[index]
                if (c == '<') {
                    if (html.startsWith("<!--", index)) {
                        val endComment = html.indexOf("-->", index + 4)
                        index = if (endComment != -1) endComment + 3 else length
                        continue
                    }

                    if (index + 1 < length && html[index + 1] == '/') {
                        val tagEnd = html.indexOf('>', index + 2)
                        if (tagEnd == -1) {
                            index = length
                            break
                        }
                        if (parentTag != null && regionMatchesTrimmedLowercase(html, index + 2, tagEnd, parentTag)) {
                            index = tagEnd + 1
                            break
                        }
                        if (parentTag == null) {
                            index = tagEnd + 1
                            continue
                        }
                        break
                    }

                    val tagEnd = findTagEnd(html, index + 1, length)
                    if (tagEnd == -1) {
                        val text = html.substring(index, length)
                        appendText(decodeEntities(text))
                        index = length
                        break
                    }

                    val contentStart = skipWhitespace(html, index + 1, tagEnd)
                    var contentEnd = tagEnd
                    val isSelfClosing = contentEnd > contentStart && html[contentEnd - 1] == '/'
                    if (isSelfClosing) {
                        contentEnd = skipWhitespaceReverse(html, contentStart, contentEnd - 1)
                    }

                    val firstSpace = indexOfWhitespace(html, contentStart, contentEnd)
                    val tagNameEnd = if (firstSpace != -1) firstSpace else contentEnd

                    val tagName = lowercaseSubstring(html, contentStart, tagNameEnd)

                    var style: TokenStyleData? = null
                    if (tagName == "span" && firstSpace != -1) {
                        val className = extractClassAttrInPlace(html, firstSpace + 1, contentEnd)
                        if (className.isNotEmpty()) {
                            style = resolveStyle(className, colorMap)
                        }
                    }

                    index = tagEnd + 1

                    if (!isSelfClosing) {
                        if (style != null) styleStack.add(style)
                        walkNodes(tagName)
                        if (style != null) styleStack.removeAt(styleStack.size - 1)
                    }
                } else {
                    val nextTag = html.indexOf('<', index)
                    val textEnd = if (nextTag != -1) nextTag else length
                    if (textEnd > index) {
                        val text = html.substring(index, textEnd)
                        appendText(decodeEntities(text))
                    }
                    index = textEnd
                }
            }
        }

        walkNodes(null)
        return tokens
    }

    private fun resolveStyle(className: String, colorMap: Map<String, TokenStyleData>): TokenStyleData {
        val classes = className.trim().split(Regex("\\s+"))
        var merged = TokenStyleData()
        for (cls in classes) {
            val style = colorMap[cls]
            if (style != null) {
                merged = TokenStyleData(
                    color = style.color ?: merged.color,
                    background = style.background ?: merged.background,
                    fontWeight = style.fontWeight ?: merged.fontWeight,
                    fontStyle = style.fontStyle ?: merged.fontStyle,
                    isUnderline = style.isUnderline || merged.isUnderline
                )
            }
        }
        return merged
    }

    private fun findTagEnd(html: String, start: Int, length: Int): Int {
        var i = start
        while (i < length) {
            val c = html[i]
            if (c == '>') return i
            if (c == '"' || c == '\'') {
                val close = html.indexOf(c, i + 1)
                if (close == -1) return -1
                i = close + 1
                continue
            }
            i++
        }
        return -1
    }

    private fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        val sb = StringBuilder(text.length)
        var i = 0
        val len = text.length
        while (i < len) {
            val c = text[i]
            if (c == '&') {
                val semi = text.indexOf(';', i)
                if (semi != -1 && semi - i < 10) {
                    val decoded = decodeEntityInline(text, i + 1, semi)
                    if (decoded != null) {
                        sb.append(decoded)
                        i = semi + 1
                        continue
                    }
                    if (text[i + 1] == '#') {
                        val code = parseNumericEntity(text, i + 2, semi)
                        if (code != null && code in 0..0x10FFFF && code !in 0xD800..0xDFFF) {
                            if (code < 0x10000) {
                                sb.append(code.toChar())
                            } else {
                                val high = (((code - 0x10000) ushr 10) + 0xD800).toChar()
                                val low = (((code - 0x10000) and 0x3FF) + 0xDC00).toChar()
                                sb.append(high)
                                sb.append(low)
                            }
                            i = semi + 1
                            continue
                        }
                    }
                    sb.append(text, i, semi + 1)
                    i = semi + 1
                } else {
                    sb.append('&')
                    i++
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun decodeEntityInline(text: String, start: Int, end: Int): String? {
        val len = end - start
        return when (len) {
            2 -> {
                when {
                    text[start] == 'l' && text[start + 1] == 't' -> "<"
                    text[start] == 'g' && text[start + 1] == 't' -> ">"
                    else -> null
                }
            }
            3 -> {
                when {
                    text[start] == 'a' && text[start + 1] == 'm' && text[start + 2] == 'p' -> "&"
                    else -> null
                }
            }
            4 -> {
                when {
                    text[start] == 'q' && regionEquals(text, start, "quot") -> "\""
                    text[start] == 'a' && regionEquals(text, start, "apos") -> "'"
                    text[start] == 'n' && regionEquals(text, start, "nbsp") -> "\u00A0"
                    else -> null
                }
            }
            else -> null
        }
    }

    private fun regionEquals(text: String, start: Int, expected: String): Boolean {
        for (j in expected.indices) {
            if (text[start + j] != expected[j]) return false
        }
        return true
    }

    private fun parseNumericEntity(text: String, start: Int, semi: Int): Int? =
        if (start < semi && text[start] == 'x') {
            parseHexInt(text, start + 1, semi)
        } else {
            parseDecInt(text, start, semi)
        }

    private fun parseHexInt(text: String, start: Int, end: Int): Int? {
        if (start >= end) return null
        var result = 0
        for (i in start until end) {
            val c = text[i]
            val digit = when (c) {
                in '0'..'9' -> c - '0'
                in 'a'..'f' -> c - 'a' + 10
                in 'A'..'F' -> c - 'A' + 10
                else -> return null
            }
            result = result * 16 + digit
            if (result > 0x10FFFF) return null
        }
        return result
    }

    private fun parseDecInt(text: String, start: Int, end: Int): Int? {
        if (start >= end) return null
        var result = 0
        for (i in start until end) {
            val c = text[i]
            if (c !in '0'..'9') return null
            result = result * 10 + (c - '0')
            if (result > 0x10FFFF) return null
        }
        return result
    }

    private fun extractClassAttrInPlace(html: String, start: Int, end: Int): String {
        var i = start
        while (i < end) {
            while (i < end && html[i].isWhitespace()) i++
            if (i >= end) break

            val eq = html.indexOf('=', i)
            if (eq == -1 || eq >= end) break

            val isClassAttr = isLastTokenClass(html, i, eq)
            i = eq + 1

            while (i < end && html[i].isWhitespace()) i++
            if (i >= end) break

            val quote = html[i]
            if (quote == '"' || quote == '\'') {
                i++
                val endQuote = html.indexOf(quote, i)
                if (endQuote != -1 && endQuote <= end) {
                    if (isClassAttr) return html.substring(i, endQuote)
                    i = endQuote + 1
                } else {
                    if (isClassAttr) return html.substring(i, end)
                    i = end
                }
            } else {
                val valueStart = i
                while (i < end && !html[i].isWhitespace()) i++
                if (isClassAttr) return html.substring(valueStart, i)
            }
        }
        return ""
    }

    private fun isLastTokenClass(html: String, start: Int, end: Int): Boolean {
        var tokenStart = end - 1
        while (tokenStart >= start && html[tokenStart].isWhitespace()) tokenStart--
        if (tokenStart < start) return false
        val tokenEnd = tokenStart + 1
        while (tokenStart > start && !html[tokenStart - 1].isWhitespace()) tokenStart--
        val tokenLen = tokenEnd - tokenStart
        if (tokenLen != 5) return false
        return (html[tokenStart] == 'c' || html[tokenStart] == 'C') &&
            (html[tokenStart + 1] == 'l' || html[tokenStart + 1] == 'L') &&
            (html[tokenStart + 2] == 'a' || html[tokenStart + 2] == 'A') &&
            (html[tokenStart + 3] == 's' || html[tokenStart + 3] == 'S') &&
            (html[tokenStart + 4] == 's' || html[tokenStart + 4] == 'S')
    }

    private fun regionMatchesTrimmedLowercase(html: String, start: Int, end: Int, expected: String): Boolean {
        var s = start
        var e = end
        while (s < e && html[s].isWhitespace()) s++
        while (e > s && html[e - 1].isWhitespace()) e--
        val len = e - s
        if (len != expected.length) return false
        for (j in 0 until len) {
            if (html[s + j].lowercaseChar() != expected[j]) return false
        }
        return true
    }

    private fun skipWhitespace(html: String, start: Int, end: Int): Int {
        var i = start
        while (i < end && html[i].isWhitespace()) i++
        return i
    }

    private fun skipWhitespaceReverse(html: String, start: Int, end: Int): Int {
        var i = end
        while (i > start && html[i - 1].isWhitespace()) i--
        return i
    }

    private fun indexOfWhitespace(html: String, start: Int, end: Int): Int {
        for (i in start until end) {
            val c = html[i]
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') return i
        }
        return -1
    }

    private fun lowercaseSubstring(html: String, start: Int, end: Int): String {
        var needsLowercase = false
        for (i in start until end) {
            if (html[i] in 'A'..'Z') {
                needsLowercase = true
                break
            }
        }
        val sub = html.substring(start, end)
        return if (needsLowercase) sub.lowercase() else sub
    }
}
