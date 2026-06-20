package dev.hossain.neon.engine.highlightjs

import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.core.HljsSelectors
import dev.hossain.neon.engine.highlightjs.internal.ThemeParser
import dev.hossain.neon.engine.highlightjs.internal.TokenStyleData
import neonproject.engine_highlightjs.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

public class HljsTheme internal constructor(
    override val name: String,
    override val isDark: Boolean,
    internal val colorMap: Map<String, TokenStyleData>
) : HighlightTheme {

    override val backgroundColorHex: String? = colorMap[HljsSelectors.BASE]?.background
    override val defaultTextColorHex: String? = colorMap[HljsSelectors.BASE]?.color

    public companion object {
        public fun fromCss(name: String, isDark: Boolean, css: String): HljsTheme {
            val colorMap = ThemeParser.parse(css)
            return HljsTheme(name, isDark, colorMap)
        }

        @OptIn(ExperimentalResourceApi::class)
        public suspend fun builtin(theme: BuiltinHljsTheme): HljsTheme {
            val bytes = Res.readBytes(theme.resourcePath)
            val css = bytes.decodeToString()
            return fromCss(theme.themeName, theme.isDark, css)
        }
    }
}

public enum class BuiltinHljsTheme(
    public val themeName: String,
    public val isDark: Boolean,
    internal val resourcePath: String
) {
    ATOM_ONE_DARK("Atom One Dark", true, "files/themes/atom-one-dark.css"),
    ATOM_ONE_LIGHT("Atom One Light", false, "files/themes/atom-one-light.css"),
    TOMORROW_NIGHT("Tomorrow Night", true, "files/themes/tomorrow-night.css"),
    TOMORROW("Tomorrow", false, "files/themes/tomorrow.css");
}
