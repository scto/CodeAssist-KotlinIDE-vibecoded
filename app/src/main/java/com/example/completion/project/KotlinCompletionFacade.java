package com.example.completion.project;

import com.example.completion.core.CompletionEngine;
import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionLogger;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.CompletionResult;
import com.example.completion.index.jar.JarClassIndex;
import com.example.completion.index.jar.JarIndexManager;
import com.example.completion.index.jar.JarIndexer;
import com.example.completion.index.jar.JarSymbolIndex;
import com.example.completion.index.metadata.KotlinMetadataIndexer;
import com.example.completion.index.metadata.KotlinMetadataReader;
import com.example.completion.index.pkg.PackageIndex;
import com.example.completion.index.repository.DefaultSymbolRepository;
import com.example.completion.index.repository.SymbolRepository;
import com.example.completion.index.source.SourcePsiIndexer;
import com.example.completion.index.source.SourceSymbolIndex;
import com.example.completion.providers.ClassCompletionProvider;
import com.example.completion.providers.ImportCompletionProvider;
import com.example.completion.providers.KeywordCompletionProvider;
import com.example.completion.providers.LocalCompletionProvider;
import com.example.completion.providers.MemberCompletionProvider;
import com.example.completion.providers.PackageCompletionProvider;
import com.example.completion.providers.SourceCompletionProvider;
import com.example.completion.psi.DefaultKotlinPsiParser;
import com.example.completion.psi.KotlinParser;
import com.example.completion.psi.KotlinPsiManager;
import com.example.completion.psi.PsiContextResolver;
import com.example.completion.resolver.LocalSymbolResolver;
import com.example.completion.resolver.MemberResolver;
import com.example.completion.resolver.SimpleTypeResolver;

import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Master Kotlin Completion Facade uniting all indexes, resolvers, parsers, and providers.
 */
public class KotlinCompletionFacade {

    private final CompletionEngine engine;
    private final KotlinParser parser;
    private final KotlinPsiManager psiManager;
    private final PsiContextResolver contextResolver;
    private final SourceSymbolIndex sourceSymbolIndex;
    private final SourcePsiIndexer sourcePsiIndexer;
    private final JarClassIndex jarClassIndex;
    private final JarSymbolIndex jarSymbolIndex;
    private final JarIndexer jarIndexer;
    private final JarIndexManager jarIndexManager;
    private final KotlinMetadataIndexer metadataIndexer;
    private final PackageIndex packageIndex;
    private final SymbolRepository symbolRepository;
    private final ProjectManager projectManager;
    private final DependencyManager dependencyManager;
    private final CompletionLogger logger;

    private KotlinCompletionFacade(Builder builder) {
        this.logger = builder.logger != null ? builder.logger : new CompletionLogger.AndroidLogger();
        this.engine = new CompletionEngine(this.logger);
        this.parser = builder.parser != null ? builder.parser : new DefaultKotlinPsiParser();
        this.psiManager = new KotlinPsiManager(this.parser);
        this.contextResolver = new PsiContextResolver();

        this.sourceSymbolIndex = new SourceSymbolIndex();
        this.sourcePsiIndexer = new SourcePsiIndexer(this.parser, this.sourceSymbolIndex, this.logger);

        this.jarClassIndex = new JarClassIndex();
        this.jarSymbolIndex = new JarSymbolIndex();
        this.jarIndexer = new JarIndexer(this.jarClassIndex, this.jarSymbolIndex, this.logger);
        this.jarIndexManager = new JarIndexManager(this.jarIndexer, this.jarClassIndex, this.jarSymbolIndex, this.logger);

        this.metadataIndexer = new KotlinMetadataIndexer(new KotlinMetadataReader(this.logger), this.logger);
        this.packageIndex = new PackageIndex();

        this.symbolRepository = new DefaultSymbolRepository(
                this.sourceSymbolIndex,
                this.jarClassIndex,
                this.jarSymbolIndex,
                this.metadataIndexer,
                this.packageIndex
        );

        this.projectManager = new ProjectManager(this.sourcePsiIndexer, this.logger);
        this.dependencyManager = new DependencyManager(this.jarIndexManager, this.packageIndex, this.logger);

        // Populate common standard packages into package index
        populateStandardPackages();

        // Wire providers
        LocalSymbolResolver localSymbolResolver = new LocalSymbolResolver();
        SimpleTypeResolver simpleTypeResolver = new SimpleTypeResolver(localSymbolResolver);
        MemberResolver memberResolver = new MemberResolver(simpleTypeResolver, this.symbolRepository);

        this.engine.registerProvider(new KeywordCompletionProvider(this.contextResolver));
        this.engine.registerProvider(new LocalCompletionProvider(localSymbolResolver, this.psiManager, this.contextResolver));
        this.engine.registerProvider(new MemberCompletionProvider(memberResolver, this.psiManager, this.contextResolver));
        this.engine.registerProvider(new SourceCompletionProvider(this.symbolRepository, this.psiManager, this.contextResolver));
        this.engine.registerProvider(new ClassCompletionProvider(this.symbolRepository, this.contextResolver));
        this.engine.registerProvider(new ImportCompletionProvider(this.symbolRepository, this.contextResolver));
        this.engine.registerProvider(new PackageCompletionProvider(this.symbolRepository, this.contextResolver));
    }

