package com.example.ide.analysis

/**
 * Descriptor for a member (method or property) in Kotlin stdlib or Android SDK.
 */
data class MemberDescriptor(
    val name: String,
    val signature: String,
    val returnType: String,
    val isMethod: Boolean = true,
    val doc: String = "",
    val snippet: String = name
)

/**
 * Descriptor for a Class / Interface / Type.
 */
data class TypeDescriptor(
    val simpleName: String,
    val fqName: String,
    val doc: String = "",
    val members: MutableList<MemberDescriptor> = mutableListOf(),
    val isInterface: Boolean = false,
    val isObject: Boolean = false
)

/**
 * Built-in Kotlin Standard Library and Android SDK symbol catalog.
 */
object KotlinStdlibCatalog {

    val types = mutableMapOf<String, TypeDescriptor>()
    val globalFunctions = mutableListOf<MemberDescriptor>()
    val kotlinKeywords = mutableListOf<MemberDescriptor>()

    init {
        loadKeywords()
        loadGlobalFunctions()
        loadTypes()
    }

    private fun loadKeywords() {
        val keywordsList = listOf(
            Triple("fun", "fun name(args): Unit", "fun \${1:name}(\${2:params}): \${3:Unit} {\n    \${4}\n}"),
            Triple("val", "val name: Type = value", "val \${1:name}: \${2:String} = \${3:\"\"}"),
            Triple("var", "var name: Type = value", "var \${1:name}: \${2:String} = \${3:\"\"}"),
            Triple("class", "class Name(params)", "class \${1:Name}(\${2:params}) {\n    \${3}\n}"),
            Triple("data class", "data class Name(val x: Type)", "data class \${1:Name}(\n    val \${2:id}: \${3:Int},\n    val \${4:name}: \${5:String}\n)"),
            Triple("interface", "interface Name", "interface \${1:Name} {\n    \${2}\n}"),
            Triple("object", "object Name", "object \${1:Name} {\n    \${2}\n}"),
            Triple("companion object", "companion object", "companion object {\n    \${1}\n}"),
            Triple("enum class", "enum class Name", "enum class \${1:Name} {\n    \${2:FIRST},\n    \${3:SECOND}\n}"),
            Triple("sealed class", "sealed class Name", "sealed class \${1:Name} {\n    \${2}\n}"),
            Triple("if", "if (condition) { ... }", "if (\${1:condition}) {\n    \${2}\n}"),
            Triple("if-else", "if (condition) { ... } else { ... }", "if (\${1:condition}) {\n    \${2}\n} else {\n    \${3}\n}"),
            Triple("when", "when (x) { ... }", "when (\${1:x}) {\n    \${2:is Type} -> \${3}\n    else -> \${4}\n}"),
            Triple("for", "for (item in collection)", "for (\${1:item} in \${2:collection}) {\n    \${3}\n}"),
            Triple("while", "while (condition)", "while (\${1:condition}) {\n    \${2}\n}"),
            Triple("try-catch", "try { ... } catch (e: Exception) { ... }", "try {\n    \${1}\n} catch (e: Exception) {\n    \${2:e.printStackTrace()}\n}"),
            Triple("main", "fun main(args: Array<String>)", "fun main(args: Array<String>) {\n    println(\"Hello, Kotlin!\")\n}")
        )
        for ((kw, sig, snip) in keywordsList) {
            kotlinKeywords.add(MemberDescriptor(kw, sig, "Keyword", false, "Kotlin keyword / snippet", snip))
        }
    }

