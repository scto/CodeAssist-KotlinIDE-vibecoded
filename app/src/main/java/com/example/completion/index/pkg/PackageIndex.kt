package com.example.completion.index.pkg

/**
 * Hierarchical package tree index supporting auto-completion for import and package statements.
 */
class PackageIndex {

    private val root = PackageNode("", "")

    fun addPackage(fullPackageName: String?) {
        if (fullPackageName == null || fullPackageName.trim().isEmpty()) return
        val segments = fullPackageName.trim().split(".")
        var current = root
        for (seg in segments) {
            if (seg.isNotEmpty()) {
                current = current.getOrCreateSubPackage(seg)
            }
        }
    }

    fun addClass(fullPackageName: String?, simpleClassName: String?) {
        val pkgName = fullPackageName ?: ""
        val segments = pkgName.trim().split(".")
        var current = root
        for (seg in segments) {
            if (seg.isNotEmpty()) {
                current = current.getOrCreateSubPackage(seg)
            }
        }
        current.addClass(simpleClassName)
    }

    fun getMatchingPackages(prefix: String?): List<String> {
        val cleanPrefix = (prefix ?: "").trim()

        val lastDot = cleanPrefix.lastIndexOf('.')
        val parentPkg = if (lastDot != -1) cleanPrefix.substring(0, lastDot) else ""
        val subPrefix = if (lastDot != -1) cleanPrefix.substring(lastDot + 1) else cleanPrefix

        val node = resolveNode(parentPkg) ?: return emptyList()

        val results = ArrayList<String>()
        for (sub in node.getSubPackages().values) {
            if (sub.name.startsWith(subPrefix)) {
                results.add(sub.fullPackageName)
            }
        }
        return results
    }

    fun getClassesInPackage(fullPackageName: String?, classPrefix: String?): List<String> {
        val node = resolveNode(fullPackageName) ?: return emptyList()

        val results = ArrayList<String>()
        val lowerPrefix = classPrefix?.lowercase() ?: ""
        for (cls in node.getClasses()) {
            if (cls.lowercase().startsWith(lowerPrefix)) {
                results.add(cls)
            }
        }
        return results
    }

    private fun resolveNode(fullPackageName: String?): PackageNode? {
        if (fullPackageName == null || fullPackageName.isEmpty()) {
            return root
        }
        val segments = fullPackageName.split(".")
        var current: PackageNode? = root
        for (seg in segments) {
            if (seg.isNotEmpty()) {
                current = current?.getSubPackage(seg)
                if (current == null) return null
            }
        }
        return current
    }
}
