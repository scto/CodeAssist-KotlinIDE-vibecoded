package com.example.completion.index.repository;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in Kotlin standard library symbols, types, top-level functions, and extension members.
 */
public final class BuiltInKotlinSymbols {

    private static final List<Symbol> BUILT_INS = new ArrayList<>();
    private static final Map<String, List<Symbol>> MEMBERS_BY_TYPE = new HashMap<>();

    static {
        // Built-in Types
        addType("String", "kotlin.String", SymbolKind.CLASS);
        addType("Int", "kotlin.Int", SymbolKind.CLASS);
        addType("Long", "kotlin.Long", SymbolKind.CLASS);
        addType("Float", "kotlin.Float", SymbolKind.CLASS);
        addType("Double", "kotlin.Double", SymbolKind.CLASS);
        addType("Boolean", "kotlin.Boolean", SymbolKind.CLASS);
        addType("Char", "kotlin.Char", SymbolKind.CLASS);
        addType("Byte", "kotlin.Byte", SymbolKind.CLASS);
        addType("Short", "kotlin.Short", SymbolKind.CLASS);
        addType("Unit", "kotlin.Unit", SymbolKind.OBJECT);
        addType("Any", "kotlin.Any", SymbolKind.CLASS);
        addType("Nothing", "kotlin.Nothing", SymbolKind.CLASS);
        addType("List", "kotlin.collections.List", SymbolKind.INTERFACE);
        addType("MutableList", "kotlin.collections.MutableList", SymbolKind.INTERFACE);
        addType("Map", "kotlin.collections.Map", SymbolKind.INTERFACE);
        addType("MutableMap", "kotlin.collections.MutableMap", SymbolKind.INTERFACE);
        addType("Set", "kotlin.collections.Set", SymbolKind.INTERFACE);
        addType("MutableSet", "kotlin.collections.MutableSet", SymbolKind.INTERFACE);
        addType("ArrayList", "java.util.ArrayList", SymbolKind.CLASS);
        addType("HashMap", "java.util.HashMap", SymbolKind.CLASS);
        addType("HashSet", "java.util.HashSet", SymbolKind.CLASS);
        addType("StringBuilder", "kotlin.text.StringBuilder", SymbolKind.CLASS);

        // Top-Level Standard Functions
        addFunction("println", "kotlin.io.println", "Unit", Collections.singletonList("message: Any?"));
        addFunction("print", "kotlin.io.print", "Unit", Collections.singletonList("message: Any?"));
        addFunction("readLine", "kotlin.io.readLine", "String?", Collections.emptyList());
        addFunction("listOf", "kotlin.collections.listOf", "List<T>", Collections.singletonList("vararg elements: T"));
        addFunction("mutableListOf", "kotlin.collections.mutableListOf", "MutableList<T>", Collections.singletonList("vararg elements: T"));
        addFunction("mapOf", "kotlin.collections.mapOf", "Map<K, V>", Collections.singletonList("vararg pairs: Pair<K, V>"));
        addFunction("mutableMapOf", "kotlin.collections.mutableMapOf", "MutableMap<K, V>", Collections.singletonList("vararg pairs: Pair<K, V>"));
        addFunction("setOf", "kotlin.collections.setOf", "Set<T>", Collections.singletonList("vararg elements: T"));
        addFunction("mutableSetOf", "kotlin.collections.mutableSetOf", "MutableSet<T>", Collections.singletonList("vararg elements: T"));
        addFunction("arrayOf", "kotlin.arrayOf", "Array<T>", Collections.singletonList("vararg elements: T"));
        addFunction("emptyList", "kotlin.collections.emptyList", "List<T>", Collections.emptyList());
        addFunction("emptyMap", "kotlin.collections.emptyMap", "Map<K, V>", Collections.emptyList());
        addFunction("emptySet", "kotlin.collections.emptySet", "Set<T>", Collections.emptyList());
        addFunction("repeat", "kotlin.repeat", "Unit", Arrays.asList("times: Int", "action: (Int) -> Unit"));
        addFunction("check", "kotlin.check", "Unit", Collections.singletonList("value: Boolean"));
        addFunction("require", "kotlin.require", "Unit", Collections.singletonList("value: Boolean"));
        addFunction("error", "kotlin.error", "Nothing", Collections.singletonList("message: Any"));

        // Members for String
        addMember("String", "length", SymbolKind.PROPERTY, "Int", Collections.emptyList());
        addMember("String", "isEmpty", SymbolKind.FUNCTION, "Boolean", Collections.emptyList());
        addMember("String", "isNotEmpty", SymbolKind.FUNCTION, "Boolean", Collections.emptyList());
        addMember("String", "isBlank", SymbolKind.FUNCTION, "Boolean", Collections.emptyList());
        addMember("String", "substring", SymbolKind.FUNCTION, "String", Collections.singletonList("startIndex: Int, endIndex: Int"));
        addMember("String", "startsWith", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("prefix: String"));
        addMember("String", "endsWith", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("suffix: String"));
        addMember("String", "contains", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("other: CharSequence"));
        addMember("String", "replace", SymbolKind.FUNCTION, "String", Collections.singletonList("oldValue: String, newValue: String"));
        addMember("String", "lowercase", SymbolKind.FUNCTION, "String", Collections.emptyList());
        addMember("String", "uppercase", SymbolKind.FUNCTION, "String", Collections.emptyList());
        addMember("String", "trim", SymbolKind.FUNCTION, "String", Collections.emptyList());
        addMember("String", "split", SymbolKind.FUNCTION, "List<String>", Collections.singletonList("delimiters: String"));
        addMember("String", "toInt", SymbolKind.FUNCTION, "Int", Collections.emptyList());
        addMember("String", "toLong", SymbolKind.FUNCTION, "Long", Collections.emptyList());
        addMember("String", "toDouble", SymbolKind.FUNCTION, "Double", Collections.emptyList());

        // Members for List / Collection
        addMember("List", "size", SymbolKind.PROPERTY, "Int", Collections.emptyList());
        addMember("List", "isEmpty", SymbolKind.FUNCTION, "Boolean", Collections.emptyList());
        addMember("List", "isNotEmpty", SymbolKind.FUNCTION, "Boolean", Collections.emptyList());
        addMember("List", "get", SymbolKind.FUNCTION, "T", Collections.singletonList("index: Int"));
        addMember("List", "first", SymbolKind.FUNCTION, "T", Collections.emptyList());
        addMember("List", "last", SymbolKind.FUNCTION, "T", Collections.emptyList());
        addMember("List", "firstOrNull", SymbolKind.FUNCTION, "T?", Collections.emptyList());
        addMember("List", "lastOrNull", SymbolKind.FUNCTION, "T?", Collections.emptyList());
        addMember("List", "filter", SymbolKind.FUNCTION, "List<T>", Collections.singletonList("predicate: (T) -> Boolean"));
        addMember("List", "map", SymbolKind.FUNCTION, "List<R>", Collections.singletonList("transform: (T) -> R"));
        addMember("List", "forEach", SymbolKind.FUNCTION, "Unit", Collections.singletonList("action: (T) -> Unit"));
        addMember("List", "contains", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("element: T"));
        addMember("List", "sorted", SymbolKind.FUNCTION, "List<T>", Collections.emptyList());
        addMember("List", "reversed", SymbolKind.FUNCTION, "List<T>", Collections.emptyList());

        // Members for MutableList
        addMember("MutableList", "add", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("element: T"));
        addMember("MutableList", "remove", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("element: T"));
        addMember("MutableList", "removeAt", SymbolKind.FUNCTION, "T", Collections.singletonList("index: Int"));
        addMember("MutableList", "clear", SymbolKind.FUNCTION, "Unit", Collections.emptyList());

        // Members for Map
        addMember("Map", "size", SymbolKind.PROPERTY, "Int", Collections.emptyList());
        addMember("Map", "keys", SymbolKind.PROPERTY, "Set<K>", Collections.emptyList());
        addMember("Map", "values", SymbolKind.PROPERTY, "Collection<V>", Collections.emptyList());
        addMember("Map", "get", SymbolKind.FUNCTION, "V?", Collections.singletonList("key: K"));
        addMember("Map", "containsKey", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("key: K"));

        // Members for Any
        addMember("Any", "toString", SymbolKind.FUNCTION, "String", Collections.emptyList());
        addMember("Any", "hashCode", SymbolKind.FUNCTION, "Int", Collections.emptyList());
        addMember("Any", "equals", SymbolKind.FUNCTION, "Boolean", Collections.singletonList("other: Any?"));
    }

