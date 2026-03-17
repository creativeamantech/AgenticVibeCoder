# 🤖 VibeCode — Android App with Local LLM + Agentic Browsing

VibeCode is an experimental, fully self-contained Android application designed to bring an **autonomous AI software engineer** directly to your mobile device or tablet. By running large language models locally on device via `llama.cpp` (or remotely via `Ollama`), VibeCode creates a persistent, sandboxed coding environment featuring an interactive agent loop, a full-fledged IDE editor, terminal, and automated browsing.

---

## 🌟 Features

*   **Local LLM Support:** Run `Qwen2.5-Coder`, `Phi-4-mini`, and others natively via `llama.cpp` using JNI.
*   **ReAct Agent Loop:** Autonomous code generation, refactoring, and debugging via a ReAct loop.
*   **Agentic Web Browsing:** Allows the AI agent to search Google/DDG, read docs, click, and extract web data via headless HTTP and an interactive JS `WebView`.
*   **Monaco Code Editor:** Full IDE experience powered by `monaco-editor` inside an Android WebView, equipped with AI ghost-text completions.
*   **Integrated Terminal:** An interactive bash terminal rendered by `xterm.js`.
*   **Persistent Memory & Auditing:** Every agent thought and action is recorded into a local SQLite Room database.
*   **System Notifications:** Keep track of the agent's progress in the background.

---

## 🏗 Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                       VibeCode App                          │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   UI Layer   │  │ Agent Engine │  │ Local Models │       │
│  │ (Compose)    │  │ (ReAct Loop) │  │ (llama.cpp)  │       │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘       │
│         │                 │                 │               │
│  ┌──────▼───────┐  ┌──────▼───────┐         │               │
│  │ Monaco /     │  │ Tool Registry│◄────────┘               │
│  │ Xterm WebV.  │  │ (File, Shell,│                       │
│  └──────────────┘  │  Web, Code)  │                       │
│                    └──────────────┘                       │
└─────────────────────────────────────────────────────────────┘
```

---

## 🚀 Setup & Execution

### Requirements
*   Android Studio Ladybug (or equivalent AGP 8.2+)
*   Kotlin 1.9.22+
*   Target SDK 35

### Connecting Ollama
By default, the application is set to rely on a remote `Ollama` server because downloading heavy LLM weights locally can consume vast amounts of device resources.
1. Install [Ollama](https://ollama.com/) on your PC.
2. Ensure your PC and Android device are on the **same Wi-Fi network**.
3. In Ollama's configuration, you must set `OLLAMA_HOST=0.0.0.0` so it accepts LAN connections.
4. Launch VibeCode, go to **Settings**, and enter your PC's local IP address (e.g. `192.168.1.x`).
5. Tap **Test Connection** to verify.

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

---

## 🤝 Contributing
Contributions are welcome. Please ensure that PRs use standard Kotlin conventions, prioritize Jetpack Compose (no XML layouts), and follow the Hilt DI structure established throughout the project phases.

## 📄 License
This project is licensed under the MIT License.
