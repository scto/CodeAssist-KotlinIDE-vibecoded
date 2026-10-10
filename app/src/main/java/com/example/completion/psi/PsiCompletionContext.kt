package com.example.completion.psi

/**
 * Encapsulates the resolved syntactic and semantic context at the cursor position.
 */
class PsiCompletionContext private constructor(builder: Builder) {

    val contextType: CompletionContextType = requireNotNull(builder.contextType) { "contextType cannot be null" }
    val prefix: String = builder.prefix
    val receiver: String = builder.receiver
    val receiverType: String = builder.receiverType
    val parsedFile: ParsedKotlinFile? = builder.parsedFile
    val targetElement: KotlinPsiElement? = builder.targetElement
    val scopeElement: KotlinPsiElement? = builder.scopeElement
    val offset: Int = builder.offset
    val line: Int = builder.line
    val column: Int = builder.column

    val isMemberAccess: Boolean
        get() = contextType == CompletionContextType.MEMBER_ACCESS && receiver.isNotEmpty()

    val isImport: Boolean
        get() = contextType == CompletionContextType.IMPORT

    val isPackage: Boolean
        get() = contextType == CompletionContextType.PACKAGE

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder {
        internal var contextType: CompletionContextType = CompletionContextType.UNKNOWN
        internal var prefix: String = ""
        internal var receiver: String = ""
        internal var receiverType: String = ""
        internal var parsedFile: ParsedKotlinFile? = null
        internal var targetElement: KotlinPsiElement? = null
        internal var scopeElement: KotlinPsiElement? = null
        internal var offset: Int = 0
        internal var line: Int = 0
        internal var column: Int = 0

        fun contextType(contextType: CompletionContextType): Builder {
            this.contextType = contextType
            return this
        }

        fun prefix(prefix: String?): Builder {
            this.prefix = prefix ?: ""
            return this
        }

        fun receiver(receiver: String?): Builder {
            this.receiver = receiver ?: ""
            return this
        }

        fun receiverType(receiverType: String?): Builder {
            this.receiverType = receiverType ?: ""
            return this
        }

        fun parsedFile(parsedFile: ParsedKotlinFile?): Builder {
            this.parsedFile = parsedFile
            return this
        }

        fun targetElement(targetElement: KotlinPsiElement?): Builder {
            this.targetElement = targetElement
            return this
        }

        fun scopeElement(scopeElement: KotlinPsiElement?): Builder {
            this.scopeElement = scopeElement
            return this
        }

        fun offset(offset: Int): Builder {
            this.offset = offset
            return this
        }

        fun line(line: Int): Builder {
            this.line = line
            return this
        }

        fun column(column: Int): Builder {
            this.column = column
            return this
        }

        fun build(): PsiCompletionContext {
            return PsiCompletionContext(this)
        }
    }
}