    private static void addType(String name, String qName, SymbolKind kind) {
        BUILT_INS.add(Symbol.builder()
                .name(name)
                .qualifiedName(qName)
                .kind(kind)
                .origin(SymbolOrigin.BUILTIN)
                .packageName(qName.substring(0, qName.lastIndexOf('.')))
                .returnType(name)
                .build());
    }

    private static void addFunction(String name, String qName, String returnType, List<String> params) {
        BUILT_INS.add(Symbol.builder()
                .name(name)
                .qualifiedName(qName)
                .kind(SymbolKind.FUNCTION)
                .origin(SymbolOrigin.BUILTIN)
                .returnType(returnType)
                .parameters(params)
                .build());
    }

    private static void addMember(String typeName, String name, SymbolKind kind, String returnType, List<String> params) {
        Symbol sym = Symbol.builder()
                .name(name)
                .qualifiedName(typeName + "." + name)
                .kind(kind)
                .origin(SymbolOrigin.BUILTIN)
                .containerName(typeName)
                .returnType(returnType)
                .parameters(params)
                .build();

        MEMBERS_BY_TYPE.computeIfAbsent(typeName, k -> new ArrayList<>()).add(sym);
    }

    public static List<Symbol> getBuiltIns() {
        return Collections.unmodifiableList(BUILT_INS);
    }

    public static List<Symbol> getMembersForType(String typeName) {
        if (typeName == null) return Collections.emptyList();
        List<Symbol> list = MEMBERS_BY_TYPE.get(typeName);
        if (list == null && !typeName.equals("Any")) {
            return MEMBERS_BY_TYPE.getOrDefault("Any", Collections.emptyList());
        }
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }
}
