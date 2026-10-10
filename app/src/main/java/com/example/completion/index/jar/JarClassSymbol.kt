package com.example.completion.index.jar

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.symbol.Symbol

/**
 * Symbol representing a class or member extracted from a JAR dependency without using ClassLoader.
 */
class JarClassSymbol(builder: Builder) : Symbol(builder) {

    val jarFilePath: String = builder.jarFilePath
    val isInterface: Boolean = builder.isInterface
    val isEnum: Boolean = builder.isEnum

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }

    class Builder : Symbol.Builder() {
        internal var jarFilePath: String = ""
        internal var isInterface: Boolean = false
        internal var isEnum: Boolean = false

        fun jarFilePath(jarFilePath: String?): Builder {
            this.jarFilePath = jarFilePath ?: ""
            return this
        }

        fun isInterface(isInterface: Boolean): Builder {
            this.isInterface = isInterface
            return this
        }

        fun isEnum(isEnum: Boolean): Builder {
            this.isEnum = isEnum
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

        override fun build(): JarClassSymbol {
            return JarClassSymbol(this)
        }
    }
}
