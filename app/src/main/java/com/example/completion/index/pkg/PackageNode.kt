package com.example.completion.index.pkg

import java.util.concurrent.ConcurrentHashMap

/**
 * Tree node representing a package segment and its child subpackages and member classes.
 */
class PackageNode(name: String?, fullPackageName: String?) {

    val name: String = name ?: ""
    val fullPackageName: String = fullPackageName ?: ""
    private val subPackages: MutableMap<String, PackageNode> = ConcurrentHashMap()
    private val classes: MutableList<String> = ArrayList()

    fun getOrCreateSubPackage(segment: String): PackageNode {
        return subPackages.computeIfAbsent(segment) { s ->
            val newFull = if (fullPackageName.isEmpty()) s else "$fullPackageName.$s"
            PackageNode(s, newFull)
        }
    }

    fun getSubPackage(segment: String): PackageNode? {
        return subPackages[segment]
    }

    fun getSubPackages(): Map<String, PackageNode> {
        return subPackages
    }

    @Synchronized
    fun addClass(simpleClassName: String?) {
        if (simpleClassName != null && !classes.contains(simpleClassName)) {
            classes.add(simpleClassName)
        }
    }

    @Synchronized
    fun getClasses(): List<String> {
        return ArrayList(classes)
    }
}
