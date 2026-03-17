# VibeCode Contribution Guidelines

Thank you for your interest in contributing to VibeCode! Whether you're fixing a bug, adding an agent tool, or improving the LLM integration, your help makes VibeCode a more powerful on-device agent.

## How to Add a New AgentTool

Adding a new tool allows the LLM to interact with the device in new ways.

1. **Create the Tool Class:**
   Navigate to `app/src/main/kotlin/com/mahavtaar/vibecoder/agent/tools/` and define a new class implementing `AgentTool` (or inherit from a base like `BaseFileTool`).

2. **Define the JSON Schema:**
   The `parameters` property must be a strictly valid JSON Schema object.
   ```kotlin
   class MyNewTool : AgentTool {
       override val name = "my_new_tool"
       override val description = "Does something cool"
       override val parameters = buildJsonObject {
           put("type", "object")
           putJsonObject("properties") {
               putJsonObject("arg1") { put("type", "string") }
           }
           put("required", JsonArray(listOf(JsonPrimitive("arg1"))))
       }

       override suspend fun execute(args: JsonObject): ToolResult {
           // Implement your logic here
           return ToolResult.Success("Done!")
       }
   }
   ```

3. **Register the Tool in Hilt:**
   Open `di/ToolsModule.kt`. Find the `provideAgentTools()` method annotated with `@ElementsIntoSet`. Add an instantiation of your new tool to the `setOf(...)`. The `ToolRegistry` will automatically pick it up and expose it to the `AgentOrchestrator`.

## How to Add a New LLM Engine

To support a new backend (e.g., MLC-LLM, Gemini Nano, etc.):

1. **Implement `LlamaEngine`:**
   Create a new class in `llm/` implementing the `LlamaEngine` interface.
   ```kotlin
   class MyNewEngine : LlamaEngine {
       override var isLoaded: Boolean = false
       // Implement loadModel, generate (returning Flow<String>), stop, unloadModel
   }
   ```

2. **Update the Engine Switcher:**
   Modify `di/LlmModule.kt` where `provideLlamaEngine` is defined. Add your engine to the `when (engineType)` block, pulling the preference from DataStore. Add the required UI dropdown option in `SettingsScreen.kt`.

## Code Style Guide

- **Kotlin First:** We strictly use Kotlin.
- **Jetpack Compose:** No XML layouts (except for standard Android assets like the Manifest or specific resources).
- **Coroutines:** All background work must be non-blocking. Use `Dispatchers.IO` for file/network operations and `Dispatchers.Main` for `WebView` operations.
- **JNI Thread Safety:** Native calls to `llama.cpp` must run sequentially on the dedicated `@Named("inferenceDispatcher")`.
- **UI Colors:** Stick to the dark theme palette defined in `Color.kt` (Background `#0D1117`, Accent `#58A6FF`, etc.).

## PR Checklist

Before submitting a pull request, ensure you have:

- [ ] Run `./gradlew lint` and resolved all errors.
- [ ] Run `./gradlew test` (if applicable tests exist).
- [ ] Run `./gradlew assembleDebug` to ensure the project compiles.
- [ ] If modifying JNI, ensure `CMakeLists.txt` builds cleanly via NDK 26.
- [ ] Avoided any hardcoded `/sdcard` paths—always use `Context.getExternalFilesDir()` to preserve Android 11+ scoped storage compliance.
- [ ] Added your new Tool or Engine to the `ARCHITECTURE.md` documentation if appropriate.
