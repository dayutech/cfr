# CFR - 另一个 Java 反编译器 \o/

[English](README_en.md)

这是 CFR 反编译器的公共代码库，主站托管于 <a href="https://www.benf.org/other/cfr">benf.org/other/cfr</a>

CFR 可以反编译现代 Java 特性 - <a href="https://www.benf.org/other/cfr/java9observations.html">包括 Java <a href="java9stringconcat.html">9</a>、<a href="https://www.benf.org/other/cfr/switch_expressions.html">12</a> 和 <a href="https://www.benf.org/other/cfr/java14instanceof_pattern">14</a> 的许多特性</a>，但它完全使用 Java 6 编写，因此可以在任何地方使用！（<a href="https://www.benf.org/other/cfr/faq.html">常见问题</a>）- 它甚至可以很好地反编译其他 JVM 语言生成的 class 文件！

使用方法：只需运行特定版本的 JAR 文件，并提供要反编译的类名（可以是 class 文件的路径，也可以是类路径上的完全限定类名）。
（使用 `--help` 查看参数列表）。

或者，要反编译整个 JAR，只需提供 JAR 路径，如果想输出文件（你可能需要！），请添加 `--outputdir /tmp/putithere`。

如果目标路径是目录，CFR 会递归扫描该目录及其所有子目录，并反编译找到的每个 `.class` 和 `.jar` 文件。

## 类过滤【新特性】

CFR 支持在反编译过程中过滤掉已知的第三方库类。这可以显著加快大型 JAR 文件的反编译速度，通过跳过来自 Spring、Apache Commons、Guava 等常见库的类来提升性能。

### 启用类过滤

使用 `--enableclassfilter` 选项启用类过滤：

```
java -jar cfr.jar myapp.jar --enableclassfilter
```

如果希望在任务结束后输出“被整包跳过”的 JAR 列表，可额外添加 `--showskippedjars`（受 `--silent` 控制）。

### 内置过滤规则

CFR 现在只保留**一小部分默认内置规则**（Spring、Apache Commons/Log4j、Guava/Gson、SLF4J/Logback、Netty、Jackson、JUnit、Mockito 等流行库）。

**扩展的第三方规则**已移至 `cfr_class_filter.conf`，以便您可以轻松编辑、删除或添加规则，而无需更改 Java 代码。

### 自定义过滤配置

您可以通过创建名为 `cfr_class_filter.conf` 的配置文件来添加自定义过滤规则。CFR 按以下顺序搜索此文件：

1. 当前工作目录
2. CFR JAR 所在目录

两个配置文件都与内置规则合并。

#### 配置文件格式

配置文件支持两个节：`[jar]` 和 `[class]`。

```
# 这是注释

# JAR 文件名前缀规则 - 过滤整个 JAR
[jar]
spring-core
spring-context
guava
commons-lang3
jackson-databind

# 类名前缀规则 - 过滤特定类
[class]
com.mycompany.internal
com.mycompany.thirdparty
org.mylibrary
```

**节详情：**

- **`[jar]` 节**：JAR 文件名前缀匹配规则
  - 根据 JAR 文件名（不含版本后缀）过滤整个 JAR 文件
  - 示例：`spring-core` 将匹配 `spring-core-5.3.0.jar`、`spring-core-6.0.0.jar`
  - 匹配不区分大小写
  - 版本号自动处理（例如 `guava` 匹配 `guava-31.1-jre.jar`）

- **`[class]` 节**：完整类名前缀匹配规则
  - 根据类的完全限定名过滤特定类
  - 示例：`org.springframework` 将匹配 `org.springframework.core.xxx`
  - 支持包前缀（`org.springframework`）和点边界前缀（`java.`、`rx.`）
  - 对于 fat-jar/war/MR-JAR 场景，匹配前会自动规范化 `BOOT-INF/classes/`、`WEB-INF/classes/`、`META-INF/versions/<版本>/` 前缀，因此 `io.vertx.`、`javax.` 等规则可直接生效

### 全类名去重缓存（可选）

