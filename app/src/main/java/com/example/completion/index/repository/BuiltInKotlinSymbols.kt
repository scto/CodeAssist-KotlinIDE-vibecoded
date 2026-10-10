package com.example.completion.index.repository

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.symbol.Symbol

import java.util.Collections

/**
 * Built-in Kotlin standard library symbols, types, top-level functions, and extension members.
 */
object BuiltInKotlinSymbols {

    private val BUILT_INS: MutableList<Symbol> = ArrayList()
    private val MEMBERS_BY_TYPE: MutableMap<String, MutableList<Symbol>> = HashMap()

    init {
        // Built-in Types
        addType("String", "kotlin.String", SymbolKind.CLASS)
        addType("Int", "kotlin.Int", SymbolKind.CLASS)
        addType("Long", "kotlin.Long", SymbolKind.CLASS)
        addType("Float", "kotlin.Float", SymbolKind.CLASS)
        addType("Double", "kotlin.Double", SymbolKind.CLASS)
        addType("Boolean", "kotlin.Boolean", SymbolKind.CLASS)
        addType("Char", "kotlin.Char", SymbolKind.CLASS)
        addType("Byte", "kotlin.Byte", SymbolKind.CLASS)
        addType("Short", "kotlin.Short", SymbolKind.CLASS)
        addType("Unit", "kotlin.Unit", SymbolKind.OBJECT)
        addType("Any", "kotlin.Any", SymbolKind.CLASS)
        addType("Nothing", "kotlin.Nothing", SymbolKind.CLASS)
        addType("List", "kotlin.collections.List", SymbolKind.INTERFACE)
        addType("MutableList", "kotlin.collections.MutableList", SymbolKind.INTERFACE)
        addType("Map", "kotlin.collections.Map", SymbolKind.INTERFACE)
        addType("MutableMap", "kotlin.collections.MutableMap", SymbolKind.INTERFACE)
        addType("Set", "kotlin.collections.Set", SymbolKind.INTERFACE)
        addType("MutableSet", "kotlin.collections.MutableSet", SymbolKind.INTERFACE)
        addType("ArrayList", "java.util.ArrayList", SymbolKind.CLASS)
        addType("HashMap", "java.util.HashMap", SymbolKind.CLASS)
        addType("HashSet", "java.util.HashSet", SymbolKind.CLASS)
        addType("StringBuilder", "kotlin.text.StringBuilder", SymbolKind.CLASS)

        // Top-Level Standard Functions
        addFunction("println", "kotlin.io.println", "Unit", listOf("message: Any?"))
        addFunction("print", "kotlin.io.print", "Unit", listOf("message: Any?"))
        addFunction("readLine", "kotlin.io.readLine", "String?", emptyList())
        addFunction("listOf", "kotlin.collections.listOf", "List<T>", listOf("vararg elements: T"))
        addFunction("mutableListOf", "kotlin.collections.mutableListOf", "MutableList<T>", listOf("vararg elements: T"))
        addFunction("mapOf", "kotlin.collections.mapOf", "Map<K, V>", listOf("vararg pairs: Pair<K, V>"))
        addFunction("mutableMapOf", "kotlin.collections.mutableMapOf", "MutableMap<K, V>", listOf("vararg pairs: Pair<K, V>"))
        addFunction("setOf", "kotlin.collections.setOf", "Set<T>", listOf("vararg elements: T"))
        addFunction("mutableSetOf", "kotlin.collections.mutableSetOf", "MutableSet<T>", listOf("vararg elements: T"))
        addFunction("arrayOf", "kotlin.arrayOf", "Array<T>", listOf("vararg elements: T"))
        addFunction("emptyList", "kotlin.collections.emptyList", "List<T>", emptyList())
        addFunction("emptyMap", "kotlin.collections.emptyMap", "Map<K, V>", emptyList())
        addFunction("emptySet", "kotlin.collections.emptySet", "Set<T>", emptyList())
        addFunction("repeat", "kotlin.repeat", "Unit", listOf("times: Int", "action: (Int) -> Unit"))
        addFunction("check", "kotlin.check", "Unit", listOf("value: Boolean"))
        addFunction("require", "kotlin.require", "Unit", listOf("value: Boolean"))
        addFunction("error", "kotlin.error", "Nothing", listOf("message: Any"))

        // Members for String
        addMember("String", "length", SymbolKind.PROPERTY, "Int", emptyList())
        addMember("String", "isEmpty", SymbolKind.FUNCTION, "Boolean", emptyList())
        addMember("String", "isNotEmpty", SymbolKind.FUNCTION, "Boolean", emptyList())
        addMember("String", "isBlank", SymbolKind.FUNCTION, "Boolean", emptyList())
        addMember("String", "substring", SymbolKind.FUNCTION, "String", listOf("startIndex: Int, endIndex: Int"))
        addMember("String", "startsWith", SymbolKind.FUNCTION, "Boolean", listOf("prefix: String"))
        addMember("String", "endsWith", SymbolKind.FUNCTION, "Boolean", listOf("suffix: String"))
        addMember("String", "contains", SymbolKind.FUNCTION, "Boolean", listOf("other: CharSequence"))
        addMember("String", "replace", SymbolKind.FUNCTION, "String", listOf("oldValue: String, newValue: String"))
        addMember("String", "lowercase", SymbolKind.FUNCTION, "String", emptyList())
        addMember("String", "uppercase", SymbolKind.FUNCTION, "String", emptyList())
        addMember("String", "trim", SymbolKind.FUNCTION, "String", emptyList())
        addMember("String", "split", SymbolKind.FUNCTION, "List<String>", listOf("delimiters: String"))
        addMember("String", "toInt", SymbolKind.FUNCTION, "Int", emptyList())
        addMember("String", "toLong", SymbolKind.FUNCTION, "Long", emptyList())
        addMember("String", "toDouble", SymbolKind.FUNCTION, "Double", emptyList())

        // Members for List / Collection
        addMember("List", "size", SymbolKind.PROPERTY, "Int", emptyList())
        addMember("List", "isEmpty", SymbolKind.FUNCTION, "Boolean", emptyList())
        addMember("List", "isNotEmpty", SymbolKind.FUNCTION, "Boolean", emptyList())
        addMember("List", "get", SymbolKind.FUNCTION, "T", listOf("index: Int"))
        addMember("List", "first", SymbolKind.FUNCTION, "T", emptyList())
        addMember("List", "last", SymbolKind.FUNCTION, "T", emptyList())
        addMember("List", "firstOrNull", SymbolKind.FUNCTION, "T?", emptyList())
        addMember("List", "lastOrNull", SymbolKind.FUNCTION, "T?", emptyList())
        addMember("List", "filter", SymbolKind.FUNCTION, "List<T>", listOf("predicate: (T) -> Boolean"))
        addMember("List", "map", SymbolKind.FUNCTION, "List<R>", listOf("transform: (T) -> R"))
        addMember("List", "forEach", SymbolKind.FUNCTION, "Unit", listOf("action: (T) -> Unit"))
        addMember("List", "contains", SymbolKind.FUNCTION, "Boolean", listOf("element: T"))
        addMember("List", "sorted", SymbolKind.FUNCTION, "List<T>", emptyList())
        addMember("List", "reversed", SymbolKind.FUNCTION, "List<T>", emptyList())

        // Members for MutableList
        addMember("MutableList", "add", SymbolKind.FUNCTION, "Boolean", listOf("element: T"))
        addMember("MutableList", "remove", SymbolKind.FUNCTION, "Boolean", listOf("element: T"))
        addMember("MutableList", "removeAt", SymbolKind.FUNCTION, "T", listOf("index: Int"))
        addMember("MutableList", "clear", SymbolKind.FUNCTION, "Unit", emptyList())

        // Members for Map
        addMember("Map", "size", SymbolKind.PROPERTY, "Int", emptyList())
        addMember("Map", "keys", SymbolKind.PROPERTY, "Set<K>", emptyList())
        addMember("Map", "values", SymbolKind.PROPERTY, "Collection<V>", emptyList())
        addMember("Map", "get", SymbolKind.FUNCTION, "V?", listOf("key: K"))
        addMember("Map", "containsKey", SymbolKind.FUNCTION, "Boolean", listOf("key: K"))

        // Members for Any
        addMember("Any", "toString", SymbolKind.FUNCTION, "String", emptyList())
        addMember("Any", "hashCode", SymbolKind.FUNCTION, "Int", emptyList())
        addMember("Any", "equals", SymbolKind.FUNCTION, "Boolean", listOf("other: Any?"))
    }

