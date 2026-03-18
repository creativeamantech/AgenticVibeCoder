# VibeCode Setup & Installation Guide

Welcome to VibeCode! This guide covers the system requirements, installation, and initial configuration needed to get your autonomous coding agent running locally.

## System Requirements

To build and run VibeCode effectively, ensure your system meets the following criteria:

- **Android Studio:** Ladybug or newer.
- **Android NDK:** Version `26.1.10909125` (required for `llama.cpp` JNI compilation).
- **Target Device:** Android 8.0 (API 26) or higher.
- **Hardware (For Local Inference):** Minimum 6GB of RAM (8GB+ recommended). The device must support the `arm64-v8a` ABI.

## Android Studio Setup

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/mahavtaar/vibecoder.git
   cd vibecoder
   ```
2. **Open the Project:**
   Launch Android Studio and select **Open**. Navigate to the cloned `vibecoder` directory.
3. **NDK Setup:**
   If you encounter NDK-related errors during the initial Gradle sync, go to **Tools > SDK Manager > SDK Tools**, check "Show Package Details", and install NDK version `26.1.10909125`.
4. **Build and Run:**
   Allow Gradle to finish syncing (which downloads `llama.cpp` automatically). Then, select your target device and click **Run (Shift + F10)**.

## First Launch Configuration

VibeCode supports two engines: a fully local `llama.cpp` engine, and a remote `Ollama` proxy over your local network.

### Option A: Connect Ollama on LAN (Recommended for testing/low-RAM devices)

If your device struggles with local inference, you can offload the computation to your PC.

1. Ensure your Android device and PC are on the same Wi-Fi network.
2. On your PC, set the environment variable `OLLAMA_HOST=0.0.0.0` and start Ollama.
3. Open VibeCode, navigate to **Settings**.
4. Under **LLM Engine**, select `Ollama`.
5. Enter your PC's IP address and Port (default `11434`).
6. Tap **Test Connection**. You should see "✅ Success".

### Option B: Fully Local On-Device Inference (llama.cpp)

1. Open the VibeCode app and navigate to **Model Manager**.
2. Tap **Download** next to `Qwen2.5-Coder-7B` (or `Phi-4-mini` for faster performance).
3. Once the download completes, tap **Load** to load the `.gguf` file into memory.
4. Navigate to **Settings** > **LLM Engine** and select `llama.cpp`.

## Troubleshooting

- **C++ Build Errors:** Ensure you have the exact NDK version (`26.1.10909125`) installed.
- **Blank Editor/Terminal:** The Monaco Editor and Xterm.js rely on active internet connections to fetch assets from Cloudflare CDNs on the first launch. Ensure your device is online.
- **OOM (Out of Memory) Crashes:** The app requests `largeHeap="true"`. However, if the app crashes instantly when loading a model, your device does not have enough contiguous RAM. Switch to a smaller model (e.g., `Phi-4-mini`) or use the `Ollama` remote fallback.
- **JNI Load Failures:** If you see `UnsatisfiedLinkError`, ensure your device or emulator architecture is exactly `arm64-v8a`. The `llama-jni` module is currently configured strictly for this architecture to optimize ARM NEON SIMD instructions.
