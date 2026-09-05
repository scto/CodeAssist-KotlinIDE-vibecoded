package com.example.completion.project;

import com.example.completion.core.CompletionLogger;
import com.example.completion.index.source.SourcePsiIndexer;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages local project workspace structure, source root tracking, and directory indexing.
 */
public class ProjectManager {

    private final SourcePsiIndexer sourcePsiIndexer;
    private final CompletionLogger logger;
    private File projectRoot;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ProjectIndexWorker");
        t.setDaemon(true);
        return t;
    });

    public ProjectManager(SourcePsiIndexer sourcePsiIndexer, CompletionLogger logger) {
        this.sourcePsiIndexer = sourcePsiIndexer;
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
    }

    public void setProjectRoot(File root) {
        this.projectRoot = root;
        if (root != null && root.exists() && root.isDirectory()) {
            executor.submit(() -> {
                logger.info("Starting recursive indexing of project: " + root.getAbsolutePath());
                sourcePsiIndexer.indexDirectoryRecursively(root);
                logger.info("Finished indexing project: " + root.getName());
            });
        }
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public void onFileModified(File file, String newContent) {
        if (file != null) {
            sourcePsiIndexer.updateFile(file, newContent);
        }
    }

    public void onFileDeleted(File file) {
        if (file != null) {
            sourcePsiIndexer.removeFile(file);
        }
    }
}
