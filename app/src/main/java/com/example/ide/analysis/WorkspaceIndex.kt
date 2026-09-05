package com.example.ide.analysis

import com.example.ide.model.Project
import com.example.ide.psi.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Workspace Symbol Indexer that tracks and parses all Kotlin files across project source directories.
 */
class WorkspaceIndex(private val parser: KtParser = KtParser()) {

    private val filePsiMap = ConcurrentHashMap<String, KtFilePsi>()
    private val globalClasses = ConcurrentHashMap<String, KtClassOrObject>()
    private val globalFunctions = ConcurrentHashMap<String, KtFunction>()
    private val globalProperties = ConcurrentHashMap<String, KtProperty>()

    fun indexProject(project: Project) {
        filePsiMap.clear()
        globalClasses.clear()
        globalFunctions.clear()
        globalProperties.clear()

        val ktFiles = project.getAllKotlinFiles()
        for (f in ktFiles) {
            indexFile(f)
        }
    }

    fun indexFile(file: File): KtFilePsi {
        val code = try {
            file.readText()
        } catch (e: Exception) {
            ""
        }
        val psi = parser.parse(code, file)
        updateIndexForPsi(file.absolutePath, psi)
        return psi
    }

    fun updateIndexForPsi(path: String, psi: KtFilePsi) {
        filePsiMap[path] = psi
        // Register top-level classes and functions
        val pkg = psi.packageDirective?.fqName ?: ""
        for (decl in psi.declarations) {
            when (decl) {
                is KtClassOrObject -> {
                    globalClasses[decl.name] = decl
                    if (pkg.isNotEmpty()) {
                        globalClasses["$pkg.${decl.name}"] = decl
                    }
                }
                is KtFunction -> {
                    globalFunctions[decl.name] = decl
                    if (pkg.isNotEmpty()) {
                        globalFunctions["$pkg.${decl.name}"] = decl
                    }
                }
                is KtProperty -> {
                    globalProperties[decl.name] = decl
                    if (pkg.isNotEmpty()) {
                        globalProperties["$pkg.${decl.name}"] = decl
                    }
                }
            }
        }
    }

    fun getPsiForFile(file: File): KtFilePsi? {
        return filePsiMap[file.absolutePath] ?: indexFile(file)
    }

    fun findClass(name: String): KtClassOrObject? = globalClasses[name]
    fun findFunction(name: String): KtFunction? = globalFunctions[name]
    fun findProperty(name: String): KtProperty? = globalProperties[name]

    fun getAllWorkspaceClasses(): List<KtClassOrObject> = globalClasses.values.distinct()
    fun getAllWorkspaceFunctions(): List<KtFunction> = globalFunctions.values.distinct()
    fun getAllWorkspaceProperties(): List<KtProperty> = globalProperties.values.distinct()
}