可通过 `--enableclassnamecache` 启用“已反编译全类名缓存”，避免在同一次任务中重复反编译同名类。

生效条件：

- 必须同时使用 `--flatoutput` 和 `--flatnojardir`
- 若缺少任一条件，`--enableclassnamecache` 会被忽略（控制台会提示）

行为说明：

- 缓存按全类名层级拆分存储（例如 `com -> test1 -> test2 -> ... -> Test`）
- 反编译前先查缓存，命中则跳过，未命中则继续反编译并写入缓存
- 缓存有容量上限，清理时优先移除“命中次数少且层级更深”的条目，以控制内存占用

### 快捷模式（可选）

可通过 `--quickmode` 一次性启用以下选项：

- `--enableclassfilter`
- `--flatoutput`
- `--flatnojardir`
- `--enableclassnamecache`
- `--showskippedjars`（兼容别名 `--showskipedjars`）

### 扩展过滤规则

`cfr_class_filter.conf` 中的扩展过滤规则涵盖 **390 个类前缀**和 **210 个 JAR 文件名前缀**，包括：

**流行库：**
- Spring Framework (spring-*、org.springframework.*)
- Apache Commons (commons-*、org.apache.*)
- Jackson JSON (jackson-*、com.fasterxml.jackson.*)
- SLF4J/Logback (org.slf4j.*、ch.qos.logback.*)
- Netty (io.netty.*)
- Bouncy Castle (org.bouncycastle.*)

**数据库/ORM：**
- MySQL、PostgreSQL、Oracle、SQLite、MongoDB、Cassandra
- Hibernate、MyBatis、JPA

**消息队列和网络：**
- Apache Kafka、RabbitMQ、Redis 客户端
- Netty、OkHttp、OkIo
- SNMP4J、SSH 库

**代码分析和生成：**
- ASM、Javassist、cglib、ByteBuddy
- ANTLR、Groovy

**测试：**
- JUnit、Mockito、EasyMock、PowerMock、Hamcrest

**以及更多第三方库...**

### 规则去重策略

类规则会按“前缀覆盖关系”进行全局精简：

- 若存在更广泛前缀，已被覆盖的精确规则会被移除。
- 示例：存在 `log4j` 时，`log4j-core` 视为冗余并移除。
- 这样可以在不改变实际过滤效果的前提下保持规则简洁。

### 使用 JAR 扫描生成类规则

可以通过 `scan_jar_packages.py` 扫描 JAR 并更新 `[class]` 规则：

```bash
python scan_jar_packages.py --scan-dir "D:\WorkDirQiax\VulnDiscov" --config-file "cfr_class_filter.conf" --min-count 10
```

说明：

- 脚本会递归扫描 `--scan-dir` 下所有 `.jar` 文件。
- 提取到的类名前缀会合并到 `[class]`。
- 被更广泛前缀覆盖的精确规则会自动剔除。

**传统格式（向后兼容）：**

没有节标题的行被视为类名前缀规则：

```
# 传统格式 - 作为类规则处理
com.mycompany.internal
org.mylibrary
```

**文件格式规则：**

- 每行应包含一条规则
- 以 `#` 开头的行被视为注释
- 空行被忽略
- 节标题（`[jar]`、`[class]`）不区分大小写

### 使用示例

