package dev.hossain.neon.core.internal

public fun unescapeJsString(jsonString: String): String {
    val inner = if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
        jsonString.substring(1, jsonString.length - 1)
    } else {
        jsonString
    }
    val sb = StringBuilder(inner.length)
    var i = 0
    while (i < inner.length) {
        val c = inner[i]
        if (c == '\\' && i + 1 < inner.length) {
            when (inner[i + 1]) {
                '"' -> { sb.append('"'); i += 2 }
                '\\' -> { sb.append('\\'); i += 2 }
                '/' -> { sb.append('/'); i += 2 }
                'b' -> { sb.append('\b'); i += 2 }
                'f' -> { sb.append('\u000C'); i += 2 }
                'n' -> { sb.append('\n'); i += 2 }
                'r' -> { sb.append('\r'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                'u' -> {
                    if (i + 5 < inner.length) {
                        val hex = inner.substring(i + 2, i + 6)
                        val codePoint = hex.toIntOrNull(16)
                        if (codePoint != null) {
                            if (codePoint in 0xD800..0xDBFF &&
                                i + 11 < inner.length &&
                                inner[i + 6] == '\\' &&
                                inner[i + 7] == 'u'
                            ) {
                                val lowHex = inner.substring(i + 8, i + 12)
                                val lowSurrogate = lowHex.toIntOrNull(16)
                                if (lowSurrogate != null && lowSurrogate in 0xDC00..0xDFFF) {
                                    val supplementary = (((codePoint - 0xD800) shl 10) or (lowSurrogate - 0xDC00)) + 0x10000
                                    val high = (((supplementary - 0x10000) ushr 10) + 0xD800).toChar()
                                    val low = (((supplementary - 0x10000) and 0x3FF) + 0xDC00).toChar()
                                    sb.append(high)
                                    sb.append(low)
                                    i += 12
                                } else {
                                    sb.append(codePoint.toChar())
                                    i += 6
                                }
                            } else {
                                sb.append(codePoint.toChar())
                                i += 6
                            }
                        } else {
                            sb.append(c)
                            i++
                        }
                    } else {
                        sb.append(c)
                        i++
                    }
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}

public fun escapeForJs(str: String): String {
    val sb = StringBuilder(str.length + 8)
    for (c in str) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '\'' -> sb.append("\\'")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\u2028' -> sb.append("\\u2028")
            '\u2029' -> sb.append("\\u2029")
            else -> {
                val code = c.code
                if (code in 0x00..0x1F) {
                    sb.append("\\u").append(code.toString(16).padStart(4, '0'))
                } else {
                    sb.append(c)
                }
            }
        }
    }
    return sb.toString()
}
