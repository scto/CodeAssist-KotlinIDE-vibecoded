package com.example.completion.index.jar

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.symbol.Symbol

import java.io.DataInputStream
import java.io.InputStream
import java.util.Collections

/**
 * High-performance, zero-dependency Java/Kotlin .class bytecode parser.
 * Extracts classes, methods, fields, and Kotlin Metadata without using ClassLoader or Reflection.
 */
object ClassFileReader {

    class ParsedClassInfo {
        var className: String = ""
        var superClassName: String = ""
        var interfaces: MutableList<String> = ArrayList()
        var accessFlags: Int = 0
        var isInterface: Boolean = false
        var isEnum: Boolean = false
        var hasKotlinMetadata: Boolean = false
        var declaredMembers: MutableList<Symbol> = ArrayList()
    }

    @JvmStatic
    @Throws(Exception::class)
    fun parse(inStream: InputStream?): ParsedClassInfo? {
        if (inStream == null) return null
        val dis = DataInputStream(inStream)

        val magic = dis.readInt()
        if (magic != -0x35014542) { // 0xCAFEBABE in signed int format
            throw IllegalArgumentException("Not a valid JVM class file (invalid magic: " + Integer.toHexString(magic) + ")")
        }

        val minorVersion = dis.readUnsignedShort()
        val majorVersion = dis.readUnsignedShort()

        val constantPoolCount = dis.readUnsignedShort()
        val constantPool = arrayOfNulls<Any>(constantPoolCount)
        val classRefNameIndices = IntArray(constantPoolCount)

        // Parse constant pool
        var i = 1
        while (i < constantPoolCount) {
            val tag = dis.readUnsignedByte()
            switchTag(tag, dis, constantPool, classRefNameIndices, i)
            if (tag == 5 || tag == 6) {
                i++ // Long/Double take two entries
            }
            i++
        }

        val info = ParsedClassInfo()
        info.accessFlags = dis.readUnsignedShort()
        info.isInterface = (info.accessFlags and 0x0200) != 0
        info.isEnum = (info.accessFlags and 0x4000) != 0

        val thisClassIdx = dis.readUnsignedShort()
        if (thisClassIdx in 1 until constantPoolCount) {
            val nameIdx = classRefNameIndices[thisClassIdx]
            if (nameIdx in 1 until constantPoolCount && constantPool[nameIdx] is String) {
                info.className = (constantPool[nameIdx] as String).replace('/', '.')
            }
        }

        val superClassIdx = dis.readUnsignedShort()
        if (superClassIdx in 1 until constantPoolCount) {
            val nameIdx = classRefNameIndices[superClassIdx]
            if (nameIdx in 1 until constantPoolCount && constantPool[nameIdx] is String) {
                info.superClassName = (constantPool[nameIdx] as String).replace('/', '.')
            }
        }

        val interfacesCount = dis.readUnsignedShort()
        for (j in 0 until interfacesCount) {
            val ifaceIdx = dis.readUnsignedShort()
            if (ifaceIdx in 1 until constantPoolCount) {
                val nameIdx = classRefNameIndices[ifaceIdx]
                if (nameIdx in 1 until constantPoolCount && constantPool[nameIdx] is String) {
                    info.interfaces.add((constantPool[nameIdx] as String).replace('/', '.'))
                }
            }
        }

        // Fields
        val fieldsCount = dis.readUnsignedShort()
        for (j in 0 until fieldsCount) {
            val fieldAccess = dis.readUnsignedShort()
            val nameIdx = dis.readUnsignedShort()
            val descIdx = dis.readUnsignedShort()

            val fieldName = if (nameIdx in 1 until constantPoolCount && constantPool[nameIdx] is String) constantPool[nameIdx] as String else ""
            val fieldDesc = if (descIdx in 1 until constantPoolCount && constantPool[descIdx] is String) constantPool[descIdx] as String else ""

            val attrCount = dis.readUnsignedShort()
            for (a in 0 until attrCount) {
                dis.readUnsignedShort() // attr name
                val attrLen = dis.readInt()
                dis.skipBytes(attrLen)
            }

            if (fieldName.isNotEmpty() && !fieldName.contains("$")) {
                info.declaredMembers.add(
                    Symbol.builder()
                        .name(fieldName)
                        .qualifiedName("${info.className}.$fieldName")
                        .kind(SymbolKind.PROPERTY)
                        .origin(SymbolOrigin.JAR)
                        .containerName(info.className)
                        .returnType(parseDescriptorType(fieldDesc))
                        .isStatic((fieldAccess and 0x0008) != 0)
                        .build()
                )
            }
        }

        // Methods
        val methodsCount = dis.readUnsignedShort()
        for (j in 0 until methodsCount) {
            val methodAccess = dis.readUnsignedShort()
            val nameIdx = dis.readUnsignedShort()
            val descIdx = dis.readUnsignedShort()

            val methodName = if (nameIdx in 1 until constantPoolCount && constantPool[nameIdx] is String) constantPool[nameIdx] as String else ""
            val methodDesc = if (descIdx in 1 until constantPoolCount && constantPool[descIdx] is String) constantPool[descIdx] as String else ""

            val attrCount = dis.readUnsignedShort()
            for (a in 0 until attrCount) {
                dis.readUnsignedShort() // attr name
                val attrLen = dis.readInt()
                dis.skipBytes(attrLen)
            }

            if (methodName.isNotEmpty() && methodName != "<clinit>" && !methodName.contains("$")) {
                val returnType = parseMethodReturnType(methodDesc)
                val params = parseMethodParameters(methodDesc)
                info.declaredMembers.add(
                    Symbol.builder()
                        .name(if (methodName == "<init>") getSimpleName(info.className) else methodName)
                        .qualifiedName("${info.className}.$methodName")
                        .kind(if (methodName == "<init>") SymbolKind.CLASS else SymbolKind.FUNCTION)
                        .origin(SymbolOrigin.JAR)
                        .containerName(info.className)
                        .returnType(if (methodName == "<init>") info.className else returnType)
                        .parameters(params)
                        .isStatic((methodAccess and 0x0008) != 0)
                        .build()
                )
            }
        }

        // Check class attributes for Kotlin Metadata
        val classAttrCount = dis.readUnsignedShort()
        for (a in 0 until classAttrCount) {
            val attrNameIdx = dis.readUnsignedShort()
            val attrLen = dis.readInt()
            val attrName = if (attrNameIdx in 1 until constantPoolCount && constantPool[attrNameIdx] is String) constantPool[attrNameIdx] as String else ""

            if ("RuntimeVisibleAnnotations" == attrName) {
                // Peek for kotlin/Metadata in the constant pool
                for (c in 1 until constantPoolCount) {
                    if (constantPool[c] is String && (constantPool[c] as String).contains("Lkotlin/Metadata;")) {
                        info.hasKotlinMetadata = true
                        break
                    }
                }
            }
            dis.skipBytes(attrLen)
        }

        return info
    }