```bash
# 使用类过滤启用反编译 JAR
java -jar cfr.jar myapp.jar --enableclassfilter --outputdir ./output

# 反编译在目录下递归找到的所有 class/jar 文件
java -jar cfr.jar ./input-dir --outputdir ./output

# 递归扫描目录并同时应用类/JAR 过滤
java -jar cfr.jar ./input-dir --enableclassfilter --outputdir ./output

# 平坦输出（递归目录输入）- 默认仍保留 JAR 的相对目录前缀
java -jar cfr.jar ./input-dir --outputdir ./output --flatoutput

# 平坦输出 + 不保留 JAR 目录前缀（不会生成 test/ 这类 JAR 目录）
java -jar cfr.jar ./input-dir --outputdir ./output --flatoutput --flatnojardir

# 平坦输出模式下启用全类名去重缓存（避免重复反编译同名类）
java -jar cfr.jar ./input-dir --outputdir ./output --flatoutput --flatnojardir --enableclassnamecache

# 快捷模式（等效于同时开启 classfilter/flatoutput/flatnojardir/classnamecache/showskippedjars）
java -jar cfr.jar ./input-dir --outputdir ./output --quickmode

# 启用类过滤并在结束后打印被跳过的 JAR 列表
java -jar cfr.jar ./input-dir --enableclassfilter --showskippedjars --outputdir ./output

# 显示 CFR 版本注释头（默认关闭）
java -jar cfr.jar myapp.jar --showversion

# 显示反编译器状态注释（默认关闭）
java -jar cfr.jar myapp.jar --comments

# 查看过滤选项的帮助
java -jar cfr.jar --help enableclassfilter

# 查看全类名缓存选项（flag）帮助
java -jar cfr.jar --help enableclassnamecache
```

# 获取 CFR

CFR 的主站是 <a href="https://www.benf.org/other/cfr">benf.org/other/cfr</a>，可以在此处下载发布版本。

自 0.145 版本起，二进制文件已随发布标签一起发布在 github 上。

您还可以从您最喜欢的 <a href="https://mvnrepository.com/artifact/org.benf/cfr">maven</a> 仓库下载 CFR，但发布通常会延迟几天，以允许发布后悔期。

# 问题

如果您遇到问题，请**不要**包含版权材料。否则我必须删除您的 issue。

# 构建 CFR

非常简单！

