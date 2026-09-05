package com.example.completion.psi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Universal Node in the Kotlin PSI / AST representation.
 * Supports parent-child navigation, text offsets, element types, and name identifiers.
 */
public class KotlinPsiElement {

    private final KtElementKind kind;
    private final String name;
    private final int startOffset;
    private final int endOffset;
    private final int line;
    private final int column;
    private String type;
    private String receiverType;
    private String expressionText;
    private KotlinPsiElement parent;
    private final List<KotlinPsiElement> children = new ArrayList<>();

    public KotlinPsiElement(KtElementKind kind, String name, int startOffset, int endOffset, int line, int column) {
        this.kind = kind;
        this.name = name != null ? name : "";
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.line = line;
        this.column = column;
    }

    public KtElementKind getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getExpressionText() {
        return expressionText;
    }

    public void setExpressionText(String expressionText) {
        this.expressionText = expressionText;
    }

    public KotlinPsiElement getParent() {
        return parent;
    }

    public void addChild(KotlinPsiElement child) {
        if (child != null) {
            child.parent = this;
            this.children.add(child);
        }
    }

    public List<KotlinPsiElement> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean containsOffset(int offset) {
        return offset >= startOffset && offset <= endOffset;
    }

    /**
     * Finds the deepest/narrowest PSI element containing the specified text offset.
     */
    public KotlinPsiElement findElementAt(int offset) {
        for (KotlinPsiElement child : children) {
            if (child.containsOffset(offset)) {
                return child.findElementAt(offset);
            }
        }
        if (containsOffset(offset)) {
            return this;
        }
        return null;
    }

    @Override
    public String toString() {
        return "KotlinPsiElement{" +
                "kind=" + kind +
                ", name='" + name + '\'' +
                ", offsets=[" + startOffset + ", " + endOffset + "]" +
                ", children=" + children.size() +
                '}';
    }
}
