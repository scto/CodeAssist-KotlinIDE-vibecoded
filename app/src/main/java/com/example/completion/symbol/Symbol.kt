package com.example.completion.symbol

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin

/**
 * Universal model for any code symbol (class, function, property, parameter, etc.)
 * across Source, JAR, Metadata, and Built-in origins.
 */
open class Symbol(builder: Builder) {

    val name: String = requireNotNull(builder.name) { "name cannot be null" }
    val qualifiedName: String = builder.qualifiedName ?: name
    val kind: SymbolKind = builder.kind ?: SymbolKind.UNKNOWN
    val origin: SymbolOrigin = builder.origin ?: SymbolOrigin.SOURCE
    val packageName: String = builder.packageName ?: ""
    val returnType: String = builder.returnType ?: ""
    val parameters: List<String> = builder.parameters?.let { ArrayList(it) } ?: emptyList()
    val containerName: String = builder.containerName ?: ""
    val documentation: String = builder.documentation ?: ""
    val receiverType: String = builder.receiverType ?: ""
    val isStatic: Boolean = builder.isStatic

    val isExtension: Boolean
        get() = receiverType.isNotEmpty()

    val parameterSignature: String
        get() {
            if (parameters.isEmpty()) {
                return "()"
            }
            return "(" + parameters.joinToString(", ") + ")"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Symbol) return false
        return isStatic == other.isStatic &&
                name == other.name &&
                qualifiedName == other.qualifiedName &&
                kind == other.kind &&
                origin == other.origin &&
                containerName == other.containerName &&
                parameters == other.parameters &&
                receiverType == other.receiverType
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + qualifiedName.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + origin.hashCode()
        result = 31 * result + containerName.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + receiverType.hashCode()
        result = 31 * result + isStatic.hashCode()
        return result
    }

    override fun toString(): String {
        return "Symbol{" +
                "name='$name'" +
                ", qualifiedName='$qualifiedName'" +
                ", kind=$kind" +
                ", origin=$origin" +
                ", returnType='$returnType'" +
                '}'
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }

        @JvmStatic
        fun builder(name: String, kind: SymbolKind?, origin: SymbolOrigin?): Builder {
            return Builder().name(name).kind(kind).origin(origin)
        }
    }

    open class Builder {
        internal var name: String? = null
        internal var qualifiedName: String? = null
        internal var kind: SymbolKind? = SymbolKind.UNKNOWN
        internal var origin: SymbolOrigin? = SymbolOrigin.SOURCE
        internal var packageName: String? = ""
        internal var returnType: String? = ""
        internal var parameters: List<String>? = emptyList()
        internal var containerName: String? = ""
        internal var documentation: String? = ""
        internal var receiverType: String? = ""
        internal var isStatic: Boolean = false

        open fun name(name: String?): Builder {
            this.name = name
            return this
        }

        open fun qualifiedName(qualifiedName: String?): Builder {
            this.qualifiedName = qualifiedName
            return this
        }

        open fun kind(kind: SymbolKind?): Builder {
            this.kind = kind
            return this
        }

        open fun origin(origin: SymbolOrigin?): Builder {
            this.origin = origin
            return this
        }

        open fun packageName(packageName: String?): Builder {
            this.packageName = packageName
            return this
        }

        open fun returnType(returnType: String?): Builder {
            this.returnType = returnType
            return this
        }

        open fun parameters(parameters: List<String>?): Builder {
            this.parameters = parameters
            return this
        }

        open fun containerName(containerName: String?): Builder {
            this.containerName = containerName
            return this
        }

        open fun documentation(documentation: String?): Builder {
            this.documentation = documentation
            return this
        }

        open fun receiverType(receiverType: String?): Builder {
            this.receiverType = receiverType
            return this
        }

        open fun isStatic(isStatic: Boolean): Builder {
            this.isStatic = isStatic
            return this
        }

        open fun build(): Symbol {
            return Symbol(this)
        }
    }
}
