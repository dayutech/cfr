package org.benf.cfr.reader.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 内置过滤规则类
 *
 * 该类提供了常见的第三方依赖包名前缀和JAR文件名前缀集合，
 * 用于在反编译过程中过滤掉不需要分析的标准库和第三方库。
 *
 * 这些过滤规则可以帮助用户专注于分析自己项目的代码，
 * 而忽略那些已经广泛使用且可信的第三方依赖。
 */
public class BuiltinFilterRules {

    /**
     * 内置类名前缀过滤规则
     * 包含常见的第三方依赖包名前缀
     */
    private static final Set<String> BUILTIN_CLASS_RULES;

    /**
     * 内置JAR文件名前缀过滤规则
     * 包含常见的第三方JAR文件名前缀
     */
    private static final Set<String> BUILTIN_JAR_RULES;

    static {
        // ==================== 内置类名过滤规则 ====================
        Set<String> classRules = new HashSet<String>();

        // Spring框架相关
        classRules.add("org.springframework");

        // Apache基金会相关项目
        classRules.add("org.apache.commons");
        classRules.add("org.apache.logging");
        classRules.add("org.apache.log4j");
        classRules.add("org.apache.maven");
        classRules.add("org.apache.http");
        classRules.add("org.apache.kafka");

        // Google相关库
        classRules.add("com.google.common");
        classRules.add("com.google.gson");
        classRules.add("com.google.code");

        // 日志框架
        classRules.add("org.slf4j");
        classRules.add("ch.qos.logback");

        // Netty网络框架
        classRules.add("io.netty");

        // 响应式编程框架
        classRules.add("io.reactivex");
        classRules.add("rx.");

        // 测试框架
        classRules.add("org.junit");
        classRules.add("org.mockito");
        classRules.add("org.hamcrest");

        // JSON处理库
        classRules.add("com.fasterxml.jackson");

        // ORM框架
        classRules.add("org.hibernate");

        // Java标准库和扩展
        classRules.add("javax.");
        classRules.add("java.");

        // Sun/Oracle内部API
        classRules.add("sun.");
        classRules.add("com.sun.");

        // 标准组织相关
        classRules.add("org.w3c");
        classRules.add("org.xml");
        classRules.add("org.omg");
        classRules.add("org.ietf");
        classRules.add("org.jcp");

        // JDK内部
        classRules.add("jdk.");

        BUILTIN_CLASS_RULES = Collections.unmodifiableSet(classRules);

        // ==================== 内置JAR过滤规则 ====================
        Set<String> jarRules = new HashSet<String>();

        // Spring框架相关JAR
        jarRules.add("spring-core");
        jarRules.add("spring-context");
        jarRules.add("spring-beans");
        jarRules.add("spring-web");
        jarRules.add("spring-webmvc");
        jarRules.add("spring-aop");
        jarRules.add("spring-data");
        jarRules.add("spring-boot");
        jarRules.add("spring-cloud");

        // Apache Commons系列JAR
        jarRules.add("commons-lang3");
        jarRules.add("commons-lang2");
        jarRules.add("commons-lang");
        jarRules.add("commons-io");
        jarRules.add("commons-collections");
        jarRules.add("commons-collections4");
        jarRules.add("commons-beanutils");
        jarRules.add("commons-codec");
        jarRules.add("commons-compress");
        jarRules.add("commons-fileupload");
        jarRules.add("commons-httpclient");
        jarRules.add("commons-net");
        jarRules.add("commons-pool");
        jarRules.add("commons-dbcp");
        jarRules.add("commons-digester");
        jarRules.add("commons-email");
        jarRules.add("commons-scxml");
        jarRules.add("commons-validator");
        jarRules.add("commons-math");
        jarRules.add("commons-text");
        jarRules.add("commons-discovery");
        jarRules.add("commons-vfs");

        // Apache Log4j系列JAR
        jarRules.add("log4j");
        jarRules.add("log4j-api");
        jarRules.add("log4j-core");
        jarRules.add("log4j-slf4j");
        jarRules.add("log4j12");

        // Apache Log4j2系列JAR
        jarRules.add("log4j-api-2");
        jarRules.add("log4j-core-2");
        jarRules.add("log4j-slf4j-2");

        // Apache Logging系列JAR
        jarRules.add("logging-log4j");

        // Apache HttpComponents系列JAR
        jarRules.add("httpcore");
        jarRules.add("httpclient");
        jarRules.add("httpmime");

        // Apache Kafka系列JAR
        jarRules.add("kafka");
        jarRules.add("kafka-clients");
        jarRules.add("kafka-streams");

        // Apache Maven系列JAR
        jarRules.add("maven");
        jarRules.add("maven-artifact");
        jarRules.add("maven-core");
        jarRules.add("maven-model");
        jarRules.add("maven-plugin-api");
        jarRules.add("maven-repository-metadata");

        // Apache Axis系列JAR
        jarRules.add("axis");
        jarRules.add("axis2");
        jarRules.add("axis2-ant-plugin");
        jarRules.add("axis2-adb");
        jarRules.add("axis2-adb-codegen");
        jarRules.add("axis2-codegen");
        jarRules.add("axis2-clustering");
        jarRules.add("axis2-corba");
        jarRules.add("axis2-fastinfoset");
        jarRules.add("axis2-java2wsdl");
        jarRules.add("axis2-jaxbri-codegen");
        jarRules.add("axis2-jaxws");
        jarRules.add("axis2-jibx");
        jarRules.add("axis2-jibx-codegen");
        jarRules.add("axis2-json");
        jarRules.add("axis2-kernel");
        jarRules.add("axis2-metadata");
        jarRules.add("axis2-mtompolicy");
        jarRules.add("axis2-saaj");
        jarRules.add("axis2-soapmonitor-servlet");
        jarRules.add("axis2-spring");
        jarRules.add("axis2-transport-base");
        jarRules.add("axis2-transport-http");
        jarRules.add("axis2-transport-jms");
        jarRules.add("axis2-transport-local");
        jarRules.add("axis2-transport-mail");
        jarRules.add("axis2-transport-tcp");
        jarRules.add("axis2-transport-udp");
        jarRules.add("axis2-transport-xmpp");
        jarRules.add("axis2-xmlbeans");
        jarRules.add("axis2-xmlbeans-codegen");

        // Apache CXF系列JAR
        jarRules.add("cxf");
        jarRules.add("cxf-core");
        jarRules.add("cxf-rt");
        jarRules.add("cxf-services");

        // Apache Neethi系列JAR (WS-Policy)
        jarRules.add("neethi");

        // Apache Axiom系列JAR (SOAP)
        jarRules.add("axiom-api");
        jarRules.add("axiom-dom");
        jarRules.add("axiom-impl");
        jarRules.add("axiom-jaxb");
        jarRules.add("axiom-truth");

        // Apache Woden系列JAR (WS-Addressing)
        jarRules.add("woden-core");

        // Apache WSdl4J系列JAR
        jarRules.add("wsdl4j");

        // Apache XMLBeans系列JAR
        jarRules.add("xmlbeans");
        jarRules.add("xmlschema-core");

        // Apache Xalan系列JAR (XSLT)
        jarRules.add("xalan");
        jarRules.add("serializer");

        // Apache StAX Utils系列JAR
        jarRules.add("stax-utils");

        // Apache XML Truth系列JAR
        jarRules.add("xml-truth");

        // Apache Ant系列JAR
        jarRules.add("ant");

        // Apache Avro系列JAR
        jarRules.add("avro");

        // Apache Derby系列JAR
        jarRules.add("derby");
        jarRules.add("derbytools");

        // Google Guava系列JAR
        jarRules.add("guava");
        jarRules.add("guava-gwt");
        jarRules.add("guava-testlib");

        // Google Gson系列JAR
        jarRules.add("gson");

        // Google Protocol Buffers
        jarRules.add("protobuf-java");
        jarRules.add("protobuf-javascript");

        // Bouncy Castle系列JAR
        jarRules.add("bcpkix");
        jarRules.add("bcprov");
        jarRules.add("bcutil");

        // SLF4J系列JAR
        jarRules.add("slf4j-api");
        jarRules.add("slf4j-simple");
        jarRules.add("slf4j-log4j12");
        jarRules.add("slf4j-nop");
        jarRules.add("slf4j-reload4j");
        jarRules.add("slf4j-simple");
        jarRules.add("slf4j-jdk14");

        // Logback系列JAR
        jarRules.add("logback-classic");
        jarRules.add("logback-core");

        // Netty系列JAR
        jarRules.add("netty");
        jarRules.add("netty-all");
        jarRules.add("netty-handler");
        jarRules.add("netty-buffer");
        jarRules.add("netty-codec");
        jarRules.add("netty-common");
        jarRules.add("netty-transport");
        jarRules.add("netty-resolver");
        jarRules.add("netty-example");
        jarRules.add("netty-tcnative-boringssl-static");

        // RxJava系列JAR
        jarRules.add("rxjava");
        jarRules.add("rxjavassist");
        jarRules.add("rxjavadoc");

        // JUnit系列JAR
        jarRules.add("junit");
        jarRules.add("junit4");
        jarRules.add("junit-jupiter");
        jarRules.add("junit-vintage");
        jarRules.add("junit-platform");

        // Mockito系列JAR
        jarRules.add("mockito");
        jarRules.add("mockito-core");
        jarRules.add("mockito-inline");
        jarRules.add("mockito-java8");

        // EasyMock系列JAR
        jarRules.add("easymock");

        // PowerMock系列JAR
        jarRules.add("powermock-easymock");

        // Hamcrest系列JAR
        jarRules.add("hamcrest");
        jarRules.add("hamcrest-core");
        jarRules.add("hamcrest-library");

        // Jackson系列JAR
        jarRules.add("jackson-core");
        jarRules.add("jackson-databind");
        jarRules.add("jackson-annotations");
        jarRules.add("jackson-dataformat");
        jarRules.add("jackson-module");
        jarRules.add("jackson-jaxrs");
        jarRules.add("jackson-databind-jr");
        jarRules.add("jackson-core-asl");

        // Hibernate系列JAR
        jarRules.add("hibernate-core");
        jarRules.add("hibernate-entitymanager");
        jarRules.add("hibernate-validator");
        jarRules.add("hibernate-ogm");
        jarRules.add("hibernate-envers");
        jarRules.add("hibernate-tools");

        // MyBatis系列JAR
        jarRules.add("mybatis");
        jarRules.add("mybatis-spring");

        // MySQL驱动JAR
        jarRules.add("mysql-connector-java");
        jarRules.add("mysql-connector-j");

        // PostgreSQL驱动JAR
        jarRules.add("postgresql");

        // Oracle驱动JAR
        jarRules.add("ojdbc");
        jarRules.add("orai18n");
        jarRules.add("ucp");

        // MariaDB驱动JAR
        jarRules.add("mariadb-java-client");

        // H2数据库驱动JAR
        jarRules.add("h2");

        // Derby数据库驱动JAR
        jarRules.add("derby");
        jarRules.add("derbytools");

        // Lombok
        jarRules.add("lombok");

        // Javassist
        jarRules.add("javassist");

        // CGLIB
        jarRules.add("cglib-nodep");

        // Objenesis (用于Mocking库)
        jarRules.add("objenesis");

        // DOM4J (XML处理)
        jarRules.add("dom4j");

        // Jettison (JSON/XML转换)
        jarRules.add("jettison");

        // OkHttp系列JAR
        jarRules.add("okhttp");
        jarRules.add("okio-jvm");
        jarRules.add("okhttp-digest");

        // OpenTracing系列JAR
        jarRules.add("opentracing-api");

        // OpenCSV系列JAR
        jarRules.add("opencsv");

        // DNS Java系列JAR
        jarRules.add("dnsjava");

        // SNMP4J系列JAR
        jarRules.add("snmp4j");
        jarRules.add("snmp4j-agent");
        jarRules.add("snmp4j-agentx");

        // Apache MINA sshd系列JAR
        jarRules.add("sshd-core");
        jarRules.add("sshd-common");
        jarRules.add("sshd-sftp");

        // Snappy系列JAR (压缩)
        jarRules.add("snappy-java");

        // Prometheus simpleclient系列JAR
        jarRules.add("simpleclient");
        jarRules.add("simpleclient_common");
        jarRules.add("simpleclient_httpserver");

        // JFreeChart系列JAR (图表)
        jarRules.add("jfreechart");
        jarRules.add("jcommon");

        // JGraph系列JAR (图形库)
        jarRules.add("jgraph");

        // JGraphT系列JAR (图论库)
        jarRules.add("jgrapht-jdk1.6");

        // JRADIUS系列JAR
        jarRules.add("jradius-core");
        jarRules.add("jradius-dictionary");

        // jose4j系列JAR (JWT)
        jarRules.add("jose4j");

        // ED DSA系列JAR (加密)
        jarRules.add("eddsa");

        // Gnu Crypto系列JAR
        jarRules.add("gnu-crypto");

        // SMPP系列JAR (SMS协议)
        jarRules.add("smpp");

        // Threeten BP系列JAR (Java 8日期时间回 port)
        jarRules.add("threetenbp");

        // Tomcat系列JAR
        jarRules.add("tomcat-api");
        jarRules.add("tomcat-catalina");
        jarRules.add("tomcat-coyote");
        jarRules.add("tomcat-jasper");
        jarRules.add("tomcat-juli");
        jarRules.add("tomcat-util");

        // Servlet/JSP API系列JAR
        jarRules.add("javax.servlet-api");
        jarRules.add("javax.servlet.jsp-api");
        jarRules.add("jsp-api");
        jarRules.add("servlet-api");

        // JAX-WS系列JAR
        jarRules.add("jaxrpc-api");
        jarRules.add("jaxws-tools");
        jarRules.add("jaxrpc");

        // SAAJ系列JAR (SOAP with Attachments)
        jarRules.add("saaj");
        jarRules.add("saaj-api");

        // JAX-RS系列JAR
        jarRules.add("jsr173_api");

        // JS-Release系列JAR
        jarRules.add("jsr305");

        // Coverity Escapers系列JAR
        jarRules.add("coverity-escapers");

        // TestUtils系列JAR
        jarRules.add("testutils");

        // DBUnit系列JAR
        jarRules.add("dbunit");

        // Java JWT系列JAR
        jarRules.add("java-jwt");

        // ONS系列JAR (Oracle NoSQL)
        jarRules.add("ons");

        // Kafka客户端系列JAR
        jarRules.add("kafka-clients");

        // Kotlin标准库系列JAR
        jarRules.add("kotlin-stdlib");

        // Apache VFS系列JAR
        jarRules.add("commons-vfs");
        jarRules.add("commons-vfs-sandbox");

        // WebDAV Servlet系列JAR
        jarRules.add("webdav-servlet");

        // Certificate Management JAR
        jarRules.add("classes15");

        // JUnit 3 Compatible
        jarRules.add("Cal-Code");

        // Core Util
        jarRules.add("core");

        BUILTIN_JAR_RULES = Collections.unmodifiableSet(jarRules);
    }

    /**
     * 获取内置类名过滤规则集合
     *
     * @return 包含内置类名过滤规则的不可修改Set集合
     */
    public static Set<String> getBuiltinClassRules() {
        return BUILTIN_CLASS_RULES;
    }

    /**
     * 获取内置JAR过滤规则集合
     *
     * @return 包含内置JAR过滤规则的不可修改Set集合
     */
    public static Set<String> getBuiltinJarRules() {
        return BUILTIN_JAR_RULES;
    }

    /**
     * 获取所有内置过滤规则（兼容方法）
     *
     * @return 包含所有内置过滤规则的不可修改Set集合
     */
    public static Set<String> getBuiltinFilterRules() {
        return BUILTIN_CLASS_RULES;
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
        for (String rule : BUILTIN_CLASS_RULES) {
            if (className.startsWith(rule)) {
                return true;
            }
        }
        return false;
    }
}
