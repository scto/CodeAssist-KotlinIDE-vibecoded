package com.example.ide.workspace

import android.content.Context
import com.example.ide.analysis.WorkspaceIndex
import com.example.ide.model.EditorTab
import com.example.ide.model.FileItem
import com.example.ide.model.Project
import java.io.File

/**
 * Workspace Manager that manages projects, templates, source directories, and file operations.
 */
class WorkspaceManager(
    private val context: Context,
    val workspaceIndex: WorkspaceIndex = WorkspaceIndex()
) {

    val workspaceRoot: File by lazy {
        val root = File(context.filesDir, "workspace")
        if (!root.exists()) root.mkdirs()
        root
    }

    var currentProject: Project? = null
        private set

    val openTabs = mutableListOf<EditorTab>()
    var activeTabIndex: Int = -1

    val activeTab: EditorTab?
        get() = if (activeTabIndex in openTabs.indices) openTabs[activeTabIndex] else null

    init {
        initializeDefaultWorkspace()
    }

    private fun initializeDefaultWorkspace() {
        val existingProjects = listProjects()
        if (existingProjects.isEmpty()) {
            createProject("MyKotlinProject", Project.ProjectType.KOTLIN_CONSOLE)
        } else {
            selectProject(existingProjects.first())
        }
    }

    fun listProjects(): List<Project> {
        val list = mutableListOf<Project>()
        val dirs = workspaceRoot.listFiles { f -> f.isDirectory } ?: emptyArray()
        for (d in dirs) {
            val srcDir = File(d, "src/main/kotlin")
            val sourceDirs = if (srcDir.exists()) mutableListOf(srcDir) else mutableListOf(d)
            list.add(Project(name = d.name, rootDir = d, sourceDirs = sourceDirs))
        }
        return list
    }

    fun selectProject(project: Project) {
        currentProject = project
        workspaceIndex.indexProject(project)

        // If no tabs open, open default Main.kt or first kotlin file
        if (openTabs.isEmpty()) {
            val ktFiles = project.getAllKotlinFiles()
            val target = ktFiles.find { it.name == "Main.kt" } ?: ktFiles.firstOrNull()
            if (target != null) {
                openFile(target)
            }
        }
    }

    fun createProject(name: String, type: Project.ProjectType): Project {
        val projectDir = File(workspaceRoot, name)
        if (!projectDir.exists()) projectDir.mkdirs()

        val srcMainKt = File(projectDir, "src/main/kotlin")
        srcMainKt.mkdirs()

        when (type) {
            Project.ProjectType.KOTLIN_CONSOLE -> {
                val mainKt = File(srcMainKt, "Main.kt")
                mainKt.writeText(
                    """
                    package com.example.app

                    fun main(args: Array<String>) {
                        println("Hello, Kotlin on CodeAssist IDE!")
                        
                        val numbers = listOf(1, 2, 3, 4, 5)
                        val squares = numbers.map { it * it }
                        println("Computed squares: ${'$'}squares")
                        
                        val user = User(id = 101, name = "Kotlin Developer")
                        user.greet()
                    }

                    data class User(
                        val id: Int,
                        val name: String,
                        val role: String = "Engineer"
                    ) {
                        fun greet() {
                            println("Welcome ${'$'}name! Your role is ${'$'}role.")
                        }
                    }
                    """.trimIndent()
                )

                val readme = File(projectDir, "README.md")
                readme.writeText("# $name\n\nKotlin Console Application developed with CodeAssist IDE.\n")
            }

            Project.ProjectType.KOTLIN_ANDROID -> {
                val mainActivity = File(srcMainKt, "MainActivity.kt")
                mainActivity.writeText(
                    """
                    package com.example.app

                    import androidx.appcompat.app.AppCompatActivity
                    import android.os.Bundle
                    import android.widget.Toast
                    import android.util.Log

                    class MainActivity : AppCompatActivity() {

                        private val tag = "MainActivity"

                        override fun onCreate(savedInstanceState: Bundle?) {
                            super.onCreate(savedInstanceState)
                            Log.i(tag, "Activity created successfully")
                            Toast.makeText(this, "Welcome to Kotlin Android!", Toast.LENGTH_SHORT).show()
                        }

                        fun computeTotal(price: Double, tax: Double): Double {
                            return price * (1.0 + tax)
                        }
                    }
                    """.trimIndent()
                )
            }

            Project.ProjectType.KOTLIN_ALGORITHMS -> {
                val algos = File(srcMainKt, "Algorithms.kt")
                algos.writeText(
                    """
                    package com.example.algorithms

                    fun main() {
                        val input = listOf(64, 34, 25, 12, 22, 11, 90)
                        println("Original: ${'$'}input")
                        println("QuickSorted: ${'$'}{quickSort(input)}")
                        println("Fibonacci(10): ${'$'}{fibonacci(10)}")
                    }

                    fun <T : Comparable<T>> quickSort(list: List<T>): List<T> {
                        if (list.size <= 1) return list
                        val pivot = list[list.size / 2]
                        val equal = list.filter { it == pivot }
                        val less = list.filter { it < pivot }
                        val greater = list.filter { it > pivot }
                        return quickSort(less) + equal + quickSort(greater)
                    }

                    fun fibonacci(n: Int): Long {
                        var a = 0L
                        var b = 1L
                        repeat(n) {
                            val temp = a + b
                            a = b
                            b = temp
                        }
                        return a
                    }
                    """.trimIndent()
                )
            }

            Project.ProjectType.KOTLIN_COROUTINES -> {
                val coroutinesFile = File(srcMainKt, "AsyncPipelines.kt")
                coroutinesFile.writeText(
                    """
                    package com.example.async

                    fun main() {
                        println("Starting async pipeline simulation...")
                        val emitter = StreamProcessor(tag = "SensorData")
                        emitter.processData(listOf("Temperature: 24C", "Humidity: 55%", "Pressure: 1013hPa"))
                    }

                    class StreamProcessor(val tag: String) {
                        fun processData(entries: List<String>) {
                            entries.forEachIndexed { index, item ->
                                val formatted = "[${'$'}tag # ${'$'}{index + 1}] ${'$'}item"
                                println(formatted)
                            }
                        }
                    }
                    """.trimIndent()
                )
            }

            Project.ProjectType.EMPTY -> {
                val emptyKt = File(srcMainKt, "Main.kt")
                emptyKt.writeText("package com.example.app\n\nfun main() {\n    println(\"Ready\")\n}\n")
            }
        }

        val project = Project(name = name, rootDir = projectDir, sourceDirs = mutableListOf(srcMainKt), type = type)
        selectProject(project)
        return project
    }

    fun openFile(file: File): EditorTab {
        val existingIndex = openTabs.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex != -1) {
            activeTabIndex = existingIndex
            return openTabs[existingIndex]
        }

        val content = try {
            file.readText()
        } catch (e: Exception) {
            ""
        }

        val tab = EditorTab(file = file, title = file.name, content = content)
        openTabs.add(tab)
        activeTabIndex = openTabs.lastIndex
        return tab
    }

    fun closeTab(index: Int) {
        if (index in openTabs.indices) {
            openTabs.removeAt(index)
            if (activeTabIndex >= openTabs.size) {
                activeTabIndex = openTabs.size - 1
            }
        }
    }

    fun saveActiveFile(newContent: String) {
        val tab = activeTab ?: return
        try {
            tab.file.writeText(newContent)
            tab.content = newContent
            tab.isModified = false
            if (tab.file.extension == "kt") {
                workspaceIndex.indexFile(tab.file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createFile(parentDir: File, fileName: String, templateContent: String = ""): File {
        val newFile = File(parentDir, fileName)
        if (!newFile.exists()) {
            newFile.createNewFile()
            if (templateContent.isNotEmpty()) {
                newFile.writeText(templateContent)
            } else if (fileName.endsWith(".kt")) {
                val simpleName = fileName.removeSuffix(".kt")
                newFile.writeText("package com.example.app\n\nclass $simpleName {\n    \n}\n")
            }
        }
        if (newFile.extension == "kt") {
            workspaceIndex.indexFile(newFile)
        }
        return newFile
    }

    fun createDirectory(parentDir: File, dirName: String): File {
        val newDir = File(parentDir, dirName)
        if (!newDir.exists()) {
            newDir.mkdirs()
        }
        return newDir
    }

    fun deleteFileOrDir(file: File): Boolean {
        // Close tabs if open
        openTabs.removeAll { it.file.absolutePath.startsWith(file.absolutePath) }
        if (activeTabIndex >= openTabs.size) {
            activeTabIndex = openTabs.size - 1
        }
        val success = file.deleteRecursively()
        if (currentProject != null) {
            workspaceIndex.indexProject(currentProject!!)
        }
        return success
    }

    fun renameFileOrDir(file: File, newName: String): File? {
        val target = File(file.parentFile, newName)
        if (file.renameTo(target)) {
            // Update tabs
            for (tab in openTabs) {
                if (tab.file.absolutePath == file.absolutePath) {
                    // Update tab
                    val idx = openTabs.indexOf(tab)
                    openTabs[idx] = EditorTab(file = target, title = target.name, content = tab.content)
                }
            }
            if (currentProject != null) {
                workspaceIndex.indexProject(currentProject!!)
            }
            return target
        }
        return null
    }

    fun buildFileTree(dir: File = currentProject?.rootDir ?: workspaceRoot, level: Int = 0): FileItem {
        val rootItem = FileItem(file = dir, name = dir.name, isDirectory = true, level = level, isExpanded = true)
        val files = dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()

        for (f in files) {
            if (f.name.startsWith(".")) continue // Skip hidden
            if (f.isDirectory) {
                val subTree = buildFileTree(f, level + 1)
                rootItem.children.add(subTree)
            } else {
                rootItem.children.add(FileItem(file = f, name = f.name, isDirectory = false, level = level + 1))
            }
        }
        return rootItem
    }
}
