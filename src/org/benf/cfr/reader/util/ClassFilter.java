package org.benf.cfr.reader.util;

import java.util.HashSet;
import java.util.Set;

/**
 * 类过滤器
 *
 * 该类提供了类过滤功能，用于在反编译过程中跳过不需要分析的类。
 *
 * 过滤规则来源：
 * 1. 内置过滤规则（常见第三方依赖）
 * 2. 配置文件中的自定义规则
 *
 * 过滤机制（按优先级顺序）：
 * 1. JAR文件名前缀匹配：首先检查JAR文件名是否匹配任何JAR前缀规则
 * 2. 全类名前缀匹配：如果JAR匹配未命中，再检查类的全限定名是否匹配任何类名前缀规则
 *
 * 使用方式：
 * - 通过 --enableclassfilter 命令行选项启用过滤功能
 * - 可通过配置文件 cfr_class_filter.conf 添加自定义过滤规则
 */
public class ClassFilter {
    private static final String BOOT_INF_CLASSES_PATH_PREFIX = "BOOT-INF/classes/";
    private static final String WEB_INF_CLASSES_PATH_PREFIX = "WEB-INF/classes/";
    private static final String BOOT_INF_CLASSES_NAME_PREFIX = "BOOT-INF.classes.";
    private static final String WEB_INF_CLASSES_NAME_PREFIX = "WEB-INF.classes.";

    /**
     * JAR文件名前缀过滤规则（内置规则 + 配置文件规则）
     */
    private final Set<String> jarPrefixRules;

    /**
     * 全类名前缀过滤规则（内置规则 + 配置文件规则）
     */
    private final Set<String> classPrefixRules;
    /**
     * class文件路径前缀过滤规则（将类名前缀中的'.'转换为'/'）
     */
    private final Set<String> classPathPrefixRules;

    /**
     * 过滤功能是否启用
     */
    private final boolean enabled;

    /**
     * 当前JAR文件名（不含路径和.jar后缀，已转为小写）
     */
    private String currentJarName;

    /**
     * 当前JAR是否已被JAR规则过滤
     */
    private boolean currentJarFiltered;

    /**
     * 构造函数
     *
     * @param enabled 是否启用过滤功能
     */
    public ClassFilter(boolean enabled) {
        this.enabled = enabled;
        this.jarPrefixRules = new HashSet<String>();
        this.classPrefixRules = new HashSet<String>();
        this.classPathPrefixRules = new HashSet<String>();
        this.currentJarName = null;
        this.currentJarFiltered = false;

        if (enabled) {
            // 加载内置规则
            this.classPrefixRules.addAll(BuiltinFilterRules.getBuiltinClassRules());
            this.jarPrefixRules.addAll(BuiltinFilterRules.getBuiltinJarRules());

            // 加载配置文件规则
            ClassFilterConfig.FilterRules configRules = ClassFilterConfig.loadFilterRules();

            // 合并配置文件JAR前缀规则
            this.jarPrefixRules.addAll(configRules.getJarPrefixRules());

            // 合并配置文件类名前缀规则
            this.classPrefixRules.addAll(configRules.getClassPrefixRules());

            for (String classPrefixRule : classPrefixRules) {
                classPathPrefixRules.add(classPrefixRule.replace('.', '/'));
            }
        }
    }

    /**
     * 设置当前正在处理的JAR文件名
     *
     * 在处理新的JAR文件时调用此方法。会自动检查JAR文件名是否匹配过滤规则。
     * 如果匹配，该JAR中的所有类都会被过滤。
     *
     * @param jarPath JAR文件的完整路径或文件名
     */
    public void setCurrentJar(String jarPath) {
        if (!enabled || jarPath == null || jarPath.isEmpty()) {
            this.currentJarName = null;
            this.currentJarFiltered = false;
            return;
        }

        // 提取JAR文件名（不含路径和.jar后缀）
        String jarName = jarPath;
        int lastSep = Math.max(jarName.lastIndexOf('/'), jarName.lastIndexOf('\\'));
        if (lastSep >= 0) {
            jarName = jarName.substring(lastSep + 1);
        }
        if (jarName.toLowerCase().endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4);
        }
        this.currentJarName = jarName.toLowerCase();

