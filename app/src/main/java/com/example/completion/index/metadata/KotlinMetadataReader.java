package com.example.completion.index.metadata;

import com.example.completion.core.CompletionLogger;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.index.jar.ClassFileReader;
import com.example.completion.symbol.Symbol;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reads and interprets Kotlin metadata annotations from compiled .class files.
 */
public class KotlinMetadataReader {

    private final CompletionLogger logger;

    public KotlinMetadataReader(CompletionLogger logger) {
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
    }

    public List<MetadataSymbol> readSymbols(InputStream classInputStream) {
        try {
            ClassFileReader.ParsedClassInfo info = ClassFileReader.parse(classInputStream);
            if (info == null || info.className.isEmpty()) {
                return Collections.emptyList();
            }

            List<MetadataSymbol> result = new ArrayList<>();
            String simpleName = ClassFileReader.getSimpleName(info.className);
            String pkg = "";
            int lastDot = info.className.lastIndexOf('.');
            if (lastDot != -1) {
                pkg = info.className.substring(0, lastDot);
            }

            // Add class symbol
            SymbolKind kind = info.isInterface ? SymbolKind.INTERFACE : (info.isEnum ? SymbolKind.ENUM : SymbolKind.CLASS);
            result.add(MetadataSymbol.builder()
                    .name(simpleName)
                    .qualifiedName(info.className)
                    .kind(kind)
                    .origin(SymbolOrigin.KOTLIN_METADATA)
                    .packageName(pkg)
                    .kotlinPackage(pkg)
                    .returnType(info.className)
                    .build());

            // Convert members to metadata symbols
            for (Symbol m : info.declaredMembers) {
                result.add(MetadataSymbol.builder()
                        .name(m.getName())
                        .qualifiedName(m.getQualifiedName())
                        .kind(m.getKind())
                        .origin(SymbolOrigin.KOTLIN_METADATA)
                        .packageName(pkg)
                        .kotlinPackage(pkg)
                        .containerName(info.className)
                        .returnType(m.getReturnType())
                        .parameters(m.getParameters())
                        .receiverType(m.getReceiverType())
                        .build());
            }

            return result;
        } catch (Throwable e) {
            logger.error("Failed to read Kotlin metadata", e);
            return Collections.emptyList();
        }
    }
}
