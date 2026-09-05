package com.example.completion.index.pkg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hierarchical package tree index supporting auto-completion for import and package statements.
 */
public class PackageIndex {

    private final PackageNode root = new PackageNode("", "");

    public void addPackage(String fullPackageName) {
        if (fullPackageName == null || fullPackageName.trim().isEmpty()) return;
        String[] segments = fullPackageName.trim().split("\\.");
        PackageNode current = root;
        for (String seg : segments) {
            if (!seg.isEmpty()) {
                current = current.getOrCreateSubPackage(seg);
            }
        }
    }

    public void addClass(String fullPackageName, String simpleClassName) {
        if (fullPackageName == null) fullPackageName = "";
        String[] segments = fullPackageName.trim().split("\\.");
        PackageNode current = root;
        for (String seg : segments) {
            if (!seg.isEmpty()) {
                current = current.getOrCreateSubPackage(seg);
            }
        }
        current.addClass(simpleClassName);
    }

    public List<String> getMatchingPackages(String prefix) {
        if (prefix == null) prefix = "";
        String cleanPrefix = prefix.trim();

        int lastDot = cleanPrefix.lastIndexOf('.');
        String parentPkg = lastDot != -1 ? cleanPrefix.substring(0, lastDot) : "";
        String subPrefix = lastDot != -1 ? cleanPrefix.substring(lastDot + 1) : cleanPrefix;

        PackageNode node = resolveNode(parentPkg);
        if (node == null) return Collections.emptyList();

        List<String> results = new ArrayList<>();
        for (PackageNode sub : node.getSubPackages().values()) {
            if (sub.getName().startsWith(subPrefix)) {
                results.add(sub.getFullPackageName());
            }
        }
        return results;
    }

    public List<String> getClassesInPackage(String fullPackageName, String classPrefix) {
        PackageNode node = resolveNode(fullPackageName);
        if (node == null) return Collections.emptyList();

        List<String> results = new ArrayList<>();
        String lowerPrefix = classPrefix != null ? classPrefix.toLowerCase() : "";
        for (String cls : node.getClasses()) {
            if (cls.toLowerCase().startsWith(lowerPrefix)) {
                results.add(cls);
            }
        }
        return results;
    }

    private PackageNode resolveNode(String fullPackageName) {
        if (fullPackageName == null || fullPackageName.isEmpty()) {
            return root;
        }
        String[] segments = fullPackageName.split("\\.");
        PackageNode current = root;
        for (String seg : segments) {
            if (!seg.isEmpty()) {
                current = current.getSubPackage(seg);
                if (current == null) return null;
            }
        }
        return current;
    }
}
