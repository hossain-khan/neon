package dev.hossain.neon.engine.highlightjs.internal

internal fun extractInlineScripts(htmlContent: String): List<String> {
    val scripts = mutableListOf<String>()
    var start = 0
    while (true) {
        val openTag = htmlContent.indexOf("<script>", start)
        if (openTag == -1) break
        val closeTag = htmlContent.indexOf("</script>", openTag)
        if (closeTag == -1) break
        scripts += htmlContent.substring(openTag + SCRIPT_OPEN_TAG.length, closeTag)
        start = closeTag + SCRIPT_CLOSE_TAG.length
    }
    return scripts
}

private const val SCRIPT_OPEN_TAG = "<script>"
private const val SCRIPT_CLOSE_TAG = "</script>"