        // 检查JAR文件名是否匹配任何前缀规则
        this.currentJarFiltered = matchesJarPrefix(this.currentJarName);
    }

    /**
     * 检查JAR文件名是否匹配任何JAR前缀规则
     *
     * @param jarName JAR文件名（不含.jar后缀，已转为小写）
     * @return 如果匹配返回true
     */
    private boolean matchesJarPrefix(String jarName) {
        if (jarName == null || jarName.isEmpty()) {
            return false;
        }

        for (String prefix : jarPrefixRules) {
            // 支持两种匹配方式：
            // 1. 精确前缀匹配：spring-core 匹配 spring-core-5.3.0
            // 2. 完整匹配：spring-core 匹配 spring-core
            if (jarName.equals(prefix) ||
                jarName.startsWith(prefix + "-") ||
                jarName.startsWith(prefix + ".")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查过滤功能是否启用
     *
     * @return 如果过滤功能启用返回true，否则返回false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 检查当前JAR是否已被过滤
     *
     * @return 如果当前JAR被JAR规则过滤返回true，否则返回false
     */
    public boolean isCurrentJarFiltered() {
        return currentJarFiltered;
    }

    /**
     * 获取所有JAR前缀过滤规则
     *
     * @return 包含所有JAR前缀规则的Set集合
     */
    public Set<String> getJarPrefixRules() {
        return new HashSet<String>(jarPrefixRules);
    }

    /**
     * 获取所有类名前缀过滤规则
     *
     * @return 包含所有类名前缀规则的Set集合
     */
    public Set<String> getClassPrefixRules() {
        return new HashSet<String>(classPrefixRules);
    }

    /**
     * 检查给定的类名是否应该被过滤
     *
     * 过滤逻辑（按优先级）：
     * 1. 如果过滤功能未启用，返回false
     * 2. 如果当前JAR已被JAR规则过滤，返回true
     * 3. 检查类名是否匹配任何类名前缀规则
     *
     * @param className 类的全限定名（如 java.lang.String）
     * @return 如果类应该被过滤返回true，否则返回false
     */
    public boolean shouldFilter(String className) {
        return shouldFilterWithLog(className, null);
    }

    /**
     * 检查给定的类名是否应该被过滤，并记录匹配的规则
     *
     * @param className 类的全限定名
     * @param matchedRule 输出参数，用于返回匹配的规则（可为null）
     * @return 如果类应该被过滤返回true，否则返回false
     */
    public boolean shouldFilterWithLog(String className, StringBuilder matchedRule) {
        if (!enabled) {
            return false;
        }

        if (className == null || className.isEmpty()) {
            return false;
        }

        className = normalizeClassNameForRuleMatch(className);

        // 首先检查JAR级别过滤
        if (currentJarFiltered) {
            if (matchedRule != null) {
                matchedRule.append("[jar]").append(currentJarName);
            }
            return true;
        }

        // 然后检查类名前缀过滤
        for (String rule : classPrefixRules) {
            if (className.startsWith(rule)) {
                if (matchedRule != null) {
                    matchedRule.append("[class]").append(rule);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * 检查JAR条目路径是否应被过滤
     *
     * @param classPath JAR中的类路径（例如 org/apache/Foo.class）
     * @return 如果该路径匹配过滤规则返回true，否则返回false
     */
    public boolean shouldFilterClassPath(String classPath) {
        return shouldFilterClassPath(classPath, null);
    }

    /**
     * 检查JAR条目路径是否应被过滤，并返回命中的规则目录前缀
     *
     * @param classPath JAR中的类路径（例如 org/apache/Foo.class）
     * @param matchedPathPrefix 输出参数，命中时返回规则目录前缀（例如 org/apache）
     * @return 如果该路径匹配过滤规则返回true，否则返回false
     */
    public boolean shouldFilterClassPath(String classPath, StringBuilder matchedPathPrefix) {
        if (!enabled) {
            return false;
        }

        if (classPath == null || classPath.isEmpty()) {
            return false;
        }

        if (currentJarFiltered) {
            if (matchedPathPrefix != null) {
                matchedPathPrefix.append("<jar>");
            }
            return true;
        }

        String normalizedClassPath = normalizeClassPathForRuleMatch(classPath);
        if (normalizedClassPath.endsWith(".class")) {
            normalizedClassPath = normalizedClassPath.substring(0, normalizedClassPath.length() - 6);
        }

        String bestMatch = null;
        for (String rule : classPathPrefixRules) {
            if (!normalizedClassPath.startsWith(rule)) {
                continue;
            }
            if (bestMatch == null || rule.length() < bestMatch.length()) {
                bestMatch = rule;
            }
        }
        if (bestMatch != null) {
            if (matchedPathPrefix != null) {
                matchedPathPrefix.append(bestMatch);
            }
            return true;
        }

        return false;
    }

    private static String normalizeClassPathForRuleMatch(String classPath) {
        String normalized = classPath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        normalized = stripPathPrefixIgnoreCase(normalized, BOOT_INF_CLASSES_PATH_PREFIX);
        normalized = stripPathPrefixIgnoreCase(normalized, WEB_INF_CLASSES_PATH_PREFIX);
        return normalized;
    }

    private static String normalizeClassNameForRuleMatch(String className) {
        String normalized = className.replace('/', '.').replace('\\', '.');
        normalized = stripNamePrefixIgnoreCase(normalized, BOOT_INF_CLASSES_NAME_PREFIX);
        normalized = stripNamePrefixIgnoreCase(normalized, WEB_INF_CLASSES_NAME_PREFIX);
        if (normalized.endsWith(".class")) {
            normalized = normalized.substring(0, normalized.length() - 6);
        }
        return normalized;
    }

    private static String stripPathPrefixIgnoreCase(String value, String prefix) {
        return startsWithIgnoreCase(value, prefix) ? value.substring(prefix.length()) : value;
    }

    private static String stripNamePrefixIgnoreCase(String value, String prefix) {
        return startsWithIgnoreCase(value, prefix) ? value.substring(prefix.length()) : value;
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        if (value.length() < prefix.length()) {
            return false;
        }
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * 获取过滤统计信息
     *
     * @return 包含过滤规则数量等信息的字符串
     */
    public String getStatistics() {
        return "Class filter status: " + (enabled ? "enabled" : "disabled") +
               ", jar prefix rules: " + jarPrefixRules.size() +
               ", class prefix rules: " + classPrefixRules.size();
    }
}
