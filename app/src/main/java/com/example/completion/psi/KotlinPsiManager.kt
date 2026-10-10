package com.example.completion.psi

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages parsed Kotlin files and coordinates caching and invalidation of PSI models.
 */
class KotlinPsiManager @JvmOverloads constructor(parser: KotlinParser? = null) {

    private val parser: KotlinParser = parser ?: DefaultKotlinPsiParser()
    private val fileCache: MutableMap<String, ParsedKotlinFile> = ConcurrentHashMap()

    fun parse(fileName: String?, source: String?): ParsedKotlinFile {
        val parsed = parser.parse(fileName, source)
        if (fileName != null) {
            fileCache[fileName] = parsed
        }
        return parsed
    }

    fun getCached(fileName: String?): ParsedKotlinFile? {
        return if (fileName != null) fileCache[fileName] else null
    }

    fun invalidate(fileName: String?) {
        if (fileName != null) {
            fileCache.remove(fileName)
        }
    }

    fun clear() {
        fileCache.clear()
    }
}
