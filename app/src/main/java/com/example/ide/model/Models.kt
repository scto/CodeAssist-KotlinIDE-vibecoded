package com.example.ide.model

import java.io.File

/**
 * Model representing a Kotlin Project in the IDE workspace.
 */
data class Project(
    val name: String,
    val rootDir: File,
    var sourceDirs: MutableList<File> = mutableListOf(),
    val type: ProjectType = ProjectType.KOTLIN_CONSOLE,
    val description: String = ""
) {
    enum class ProjectType {
        KOTLIN_CONSOLE,
        KOTLIN_ANDROID,
        KOTLIN_ALGORITHMS,
        KOTLIN_COROUTINES,
        EMPTY
    }

    fun getAllKotlinFiles(): List<File> {
        val list = mutableListOf<File>()
        for (srcDir in sourceDirs) {
            if (srcDir.exists() && srcDir.isDirectory) {
                srcDir.walkTopDown()
                    .filter { it.isFile && (it.extension == "kt" || it.extension == "kts") }
                    .forEach { list.add(it) }
            }
        }
        return list
    }
}

/**
 * Tree node representation for File Explorer.
 */
data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val level: Int = 0,
    var isExpanded: Boolean = false,
    var children: MutableList<FileItem> = mutableListOf(),
    var isSelected: Boolean = false
) {
    val isKotlinFile: Boolean
        get() = !isDirectory && (file.extension.equals("kt", ignoreCase = true) || file.extension.equals("kts", ignoreCase = true))

    val isGradleFile: Boolean
        get() = !isDirectory && (file.name.endsWith(".gradle") || file.name.endsWith(".gradle.kts"))

    val isXmlFile: Boolean
        get() = !isDirectory && file.extension.equals("xml", ignoreCase = true)
}

/**
 * Model representing an open editor tab.
 */
data class EditorTab(
    val file: File,
    var title: String = file.name,
    var content: String = "",
    var isModified: Boolean = false,
    var cursorPosition: Int = 0,
    var line: Int = 1,
    var column: Int = 1
)

/**
 * Diagnostic problem item for errors and warnings.
 */
data class DiagnosticItem(
    val file: File?,
    val line: Int,
    val column: Int,
    val endLine: Int = line,
    val endColumn: Int = column + 1,
    val message: String,
    val severity: Severity = Severity.ERROR,
    val rule: String = "syntax",
    val quickFixText: String? = null
) {
    enum class Severity {
        ERROR,
        WARNING,
        INFO,
        HINT
    }
}

/**
 * PSI outline item representing a structural element in Kotlin file.
 */
data class PsiOutlineNode(
    val kind: OutlineKind,
    val name: String,
    val details: String = "",
    val line: Int = 1,
    val column: Int = 1,
    val children: MutableList<PsiOutlineNode> = mutableListOf()
) {
    enum class OutlineKind {
        PACKAGE,
        IMPORT_GROUP,
        IMPORT,
        CLASS,
        DATA_CLASS,
        INTERFACE,
        ENUM_CLASS,
        OBJECT,
        COMPANION_OBJECT,
        FUNCTION,
        PROPERTY,
        VARIABLE,
        CONSTRUCTOR
    }
}