    private fun addType(name: String, qName: String, kind: SymbolKind) {
        val lastDot = qName.lastIndexOf('.')
        val pkg = if (lastDot != -1) qName.substring(0, lastDot) else ""
        BUILT_INS.add(
            Symbol.builder()
                .name(name)
                .qualifiedName(qName)
                .kind(kind)
                .origin(SymbolOrigin.BUILTIN)
                .packageName(pkg)
                .returnType(name)
                .build()
        )
    }

    private fun addFunction(name: String, qName: String, returnType: String, params: List<String>) {
        BUILT_INS.add(
            Symbol.builder()
                .name(name)
                .qualifiedName(qName)
                .kind(SymbolKind.FUNCTION)
                .origin(SymbolOrigin.BUILTIN)
                .returnType(returnType)
                .parameters(params)
                .build()
        )
    }

    private fun addMember(typeName: String, name: String, kind: SymbolKind, returnType: String, params: List<String>) {
        val sym = Symbol.builder()
            .name(name)
            .qualifiedName("$typeName.$name")
            .kind(kind)
            .origin(SymbolOrigin.BUILTIN)
            .containerName(typeName)
            .returnType(returnType)
            .parameters(params)
            .build()

        MEMBERS_BY_TYPE.computeIfAbsent(typeName) { ArrayList() }.add(sym)
    }

    @JvmStatic
    fun getBuiltIns(): List<Symbol> {
        return Collections.unmodifiableList(BUILT_INS)
    }

    @JvmStatic
    fun getMembersForType(typeName: String?): List<Symbol> {
        if (typeName == null) return Collections.emptyList()
        val list = MEMBERS_BY_TYPE[typeName]
        if (list == null && typeName != "Any") {
            return MEMBERS_BY_TYPE["Any"]?.let { Collections.unmodifiableList(it) } ?: Collections.emptyList()
        }
        return if (list != null) Collections.unmodifiableList(list) else Collections.emptyList()
    }
}
