package com.example.completion.project

import com.example.completion.core.CompletionEngine
import com.example.completion.core.CompletionLogger
import com.example.completion.core.CompletionRequest
import com.example.completion.core.CompletionResult
import com.example.completion.index.jar.JarClassIndex
import com.example.completion.index.jar.JarIndexManager
import com.example.completion.index.jar.JarIndexer
import com.example.completion.index.jar.JarSymbolIndex
import com.example.completion.index.metadata.KotlinMetadataIndexer
import com.example.completion.index.metadata.KotlinMetadataReader
import com.example.completion.index.pkg.PackageIndex
import com.example.completion.index.repository.DefaultSymbolRepository
import com.example.completion.index.repository.SymbolRepository
import com.example.completion.index.source.SourcePsiIndexer
import com.example.completion.index.source.SourceSymbolIndex
import com.example.completion.providers.ClassCompletionProvider
import com.example.completion.providers.ImportCompletionProvider
import com.example.completion.providers.KeywordCompletionProvider
import com.example.completion.providers.LocalCompletionProvider
import com.example.completion.providers.MemberCompletionProvider
import com.example.completion.providers.PackageCompletionProvider
import com.example.completion.providers.SourceCompletionProvider
import com.example.completion.psi.DefaultKotlinPsiParser
import com.example.completion.psi.KotlinParser
import com.example.completion.psi.KotlinPsiManager
import com.example.completion.psi.PsiContextResolver
import com.example.completion.resolver.LocalSymbolResolver
import com.example.completion.resolver.MemberResolver
import com.example.completion.resolver.SimpleTypeResolver

import java.io.File
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future

/**
 * Master Kotlin Completion Facade uniting all indexes, resolvers, parsers, and providers.
 */
class KotlinCompletionFacade private constructor(builder: Builder) {

    val engine: CompletionEngine
    val parser: KotlinParser
    val psiManager: KotlinPsiManager
    val contextResolver: PsiContextResolver
    val sourceSymbolIndex: SourceSymbolIndex
    val sourcePsiIndexer: SourcePsiIndexer
    val jarClassIndex: JarClassIndex
    val jarSymbolIndex: JarSymbolIndex
    val jarIndexer: JarIndexer
    val jarIndexManager: JarIndexManager
    val metadataIndexer: KotlinMetadataIndexer
    val packageIndex: PackageIndex
    val symbolRepository: SymbolRepository
    val projectManager: ProjectManager
    val dependencyManager: DependencyManager
    val logger: CompletionLogger

    init {
        this.logger = builder.logger ?: CompletionLogger.AndroidLogger()
        this.engine = CompletionEngine(this.logger)
        this.parser = builder.parser ?: DefaultKotlinPsiParser()
        this.psiManager = KotlinPsiManager(this.parser)
        this.contextResolver = PsiContextResolver()

        this.sourceSymbolIndex = SourceSymbolIndex()
        this.sourcePsiIndexer = SourcePsiIndexer(this.parser, this.sourceSymbolIndex, this.logger)

        this.jarClassIndex = JarClassIndex()
        this.jarSymbolIndex = JarSymbolIndex()
        this.jarIndexer = JarIndexer(this.jarClassIndex, this.jarSymbolIndex, this.logger)
        this.jarIndexManager = JarIndexManager(this.jarIndexer, this.jarClassIndex, this.jarSymbolIndex, this.logger)

        this.metadataIndexer = KotlinMetadataIndexer(KotlinMetadataReader(this.logger), this.logger)
        this.packageIndex = PackageIndex()

        this.symbolRepository = DefaultSymbolRepository(
            this.sourceSymbolIndex,
            this.jarClassIndex,
            this.jarSymbolIndex,
            this.metadataIndexer,
            this.packageIndex
        )

        this.projectManager = ProjectManager(this.sourcePsiIndexer, this.logger)
        this.dependencyManager = DependencyManager(this.jarIndexManager, this.packageIndex, this.logger)

        // Populate common standard packages into package index
        populateStandardPackages()

        // Wire providers
        val localSymbolResolver = LocalSymbolResolver()
        val simpleTypeResolver = SimpleTypeResolver(localSymbolResolver)
        val memberResolver = MemberResolver(simpleTypeResolver, this.symbolRepository)

        this.engine.registerProvider(KeywordCompletionProvider(this.contextResolver))
        this.engine.registerProvider(LocalCompletionProvider(localSymbolResolver, this.psiManager, this.contextResolver))
        this.engine.registerProvider(MemberCompletionProvider(memberResolver, this.psiManager, this.contextResolver))
        this.engine.registerProvider(SourceCompletionProvider(this.symbolRepository, this.psiManager, this.contextResolver))
        this.engine.registerProvider(ClassCompletionProvider(this.symbolRepository, this.contextResolver))
        this.engine.registerProvider(ImportCompletionProvider(this.symbolRepository, this.contextResolver))
        this.engine.registerProvider(PackageCompletionProvider(this.symbolRepository, this.contextResolver))
    }

    private fun populateStandardPackages() {
        packageIndex.addPackage("kotlin")
        packageIndex.addPackage("kotlin.collections")
        packageIndex.addPackage("kotlin.io")
        packageIndex.addPackage("kotlin.text")
        packageIndex.addPackage("kotlin.math")
        packageIndex.addPackage("kotlin.coroutines")
        packageIndex.addPackage("java.util")
        packageIndex.addPackage("java.io")
        packageIndex.addPackage("java.lang")
        packageIndex.addPackage("android.os")
        packageIndex.addPackage("android.view")
        packageIndex.addPackage("android.widget")
        packageIndex.addPackage("android.content")
        packageIndex.addPackage("androidx.compose.runtime")
        packageIndex.addPackage("androidx.compose.material3")
        packageIndex.addPackage("androidx.compose.foundation")
        packageIndex.addPackage("androidx.compose.ui")

        packageIndex.addClass("kotlin.collections", "List")
        packageIndex.addClass("kotlin.collections", "Map")
        packageIndex.addClass("kotlin.collections", "Set")
        packageIndex.addClass("kotlin.collections", "ArrayList")
        packageIndex.addClass("kotlin.collections", "HashMap")
        packageIndex.addClass("java.util", "ArrayList")
        packageIndex.addClass("java.util", "HashMap")
        packageIndex.addClass("java.util", "HashSet")
        packageIndex.addClass("java.util", "Date")
        packageIndex.addClass("java.util", "UUID")
    }

    fun complete(request: CompletionRequest): CompletionResult {
        return engine.complete(request)
    }

    /** Scope für asynchrone Completion-Anfragen; wird in [shutdown] beendet. */
    private val asyncScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun completeAsync(request: CompletionRequest): CompletableFuture<CompletionResult> {
        return asyncScope.future { engine.completeAsync(request) }
    }

    fun updateActiveFileContent(filePath: String?, content: String?) {
        if (filePath != null && content != null) {
            psiManager.parse(filePath, content)
            sourcePsiIndexer.updateFile(File(filePath), content)
        }
    }

    fun shutdown() {
        asyncScope.cancel()
        engine.shutdown()
    }

    companion object {
        @JvmStatic
        fun createDefault(): KotlinCompletionFacade {
            return Builder().build()
        }
    }

    class Builder {
        internal var logger: CompletionLogger? = null
        internal var parser: KotlinParser? = null

        fun logger(logger: CompletionLogger?): Builder {
            this.logger = logger
            return this
        }

        fun parser(parser: KotlinParser?): Builder {
            this.parser = parser
            return this
        }

        fun build(): KotlinCompletionFacade {
            return KotlinCompletionFacade(this)
        }
    }
}
