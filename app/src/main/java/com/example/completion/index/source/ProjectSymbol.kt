package com.example.completion.index.source

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.symbol.Symbol

/**
 * Symbol extracted from project source files with file path and container metadata.
 */
class ProjectSymbol(builder: Builder) : Symbol(builder) {

    val sourceFilePath: String = builder.sourceFilePath
    val lastModified: Long = builder.lastModified

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder : Symbol.Builder() {
        internal var sourceFilePath: String = ""
        internal var lastModified: Long = 0

        fun sourceFilePath(sourceFilePath: String?): Builder {
            this.sourceFilePath = sourceFilePath ?: ""
            return this
        }

        fun lastModified(lastModified: Long): Builder {
            this.lastModified = lastModified
            return this
        }

        override fun name(name: String?): Builder {
            super.name(name)
            return this
        }

        override fun qualifiedName(qualifiedName: String?): Builder {
            super.qualifiedName(qualifiedName)
            return this
        }

        override fun kind(kind: SymbolKind?): Builder {
            super.kind(kind)
            return this
        }

        override fun origin(origin: SymbolOrigin?): Builder {
            super.origin(origin)
            return this
        }

        override fun packageName(packageName: String?): Builder {
            super.packageName(packageName)
            return this
        }

        override fun returnType(returnType: String?): Builder {
            super.returnType(returnType)
            return this
        }

        override fun parameters(parameters: List<String>?): Builder {
            super.parameters(parameters)
            return this
        }

        override fun containerName(containerName: String?): Builder {
            super.containerName(containerName)
            return this
        }

        override fun receiverType(receiverType: String?): Builder {
            super.receiverType(receiverType)
            return this
        }

        override fun build(): ProjectSymbol {
            return ProjectSymbol(this)
        }
    }
}
