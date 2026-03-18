package com.mahavtaar.vibecoder.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStreamReader
import java.util.UUID

class TerminalSession(initialDir: String) {
    val id: String = UUID.randomUUID().toString()

    private val _cwd = MutableStateFlow(initialDir)
    val cwd: StateFlow<String> = _cwd.asStateFlow()

    private val _output = MutableSharedFlow<String>(replay = 50)
    val output: SharedFlow<String> = _output.asSharedFlow()

    private var currentProcess: Process? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var outputJob: Job? = null

    init {
        scope.launch {
            _output.emit("Session started at ${_cwd.value}\n")
        }
    }

    suspend fun executeCommand(command: String) = withContext(Dispatchers.IO) {
        if (command.isBlank()) return@withContext

        try {
            val cmdList = mutableListOf("/system/bin/sh", "-c")

            // Handle 'cd' locally since ProcessBuilder spawning a new shell forgets it
            if (command.startsWith("cd ")) {
                val newDir = command.removePrefix("cd ").trim()
                val targetFile = File(_cwd.value, newDir).canonicalFile
                if (targetFile.exists() && targetFile.isDirectory) {
                    _cwd.value = targetFile.absolutePath
                } else {
                    _output.emit("\u001B[31msh: cd: $newDir: No such file or directory\u001B[0m\n")
                }
                return@withContext
            }

            cmdList.add(command)

            currentProcess?.destroy()

            val pb = ProcessBuilder(cmdList)
                .directory(File(_cwd.value))
                .redirectErrorStream(true)

            val process = pb.start()
            currentProcess = process

            val reader = InputStreamReader(process.inputStream)

            outputJob?.cancel()
            outputJob = scope.launch {
                val buffer = CharArray(1024)
                var read: Int
                while (isActive) {
                    read = reader.read(buffer)
                    if (read == -1) break
                    val chunk = String(buffer, 0, read)
                    // Basic ANSI color handling could be applied here if needed,
                    // but redirectErrorStream combines stdout and stderr.
                    _output.emit(chunk)
                }
            }

            process.waitFor()

            // Try to update CWD if the command was a complex script that might have changed directories,
            // though standard subshells won't affect the parent process.

            // Emit prompt after command finishes
            _output.emit("\r\n\x1b[32m${_cwd.value}\x1b[0m $ ")

        } catch (e: Exception) {
            _output.emit("\u001B[31mFailed to execute command: ${e.message}\u001B[0m\n")
            _output.emit("\r\n\x1b[32m${_cwd.value}\x1b[0m $ ")
        }
    }

    fun resize(cols: Int, rows: Int) {
        // Stub for future pseudo-terminal (PTY) support. ProcessBuilder does not support PTY sizing natively.
    }

    fun kill() {
        currentProcess?.destroyForcibly()
        currentProcess = null
        outputJob?.cancel()
    }

    fun getCwd(): String = _cwd.value
}