    private fun loadGlobalFunctions() {
        val funcs = listOf(
            MemberDescriptor("println", "println(message: Any?)", "Unit", true, "Prints the given message to the standard output.", "println(\$1)"),
            MemberDescriptor("print", "print(message: Any?)", "Unit", true, "Prints the given message to the standard output.", "print(\$1)"),
            MemberDescriptor("readLine", "readLine(): String?", "String?", true, "Reads a line of input from standard input.", "readLine()"),
            MemberDescriptor("listOf", "listOf<T>(vararg elements: T): List<T>", "List<T>", true, "Returns an immutable list containing only the specified elements.", "listOf(\$1)"),
            MemberDescriptor("mutableListOf", "mutableListOf<T>(vararg elements: T): MutableList<T>", "MutableList<T>", true, "Returns a mutable list containing only the specified elements.", "mutableListOf(\$1)"),
            MemberDescriptor("mapOf", "mapOf<K, V>(vararg pairs: Pair<K, V>): Map<K, V>", "Map<K, V>", true, "Returns an immutable map containing the specified pairs.", "mapOf(\$1 to \$2)"),
            MemberDescriptor("mutableMapOf", "mutableMapOf<K, V>(vararg pairs: Pair<K, V>): MutableMap<K, V>", "MutableMap<K, V>", true, "Returns a mutable map containing the specified pairs.", "mutableMapOf(\$1)"),
            MemberDescriptor("setOf", "setOf<T>(vararg elements: T): Set<T>", "Set<T>", true, "Returns an immutable set containing only the specified elements.", "setOf(\$1)"),
            MemberDescriptor("mutableSetOf", "mutableSetOf<T>(vararg elements: T): MutableSet<T>", "MutableSet<T>", true, "Returns a mutable set containing only the specified elements.", "mutableSetOf(\$1)"),
            MemberDescriptor("arrayOf", "arrayOf<T>(vararg elements: T): Array<T>", "Array<T>", true, "Returns an array containing the specified elements.", "arrayOf(\$1)"),
            MemberDescriptor("repeat", "repeat(times: Int, action: (Int) -> Unit)", "Unit", true, "Executes the given function action specified number of times.", "repeat(\${1:10}) {\n    \${2}\n}"),
            MemberDescriptor("maxOf", "maxOf<T : Comparable<T>>(a: T, b: T): T", "T", true, "Returns the greater of two values.", "maxOf(\$1, \$2)"),
            MemberDescriptor("minOf", "minOf<T : Comparable<T>>(a: T, b: T): T", "T", true, "Returns the smaller of two values.", "minOf(\$1, \$2)"),
            MemberDescriptor("lazy", "lazy(initializer: () -> T): Lazy<T>", "Lazy<T>", true, "Creates a new instance of the Lazy that uses the specified initialization function.", "lazy { \$1 }"),
            MemberDescriptor("check", "check(value: Boolean, lazyMessage: () -> Any)", "Unit", true, "Throws an IllegalStateException if the value is false.", "check(\$1)"),
            MemberDescriptor("require", "require(value: Boolean, lazyMessage: () -> Any)", "Unit", true, "Throws an IllegalArgumentException if the value is false.", "require(\$1)"),
            MemberDescriptor("error", "error(message: Any): Nothing", "Nothing", true, "Throws an IllegalStateException with the given message.", "error(\$1)")
        )
        globalFunctions.addAll(funcs)
    }

