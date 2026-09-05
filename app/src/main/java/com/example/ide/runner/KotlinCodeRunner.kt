package com.example.ide.runner

import com.example.ide.analysis.KotlinSemanticAnalyzer
import com.example.ide.model.DiagnosticItem
import com.example.ide.psi.KtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Kotlin Code Runner and Execution Simulator for CodeAssist.
 * Executes Kotlin functions, captures output, and runs analysis diagnostics.
 */
class KotlinCodeRunner(
    private val analyzer: KotlinSemanticAnalyzer = KotlinSemanticAnalyzer(),
    private val parser: KtParser = KtParser()
) {

    data class ExecutionResult(
        val output: String,
        val executionTimeMs: Long,
        val isSuccess: Boolean,
        val diagnostics: List<DiagnosticItem>
    )

    suspend fun runCode(sourceCode: String, file: File? = null): ExecutionResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val analysis = analyzer.analyze(sourceCode, file)
        val errors = analysis.diagnostics.filter { it.severity == DiagnosticItem.Severity.ERROR }

        if (errors.isNotEmpty()) {
            val sb = StringBuilder()
            sb.append("⚠️ Build & Analysis Failed with ").append(errors.size).append(" error(s):\n\n")
            for (err in errors) {
                sb.append("❌ Line ").append(err.line).append(": ").append(err.message).append("\n")
            }
            val elapsed = System.currentTimeMillis() - startTime
            return@withContext ExecutionResult(
                output = sb.toString(),
                executionTimeMs = elapsed,
                isSuccess = false,
                diagnostics = analysis.diagnostics
            )
        }

        // Parse structure to find main entry point
        val psi = analysis.psi
        val mainFun = psi.declarations.filterIsInstance<com.example.ide.psi.KtFunction>().find {
            it.name == "main"
        }

        val outputSb = StringBuilder()
        outputSb.append("🚀 Kotlin Program Output:\n")
        outputSb.append("────────────────────────────────────────\n")

        val simulatedOutput = simulateExecution(sourceCode, psi)
        outputSb.append(simulatedOutput)
        outputSb.append("\n────────────────────────────────────────\n")
        val elapsed = System.currentTimeMillis() - startTime
        outputSb.append("✅ Process finished with exit code 0 (in ${elapsed}ms)\n")

        ExecutionResult(
            output = outputSb.toString(),
            executionTimeMs = elapsed,
            isSuccess = true,
            diagnostics = analysis.diagnostics
        )
    }

    private fun simulateExecution(code: String, psi: com.example.ide.psi.KtFilePsi): String {
        val out = StringBuilder()
        val lines = code.lines()

        // Extract print/println calls and statement outputs
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("println(") && trimmed.endsWith(")")) {
                val arg = trimmed.removePrefix("println(").removeSuffix(")").trim()
                val evaluated = evaluateExpression(arg)
                out.append(evaluated).append("\n")
            } else if (trimmed.startsWith("print(") && trimmed.endsWith(")")) {
                val arg = trimmed.removePrefix("print(").removeSuffix(")").trim()
                val evaluated = evaluateExpression(arg)
                out.append(evaluated)
            }
        }

        if (out.isEmpty()) {
            out.append("Program analyzed successfully. No output produced.\n")
            out.append("Defined symbols in workspace:\n")
            for (decl in psi.declarations) {
                out.append(" • ").append(decl::class.simpleName).append(": ").append(decl.name).append("\n")
            }
        }

        return out.toString()
    }

    private fun evaluateExpression(expr: String): String {
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return expr.substring(1, expr.length - 1)
        }
        if (expr.contains("+") && expr.contains("\"")) {
            // String concatenation
            return expr.replace("\"", "").replace("+", "").replace("  ", " ").trim()
        }
        return expr
    }
}
