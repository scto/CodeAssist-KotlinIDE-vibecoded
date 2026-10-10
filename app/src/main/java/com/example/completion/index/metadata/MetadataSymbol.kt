package com.example.completion.index.metadata

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.symbol.Symbol

/**
 * Symbol extracted from Kotlin class file metadata (functions, properties, constructors, typealiases).
 */
class MetadataSymbol(builder: Builder) : Symbol(builder) {

    val kotlinPackage: String = builder.kotlinPackage

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder : Symbol.Builder() {
        internal var kotlinPackage: String = ""

        fun kotlinPackage(kotlinPackage: String?): Builder {
            this.kotlinPackage = kotlinPackage ?: ""
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

        override fun build(): MetadataSymbol {
            return MetadataSymbol(this)
        }
    }
}
