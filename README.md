# VibeCode — Agentic AI Coder on Android

VibeCode is an experimental, fully self-contained Android application designed to bring an **autonomous AI software engineer** directly to your mobile device or tablet. By running large language models locally on device via `llama.cpp` (or remotely via `Ollama`), VibeCode creates a persistent, sandboxed coding environment featuring an interactive agent loop, a full-fledged IDE editor, terminal, and automated browsing.

---

## 🌟 Features

*   🧠 **Local LLM Support:** Run `Qwen2.5-Coder`, `Phi-4-mini`, and others natively via `llama.cpp` using JNI.
*   🤖 **ReAct Agent Loop:** Autonomous code generation, refactoring, and debugging via a ReAct loop.
*   🌐 **Agentic Web Browsing:** Allows the AI agent to search Google/DDG, read docs, click, and extract web data via headless HTTP and an interactive JS `WebView`.
*   💻 **Monaco Code Editor:** Full IDE experience powered by `monaco-editor` inside an Android WebView, equipped with AI ghost-text completions.
*   ⌨️ **Integrated Terminal:** An interactive bash terminal rendered by `xterm.js` that runs real Android shell sessions.
*   💾 **Persistent Memory & Auditing:** Every agent thought and action is recorded into a local SQLite Room database.
*   🔔 **System Notifications:** Keep track of the agent's progress in the background and approve/reject dangerous actions.

---

## 📸 Screenshots

*(Screenshot placeholders — Add screenshots of the Split-Pane Editor, Terminal, Settings, and Audit Log here)*

---

## 🚀 Quick Start

Get your personal on-device coding agent running in 3 simple steps:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/mahavtaar/vibecoder.git
   cd vibecoder
   ```
2. **Open in Android Studio:**
   Open the `vibecoder` directory in Android Studio (Ladybug or newer). Let Gradle sync and download the native `llama.cpp` source automatically.
3. **Run on Device:**
   Connect an Android device (API 26+, `arm64-v8a`) or emulator and hit **Run**. Ensure you have at least 6GB of RAM.

For more detailed setup instructions, including connecting to a remote Ollama instance, see [INSTALL.md](INSTALL.md).

---

## 🏗 Architecture Overview

VibeCode utilizes a complex interplay between Jetpack Compose UIs, Room databases, and native C++ JNI layers.

For a complete look at the data flows and dependency injection structure, read the [ARCHITECTURE.md](ARCHITECTURE.md) guide.

---

## 🤖 Supported Models

| Model | Recommended Size (Q4_K_M) | Minimum RAM | Notes |
|-------|---------------------------|-------------|-------|
| **Qwen2.5-Coder-7B** | ~4.8 GB | 8 GB | Best all-around coding performance |
| **DeepSeek-Coder-V2-Lite** | ~10 GB | 12 GB+ | Deep reasoning, heavy |
| **Phi-4-mini** | ~2.5 GB | 6 GB | Excellent for mobile performance |
| **Llama-3.2-3B** | ~2.2 GB | 6 GB | Fast, good general knowledge |

---

## ✅ Phase Completion Status

| Phase | Description | Status |
|-------|-------------|--------|
| **1** | Project Skeleton & UI Shell | ✅ |
| **2** | LLM Core (Ollama/Llama.cpp Interfaces) | ✅ |
| **3** | Agent Tools (File, Shell, Memory, Code) | ✅ |
| **4** | ReAct Agent Loop & Chat UI | ✅ |
| **5** | Agentic Browsing Engine | ✅ |
| **6** | Monaco Code Editor & File Tree | ✅ |
| **7** | Terminal Engine (`xterm.js`) | ✅ |
| **8** | Settings & Workflow Templates | ✅ |
| **9** | Polish, Audits, Notifications, Final Wiring | ✅ |
| **10**| Native `llama.cpp` JNI Engine Integration | ✅ |

---

## 🗺️ Roadmap

- [ ] **Voice Input:** Use Android `SpeechRecognizer` for hands-free task dictation.
- [ ] **Vision Tool:** Capture screenshots and send them to multimodal LLMs for UI debugging.
- [ ] **Plugin System:** Define custom tools as Kotlin scripts loaded dynamically at runtime via `javax.script`.
- [ ] **Cloud Sync:** Sync projects to Google Drive via Android SAF.

---

## 🤝 Contributing
Contributions are welcome! Please read our [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, style guide, and the process for submitting pull requests to add new tools or engines.

## 📄 License
This project is licensed under the MIT License.
