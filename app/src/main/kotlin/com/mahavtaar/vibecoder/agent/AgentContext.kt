package com.mahavtaar.vibecoder.agent

data class AgentContext(
    val workingDir: String,
    val projectType: String = "Kotlin Android (Jetpack Compose)",
    val sessionId: String,
    val maxSteps: Int = 20,
    val enabledTools: Set<String> = emptySet(),
    val autoConfirmShell: Boolean = false,
    val autoConfirmWrite: Boolean = true
)
