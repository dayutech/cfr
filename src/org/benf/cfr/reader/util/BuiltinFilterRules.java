package org.benf.cfr.reader.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 内置过滤规则类
 * 
 * 该类提供了常见的第三方依赖包名前缀集合，用于在反编译过程中
 * 过滤掉不需要分析的标准库和第三方库类。
 * 
 * 这些过滤规则可以帮助用户专注于分析自己项目的代码，
 * 而忽略那些已经广泛使用且可信的第三方依赖。
 */
public class BuiltinFilterRules {

    /**
     * 内置过滤规则集合
     * 包含常见的第三方依赖包名前缀和Java标准库前缀
     */
    private static final Set<String> BUILTIN_FILTER_RULES;

    static {
        Set<String> rules = new HashSet<String>();

        // Spring框架相关
        rules.add("org.springframework");

        // Apache基金会相关项目
        rules.add("org.apache.commons");
        rules.add("org.apache.logging");
        rules.add("org.apache.log4j");
        rules.add("org.apache.maven");
        rules.add("org.apache.http");
        rules.add("org.apache.kafka");

        // Google相关库
        rules.add("com.google.common");
        rules.add("com.google.gson");
        rules.add("com.google.code");

        // 日志框架
        rules.add("org.slf4j");
        rules.add("ch.qos.logback");

        // Netty网络框架
        rules.add("io.netty");

        // 响应式编程框架
        rules.add("io.reactivex");
        rules.add("rx.");

        // 测试框架
        rules.add("org.junit");
        rules.add("org.mockito");
        rules.add("org.hamcrest");

        // JSON处理库
        rules.add("com.fasterxml.jackson");

        // ORM框架
        rules.add("org.hibernate");

        // Java标准库和扩展
        rules.add("javax.");
        rules.add("java.");

        // Sun/Oracle内部API
        rules.add("sun.");
        rules.add("com.sun.");

        // 标准组织相关
        rules.add("org.w3c");
        rules.add("org.xml");
        rules.add("org.omg");
        rules.add("org.ietf");
        rules.add("org.jcp");

        // JDK内部
        rules.add("jdk.");

        BUILTIN_FILTER_RULES = Collections.unmodifiableSet(rules);
    }

    /**
     * 获取内置过滤规则集合
     * 
     * 返回一个不可修改的Set集合，包含所有预定义的第三方依赖包名前缀。
     * 这些前缀可用于匹配类名，判断是否应该被过滤。
     * 
     * @return 包含内置过滤规则的不可修改Set集合
     */
    public static Set<String> getBuiltinFilterRules() {
        return BUILTIN_FILTER_RULES;
    }

    /**
     * 检查给定的类名是否匹配任何内置过滤规则
     * 
     * @param className 要检查的类名
     * @return 如果类名以任何过滤规则前缀开头，返回true；否则返回false
     */
    public static boolean matchesBuiltinFilter(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        for (String rule : BUILTIN_FILTER_RULES) {
            if (className.startsWith(rule)) {
                return true;
            }
        }
        return false;
    }
}
