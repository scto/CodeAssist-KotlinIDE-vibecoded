package com.example.completion.psi;

import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of an analyzed Kotlin file containing the AST/PSI root,
 * package declaration, imports, and top-level / inner declarations.
 */
public class ParsedKotlinFile {

    private final String fileName;
    private final String source;
    private final String packageName;
    private final List<String> imports;
    private final KotlinPsiElement rootElement;
    private final List<Symbol> topLevelSymbols;

    public ParsedKotlinFile(String fileName, String source, String packageName, List<String> imports,
                            KotlinPsiElement rootElement, List<Symbol> topLevelSymbols) {
        this.fileName = fileName != null ? fileName : "Unknown.kt";
        this.source = source != null ? source : "";
        this.packageName = packageName != null ? packageName : "";
        this.imports = imports != null ? Collections.unmodifiableList(new ArrayList<>(imports)) : Collections.emptyList();
        this.rootElement = rootElement;
        this.topLevelSymbols = topLevelSymbols != null ? Collections.unmodifiableList(new ArrayList<>(topLevelSymbols)) : Collections.emptyList();
    }

    public String getFileName() {
        return fileName;
    }

    public String getSource() {
        return source;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<String> getImports() {
        return imports;
    }

    public KotlinPsiElement getRootElement() {
        return rootElement;
    }

    public List<Symbol> getTopLevelSymbols() {
        return topLevelSymbols;
    }

    public KotlinPsiElement findElementAt(int offset) {
        return rootElement != null ? rootElement.findElementAt(offset) : null;
    }
}
