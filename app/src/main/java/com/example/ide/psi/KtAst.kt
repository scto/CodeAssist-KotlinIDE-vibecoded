package com.example.ide.psi

import java.io.File

/**
 * Kotlin Abstract Syntax Tree (AST / PSI) node structures.
 */
sealed class KtPsiNode {
    abstract val line: Int
    abstract val column: Int
}

data class KtFilePsi(
    val file: File?,
    var packageDirective: KtPackageDirective? = null,
    val imports: MutableList<KtImportDirective> = mutableListOf(),
    val declarations: MutableList<KtDeclaration> = mutableListOf(),
    val allTokens: List<KtToken> = emptyList(),
    val syntaxErrors: MutableList<KtSyntaxError> = mutableListOf()
) : KtPsiNode() {
    override val line: Int get() = 1
    override val column: Int get() = 1
}

data class KtPackageDirective(
    val fqName: String,
    override val line: Int,
    override val column: Int
) : KtPsiNode()

data class KtImportDirective(
    val importedFqName: String,
    val isAllUnder: Boolean = false,
    val alias: String? = null,
    override val line: Int,
    override val column: Int
) : KtPsiNode()

sealed class KtDeclaration : KtPsiNode() {
    abstract val name: String
    abstract val modifiers: List<String>
    abstract val annotations: List<String>
}

data class KtClassOrObject(
    override val name: String,
    val kind: ClassKind,
    override val modifiers: List<String> = emptyList(),
    override val annotations: List<String> = emptyList(),
    val superTypes: List<String> = emptyList(),
    val primaryConstructorParams: List<KtParameter> = emptyList(),
    val secondaryConstructors: List<KtConstructor> = emptyList(),
    val functions: MutableList<KtFunction> = mutableListOf(),
    val properties: MutableList<KtProperty> = mutableListOf(),
    val innerClasses: MutableList<KtClassOrObject> = mutableListOf(),
    override val line: Int,
    override val column: Int
) : KtDeclaration() {
    enum class ClassKind {
        CLASS,
        DATA_CLASS,
        INTERFACE,
        ENUM_CLASS,
        SEALED_CLASS,
        OBJECT,
        COMPANION_OBJECT
    }
}

data class KtFunction(
    override val name: String,
    val parameters: List<KtParameter> = emptyList(),
    val returnType: String = "Unit",
    val isSuspend: Boolean = false,
    val isInline: Boolean = false,
    val isOverride: Boolean = false,
    override val modifiers: List<String> = emptyList(),
    override val annotations: List<String> = emptyList(),
    val localVariables: MutableList<KtProperty> = mutableListOf(),
    val bodyStatements: List<String> = emptyList(),
    override val line: Int,
    override val column: Int
) : KtDeclaration()

data class KtProperty(
    override val name: String,
    val type: String = "Any",
    val isVar: Boolean = false,
    val isOverride: Boolean = false,
    val initializer: String? = null,
    override val modifiers: List<String> = emptyList(),
    override val annotations: List<String> = emptyList(),
    override val line: Int,
    override val column: Int
) : KtDeclaration()

data class KtParameter(
    val name: String,
    val type: String = "Any",
    val defaultValue: String? = null,
    val isVararg: Boolean = false,
    val isValOrVar: Boolean = false,
    val isVar: Boolean = false,
    override val line: Int,
    override val column: Int
) : KtPsiNode()

data class KtConstructor(
    val parameters: List<KtParameter> = emptyList(),
    val isPrimary: Boolean = false,
    override val line: Int,
    override val column: Int
) : KtPsiNode()

data class KtSyntaxError(
    val message: String,
    val line: Int,
    val column: Int,
    val length: Int = 1,
    val fixSuggestion: String? = null
)
