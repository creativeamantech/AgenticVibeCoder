package com.mahavtaar.vibecoder.agent

data class WorkflowTemplate(
    val name: String,
    val description: String,
    val taskPrompt: String
)

object WorkflowTemplates {
    val ALL = listOf(
        WorkflowTemplate(
            name = "Scaffold Project",
            description = "Creates standard Android project folder skeleton",
            taskPrompt = "Scaffold a new Android project named 'MyApp' with standard Compose architecture."
        ),
        WorkflowTemplate(
            name = "Debug & Fix",
            description = "Analyzes logs and files to fix errors",
            taskPrompt = "Analyze the codebase for errors, search for solutions, and patch the files."
        ),
        WorkflowTemplate(
            name = "Research & Implement",
            description = "Researches a feature and implements it",
            taskPrompt = "Research how to implement CameraX in Compose, add dependencies, and generate code."
        ),
        WorkflowTemplate(
            name = "Publish to GitHub",
            description = "Initializes git, commits, and pushes to remote",
            taskPrompt = "Initialize a git repository, commit all files, and push to remote origin."
        )
    )
}