    private void populateStandardPackages() {
        packageIndex.addPackage("kotlin");
        packageIndex.addPackage("kotlin.collections");
        packageIndex.addPackage("kotlin.io");
        packageIndex.addPackage("kotlin.text");
        packageIndex.addPackage("kotlin.math");
        packageIndex.addPackage("kotlin.coroutines");
        packageIndex.addPackage("java.util");
        packageIndex.addPackage("java.io");
        packageIndex.addPackage("java.lang");
        packageIndex.addPackage("android.os");
        packageIndex.addPackage("android.view");
        packageIndex.addPackage("android.widget");
        packageIndex.addPackage("android.content");
        packageIndex.addPackage("androidx.compose.runtime");
        packageIndex.addPackage("androidx.compose.material3");
        packageIndex.addPackage("androidx.compose.foundation");
        packageIndex.addPackage("androidx.compose.ui");

        packageIndex.addClass("kotlin.collections", "List");
        packageIndex.addClass("kotlin.collections", "Map");
        packageIndex.addClass("kotlin.collections", "Set");
        packageIndex.addClass("kotlin.collections", "ArrayList");
        packageIndex.addClass("kotlin.collections", "HashMap");
        packageIndex.addClass("java.util", "ArrayList");
        packageIndex.addClass("java.util", "HashMap");
        packageIndex.addClass("java.util", "HashSet");
        packageIndex.addClass("java.util", "Date");
        packageIndex.addClass("java.util", "UUID");
    }

    public static KotlinCompletionFacade createDefault() {
        return new Builder().build();
    }

    public CompletionResult complete(CompletionRequest request) {
        return engine.complete(request);
    }

    public CompletableFuture<CompletionResult> completeAsync(CompletionRequest request) {
        return engine.completeAsync(request);
    }

    public void updateActiveFileContent(String filePath, String content) {
        if (filePath != null && content != null) {
            psiManager.parse(filePath, content);
            sourcePsiIndexer.updateFile(new File(filePath), content);
        }
    }

    public ProjectManager getProjectManager() {
        return projectManager;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public SymbolRepository getSymbolRepository() {
        return symbolRepository;
    }

    public CompletionEngine getEngine() {
        return engine;
    }

    public void shutdown() {
        engine.shutdown();
    }

    public static class Builder {
        private CompletionLogger logger;
        private KotlinParser parser;

        public Builder logger(CompletionLogger logger) {
            this.logger = logger;
            return this;
        }

        public Builder parser(KotlinParser parser) {
            this.parser = parser;
            return this;
        }

        public KotlinCompletionFacade build() {
            return new KotlinCompletionFacade(this);
        }
    }
}
