package dev.hossain.neon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.neon.core.HighlightTimings
import dev.hossain.neon.core.HighlightTheme
import dev.hossain.neon.ui.HighlightEngineProvider
import dev.hossain.neon.ui.SyntaxHighlightedCode
import dev.hossain.neon.ui.rememberHighlightEngine
import dev.hossain.neon.engine.highlightjs.HljsEngineFactory
import dev.hossain.neon.engine.highlightjs.HljsConfig
import dev.hossain.neon.engine.highlightjs.HljsTheme
import dev.hossain.neon.engine.highlightjs.BuiltinHljsTheme
import dev.hossain.neon.engine.shiki.ShikiNetworkEngineFactory
import dev.hossain.neon.engine.shiki.ShikiNetworkConfig
import dev.hossain.neon.engine.shiki.ShikiTheme

private val PredefinedSamples = mapOf(
    "kotlin" to """
    package dev.hossain.neon.sample

    import kotlinx.coroutines.*

    data class Task(val id: Int, val name: String, val durationMs: Long)

    fun main() = runBlocking {
        val tasks = listOf(
            Task(1, "Init Engine", 1500L),
            Task(2, "Fetch Tokens", 2500L),
            Task(3, "Render Code", 800L)
        )

        println("Starting tasks concurrently...")
        val jobs = tasks.map { task ->
            async {
                delay(task.durationMs)
                println("Completed task: ${'$'}{task.name}")
            }
        }
        jobs.awaitAll()
        println("All tasks finished successfully!")
    }
    """.trimIndent(),

    "javascript" to """
    // JavaScript asynchronous task executor
    const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms));

    const tasks = [
        { id: 1, name: "Init Engine", durationMs: 1500 },
        { id: 2, name: "Fetch Tokens", durationMs: 2500 },
        { id: 3, name: "Render Code", durationMs: 800 }
    ];

    async function runTasks() {
        console.log("Starting tasks concurrently...");
        const promises = tasks.map(async (task) => {
            await delay(task.durationMs);
            console.log(`Completed task: ${'$'}{task.name}`);
        });
        await Promise.all(promises);
        console.log("All tasks finished successfully!");
    }

    runTasks();
    """.trimIndent(),

    "python" to """
    # Python asyncio concurrent task executor
    import asyncio

    async def execute_task(name, duration):
        await asyncio.sleep(duration)
        print(f"Completed task: {name}")

    async def main():
        tasks = [
            execute_task("Init Engine", 1.5),
            execute_task("Fetch Tokens", 2.5),
            execute_task("Render Code", 0.8)
        ]
        print("Starting tasks concurrently...")
        await asyncio.gather(*tasks)
        print("All tasks finished successfully!")

    if __name__ == "__main__":
        asyncio.run(main())
    """.trimIndent(),

    "html" to """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Neon Syntax Highlighter</title>
        <style>
            body {
                background-color: #0f172a;
                color: #f1f5f9;
                font-family: sans-serif;
            }
        </style>
    </head>
    <body>
        <h1>Welcome to Neon</h1>
        <p>A modular Kotlin Multiplatform syntax highlighting library.</p>
    </body>
    </html>
    """.trimIndent()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        val hljsEngine = rememberHighlightEngine(HljsEngineFactory, HljsConfig.Default)
        val shikiEngine = rememberHighlightEngine(ShikiNetworkEngineFactory, ShikiNetworkConfig.Default)

        var selectedEngineName by remember { mutableStateOf("highlightjs") }
        val currentEngine = if (selectedEngineName == "highlightjs") hljsEngine else shikiEngine

        var selectedLanguage by remember { mutableStateOf("kotlin") }
        var code by remember { mutableStateOf(PredefinedSamples["kotlin"] ?: "") }
        var showLineNumbers by remember { mutableStateOf(true) }

        val highlightjsThemes = listOf("Atom One Dark", "Atom One Light", "Tomorrow Night", "Tomorrow")
        val shikiThemes = listOf("github-dark", "github-light", "one-dark-pro", "dracula", "min-light")

        var selectedThemeName by remember { mutableStateOf("Atom One Dark") }

        // Adjust theme name selection when engine changes
        LaunchedEffect(selectedEngineName) {
            selectedThemeName = if (selectedEngineName == "highlightjs") "Atom One Dark" else "github-dark"
        }

        var currentTheme: HighlightTheme? by remember { mutableStateOf(null) }
        var isThemeLoading by remember { mutableStateOf(false) }

        LaunchedEffect(selectedEngineName, selectedThemeName) {
            isThemeLoading = true
            currentTheme = if (selectedEngineName == "highlightjs") {
                val builtinEnum = when (selectedThemeName) {
                    "Atom One Dark" -> BuiltinHljsTheme.ATOM_ONE_DARK
                    "Atom One Light" -> BuiltinHljsTheme.ATOM_ONE_LIGHT
                    "Tomorrow Night" -> BuiltinHljsTheme.TOMORROW_NIGHT
                    "Tomorrow" -> BuiltinHljsTheme.TOMORROW
                    else -> BuiltinHljsTheme.ATOM_ONE_DARK
                }
                HljsTheme.builtin(builtinEnum)
            } else {
                ShikiTheme.builtin(selectedThemeName)
            }
            isThemeLoading = false
        }

        var timings by remember { mutableStateOf<HighlightTimings?>(null) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Neon Syntax Highlighter Showcase",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val useRow = maxWidth > 800.dp
                if (useRow) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left control panel
                        Surface(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                ControlPanelContent(
                                    selectedEngineName = selectedEngineName,
                                    onEngineSelected = { selectedEngineName = it },
                                    selectedThemeName = selectedThemeName,
                                    onThemeSelected = { selectedThemeName = it },
                                    highlightjsThemes = highlightjsThemes,
                                    shikiThemes = shikiThemes,
                                    selectedLanguage = selectedLanguage,
                                    onLanguageSelected = { lang ->
                                        selectedLanguage = lang
                                        code = PredefinedSamples[lang] ?: ""
                                    },
                                    showLineNumbers = showLineNumbers,
                                    onShowLineNumbersToggled = { showLineNumbers = it },
                                    timings = timings,
                                    errorMsg = errorMsg
                                )
                            }
                        }

                        // Right Editor & Preview
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            EditorSection(
                                code = code,
                                onCodeChange = { code = it }
                            )

                            PreviewSection(
                                code = code,
                                selectedLanguage = selectedLanguage,
                                currentEngine = currentEngine,
                                currentTheme = currentTheme,
                                isThemeLoading = isThemeLoading,
                                showLineNumbers = showLineNumbers,
                                onHighlightComplete = { result ->
                                    timings = result.timings
                                    errorMsg = null
                                },
                                onError = { ex ->
                                    errorMsg = ex.message ?: "Unknown syntax highlighting error"
                                }
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                ControlPanelContent(
                                    selectedEngineName = selectedEngineName,
                                    onEngineSelected = { selectedEngineName = it },
                                    selectedThemeName = selectedThemeName,
                                    onThemeSelected = { selectedThemeName = it },
                                    highlightjsThemes = highlightjsThemes,
                                    shikiThemes = shikiThemes,
                                    selectedLanguage = selectedLanguage,
                                    onLanguageSelected = { lang ->
                                        selectedLanguage = lang
                                        code = PredefinedSamples[lang] ?: ""
                                    },
                                    showLineNumbers = showLineNumbers,
                                    onShowLineNumbersToggled = { showLineNumbers = it },
                                    timings = timings,
                                    errorMsg = errorMsg
                                )
                            }
                        }

                        EditorSection(
                            code = code,
                            onCodeChange = { code = it }
                        )

                        PreviewSection(
                            code = code,
                            selectedLanguage = selectedLanguage,
                            currentEngine = currentEngine,
                            currentTheme = currentTheme,
                            isThemeLoading = isThemeLoading,
                            showLineNumbers = showLineNumbers,
                            onHighlightComplete = { result ->
                                timings = result.timings
                                errorMsg = null
                            },
                            onError = { ex ->
                                errorMsg = ex.message ?: "Unknown syntax highlighting error"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPanelContent(
    selectedEngineName: String,
    onEngineSelected: (String) -> Unit,
    selectedThemeName: String,
    onThemeSelected: (String) -> Unit,
    highlightjsThemes: List<String>,
    shikiThemes: List<String>,
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    showLineNumbers: Boolean,
    onShowLineNumbersToggled: (Boolean) -> Unit,
    timings: HighlightTimings?,
    errorMsg: String?
) {
    Text("Configuration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

    Spacer(modifier = Modifier.height(4.dp))

    Text("Highlight Engine", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val engines = listOf("highlightjs" to "Highlight.js", "shiki-network" to "Shiki Net")
        engines.forEach { (id, label) ->
            FilterChip(
                selected = selectedEngineName == id,
                onClick = { onEngineSelected(id) },
                label = { Text(label) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Text("Predefined Snippet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val languages = listOf("kotlin", "javascript", "python", "html")
        languages.forEach { lang ->
            FilterChip(
                selected = selectedLanguage == lang,
                onClick = { onLanguageSelected(lang) },
                label = { Text(lang.replaceFirstChar { it.uppercase() }) },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Text("Theme Selection", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val themes = if (selectedEngineName == "highlightjs") highlightjsThemes else shikiThemes
        themes.forEach { theme ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        if (selectedThemeName == theme) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .background(
                        if (selectedThemeName == theme) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                RadioButton(
                    selected = selectedThemeName == theme,
                    onClick = { onThemeSelected(theme) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(theme, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = showLineNumbers,
            onCheckedChange = onShowLineNumbersToggled
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Show Line Gutters", style = MaterialTheme.typography.bodyMedium)
    }

    Divider()

    Text("Performance Metric", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

    if (errorMsg != null) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⚠️", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Text(errorMsg, style = MaterialTheme.typography.bodySmall)
            }
        }
    } else if (timings != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricRow(
                label = "JS Bridge / HTTP Net",
                value = "${timings.jsBridge.inWholeMicroseconds / 1000.0} ms"
            )
            MetricRow(
                label = "JSON Escape/Unescape",
                value = "${timings.jsonUnescape.inWholeMicroseconds / 1000.0} ms"
            )
            MetricRow(
                label = "HTML / Token Parse",
                value = "${timings.htmlParse.inWholeMicroseconds / 1000.0} ms"
            )
            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            MetricRow(
                label = "Total Processing Time",
                value = "${timings.total.inWholeMicroseconds / 1000.0} ms",
                isBold = true
            )
        }
    } else {
        Text(
            "Modify the code or change the settings to update timings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditorSection(
    code: String,
    onCodeChange: (String) -> Unit
) {
    Text(
        "Source Code Editor",
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ColumnScope.PreviewSection(
    code: String,
    selectedLanguage: String,
    currentEngine: dev.hossain.neon.core.HighlightEngine,
    currentTheme: HighlightTheme?,
    isThemeLoading: Boolean,
    showLineNumbers: Boolean,
    onHighlightComplete: (dev.hossain.neon.core.HighlightResult) -> Unit,
    onError: (dev.hossain.neon.core.HighlightException) -> Unit
) {
    Text(
        "Syntax Colored Output",
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 4.dp)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isThemeLoading || currentTheme == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                HighlightEngineProvider(engine = currentEngine) {
                    SyntaxHighlightedCode(
                        code = code,
                        language = selectedLanguage,
                        theme = currentTheme,
                        modifier = Modifier.fillMaxSize(),
                        showLineNumbers = showLineNumbers,
                        onHighlightComplete = onHighlightComplete,
                        onError = onError,
                        placeholder = {
                            Text(
                                text = it,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}