# VibeCode Architecture Overview

VibeCode follows a modular, heavily decoupled architecture utilizing **Clean Architecture**, **MVVM (Model-View-ViewModel)**, and **Dependency Injection via Hilt**. It spans an Android presentation layer built with Jetpack Compose, an orchestration layer to loop ReAct agents, and a native JNI backend wrapping `llama.cpp`.

## System Architecture Diagram

```text
┌─────────────────────────────────────────────────────────────┐
│                       VibeCode Application                  │
│                                                             │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐   │
│  │   MainScreen   │ │  Settings /    │ │   Workflows    │   │
│  │ (Split Pane)   │ │  Onboarding    │ │   Screen       │   │
│  └──────┬─────────┘ └──────┬─────────┘ └──────┬─────────┘   │
│         │                  │                  │             │
│  ┌──────▼────────┐  ┌──────▼────────┐  ┌──────▼────────┐    │
│  │EditorViewModel│  │SettingsViewMdl│  │ MainViewModel │    │
│  └──────┬────────┘  └──────┬────────┘  └──────┬────────┘    │
│         │                  │                  │             │
│  ┌──────▼────────┐  ┌──────▼────────┐  ┌──────▼────────┐    │
│  │FileTabManager │  │  AppSettings  │  │AgentViewModel │    │
│  └──────┬────────┘  └──────┬────────┘  └──────┬────────┘    │
│         │                  │                  │             │
│  ┌──────▼──────────────────▼──────────────────▼────────┐    │
│  │                   AgentOrchestrator                 │    │
│  │           (ReAct Loop, Event Generation)            │    │
│  └──────┬──────────────────┬──────────────────┬────────┘    │
│         │                  │                  │             │
│  ┌──────▼────────┐  ┌──────▼────────┐  ┌──────▼────────┐    │
│  │ Tool Registry │  │  AgentMemory  │  │  LlamaEngine  │    │
│  │ (File, Shell, │  │ (Room SQLite) │  │ (Ollama/JNI)  │    │
│  │  Web, Code)   │  └───────────────┘  └──────┬────────┘    │
│  └───────────────┘                            │             │
│                                        ┌──────▼────────┐    │
│                                        │  llama_jni.so │    │
│                                        └───────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Data Flows

### Agent Loop (ReAct)
1. **User Input** (`AgentChatPanel`)
2. `MainScreen` → `AgentViewModel.startTask()`
3. `AgentOrchestrator.runTask()`
4. Loop Begins: Build System Prompt + Current Context
5. `LlamaEngine.generate()` (Streams output from `OllamaEngine` or `LlamaCppEngine`)
6. `ReActParser` processes LLM output
7. Tool Invoked → `ToolRegistry.getTool()`
8. `[FileTool / ShellTool / BrowsingTool / CodeTool].execute()`
9. `AuditLogDao` records step
10. `AgentEvent` emitted → `AgentViewModel` → UI Update
11. Loop Repeats (until `FINAL_ANSWER`)

### Browsing Engine
1. `BrowsingTool.execute()`
2. `BrowsingAgent.navigate()`
3. Checks mode:
   - JS Enabled? → `AndroidWebViewBridge` (Injects `agent_bridge.js`, executes JS)
   - Headless? → `HttpBrowser` (Fetches via Ktor, parses via Ksoup)
4. UI displays `WebView` from `BrowsingService` (foreground persistent) via `WebViewProvider`.

### Monaco Code Editor
1. `FileTreePanel` selection
2. `EditorViewModel.openFile()`
3. `FileTabManager` reads file, updates `StateFlow`
4. `MonacoWebView` updates `WebView` via `AndroidView`
5. User edits → `EditorJsBridge` captures `onContentChanged()`
6. `InlineCompletion` builds FIM prompt → `LlamaEngine`
7. Ghost text injected via `window.showGhostText()`

### Xterm Terminal
1. `TerminalPanel` (UI)
2. `TerminalViewModel`
3. `TerminalSessionManager.createSession()`
4. `TerminalSession` starts `ProcessBuilder("/system/bin/sh")`
5. Coroutines pipe STDOUT/STDERR flow → `XtermWebView`
6. JS `window.writeToTerminal()` renders ANSI output

## Hilt Module Map
- **AppModule**: Base Android context, `WakeLockManager`.
- **LlmModule**: Provides `LlamaEngine` (delegating via DataStore), `ModelDownloader`, and the dedicated `newSingleThreadContext("llama-inference")` dispatcher.
- **ToolsModule**: Injects the `AgentDatabase`, `InMemoryHashMap`, `ToolRegistry`, and `@ElementsIntoSet` for all 20+ agent tools.
- **BrowserModule**: Provides `HttpBrowser`, `WebViewBridge`, `BrowsingAgent`, and the `WebViewProvider` singleton holding the `BrowsingService` instance.
- **TerminalModule**: Exposes `TerminalSessionManager`.

## Room Database Schema
- **`AgentSession`**: UUID (`sessionId`), task description, start/end timestamps, status (RUNNING/COMPLETED/FAILED).
- **`AuditLog`**: Primary key ID, foreign-key link to `sessionId`, timestamp, step number, event type (`THOUGHT`, `ACTION_STARTED`), tool name, JSON inputs, and string outputs.
- **`MemoryEntity`**: Key-value pairs simulating the persistent long-term storage of the agent.
