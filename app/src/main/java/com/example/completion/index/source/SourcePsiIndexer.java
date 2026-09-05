package com.example.completion.index.source;

import com.example.completion.core.CompletionLogger;
import com.example.completion.psi.KotlinParser;
import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.symbol.Symbol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Incremental Source Indexer that indexes Kotlin source files using Kotlin PSI/AST parser.
 */
public class SourcePsiIndexer {

    private final KotlinParser parser;
    private final SourceSymbolIndex sourceSymbolIndex;
    private final CompletionLogger logger;

    public SourcePsiIndexer(KotlinParser parser, SourceSymbolIndex sourceSymbolIndex, CompletionLogger logger) {
        this.parser = parser;
        this.sourceSymbolIndex = sourceSymbolIndex;
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
    }

    public void indexFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) return;
        if (!file.getName().endsWith(".kt") && !file.getName().endsWith(".kts")) return;

        try {
            String content = readFileContent(file);
            updateFile(file, content);
        } catch (Throwable e) {
            logger.error("Failed to index source file: " + file.getAbsolutePath(), e);
        }
    }

    public void updateFile(File file, String newContent) {
        if (file == null) return;
        String filePath = file.getAbsolutePath();
        long lastMod = file.lastModified();

        try {
            ParsedKotlinFile parsed = parser.parse(file.getName(), newContent);
            List<ProjectSymbol> projectSymbols = new ArrayList<>();

            for (Symbol s : parsed.getTopLevelSymbols()) {
                projectSymbols.add(ProjectSymbol.builder()
                        .name(s.getName())
                        .qualifiedName(s.getQualifiedName())
                        .kind(s.getKind())
                        .origin(s.getOrigin())
                        .packageName(s.getPackageName())
                        .returnType(s.getReturnType())
                        .parameters(s.getParameters())
                        .containerName(s.getContainerName())
                        .receiverType(s.getReceiverType())
                        .sourceFilePath(filePath)
                        .lastModified(lastMod)
                        .build());
            }

            sourceSymbolIndex.updateFileSymbols(filePath, projectSymbols);
            logger.debug("Indexed source file: " + file.getName() + " (" + projectSymbols.size() + " symbols)");
        } catch (Throwable e) {
            logger.error("Failed to parse and index content for: " + filePath, e);
        }
    }

    public void removeFile(File file) {
        if (file != null) {
            sourceSymbolIndex.removeFile(file.getAbsolutePath());
        }
    }

    public void indexDirectoryRecursively(File directory) {
        if (directory == null || !directory.exists()) return;
        if (directory.isFile()) {
            indexFile(directory);
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    indexDirectoryRecursively(f);
                } else if (f.getName().endsWith(".kt") || f.getName().endsWith(".kts")) {
                    indexFile(f);
                }
            }
        }
    }

    private String readFileContent(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
