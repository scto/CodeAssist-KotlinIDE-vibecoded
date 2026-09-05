package com.example.completion.index.repository;

import com.example.completion.index.jar.JarClassSymbol;
import com.example.completion.symbol.Symbol;

import java.util.List;

/**
 * Unified Repository interface querying symbols across Source, JAR, Metadata, and Built-ins.
 */
public interface SymbolRepository {

    List<Symbol> search(String prefix);

    List<JarClassSymbol> searchClasses(String prefix);

    List<Symbol> findMembers(String containerClassName);

    List<Symbol> findExtensionFunctions(String receiverType);

    List<String> getMatchingPackages(String prefix);

    List<String> getClassesInPackage(String packageName, String classPrefix);
}
