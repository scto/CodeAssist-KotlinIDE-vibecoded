package com.example.completion.index.pkg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tree node representing a package segment and its child subpackages and member classes.
 */
public class PackageNode {

    private final String name;
    private final String fullPackageName;
    private final Map<String, PackageNode> subPackages = new ConcurrentHashMap<>();
    private final List<String> classes = new ArrayList<>();

    public PackageNode(String name, String fullPackageName) {
        this.name = name != null ? name : "";
        this.fullPackageName = fullPackageName != null ? fullPackageName : "";
    }

    public String getName() {
        return name;
    }

    public String getFullPackageName() {
        return fullPackageName;
    }

    public PackageNode getOrCreateSubPackage(String segment) {
        return subPackages.computeIfAbsent(segment, s -> {
            String newFull = fullPackageName.isEmpty() ? s : fullPackageName + "." + s;
            return new PackageNode(s, newFull);
        });
    }

    public PackageNode getSubPackage(String segment) {
        return subPackages.get(segment);
    }

    public Map<String, PackageNode> getSubPackages() {
        return Collections.unmodifiableMap(subPackages);
    }

    public synchronized void addClass(String simpleClassName) {
        if (simpleClassName != null && !classes.contains(simpleClassName)) {
            classes.add(simpleClassName);
        }
    }

    public synchronized List<String> getClasses() {
        return Collections.unmodifiableList(new ArrayList<>(classes));
    }
}
