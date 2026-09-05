package com.example.completion.index.repository;

import com.example.completion.index.jar.JarClassIndex;
import com.example.completion.index.jar.JarClassSymbol;
import com.example.completion.index.jar.JarSymbolIndex;
import com.example.completion.index.metadata.KotlinMetadataIndexer;
import com.example.completion.index.metadata.MetadataSymbol;
import com.example.completion.index.pkg.PackageIndex;
import com.example.completion.index.source.ProjectSymbol;
import com.example.completion.index.source.SourceSymbolIndex;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Unified implementation of SymbolRepository querying across all indexes.
 */
public class DefaultSymbolRepository implements SymbolRepository {

    private final SourceSymbolIndex sourceSymbolIndex;
    private final JarClassIndex jarClassIndex;
    private final JarSymbolIndex jarSymbolIndex;
    private final KotlinMetadataIndexer metadataIndexer;
    private final PackageIndex packageIndex;

    public DefaultSymbolRepository(SourceSymbolIndex sourceSymbolIndex,
                                   JarClassIndex jarClassIndex,
                                   JarSymbolIndex jarSymbolIndex,
                                   KotlinMetadataIndexer metadataIndexer,
                                   PackageIndex packageIndex) {
        this.sourceSymbolIndex = sourceSymbolIndex;
        this.jarClassIndex = jarClassIndex;
        this.jarSymbolIndex = jarSymbolIndex;
        this.metadataIndexer = metadataIndexer;
        this.packageIndex = packageIndex != null ? packageIndex : new PackageIndex();
    }

    @Override
    public List<Symbol> search(String prefix) {
        String lowerPrefix = prefix != null ? prefix.toLowerCase(Locale.ROOT) : "";
        List<Symbol> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Source Symbols
        if (sourceSymbolIndex != null) {
            for (ProjectSymbol s : sourceSymbolIndex.findByPrefix(prefix)) {
                if (seen.add(s.getQualifiedName() + ":" + s.getKind())) {
                    results.add(s);
                }
            }
        }

        // 2. Built-in Symbols
        for (Symbol b : BuiltInKotlinSymbols.getBuiltIns()) {
            if (b.getName().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                if (seen.add(b.getQualifiedName() + ":" + b.getKind())) {
                    results.add(b);
                }
            }
        }

        // 3. JAR Class Symbols
        if (jarClassIndex != null) {
            for (JarClassSymbol j : jarClassIndex.findByPrefix(prefix)) {
                if (seen.add(j.getQualifiedName() + ":" + j.getKind())) {
                    results.add(j);
                }
            }
        }

        // 4. Metadata Symbols
        if (metadataIndexer != null) {
            for (MetadataSymbol m : metadataIndexer.getAllMetadataSymbols()) {
                if (m.getName().toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                    if (seen.add(m.getQualifiedName() + ":" + m.getKind())) {
                        results.add(m);
                    }
                }
            }
        }

        return results;
    }

    @Override
    public List<JarClassSymbol> searchClasses(String prefix) {
        return jarClassIndex != null ? jarClassIndex.findByPrefix(prefix) : Collections.emptyList();
    }

    @Override
    public List<Symbol> findMembers(String containerClassName) {
        if (containerClassName == null || containerClassName.isEmpty()) {
            return Collections.emptyList();
        }

        List<Symbol> members = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Built-in members
        for (Symbol b : BuiltInKotlinSymbols.getMembersForType(containerClassName)) {
            if (seen.add(b.getName())) {
                members.add(b);
            }
        }

        // JAR members
        if (jarSymbolIndex != null) {
            for (Symbol j : jarSymbolIndex.getMembers(containerClassName)) {
                if (seen.add(j.getName())) {
                    members.add(j);
                }
            }
        }

        // Source members
        if (sourceSymbolIndex != null) {
            for (ProjectSymbol s : sourceSymbolIndex.getAllSymbols()) {
                if (containerClassName.equals(s.getContainerName())) {
                    if (seen.add(s.getName())) {
                        members.add(s);
                    }
                }
            }
        }

        return members;
    }

    @Override
    public List<Symbol> findExtensionFunctions(String receiverType) {
        if (receiverType == null || receiverType.isEmpty()) return Collections.emptyList();
        List<Symbol> results = new ArrayList<>();

        if (sourceSymbolIndex != null) {
            for (ProjectSymbol s : sourceSymbolIndex.getAllSymbols()) {
                if (s.isExtension() && (receiverType.equals(s.getReceiverType()) || "Any".equals(s.getReceiverType()))) {
                    results.add(s);
                }
            }
        }

        return results;
    }

    @Override
    public List<String> getMatchingPackages(String prefix) {
        return packageIndex.getMatchingPackages(prefix);
    }

    @Override
    public List<String> getClassesInPackage(String packageName, String classPrefix) {
        return packageIndex.getClassesInPackage(packageName, classPrefix);
    }

    public PackageIndex getPackageIndex() {
        return packageIndex;
    }
}