    private fun loadTypes() {
        // String
        val stringType = TypeDescriptor("String", "kotlin.String", "The String class represents character strings in Kotlin.")
        stringType.members.addAll(listOf(
            MemberDescriptor("length", "val length: Int", "Int", false, "Returns the length of this char sequence."),
            MemberDescriptor("isEmpty", "fun isEmpty(): Boolean", "Boolean", true, "Returns true if this char sequence is empty."),
            MemberDescriptor("isNotEmpty", "fun isNotEmpty(): Boolean", "Boolean", true, "Returns true if this char sequence is not empty."),
            MemberDescriptor("isBlank", "fun isBlank(): Boolean", "Boolean", true, "Returns true if this char sequence is empty or consists solely of whitespace."),
            MemberDescriptor("isNotBlank", "fun isNotBlank(): Boolean", "Boolean", true, "Returns true if this char sequence is not empty and contains non-whitespace."),
            MemberDescriptor("substring", "fun substring(startIndex: Int, endIndex: Int): String", "String", true, "Returns a substring of chars.", "substring(\$1, \$2)"),
            MemberDescriptor("split", "fun split(vararg delimiters: String): List<String>", "List<String>", true, "Splits this char sequence to a list of strings around occurrences of delimiters.", "split(\$1)"),
            MemberDescriptor("replace", "fun replace(oldValue: String, newValue: String): String", "String", true, "Returns a new string with all occurrences of oldValue replaced by newValue.", "replace(\$1, \$2)"),
            MemberDescriptor("trim", "fun trim(): String", "String", true, "Returns a substring with whitespace stripped from each end."),
            MemberDescriptor("lowercase", "fun lowercase(): String", "String", true, "Returns a copy of this string converted to lower case."),
            MemberDescriptor("uppercase", "fun uppercase(): String", "String", true, "Returns a copy of this string converted to upper case."),
            MemberDescriptor("startsWith", "fun startsWith(prefix: String): Boolean", "Boolean", true, "Returns true if this char sequence starts with the specified prefix.", "startsWith(\$1)"),
            MemberDescriptor("endsWith", "fun endsWith(suffix: String): Boolean", "Boolean", true, "Returns true if this char sequence ends with the specified suffix.", "endsWith(\$1)"),
            MemberDescriptor("contains", "fun contains(other: CharSequence): Boolean", "Boolean", true, "Returns true if this char sequence contains the specified sequence.", "contains(\$1)"),
            MemberDescriptor("toInt", "fun toInt(): Int", "Int", true, "Parses the string as an Int number."),
            MemberDescriptor("toIntOrNull", "fun toIntOrNull(): Int?", "Int?", true, "Parses the string as an Int number and returns the result or null."),
            MemberDescriptor("toDouble", "fun toDouble(): Double", "Double", true, "Parses the string as a Double number."),
            MemberDescriptor("toDoubleOrNull", "fun toDoubleOrNull(): Double?", "Double?", true, "Parses the string as a Double number or null."),
            MemberDescriptor("toLong", "fun toLong(): Long", "Long", true, "Parses the string as a Long number.")
        ))
        addType(stringType)

        // List
        val listType = TypeDescriptor("List", "kotlin.collections.List", "A generic ordered collection of elements.", isInterface = true)
        listType.members.addAll(listOf(
            MemberDescriptor("size", "val size: Int", "Int", false, "Returns the size of the collection."),
            MemberDescriptor("isEmpty", "fun isEmpty(): Boolean", "Boolean", true, "Returns true if the collection is empty."),
            MemberDescriptor("isNotEmpty", "fun isNotEmpty(): Boolean", "Boolean", true, "Returns true if the collection is not empty."),
            MemberDescriptor("get", "fun get(index: Int): T", "T", true, "Returns the element at the specified index.", "get(\$1)"),
            MemberDescriptor("first", "fun first(): T", "T", true, "Returns the first element."),
            MemberDescriptor("firstOrNull", "fun firstOrNull(): T?", "T?", true, "Returns the first element or null if empty."),
            MemberDescriptor("last", "fun last(): T", "T", true, "Returns the last element."),
            MemberDescriptor("lastOrNull", "fun lastOrNull(): T?", "T?", true, "Returns the last element or null if empty."),
            MemberDescriptor("filter", "fun filter(predicate: (T) -> Boolean): List<T>", "List<T>", true, "Returns a list containing only elements matching predicate.", "filter { \$1 }"),
            MemberDescriptor("map", "fun map(transform: (T) -> R): List<R>", "List<R>", true, "Returns a list containing results of applying transform.", "map { \$1 }"),
            MemberDescriptor("forEach", "fun forEach(action: (T) -> Unit): Unit", "Unit", true, "Performs the given action on each element.", "forEach { \${1:item} ->\n    \${2}\n}"),
            MemberDescriptor("sorted", "fun sorted(): List<T>", "List<T>", true, "Returns a list of all elements sorted according to natural sort order."),
            MemberDescriptor("sortedBy", "fun sortedBy(crossinline selector: (T) -> R?): List<T>", "List<T>", true, "Returns a list of elements sorted by selector.", "sortedBy { \$1 }"),
            MemberDescriptor("joinToString", "fun joinToString(separator: CharSequence = \", \"): String", "String", true, "Creates a string from all elements separated using separator.", "joinToString(\$1)"),
            MemberDescriptor("contains", "fun contains(element: T): Boolean", "Boolean", true, "Checks if the specified element is contained in this collection.", "contains(\$1)")
        ))
        addType(listType)

        // MutableList
        val mutableListType = TypeDescriptor("MutableList", "kotlin.collections.MutableList", "A generic ordered and mutable collection of elements.", isInterface = true)
        mutableListType.members.addAll(listType.members)
        mutableListType.members.addAll(listOf(
            MemberDescriptor("add", "fun add(element: T): Boolean", "Boolean", true, "Adds the specified element to the collection.", "add(\$1)"),
            MemberDescriptor("addAll", "fun addAll(elements: Collection<T>): Boolean", "Boolean", true, "Adds all elements of the specified collection.", "addAll(\$1)"),
            MemberDescriptor("remove", "fun remove(element: T): Boolean", "Boolean", true, "Removes a single instance of the specified element.", "remove(\$1)"),
            MemberDescriptor("removeAt", "fun removeAt(index: Int): T", "T", true, "Removes an element at the specified index.", "removeAt(\$1)"),
            MemberDescriptor("clear", "fun clear(): Unit", "Unit", true, "Removes all elements from this collection.")
        ))
        addType(mutableListType)

        // Map
        val mapType = TypeDescriptor("Map", "kotlin.collections.Map", "A collection that holds pairs of objects (keys and values).", isInterface = true)
        mapType.members.addAll(listOf(
            MemberDescriptor("size", "val size: Int", "Int", false, "Returns the number of key/value pairs in the map."),
            MemberDescriptor("keys", "val keys: Set<K>", "Set<K>", false, "Returns a read-only Set of all keys in this map."),
            MemberDescriptor("values", "val values: Collection<V>", "Collection<V>", false, "Returns a read-only Collection of all values in this map."),
            MemberDescriptor("entries", "val entries: Set<Map.Entry<K, V>>", "Set<Map.Entry<K, V>>", false, "Returns a read-only Set of all key/value pairs in this map."),
            MemberDescriptor("get", "fun get(key: K): V?", "V?", true, "Returns the value corresponding to the given key, or null.", "get(\$1)"),
            MemberDescriptor("getOrDefault", "fun getOrDefault(key: K, defaultValue: V): V", "V", true, "Returns the value for key or defaultValue.", "getOrDefault(\$1, \$2)"),
            MemberDescriptor("containsKey", "fun containsKey(key: K): Boolean", "Boolean", true, "Returns true if the map contains the specified key.", "containsKey(\$1)"),
            MemberDescriptor("containsValue", "fun containsValue(value: V): Boolean", "Boolean", true, "Returns true if the map maps one or more keys to the specified value.", "containsValue(\$1)")
        ))
        addType(mapType)

        // Int / Double / Math
        val intType = TypeDescriptor("Int", "kotlin.Int", "Represents a 32-bit signed integer.")
        intType.members.addAll(listOf(
            MemberDescriptor("toString", "fun toString(): String", "String", true, "Returns a string representation of the object."),
            MemberDescriptor("toDouble", "fun toDouble(): Double", "Double", true, "Converts this Int value to Double."),
            MemberDescriptor("toLong", "fun toLong(): Long", "Long", true, "Converts this Int value to Long."),
            MemberDescriptor("toFloat", "fun toFloat(): Float", "Float", true, "Converts this Int value to Float."),
            MemberDescriptor("coerceIn", "fun coerceIn(minimumValue: Int, maximumValue: Int): Int", "Int", true, "Ensures that this value lies in the specified range.", "coerceIn(\$1, \$2)"),
            MemberDescriptor("coerceAtLeast", "fun coerceAtLeast(minimumValue: Int): Int", "Int", true, "Ensures that this value is not less than the specified minimumValue.", "coerceAtLeast(\$1)"),
            MemberDescriptor("coerceAtMost", "fun coerceAtMost(maximumValue: Int): Int", "Int", true, "Ensures that this value is not greater than the specified maximumValue.", "coerceAtMost(\$1)")
        ))
        addType(intType)

        // Android View & Activity Essentials
        val contextType = TypeDescriptor("Context", "android.content.Context", "Interface to global information about an application environment.")
        contextType.members.addAll(listOf(
            MemberDescriptor("packageName", "val packageName: String", "String", false, "Return the name of this application's package."),
            MemberDescriptor("getString", "fun getString(resId: Int): String", "String", true, "Returns a localized string from the application's package's default string table.", "getString(\$1)"),
            MemberDescriptor("getSystemService", "fun getSystemService(name: String): Any?", "Any?", true, "Return the handle to a system-level service by name.", "getSystemService(\$1)"),
            MemberDescriptor("startActivity", "fun startActivity(intent: Intent): Unit", "Unit", true, "Launch a new activity.", "startActivity(\$1)")
        ))
        addType(contextType)

        val activityType = TypeDescriptor("AppCompatActivity", "androidx.appcompat.app.AppCompatActivity", "Base class for activities that use the support library action bar features.")
        activityType.members.addAll(contextType.members)
        activityType.members.addAll(listOf(
            MemberDescriptor("findViewById", "fun <T : View> findViewById(id: Int): T", "T", true, "Finds a view that was identified by the android:id XML attribute.", "findViewById(\$1)"),
            MemberDescriptor("setContentView", "fun setContentView(layoutResID: Int): Unit", "Unit", true, "Set the activity content from a layout resource.", "setContentView(\$1)"),
            MemberDescriptor("finish", "fun finish(): Unit", "Unit", true, "Call this when your activity is done and should be closed."),
            MemberDescriptor("runOnUiThread", "fun runOnUiThread(action: Runnable): Unit", "Unit", true, "Runs the specified action on the UI thread.", "runOnUiThread {\n    \$1\n}")
        ))
        addType(activityType)

        val viewType = TypeDescriptor("View", "android.view.View", "This class represents the basic building block for user interface components.")
        viewType.members.addAll(listOf(
            MemberDescriptor("id", "var id: Int", "Int", false, "Returns this view's identifier."),
            MemberDescriptor("visibility", "var visibility: Int", "Int", false, "Returns the visibility of this view (VISIBLE, INVISIBLE, GONE)."),
            MemberDescriptor("setOnClickListener", "fun setOnClickListener(l: View.OnClickListener?): Unit", "Unit", true, "Register a callback to be invoked when this view is clicked.", "setOnClickListener {\n    \$1\n}"),
            MemberDescriptor("context", "val context: Context", "Context", false, "Returns the context the view is running in.")
        ))
        addType(viewType)

        val toastType = TypeDescriptor("Toast", "android.widget.Toast", "A toast provides simple feedback about an operation in a small popup.")
        toastType.members.addAll(listOf(
            MemberDescriptor("makeText", "fun makeText(context: Context, text: CharSequence, duration: Int): Toast", "Toast", true, "Make a standard toast that just contains a text view.", "Toast.makeText(\${1:context}, \${2:\"Message\"}, Toast.LENGTH_SHORT).show()")
        ))
        addType(toastType)

        val logType = TypeDescriptor("Log", "android.util.Log", "API for sending log output.")
        logType.members.addAll(listOf(
            MemberDescriptor("d", "fun d(tag: String, msg: String): Int", "Int", true, "Send a DEBUG log message.", "Log.d(\${1:\"TAG\"}, \${2:\"Message\"})"),
            MemberDescriptor("e", "fun e(tag: String, msg: String): Int", "Int", true, "Send an ERROR log message.", "Log.e(\${1:\"TAG\"}, \${2:\"Error\"})"),
            MemberDescriptor("i", "fun i(tag: String, msg: String): Int", "Int", true, "Send an INFO log message.", "Log.i(\${1:\"TAG\"}, \${2:\"Info\"})")
        ))
        addType(logType)
    }

