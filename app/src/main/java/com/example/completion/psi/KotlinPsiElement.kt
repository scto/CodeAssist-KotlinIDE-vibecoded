package com.example.completion.psi

/**
 * Universal Node in the Kotlin PSI / AST representation.
 * Supports parent-child navigation, text offsets, element types, and name identifiers.
 */
class KotlinPsiElement(
    val kind: KtElementKind,
    name: String?,
    val startOffset: Int,
    val endOffset: Int,
    val line: Int,
    val column: Int
) {
    val name: String = name ?: ""
    var type: String? = null
    var receiverType: String? = null
    var expressionText: String? = null
    var parent: KotlinPsiElement? = null
        internal set

    private val _children: MutableList<KotlinPsiElement> = ArrayList()
    val children: List<KotlinPsiElement>
        get() = _children

    fun addChild(child: KotlinPsiElement?) {
        if (child != null) {
            child.parent = this
            _children.add(child)
        }
    }

    fun containsOffset(offset: Int): Boolean {
        return offset in startOffset..endOffset
    }

    /**
     * Finds the deepest/narrowest PSI element containing the specified text offset.
     */
    fun findElementAt(offset: Int): KotlinPsiElement? {
        for (child in _children) {
            if (child.containsOffset(offset)) {
                return child.findElementAt(offset)
            }
        }
        if (containsOffset(offset)) {
            return this
        }
        return null
    }

    override fun toString(): String {
        return "KotlinPsiElement{" +
                "kind=" + kind +
                ", name='" + name + '\'' +
                ", offsets=[" + startOffset + ", " + endOffset + "]" +
                ", children=" + _children.size +
                '}'
    }
}
