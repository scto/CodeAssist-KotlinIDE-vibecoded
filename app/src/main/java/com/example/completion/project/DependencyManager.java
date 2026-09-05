package com.example.completion.project;

import com.example.completion.core.CompletionLogger;
import com.example.completion.index.jar.JarIndexManager;
import com.example.completion.index.pkg.PackageIndex;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages external JAR / AAR dependencies and triggers background indexing.
 */
public class DependencyManager {

    private final JarIndexManager jarIndexManager;
    private final PackageIndex packageIndex;
    private final CompletionLogger logger;
    private final Set<File> dependencyFiles = new CopyOnWriteArraySet<>();

    public DependencyManager(JarIndexManager jarIndexManager, PackageIndex packageIndex, CompletionLogger logger) {
        this.jarIndexManager = jarIndexManager;
        this.packageIndex = packageIndex;
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
    }

    public void addDependency(File jarOrAarFile) {
        if (jarOrAarFile == null || !jarOrAarFile.exists()) return;
        dependencyFiles.add(jarOrAarFile);
        jarIndexManager.indexJarAsync(jarOrAarFile);
        logger.info("Added dependency: " + jarOrAarFile.getName());
    }

    public void addDependencies(List<File> files) {
        if (files == null) return;
        for (File f : files) {
            addDependency(f);
        }
    }

    public void removeDependency(File jarFile) {
        if (jarFile != null) {
            dependencyFiles.remove(jarFile);
            jarIndexManager.invalidate(jarFile);
        }
    }

    public List<File> getDependencies() {
        return Collections.unmodifiableList(new ArrayList<>(dependencyFiles));
    }

    public void clear() {
        dependencyFiles.clear();
        jarIndexManager.clear();
    }
}
