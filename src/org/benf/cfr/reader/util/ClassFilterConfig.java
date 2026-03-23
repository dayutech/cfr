package org.benf.cfr.reader.util;

import org.benf.cfr.reader.Main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * 类过滤配置加载器
 * 
 * 该类负责从配置文件加载类过滤规则。支持从以下两个位置加载配置文件：
 * 1. 当前工作目录
 * 2. CFR jar所在目录
 * 
 * 两个位置的配置文件规则会合并，共同构成完整的过滤规则集。
 * 
 * 配置文件格式：
 * 配置文件分为两个节：[jar] 和 [class]
 * 
 * [jar] 节：JAR文件名前缀匹配规则
 * - 用于根据JAR文件名过滤整个JAR包中的所有类
 * - 例如：spring-core, guava, commons-lang3
 * - 匹配时会忽略版本号后缀（如 spring-core-5.3.0.jar 会匹配 spring-core）
 * 
 * [class] 节：全类名前缀匹配规则
 * - 用于根据类的全限定名过滤特定类
 * - 例如：org.springframework, com.google.common
 * 
 * 注释：以 # 开头的行视为注释，空行会被忽略
 */
public class ClassFilterConfig {

    /**
     * 配置文件名称
     */
    public static final String CONFIG_FILE_NAME = "cfr_class_filter.conf";

    /**
     * JAR文件名过滤规则节标记
     */
    private static final String SECTION_JAR = "[jar]";

    /**
     * 类名过滤规则节标记
     */
    private static final String SECTION_CLASS = "[class]";

    /**
     * 配置规则结果类
     * 包含JAR文件名前缀规则和类名前缀规则
     */
    public static class FilterRules {
        private final Set<String> jarPrefixRules;
        private final Set<String> classPrefixRules;

        public FilterRules() {
            this.jarPrefixRules = new HashSet<String>();
            this.classPrefixRules = new HashSet<String>();
        }

        public FilterRules(Set<String> jarPrefixRules, Set<String> classPrefixRules) {
            this.jarPrefixRules = jarPrefixRules;
            this.classPrefixRules = classPrefixRules;
        }

        public Set<String> getJarPrefixRules() {
            return jarPrefixRules;
        }

        public Set<String> getClassPrefixRules() {
            return classPrefixRules;
        }

        public int getTotalRuleCount() {
            return jarPrefixRules.size() + classPrefixRules.size();
        }
    }

    /**
     * 获取当前工作目录下的配置文件路径
     * 
     * @return 工作目录下配置文件的完整路径
     */
    public static String getWorkingDirConfigPath() {
        return System.getProperty("user.dir") + File.separator + CONFIG_FILE_NAME;
    }

