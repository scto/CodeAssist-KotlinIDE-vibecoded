package com.example

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.databinding.ActivityMainBinding
import com.example.databinding.DialogNewFileBinding
import com.example.databinding.DialogNewProjectBinding
import com.example.databinding.DialogWorkspaceSettingsBinding
import com.example.ide.analysis.KotlinSemanticAnalyzer
import com.example.ide.completion.KotlinCompletionProvider
import com.example.ide.lang.KotlinLanguage
import com.example.ide.model.DiagnosticItem
import com.example.ide.model.FileItem
import com.example.ide.model.Project
import com.example.ide.model.PsiOutlineNode
import com.example.ide.psi.KtClassOrObject
import com.example.ide.psi.KtFunction
import com.example.ide.psi.KtParser
import com.example.ide.psi.KtProperty
import com.example.ide.runner.KotlinCodeRunner
import com.example.ide.ui.adapter.DiagnosticsAdapter
import com.example.ide.ui.adapter.EditorTabAdapter
import com.example.ide.ui.adapter.FileTreeAdapter
import com.example.ide.ui.adapter.PsiOutlineAdapter
import com.example.ide.ui.adapter.SymbolBarAdapter
import com.example.ide.workspace.WorkspaceManager
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var workspaceManager: WorkspaceManager
    private lateinit var semanticAnalyzer: KotlinSemanticAnalyzer
    private lateinit var codeRunner: KotlinCodeRunner
    private val ktParser = KtParser()

    private lateinit var tabAdapter: EditorTabAdapter
    private lateinit var fileTreeAdapter: FileTreeAdapter
    private lateinit var diagnosticsAdapter: DiagnosticsAdapter
    private lateinit var outlineAdapter: PsiOutlineAdapter
    private lateinit var symbolBarAdapter: SymbolBarAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var analyzeRunnable: Runnable? = null

    private var currentDiagnostics = listOf<DiagnosticItem>()
    private var currentOutlineNodes = listOf<PsiOutlineNode>()

    private var selectedBottomTab = BottomTab.PROBLEMS

    enum class BottomTab {
        PROBLEMS,
        CONSOLE,
        STRUCTURE,
        WORKSPACE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initWorkspace()
        initCodeEditor()
        initAdapters()
        initToolbarActions()
        initBottomPanel()
        initSearchAndReplace()
        initDrawerActions()

        refreshAll()
    }

    private fun initWorkspace() {
        workspaceManager = WorkspaceManager(this)
        semanticAnalyzer = KotlinSemanticAnalyzer(workspaceManager.workspaceIndex, ktParser)
        codeRunner = KotlinCodeRunner(semanticAnalyzer, ktParser)
    }

    private fun initCodeEditor() {
        val editor = binding.codeEditor
        editor.typefaceText = Typeface.MONOSPACE
        editor.setTextSize(14f)
        editor.isLineNumberEnabled = true
        editor.isWordwrap = false

        // Custom modern IDE dark color scheme
        val colorScheme = editor.colorScheme
        colorScheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, 0xFF1E1F22.toInt())
        colorScheme.setColor(EditorColorScheme.LINE_NUMBER_BACKGROUND, 0xFF232529.toInt())
        colorScheme.setColor(EditorColorScheme.LINE_NUMBER, 0xFF6F737A.toInt())
        colorScheme.setColor(EditorColorScheme.LINE_NUMBER_CURRENT, 0xFFE6E6E6.toInt())
        colorScheme.setColor(EditorColorScheme.CURRENT_LINE, 0xFF2B2D30.toInt())
        colorScheme.setColor(EditorColorScheme.SELECTION_INSERT, 0xFF7F52FF.toInt())
        colorScheme.setColor(EditorColorScheme.SELECTION_HANDLE, 0xFF7F52FF.toInt())
        colorScheme.setColor(EditorColorScheme.SELECTED_TEXT_BACKGROUND, 0x557F52FF.toInt())
        colorScheme.setColor(EditorColorScheme.TEXT_NORMAL, 0xFFE6E6E6.toInt())
        colorScheme.setColor(EditorColorScheme.KEYWORD, 0xFFCC7832.toInt())
        colorScheme.setColor(EditorColorScheme.LITERAL, 0xFF6A8759.toInt())
        colorScheme.setColor(EditorColorScheme.COMMENT, 0xFF808080.toInt())
        colorScheme.setColor(EditorColorScheme.OPERATOR, 0xFF9876AA.toInt())
        colorScheme.setColor(EditorColorScheme.IDENTIFIER_NAME, 0xFFFFC66D.toInt())

        val completionProvider = KotlinCompletionProvider(workspaceManager.workspaceIndex, ktParser)
        editor.setEditorLanguage(KotlinLanguage(completionProvider))

        // Configure Keyboard with Password flag as requested for coding without auto-correct interference
        editor.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS

        // Selection / Cursor change listener
        editor.subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
            val cursor = editor.cursor
            val line = cursor.leftLine + 1
            val col = cursor.leftColumn + 1
            binding.tvCursorPosition.text = "Ln $line, Col $col"
        }

        // Text change listener with debounced analysis
        editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
            val active = workspaceManager.activeTab ?: return@subscribeEvent
            active.isModified = true
            tabAdapter.notifyDataSetChanged()

            // Debounce analysis
            analyzeRunnable?.let { handler.removeCallbacks(it) }
            analyzeRunnable = Runnable {
                runBackgroundAnalysis()
            }
            handler.postDelayed(analyzeRunnable!!, 600)
        }
    }

    private fun initAdapters() {
        // 1. Editor Tabs Adapter
        tabAdapter = EditorTabAdapter(
            tabs = workspaceManager.openTabs,
            activeIndex = workspaceManager.activeTabIndex,
            onTabClick = { pos ->
                switchTab(pos)
            },
            onTabClose = { pos ->
                closeTab(pos)
            }
        )
        binding.rvEditorTabs.adapter = tabAdapter

        // 2. File Tree Adapter
        val rootNode = workspaceManager.buildFileTree()
        fileTreeAdapter = FileTreeAdapter(
            rootItem = rootNode,
            onFileClick = { item ->
                openFileInEditor(item.file)
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            },
            onNewFileUnder = { item ->
                showNewFileDialog(item.file)
            },
            onNewDirUnder = { item ->
                showNewFolderDialog(item.file)
            },
            onRenameItem = { item ->
                showRenameDialog(item.file)
            },
            onDeleteItem = { item ->
                showDeleteConfirmDialog(item.file)
            }
        )
        binding.rvFileTree.adapter = fileTreeAdapter

        // 3. Diagnostics Adapter
        diagnosticsAdapter = DiagnosticsAdapter(
            diagnostics = currentDiagnostics,
            onItemClick = { diag ->
                jumpToLocation(diag.line, diag.column)
            },
            onQuickFixClick = { diag ->
                applyQuickFix(diag)
            }
        )
        binding.rvDiagnostics.adapter = diagnosticsAdapter

        // 4. PSI Structure Outline Adapter
        outlineAdapter = PsiOutlineAdapter(
            nodes = currentOutlineNodes,
            onNodeClick = { node ->
                jumpToLocation(node.line, node.column)
            }
        )
        binding.rvStructureOutline.adapter = outlineAdapter

        // 5. Symbol Bar Adapter
        val symbols = listOf(
            "{", "}", "(", ")", "[", "]", ".", ":", ";", "=", ",", "\"", "'",
            "->", "?", "!", "&", "|", "Tab", "println", "fun", "val", "var", "class"
        )
        symbolBarAdapter = SymbolBarAdapter(symbols) { sym ->
            handleSymbolClick(sym)
        }
        binding.rvSymbolBar.adapter = symbolBarAdapter
    }

    private fun initToolbarActions() {
        binding.btnOpenDrawer.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.btnRunCode.setOnClickListener {
            runKotlinProgram()
        }

        binding.btnSave.setOnClickListener {
            saveCurrentFile()
        }

        binding.btnUndo.setOnClickListener {
            binding.codeEditor.undo()
        }

        binding.btnRedo.setOnClickListener {
            binding.codeEditor.redo()
        }

        binding.btnSearch.setOnClickListener {
            val isVisible = binding.searchBarContainer.visibility == View.VISIBLE
            binding.searchBarContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
            if (!isVisible) binding.etSearchQuery.requestFocus()
        }

        binding.btnDiagnosticsBadge.setOnClickListener {
            showBottomPanel(BottomTab.PROBLEMS)
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        binding.btnQuickNewFile.setOnClickListener {
            val project = workspaceManager.currentProject
            val targetDir = project?.sourceDirs?.firstOrNull() ?: project?.rootDir ?: workspaceManager.workspaceRoot
            showNewFileDialog(targetDir)
        }

        binding.btnEmptyOpenProject.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun initDrawerActions() {
        binding.btnSelectProject.setOnClickListener {
            showProjectSelectDialog()
        }

        binding.btnDrawerNewProject.setOnClickListener {
            showNewProjectDialog()
        }

        binding.btnDrawerNewFile.setOnClickListener {
            val project = workspaceManager.currentProject
            val targetDir = project?.sourceDirs?.firstOrNull() ?: project?.rootDir ?: workspaceManager.workspaceRoot
            showNewFileDialog(targetDir)
        }
    }

    private fun initBottomPanel() {
        binding.btnToggleBottomPanel.setOnClickListener {
            val isVisible = binding.bottomPanelContainer.visibility == View.VISIBLE
            binding.bottomPanelContainer.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.btnToggleBottomPanel.text = if (isVisible) "Panel ▲" else "Panel ▼"
        }

        binding.btnCloseBottomPanel.setOnClickListener {
            binding.bottomPanelContainer.visibility = View.GONE
            binding.btnToggleBottomPanel.text = "Panel ▲"
        }

        binding.tabPanelProblems.setOnClickListener { showBottomPanel(BottomTab.PROBLEMS) }
        binding.tabPanelConsole.setOnClickListener { showBottomPanel(BottomTab.CONSOLE) }
        binding.tabPanelStructure.setOnClickListener { showBottomPanel(BottomTab.STRUCTURE) }
        binding.tabPanelWorkspace.setOnClickListener { showBottomPanel(BottomTab.WORKSPACE) }
    }

    private fun showBottomPanel(tab: BottomTab) {
        selectedBottomTab = tab
        binding.bottomPanelContainer.visibility = View.VISIBLE
        binding.btnToggleBottomPanel.text = "Panel ▼"

        // Update tab header styles
        val primaryColor = getColor(R.color.ide_primary)
        val secondaryColor = getColor(R.color.ide_text_secondary)

        binding.tabPanelProblems.setTextColor(if (tab == BottomTab.PROBLEMS) primaryColor else secondaryColor)
        binding.tabPanelConsole.setTextColor(if (tab == BottomTab.CONSOLE) primaryColor else secondaryColor)
        binding.tabPanelStructure.setTextColor(if (tab == BottomTab.STRUCTURE) primaryColor else secondaryColor)
        binding.tabPanelWorkspace.setTextColor(if (tab == BottomTab.WORKSPACE) primaryColor else secondaryColor)

        // Show selected frame
        binding.rvDiagnostics.visibility = if (tab == BottomTab.PROBLEMS) View.VISIBLE else View.GONE
        binding.layoutConsoleView.visibility = if (tab == BottomTab.CONSOLE) View.VISIBLE else View.GONE
        binding.rvStructureOutline.visibility = if (tab == BottomTab.STRUCTURE) View.VISIBLE else View.GONE
        binding.layoutWorkspaceInfo.visibility = if (tab == BottomTab.WORKSPACE) View.VISIBLE else View.GONE
    }

    private fun initSearchAndReplace() {
        binding.btnCloseSearch.setOnClickListener {
            binding.searchBarContainer.visibility = View.GONE
        }

        binding.btnFindNext.setOnClickListener {
            val query = binding.etSearchQuery.text.toString()
            if (query.isNotEmpty()) {
                binding.codeEditor.searcher.search(query, io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(false, false))
                binding.codeEditor.searcher.gotoNext()
            }
        }

        binding.btnFindPrev.setOnClickListener {
            val query = binding.etSearchQuery.text.toString()
            if (query.isNotEmpty()) {
                binding.codeEditor.searcher.search(query, io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions(false, false))
                binding.codeEditor.searcher.gotoPrevious()
            }
        }

        binding.btnReplaceOne.setOnClickListener {
            val replaceWith = binding.etReplaceQuery.text.toString()
            binding.codeEditor.searcher.replaceThis(replaceWith)
        }

        binding.btnReplaceAll.setOnClickListener {
            val replaceWith = binding.etReplaceQuery.text.toString()
            binding.codeEditor.searcher.replaceAll(replaceWith)
        }
    }

    private fun handleSymbolClick(symbol: String) {
        val editor = binding.codeEditor
        when (symbol) {
            "Tab" -> editor.insertText("    ", 4)
            "println" -> editor.insertText("println(\"\")", 9)
            "fun" -> editor.insertText("fun name() {\n    \n}", 4)
            "val" -> editor.insertText("val name = ", 4)
            "var" -> editor.insertText("var name = ", 4)
            "class" -> editor.insertText("class Name {\n    \n}", 6)
            "{" -> editor.insertText("{\n    \n}", 2)
            "(" -> editor.insertText("()", 1)
            "[" -> editor.insertText("[]", 1)
            "\"" -> editor.insertText("\"\"", 1)
            "'" -> editor.insertText("''", 1)
            else -> editor.insertText(symbol, symbol.length)
        }
    }

    private fun refreshAll() {
        val project = workspaceManager.currentProject
        binding.tvProjectTitle.text = project?.name ?: "No Project"
        binding.tvDrawerProjectName.text = project?.name ?: "No Project"

        // Refresh File Explorer Tree
        val rootNode = workspaceManager.buildFileTree()
        fileTreeAdapter.updateRoot(rootNode)

        // Refresh Tabs
        tabAdapter.notifyDataSetChanged()
        tabAdapter.setActiveIndex(workspaceManager.activeTabIndex)

        val active = workspaceManager.activeTab
        if (active != null) {
            binding.emptyStateLayout.visibility = View.GONE
            binding.codeEditor.visibility = View.VISIBLE
            binding.tvActiveFilePath.text = active.file.relativeToOrSelf(project?.rootDir ?: active.file.parentFile).path
            binding.codeEditor.setText(active.content)
            runBackgroundAnalysis()
        } else {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.codeEditor.visibility = View.GONE
            binding.tvActiveFilePath.text = ""
        }

        updateWorkspaceInfoView()
    }

    private fun switchTab(position: Int) {
        val current = workspaceManager.activeTab
        if (current != null) {
            current.content = binding.codeEditor.text.toString()
        }

        workspaceManager.activeTabIndex = position
        tabAdapter.setActiveIndex(position)

        val newActive = workspaceManager.activeTab
        if (newActive != null) {
            binding.emptyStateLayout.visibility = View.GONE
            binding.codeEditor.visibility = View.VISIBLE
            binding.tvActiveFilePath.text = newActive.file.path
            binding.codeEditor.setText(newActive.content)
            runBackgroundAnalysis()
        }
    }

    private fun closeTab(position: Int) {
        workspaceManager.closeTab(position)
        tabAdapter.notifyDataSetChanged()
        tabAdapter.setActiveIndex(workspaceManager.activeTabIndex)

        val active = workspaceManager.activeTab
        if (active != null) {
            binding.emptyStateLayout.visibility = View.GONE
            binding.codeEditor.visibility = View.VISIBLE
            binding.tvActiveFilePath.text = active.file.path
            binding.codeEditor.setText(active.content)
            runBackgroundAnalysis()
        } else {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.codeEditor.visibility = View.GONE
            binding.tvActiveFilePath.text = ""
        }
    }

    private fun openFileInEditor(file: File) {
        val current = workspaceManager.activeTab
        if (current != null) {
            current.content = binding.codeEditor.text.toString()
        }

        workspaceManager.openFile(file)
        tabAdapter.notifyDataSetChanged()
        tabAdapter.setActiveIndex(workspaceManager.activeTabIndex)

        val active = workspaceManager.activeTab
        if (active != null) {
            binding.emptyStateLayout.visibility = View.GONE
            binding.codeEditor.visibility = View.VISIBLE
            binding.tvActiveFilePath.text = active.file.path
            binding.codeEditor.setText(active.content)
            runBackgroundAnalysis()
        }
    }

    private fun saveCurrentFile() {
        val active = workspaceManager.activeTab
        if (active != null) {
            val content = binding.codeEditor.text.toString()
            workspaceManager.saveActiveFile(content)
            tabAdapter.notifyDataSetChanged()
            Toast.makeText(this, "Saved ${active.title}", Toast.LENGTH_SHORT).show()
            runBackgroundAnalysis()
        }
    }

    private fun runBackgroundAnalysis() {
        val active = workspaceManager.activeTab ?: return
        val code = binding.codeEditor.text.toString()

        val analysisResult = semanticAnalyzer.analyze(code, active.file)
        currentDiagnostics = analysisResult.diagnostics

        // Update Diagnostics UI
        val errorsCount = currentDiagnostics.count { it.severity == DiagnosticItem.Severity.ERROR }
        val warningsCount = currentDiagnostics.count { it.severity == DiagnosticItem.Severity.WARNING }

        binding.tvDiagCount.text = "$errorsCount"
        binding.tabPanelProblems.text = "Problems ($errorsCount)"
        diagnosticsAdapter.updateDiagnostics(currentDiagnostics)

        if (errorsCount > 0) {
            binding.tvAnalysisStatusIndicator.text = "❌ $errorsCount Error(s)"
            binding.tvAnalysisStatusIndicator.setTextColor(getColor(R.color.status_error))
        } else if (warningsCount > 0) {
            binding.tvAnalysisStatusIndicator.text = "⚠️ $warningsCount Warning(s)"
            binding.tvAnalysisStatusIndicator.setTextColor(getColor(R.color.status_warning))
        } else {
            binding.tvAnalysisStatusIndicator.text = "✅ Analysis OK"
            binding.tvAnalysisStatusIndicator.setTextColor(getColor(R.color.status_success))
        }

        // Build Structure (PSI Outline)
        val outline = mutableListOf<PsiOutlineNode>()
        val psi = analysisResult.psi

        psi.packageDirective?.let {
            outline.add(PsiOutlineNode(PsiOutlineNode.OutlineKind.PACKAGE, it.fqName, "Package", it.line, it.column))
        }

        for (imp in psi.imports) {
            outline.add(PsiOutlineNode(PsiOutlineNode.OutlineKind.IMPORT, imp.importedFqName, "Import", imp.line, imp.column))
        }

        for (decl in psi.declarations) {
            when (decl) {
                is KtClassOrObject -> {
                    val kind = when (decl.kind) {
                        KtClassOrObject.ClassKind.DATA_CLASS -> PsiOutlineNode.OutlineKind.DATA_CLASS
                        KtClassOrObject.ClassKind.INTERFACE -> PsiOutlineNode.OutlineKind.INTERFACE
                        KtClassOrObject.ClassKind.OBJECT,
                        KtClassOrObject.ClassKind.COMPANION_OBJECT -> PsiOutlineNode.OutlineKind.OBJECT
                        KtClassOrObject.ClassKind.ENUM_CLASS -> PsiOutlineNode.OutlineKind.ENUM_CLASS
                        else -> PsiOutlineNode.OutlineKind.CLASS
                    }
                    outline.add(PsiOutlineNode(kind, decl.name, "${decl.kind.name.lowercase()} (${decl.functions.size} funs)", decl.line, decl.column))
                    for (f in decl.functions) {
                        outline.add(PsiOutlineNode(PsiOutlineNode.OutlineKind.FUNCTION, "  ${f.name}()", f.returnType, f.line, f.column))
                    }
                    for (p in decl.properties) {
                        outline.add(PsiOutlineNode(PsiOutlineNode.OutlineKind.PROPERTY, "  ${p.name}", p.type, p.line, p.column))
                    }
                }
                is KtFunction -> {
                    outline.add(PsiOutlineNode(PsiOutlineNode.OutlineKind.FUNCTION, "${decl.name}()", decl.returnType, decl.line, decl.column))
                }
                is KtProperty -> {
                    outline.add(PsiOutlineNode(PsiOutlineNode.OutlineKind.PROPERTY, decl.name, decl.type, decl.line, decl.column))
                }
            }
        }

        currentOutlineNodes = outline
        outlineAdapter.updateNodes(outline)
    }

    private fun runKotlinProgram() {
        val active = workspaceManager.activeTab
        if (active == null) {
            Toast.makeText(this, "Please open a Kotlin file first", Toast.LENGTH_SHORT).show()
            return
        }

        saveCurrentFile()
        showBottomPanel(BottomTab.CONSOLE)
        binding.tvConsoleOutput.text = "⚡ Compiling & Analyzing ${active.title}..."

        lifecycleScope.launch {
            val code = binding.codeEditor.text.toString()
            val result = codeRunner.runCode(code, active.file)
            binding.tvConsoleOutput.text = result.output
        }
    }

    private fun jumpToLocation(line: Int, column: Int) {
        val targetLine = (line - 1).coerceAtLeast(0)
        val targetCol = (column - 1).coerceAtLeast(0)
        binding.codeEditor.setSelection(targetLine, targetCol)
        binding.codeEditor.requestFocus()
    }

    private fun applyQuickFix(diag: DiagnosticItem) {
        if (diag.quickFixText == null) return
        val editor = binding.codeEditor
        when {
            diag.rule == "unresolved-reference" -> {
                // Add import or declaration
                val code = editor.text.toString()
                val pkgMatch = Regex("package [^\\n]+").find(code)
                val importLine = "\nimport ${diag.message.substringAfter("'").substringBefore("'")}\n"
                if (pkgMatch != null) {
                    val pos = pkgMatch.range.last + 1
                    val newCode = code.substring(0, pos) + importLine + code.substring(pos)
                    editor.setText(newCode)
                } else {
                    editor.setText(importLine + code)
                }
                Toast.makeText(this, "Applied quick fix: Added import", Toast.LENGTH_SHORT).show()
                runBackgroundAnalysis()
            }
            diag.rule == "syntax-error" -> {
                Toast.makeText(this, "Quick Fix: ${diag.quickFixText}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showNewProjectDialog() {
        val dialogBinding = DialogNewProjectBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_create, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etProjectName.text.toString().trim()
                if (name.isEmpty()) {
                    dialogBinding.etProjectName.error = "Project name is required"
                    return@setOnClickListener
                }

                val type = when (dialogBinding.rgProjectTemplates.checkedRadioButtonId) {
                    R.id.rbConsoleApp -> Project.ProjectType.KOTLIN_CONSOLE
                    R.id.rbAndroidApp -> Project.ProjectType.KOTLIN_ANDROID
                    R.id.rbAlgorithmsApp -> Project.ProjectType.KOTLIN_ALGORITHMS
                    R.id.rbCoroutinesApp -> Project.ProjectType.KOTLIN_COROUTINES
                    else -> Project.ProjectType.EMPTY
                }

                workspaceManager.createProject(name, type)
                refreshAll()
                dialog.dismiss()
                Toast.makeText(this, "Created Project $name", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showProjectSelectDialog() {
        val projects = workspaceManager.listProjects()
        val names = projects.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Switch Project")
            .setItems(names) { _, which ->
                workspaceManager.selectProject(projects[which])
                refreshAll()
            }
            .setPositiveButton("+ New Project") { _, _ ->
                showNewProjectDialog()
            }
            .setNegativeButton(R.string.btn_close, null)
            .show()
    }

    private fun showNewFileDialog(parentDir: File) {
        val dialogBinding = DialogNewFileBinding.inflate(LayoutInflater.from(this))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_create, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                var fileName = dialogBinding.etFileName.text.toString().trim()
                if (fileName.isEmpty()) {
                    dialogBinding.etFileName.error = "File name is required"
                    return@setOnClickListener
                }

                val checkedId = dialogBinding.rgFileKinds.checkedRadioButtonId
                if (checkedId == R.id.rbDirectory) {
                    workspaceManager.createDirectory(parentDir, fileName)
                } else {
                    if (!fileName.endsWith(".kt") && !fileName.endsWith(".kts")) {
                        fileName += ".kt"
                    }
                    val simpleName = fileName.removeSuffix(".kt")
                    val template = when (checkedId) {
                        R.id.rbKtClass -> "package com.example.app\n\nclass $simpleName {\n    \n}\n"
                        R.id.rbKtDataClass -> "package com.example.app\n\ndata class $simpleName(\n    val id: Int,\n    val name: String\n)\n"
                        R.id.rbKtInterface -> "package com.example.app\n\ninterface $simpleName {\n    fun execute()\n}\n"
                        R.id.rbKtObject -> "package com.example.app\n\nobject $simpleName {\n    \n}\n"
                        else -> "package com.example.app\n\nfun main() {\n    \n}\n"
                    }
                    val createdFile = workspaceManager.createFile(parentDir, fileName, template)
                    openFileInEditor(createdFile)
                }

                refreshAll()
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun showNewFolderDialog(parentDir: File) {
        val input = EditText(this).apply {
            hint = "Folder Name"
        }
        AlertDialog.Builder(this)
            .setTitle("Create New Folder")
            .setView(input)
            .setPositiveButton(R.string.btn_create) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    workspaceManager.createDirectory(parentDir, name)
                    refreshAll()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showRenameDialog(file: File) {
        val input = EditText(this).apply {
            setText(file.name)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_rename_title)
            .setView(input)
            .setPositiveButton(R.string.btn_apply) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    workspaceManager.renameFileOrDir(file, newName)
                    refreshAll()
                }
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showDeleteConfirmDialog(file: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.delete_confirm_msg, file.name))
            .setPositiveButton("Delete") { _, _ ->
                workspaceManager.deleteFileOrDir(file)
                refreshAll()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showSettingsDialog() {
        val dialogBinding = DialogWorkspaceSettingsBinding.inflate(LayoutInflater.from(this))
        val project = workspaceManager.currentProject

        dialogBinding.etSourceDirs.setText(
            project?.sourceDirs?.joinToString(", ") { it.relativeToOrSelf(project.rootDir).path } ?: "src/main/kotlin"
        )
        dialogBinding.etFontSize.setText("14")

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_apply) { _, _ ->
                val font = dialogBinding.etFontSize.text.toString().toFloatOrNull() ?: 14f
                binding.codeEditor.setTextSize(font)

                val dirsStr = dialogBinding.etSourceDirs.text.toString()
                if (project != null && dirsStr.isNotEmpty()) {
                    val rawPaths = dirsStr.split(",").map { it.trim() }
                    project.sourceDirs.clear()
                    for (p in rawPaths) {
                        val f = File(project.rootDir, p)
                        if (!f.exists()) f.mkdirs()
                        project.sourceDirs.add(f)
                    }
                    workspaceManager.workspaceIndex.indexProject(project)
                    runBackgroundAnalysis()
                    updateWorkspaceInfoView()
                }
                Toast.makeText(this, "Settings Applied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_close, null)
            .show()
    }

    private fun updateWorkspaceInfoView() {
        val project = workspaceManager.currentProject ?: return
        val ktFiles = project.getAllKotlinFiles()
        val classes = workspaceManager.workspaceIndex.getAllWorkspaceClasses()
        val functions = workspaceManager.workspaceIndex.getAllWorkspaceFunctions()

        val sb = StringBuilder()
        sb.append("📁 Project: ").append(project.name).append("\n")
        sb.append("📍 Root: ").append(project.rootDir.path).append("\n")
        sb.append("📦 Source Dirs: ").append(project.sourceDirs.joinToString { it.name }).append("\n")
        sb.append("📄 Total Kotlin Files: ").append(ktFiles.size).append("\n")
        sb.append("⚡ Indexed Classes: ").append(classes.size).append("\n")
        sb.append("⚡ Indexed Functions: ").append(functions.size).append("\n\n")
        sb.append("Included Libraries & SDKs:\n")
        sb.append(" • Kotlin Standard Library 2.0\n")
        sb.append(" • AndroidX AppCompat & Core SDK\n")
        sb.append(" • Sora CodeEditor 0.23.6\n")

        binding.tvWorkspaceDetails.text = sb.toString()
    }
}