    private fun switchTag(tag: Int, dis: DataInputStream, constantPool: Array<Any?>, classRefNameIndices: IntArray, i: Int) {
        when (tag) {
            1 -> constantPool[i] = dis.readUTF()
            3, 4 -> dis.readInt()
            5, 6 -> dis.readLong()
            7 -> classRefNameIndices[i] = dis.readUnsignedShort()
            8, 16, 19, 20 -> dis.readUnsignedShort()
            9, 10, 11, 12, 17, 18 -> dis.readInt()
            15 -> {
                dis.readByte()
                dis.readUnsignedShort()
            }
        }
    }

    private fun parseDescriptorType(desc: String?): String {
        if (desc == null || desc.isEmpty()) return "Any"
        return when (desc[0]) {
            'V' -> "Unit"
            'Z' -> "Boolean"
            'B' -> "Byte"
            'C' -> "Char"
            'S' -> "Short"
            'I' -> "Int"
            'J' -> "Long"
            'F' -> "Float"
            'D' -> "Double"
            'L' -> {
                val semi = desc.indexOf(';')
                if (semi > 1) {
                    val full = desc.substring(1, semi).replace('/', '.')
                    getSimpleName(full)
                } else "Any"
            }
            '[' -> "Array<" + parseDescriptorType(desc.substring(1)) + ">"
            else -> "Any"
        }
    }

    private fun parseMethodReturnType(desc: String?): String {
        if (desc == null) return "Unit"
        val closeParen = desc.indexOf(')')
        if (closeParen != -1 && closeParen + 1 < desc.length) {
            return parseDescriptorType(desc.substring(closeParen + 1))
        }
        return "Unit"
    }

    private fun parseMethodParameters(desc: String?): List<String> {
        if (desc == null || !desc.startsWith("(")) return Collections.emptyList()
        val closeParen = desc.indexOf(')')
        if (closeParen == -1) return Collections.emptyList()

        val list = ArrayList<String>()
        var i = 1
        while (i < closeParen) {
            val c = desc[i]
            if (c == 'L') {
                val semi = desc.indexOf(';', i)
                if (semi != -1) {
                    val full = desc.substring(i + 1, semi).replace('/', '.')
                    list.add(getSimpleName(full))
                    i = semi + 1
                } else {
                    break
                }
            } else if (c == '[') {
                i++
                if (i < closeParen && desc[i] == 'L') {
                    val semi = desc.indexOf(';', i)
                    if (semi != -1) {
                        val full = desc.substring(i + 1, semi).replace('/', '.')
                        list.add("Array<" + getSimpleName(full) + ">")
                        i = semi + 1
                    }
                } else if (i < closeParen) {
                    list.add("Array<" + parseDescriptorType(desc[i].toString()) + ">")
                    i++
                }
            } else {
                list.add(parseDescriptorType(c.toString()))
                i++
            }
        }
        return list
    }

    @JvmStatic
    fun getSimpleName(qualifiedName: String?): String {
        if (qualifiedName == null) return ""
        val dot = qualifiedName.lastIndexOf('.')
        return if (dot != -1) qualifiedName.substring(dot + 1) else qualifiedName
    }
}