只需确保已安装 [Maven](https://maven.apache.org/)。然后在项目根目录运行 `mvn compile` 即可获得所需内容。

注意：如果在尝试编译项目时遇到 `maven-compiler-plugin...: Compilation failure` 错误，那么您的 `JAVA_HOME` 环境变量可能指向不支持 `6` 作为 `source` 或 `target` 编译选项的 JDK 版本。请将 `JAVA_HOME` 指向支持编译到 Java 1.6 的 JDK 版本（如 JDK 11）。还要注意，如果您使用的 Maven 版本 `>=3.3.1`，则您使用的 Java 版本可能需要大于 1.6（因为 Maven 3.3.1 需要 Java 1.7）。最佳解决方案是为 `PATH` 和 `JAVA_HOME` 使用 JDK 8、9、10 或 11。

主类是 `org.benf.cfr.reader.Main`，因此构建完成后，您可以（从 `target/classes`）测试它：
```
java org.benf.cfr.reader.Main java.lang.Object
```
让 CFR 反编译 `java.lang.Object`。


## 反编译测试

作为 Maven 构建的一部分，会自动执行反编译测试。它们验证 CFR 的当前反编译输出与预期的先前输出是否匹配。测试数据（Java 类和 JAR 文件）是单独 Git 仓库的一部分；因此需要使用 `git clone --recurse-submodules` 克隆此仓库。预期输出和 CFR 测试配置是此仓库的一部分，以便在不修改相应测试数据的情况下进行更改。测试数据在 `decompilation-test/test-data` 目录中，相应的预期数据和自定义配置在 `decompilation-test/test-data-expected-output` 目录中（具有类似的目录结构，请参阅下面的[预期数据结构](#预期数据结构)）。

反编译测试也由 GitHub workflow 执行，如果测试失败，统一 diff 可在名为 "decompilation-test-failures-diff" 的 [workflow artifact](https://docs.github.com/en/actions/managing-workflow-runs/downloading-workflow-artifacts) 中获取。

**预期输出不是黄金标准**，它只是描述当前预期的输出。如果反编译结果的更改是合理的，可以调整预期输出。

测试类是 [`org.benf.cfr.test.DecompilationTest`](decompilation-test/src/org/benf/cfr/test/DecompilationTest.java)。可以修改它来调整测试目录，或忽略某些类文件或 JAR。此外，还可以直接从 IDE 执行那里的测试。这通常比 Maven 显示的结果更好，并且允许使用内置 IDE 功能显示预期数据和实际数据之间的差异。

### 选项文件

可以通过添加选项文件来自定义反编译过程。它的每一行指定一个 CFR 选项，键和值用空格分隔。空行和以 `#` 开头的行被忽略，可用于注释。

示例：
```
# 启用标识符重命名
renameillegalidents true
```

请参阅下面的[预期数据结构](#预期数据结构)了解如何命名文件以及放置位置。

### 预期数据结构

#### 类文件

对于类文件，预期数据和自定义配置位于 `test-data-expected-output` 下相应的位置，文件名基于类文件名。

例如，对于类文件 `test-data/classes/subdir/MyClass.class`，可以使用以下文件：

- `test-data-expected-output/classes/subdir/`
    - `MyClass.expected.java`
      包含预期的反编译 Java 输出，可选地带[反编译注释](#反编译注释)。
    - `MyClass.options`
      可选的[选项文件](#选项文件)，用于自定义反编译。
    - `MyClass.expected.summary`
      包含 CFR API 报告的预期摘要。当不产生摘要时可以省略。
    - `MyClass.expected.exceptions`
      包含 CFR API 报告的预期异常。当不报告异常时可以省略。

#### JAR 文件

对于 JAR 文件，预期数据和自定义配置位于 `test-data-expected-output` 下相应位置下同名目录中。预期 Java 输出文件在其文件名中包含包名，例如 `mypackage.MyClass.java`。选项文件和预期摘要及异常文件使用 "文件名" `_`。[多版本 JAR](https://openjdk.java.net/jeps/238) 的目录包含版本特定类的子目录。目录名的形式为 `java-<version>`。

例如，对于多版本 JAR 文件 `test-data/jars/subdir/MyJar.jar`，可以使用以下文件：
- `test-data-expected-output/jars/subdir/MyJar/`
    - `mypackage.MyClass.java`
      包含类 `mypackage.MyClass` 的预期反编译 Java 输出，可选地带[反编译注释](#反编译注释)。
    - `java-11/mypackage.MyClass.java`
      包含特定于 Java 11 及更高版本的类文件的预期反编译 Java 输出（用于多版本 JAR）。
    - `_.options`
      可选的[选项文件](#选项文件)，用于自定义反编译。
    - `_.expected.summary`
      包含 CFR API 报告的预期摘要。当不产生摘要时可以省略。
    - `_.expected.exceptions`
      包含 CFR API 报告的预期异常。当不报告异常时可以省略。

### 反编译注释

预期的 Java 输出文件支持注释表示_反编译注释_。在与实际 Java 输出进行比较时会忽略它们，例如可以用于指示不正确或可改进的 CFR 输出。有两种反编译注释：

- 行注释：以 `//#` 开头（可选地以空白字符为前缀）
- 行内注释：`/*# ... #*/`

应谨慎使用行反编译注释，特别是在大文件中，因为它们会在比较期间移动行号（由于在比较时被删除），可能会造成混淆。

示例：
```java
//# 行反编译注释
public class MyClass {
    public static void main(String[] stringArray/*# 行内反编译注释 #*/) {
        ...
    }
}
```

### 更新/创建预期数据

当为反编译测试添加大量新类或 JAR 文件时，或者当 CFR 的更改影响大量类或 JAR 文件的输出时，手动创建或更新预期数据可能相当繁琐。对于这些情况，存在以下系统属性可以帮助您。它们可以在运行测试时使用 `-D<system-property>` 设置。但是，当设置这些系统属性时，相应的测试仍会失败（但预期数据会更新），以防止将它们用于常规测试执行。

- `cfr.decompilation-test.create-expected`
  基于当前 CFR 输出生成所有缺失的预期测试数据。
- `cfr.decompilation-test.update-expected`
  更新预期测试数据以匹配 CFR 生成的实际数据。请注意，这不适用于使用[反编译注释](#反编译注释)的预期 Java 输出，因为这些注释会丢失。受影响的测试必须手动更新。