    private fun addType(desc: TypeDescriptor) {
        types[desc.simpleName] = desc
        types[desc.fqName] = desc
    }

    fun findType(name: String): TypeDescriptor? {
        return types[name]
    }

    fun getMembersForType(typeName: String): List<MemberDescriptor> {
        val cleanName = typeName.removeSuffix("?").trim()
        val desc = types[cleanName]
        if (desc != null) {
            return desc.members
        }
        // Universal members on Any
        return listOf(
            MemberDescriptor("toString", "fun toString(): String", "String", true, "Returns a string representation of the object."),
            MemberDescriptor("equals", "fun equals(other: Any?): Boolean", "Boolean", true, "Indicates whether some other object is equal to this one.", "equals(\$1)"),
            MemberDescriptor("hashCode", "fun hashCode(): Int", "Int", true, "Returns a hash code value for the object."),
            MemberDescriptor("let", "fun <T, R> T.let(block: (T) -> R): R", "R", true, "Calls the specified function block with this value as its argument.", "let { \${1:it} ->\n    \${2}\n}"),
            MemberDescriptor("also", "fun <T> T.also(block: (T) -> Unit): T", "T", true, "Calls the specified function block with this value as its argument and returns this value.", "also { \${1:it} ->\n    \${2}\n}"),
            MemberDescriptor("apply", "fun <T> T.apply(block: T.() -> Unit): T", "T", true, "Calls the specified function block with this value as its receiver.", "apply {\n    \${1}\n}"),
            MemberDescriptor("run", "fun <T, R> T.run(block: T.() -> R): R", "R", true, "Calls the specified function block with this value as its receiver.", "run {\n    \${1}\n}")
        )
    }
}
