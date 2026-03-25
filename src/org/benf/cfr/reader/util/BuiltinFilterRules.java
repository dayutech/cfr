package org.benf.cfr.reader.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Built-in filter rules.
 *
 * Keep this list intentionally small: only the most common dependencies that are
 * very likely to be non-target libraries. The extended rule set lives in
 * cfr_class_filter.conf.
 */
public class BuiltinFilterRules {

    /**
     * Minimal built-in class prefix rules.
     */
    private static final Set<String> BUILTIN_CLASS_RULES;

    /**
     * Minimal built-in jar prefix rules.
     */
    private static final Set<String> BUILTIN_JAR_RULES;

    static {
        Set<String> classRules = new HashSet<String>();

        classRules.add("org.springframework");
        classRules.add("org.apache.commons");
        classRules.add("org.apache.logging");
        classRules.add("org.apache.log4j");
        classRules.add("com.google.common");
        classRules.add("com.google.gson");
        classRules.add("org.slf4j");
        classRules.add("ch.qos.logback");
        classRules.add("io.netty");
        classRules.add("com.fasterxml.jackson");
        classRules.add("org.junit");
        classRules.add("org.mockito");

        BUILTIN_CLASS_RULES = Collections.unmodifiableSet(classRules);

        Set<String> jarRules = new HashSet<String>();

        jarRules.add("spring-core");
        jarRules.add("spring-context");
        jarRules.add("spring-beans");
        jarRules.add("spring-web");
        jarRules.add("spring-webmvc");
        jarRules.add("spring-boot");
        jarRules.add("commons-lang3");
        jarRules.add("commons-io");
        jarRules.add("commons-collections4");
        jarRules.add("guava");
        jarRules.add("gson");
        jarRules.add("slf4j-api");
        jarRules.add("logback-classic");
        jarRules.add("logback-core");
        jarRules.add("log4j-api");
        jarRules.add("log4j-core");
        jarRules.add("netty");
        jarRules.add("netty-all");
        jarRules.add("jackson-core");
        jarRules.add("jackson-databind");
        jarRules.add("jackson-annotations");
        jarRules.add("junit");
        jarRules.add("junit-jupiter");
        jarRules.add("mockito-core");
        jarRules.add("hibernate-core");

        BUILTIN_JAR_RULES = Collections.unmodifiableSet(jarRules);
    }

    /**
     * @return immutable set of built-in class prefix filter rules
     */
    public static Set<String> getBuiltinClassRules() {
        return BUILTIN_CLASS_RULES;
    }

    /**
     * @return immutable set of built-in jar prefix filter rules
     */
    public static Set<String> getBuiltinJarRules() {
        return BUILTIN_JAR_RULES;
    }

    /**
     * Compatibility helper.
     */
    public static Set<String> getBuiltinFilterRules() {
        return BUILTIN_CLASS_RULES;
    }

    /**
     * @return true if className matches any built-in class prefix rule
     */
    public static boolean matchesBuiltinFilter(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        for (String rule : BUILTIN_CLASS_RULES) {
            if (className.startsWith(rule)) {
                return true;
            }
        }
        return false;
    }
}
