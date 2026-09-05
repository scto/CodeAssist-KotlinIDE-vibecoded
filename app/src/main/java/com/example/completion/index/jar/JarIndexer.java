package com.example.completion.index.jar;

import com.example.completion.core.CompletionLogger;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Indexes bytecode entries from JAR dependency files directly without Reflection or ClassLoader.
 */
public class JarIndexer {

    private final JarClassIndex classIndex;
    private final JarSymbolIndex symbolIndex;
    private final CompletionLogger logger;

    public JarIndexer(JarClassIndex classIndex, JarSymbolIndex symbolIndex, CompletionLogger logger) {
        this.classIndex = classIndex;
        this.symbolIndex = symbolIndex;
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
    }

    public void indexJar(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) return;

        String path = jarFile.getAbsolutePath();
        int indexedClasses = 0;
        int indexedMembers = 0;

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (name.endsWith(".class") && !name.contains("$") && !name.startsWith("META-INF/")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        ClassFileReader.ParsedClassInfo info = ClassFileReader.parse(in);
                        if (info != null && !info.className.isEmpty()) {
                            String packageName = "";
                            int lastDot = info.className.lastIndexOf('.');
                            if (lastDot != -1) {
                                packageName = info.className.substring(0, lastDot);
                            }

                            SymbolKind kind = info.isInterface ? SymbolKind.INTERFACE : (info.isEnum ? SymbolKind.ENUM : SymbolKind.CLASS);

                            JarClassSymbol classSymbol = JarClassSymbol.builder()
                                    .name(ClassFileReader.getSimpleName(info.className))
                                    .qualifiedName(info.className)
                                    .kind(kind)
                                    .origin(SymbolOrigin.JAR)
                                    .packageName(packageName)
                                    .jarFilePath(path)
                                    .isInterface(info.isInterface)
                                    .isEnum(info.isEnum)
                                    .build();

                            classIndex.addClass(classSymbol);
                            indexedClasses++;

                            if (!info.declaredMembers.isEmpty()) {
                                symbolIndex.addMembers(info.className, info.declaredMembers);
                                indexedMembers += info.declaredMembers.size();
                            }
                        }
                    } catch (Throwable e) {
                        // Skip malformed individual entries
                    }
                }
            }
            logger.debug("Indexed JAR: " + jarFile.getName() + " -> " + indexedClasses + " classes, " + indexedMembers + " members");
        } catch (Throwable e) {
            logger.error("Failed to index JAR file: " + path, e);
        }
    }
}