    /**
     * 获取CFR jar所在目录下的配置文件路径
     * 
     * 通过反射获取CFR主类所在的jar文件位置，然后定位配置文件路径。
     * 
     * @return CFR目录下配置文件的完整路径，如果无法确定则返回null
     */
    public static String getCfrDirConfigPath() {
        try {
            ProtectionDomain protectionDomain = Main.class.getProtectionDomain();
            URL location = protectionDomain.getCodeSource().getLocation();
            File file = new File(location.toURI());
            if (file.isFile()) {
                file = file.getParentFile();
            }
            if (file != null) {
                return file.getAbsolutePath() + File.separator + CONFIG_FILE_NAME;
            }
        } catch (URISyntaxException e) {
            System.out.println("警告: 无法确定CFR目录位置: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("警告: 安全限制，无法获取CFR目录位置: " + e.getMessage());
        }
        return null;
    }

    /**
     * 加载所有配置文件中的过滤规则
     * 
     * @return 包含所有配置文件规则的FilterRules对象
     */
    public static FilterRules loadFilterRules() {
        return loadFilterRules(false);
    }

    /**
     * 加载所有配置文件中的过滤规则
     * 
     * 从工作目录和CFR目录加载配置文件，并合并所有规则。
     * 如果配置文件不存在或读取失败，将跳过该文件。
     * 
     * @param silent 是否静默模式（不输出日志）
     * @return 包含所有配置文件规则的FilterRules对象
     */
    public static FilterRules loadFilterRules(boolean silent) {
        FilterRules result = new FilterRules();

        String workingDirPath = getWorkingDirConfigPath();
        FilterRules workingDirRules = loadConfigFromFile(workingDirPath, silent);
        if (workingDirRules.getTotalRuleCount() > 0) {
            mergeRules(result, workingDirRules);
            if (!silent) {
                System.out.println("从工作目录加载了 " + workingDirRules.getTotalRuleCount() + " 条过滤规则: " + workingDirPath);
            }
        }

        String cfrDirPath = getCfrDirConfigPath();
        if (cfrDirPath != null && !cfrDirPath.equals(workingDirPath)) {
            FilterRules cfrDirRules = loadConfigFromFile(cfrDirPath, silent);
            if (cfrDirRules.getTotalRuleCount() > 0) {
                int oldCount = result.getTotalRuleCount();
                mergeRules(result, cfrDirRules);
                int newRules = result.getTotalRuleCount() - oldCount;
                if (!silent) {
                    System.out.println("从CFR目录加载了 " + cfrDirRules.getTotalRuleCount() + " 条过滤规则（其中 " + newRules + " 条为新规则）: " + cfrDirPath);
                }
            }
        }

        return result;
    }

    /**
     * 合并两个FilterRules对象
     */
    private static void mergeRules(FilterRules target, FilterRules source) {
        target.jarPrefixRules.addAll(source.jarPrefixRules);
        target.classPrefixRules.addAll(source.classPrefixRules);
    }

    /**
     * 从指定文件加载过滤规则
     * 
     * 解析配置文件，提取有效的JAR前缀和类名前缀规则。
     * 配置文件格式支持两个节：[jar] 和 [class]
     * 
     * @param filePath 配置文件的完整路径
     * @param silent 是否静默模式
     * @return 包含该文件中所有有效规则的FilterRules对象
     */
    private static FilterRules loadConfigFromFile(String filePath, boolean silent) {
        FilterRules rules = new FilterRules();
        File file = new File(filePath);

        if (!file.exists()) {
            return rules;
        }

        if (!file.canRead()) {
            if (!silent) {
                System.out.println("警告: 无法读取配置文件: " + filePath);
            }
            return rules;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineNumber = 0;
            int currentSection = 0; // 0: 未指定, 1: jar节, 2: class节

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // 跳过空行
                if (line.isEmpty()) {
                    continue;
                }

                // 跳过注释行
                if (line.startsWith("#")) {
                    continue;
                }

                // 检查节标记
                if (line.equalsIgnoreCase(SECTION_JAR)) {
                    currentSection = 1;
                    continue;
                } else if (line.equalsIgnoreCase(SECTION_CLASS)) {
                    currentSection = 2;
                    continue;
                }

                // 根据当前节解析规则
                if (currentSection == 1) {
                    // JAR节规则
                    String jarPrefix = normalizeJarPrefix(line);
                    if (jarPrefix != null) {
                        rules.jarPrefixRules.add(jarPrefix);
                    } else {
                        if (!silent) {
                            System.out.println("警告: 配置文件第 " + lineNumber + " 行包含无效的JAR前缀: " + line);
                        }
                    }
                } else if (currentSection == 2) {
                    // 类名节规则
                    if (isValidPackagePrefix(line)) {
                        rules.classPrefixRules.add(line);
                    } else {
                        if (!silent) {
                            System.out.println("警告: 配置文件第 " + lineNumber + " 行包含无效的类名前缀: " + line);
                        }
                    }
                } else {
                    // 未指定节时的兼容处理：默认作为类名前缀
                    if (isValidPackagePrefix(line)) {
                        rules.classPrefixRules.add(line);
                    } else {
                        if (!silent) {
                            System.out.println("警告: 配置文件第 " + lineNumber + " 行包含无效的前缀（未指定节）: " + line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (!silent) {
                System.out.println("警告: 读取配置文件时发生错误: " + filePath + " - " + e.getMessage());
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // 忽略关闭错误
                }
            }
        }

        return rules;
    }

    /**
     * 规范化JAR文件名前缀
     * 
     * JAR文件名前缀应该是有效的标识符，可以包含字母、数字、连字符和下划线
     * 
     * @param prefix 原始前缀字符串
     * @return 规范化后的前缀，如果无效则返回null
     */
    private static String normalizeJarPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }

        // 转换为小写以便不区分大小写匹配
        prefix = prefix.toLowerCase().trim();

        // 移除可能的.jar后缀
        if (prefix.endsWith(".jar")) {
            prefix = prefix.substring(0, prefix.length() - 4);
        }

        if (prefix.isEmpty()) {
            return null;
        }

        // 验证字符：只允许字母、数字、连字符、下划线和点
        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '-' && c != '_' && c != '.') {
                return null;
            }
        }

        return prefix;
    }

    /**
     * 验证包名前缀是否有效
     * 
     * 有效的包名前缀应符合Java包命名规范：
     * - 只包含字母、数字、下划线、点和美元符号
     * - 不能以数字或点开头
     * - 不能以点结尾
     * 
     * @param prefix 要验证的包名前缀
     * @return 如果前缀有效返回true，否则返回false
     */
    private static boolean isValidPackagePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return false;
        }

        if (prefix.startsWith(".") || prefix.endsWith(".")) {
            return false;
        }

        if (Character.isDigit(prefix.charAt(0))) {
            return false;
        }

        for (int i = 0; i < prefix.length(); i++) {
            char c = prefix.charAt(i);
            if (!Character.isJavaIdentifierPart(c) && c != '.') {
                return false;
            }
        }

        return true;
    }
}
