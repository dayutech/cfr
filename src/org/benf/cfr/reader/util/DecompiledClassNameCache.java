package org.benf.cfr.reader.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A bounded hierarchical cache of fully-qualified class names that have already
 * been decompiled in the current run.
 */
public class DecompiledClassNameCache {
    private static final String BOOT_INF_CLASSES_NAME_PREFIX = "BOOT-INF.classes.";
    private static final String WEB_INF_CLASSES_NAME_PREFIX = "WEB-INF.classes.";
    private static final int DEFAULT_MAX_TERMINAL_ENTRIES = 32768;
    private static final int DEFAULT_TARGET_TERMINAL_ENTRIES = 24576;

    private final Node root = new Node(null, null, 0);
    private final int maxTerminalEntries;
    private final int targetTerminalEntries;
    private int terminalEntries;

    public DecompiledClassNameCache() {
        this(DEFAULT_MAX_TERMINAL_ENTRIES, DEFAULT_TARGET_TERMINAL_ENTRIES);
    }

    DecompiledClassNameCache(int maxTerminalEntries, int targetTerminalEntries) {
        this.maxTerminalEntries = Math.max(1024, maxTerminalEntries);
        this.targetTerminalEntries = Math.max(512, Math.min(targetTerminalEntries, this.maxTerminalEntries));
    }

    /**
     * @return true when the class name already exists in the cache.
     */
    public boolean contains(String className) {
        String normalized = normalizeClassName(className);
        if (normalized == null) return false;

        Node current = root;
        int start = 0;
        for (int x = 0; x <= normalized.length(); ++x) {
            if (x != normalized.length() && normalized.charAt(x) != '.') continue;
            if (x == start) {
                start = x + 1;
                continue;
            }
            String token = normalized.substring(start, x);
            current = current.children.get(token);
            if (current == null) {
                return false;
            }
            start = x + 1;
        }

        if (!current.terminal) {
            return false;
        }
        current.hitCount++;
        return true;
    }

    /**
     * Add a class name to the cache after it has been decompiled successfully.
     */
    public void remember(String className) {
        String normalized = normalizeClassName(className);
        if (normalized == null) return;

        Node current = root;
        int start = 0;
        for (int x = 0; x <= normalized.length(); ++x) {
            if (x != normalized.length() && normalized.charAt(x) != '.') continue;
            if (x == start) {
                start = x + 1;
                continue;
            }
            String token = normalized.substring(start, x);
            Node next = current.children.get(token);
            if (next == null) {
                next = new Node(current, token, current.depth + 1);
                current.children.put(token, next);
            }
            current = next;
            start = x + 1;
        }

        if (!current.terminal) {
            current.terminal = true;
            current.hitCount = 1;
            terminalEntries++;
        } else {
            current.hitCount++;
        }

        if (terminalEntries > maxTerminalEntries) {
            cleanup();
        }
    }

    private void cleanup() {
        if (terminalEntries <= maxTerminalEntries) return;

        List<Node> terminals = new ArrayList<Node>(terminalEntries);
        collectTerminalNodes(root, terminals);
        Collections.sort(terminals, new Comparator<Node>() {
            @Override
            public int compare(Node a, Node b) {
                if (a.hitCount != b.hitCount) {
                    return a.hitCount - b.hitCount;
                }
                return b.depth - a.depth;
            }
        });

        int toRemove = terminalEntries - targetTerminalEntries;
        for (Node node : terminals) {
            if (toRemove <= 0) break;
            if (!node.terminal) continue;
            node.terminal = false;
            node.hitCount = 0;
            terminalEntries--;
            toRemove--;
            pruneNode(node);
        }
    }

    private void collectTerminalNodes(Node node, List<Node> out) {
        if (node.terminal) out.add(node);
        for (Node child : node.children.values()) {
            collectTerminalNodes(child, out);
        }
    }

    private void pruneNode(Node node) {
        Node current = node;
        while (current.parent != null && !current.terminal && current.children.isEmpty()) {
            Node parent = current.parent;
            parent.children.remove(current.tokenFromParent);
            current = parent;
        }
    }

    private static String normalizeClassName(String className) {
        if (className == null || className.isEmpty()) return null;
        String normalized = className.replace('/', '.').replace('\\', '.');
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        normalized = stripNamePrefixIgnoreCase(normalized, BOOT_INF_CLASSES_NAME_PREFIX);
        normalized = stripNamePrefixIgnoreCase(normalized, WEB_INF_CLASSES_NAME_PREFIX);
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        if (normalized.isEmpty()) return null;
        return normalized;
    }

    private static String stripNamePrefixIgnoreCase(String value, String prefix) {
        if (value.length() < prefix.length()) {
            return value;
        }
        if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return value.substring(prefix.length());
        }
        return value;
    }

    private static class Node {
        private final Node parent;
        private final String tokenFromParent;
        private final int depth;
        private final Map<String, Node> children = new HashMap<String, Node>();
        private boolean terminal;
        private int hitCount;

        private Node(Node parent, String tokenFromParent, int depth) {
            this.parent = parent;
            this.tokenFromParent = tokenFromParent;
            this.depth = depth;
        }
    }
}
