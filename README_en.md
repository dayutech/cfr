# CFR - Another Java Decompiler \o/

This is the public repository for the CFR decompiler, main site hosted at <a href="https://www.benf.org/other/cfr">benf.org/other/cfr</a>

CFR will decompile modern Java features - <a href="https://www.benf.org/other/cfr/java9observations.html">including much of Java <a href="java9stringconcat.html">9</a>, <a href="https://www.benf.org/other/cfr/switch_expressions.html">12</a> &amp; <a href="https://www.benf.org/other/cfr/java14instanceof_pattern">14</a>, but is written entirely in Java 6, so will work anywhere!  (<a href="https://www.benf.org/other/cfr/faq.html">FAQ</a>) - It'll even make a decent go of turning class files from other JVM languages back into java!</p>

To use, simply run the specific version jar, with the class name(s) you want to decompile (either as a path to a class file, or as a fully qualified classname on your classpath).
(`--help` to list arguments).

Alternately, to decompile an entire jar, simply provide the jar path, and if you want to emit files (which you probably do!) add `--outputdir /tmp/putithere`.

If the target path is a directory, CFR will recursively scan that directory and all subdirectories, and decompile every `.class` and `.jar` file it finds.

## Class Filtering

CFR supports filtering out known third-party library classes during decompilation. This can significantly speed up decompilation of large JAR files by skipping classes from common libraries like Spring, Apache Commons, Guava, etc.

### Enable Class Filtering

Use the `--enableclassfilter` option to enable class filtering:

```
java -jar cfr.jar myapp.jar --enableclassfilter
```

If you also want a final list of fully skipped JARs, add `--showskippedjars` (it is suppressed by `--silent`).

### Built-in Filter Rules

CFR now keeps only a **small default built-in set** (popular libraries such as Spring, Apache Commons/Log4j, Guava/Gson, SLF4J/Logback, Netty, Jackson, JUnit, Mockito).

The **extended third-party rules** have been moved to `cfr_class_filter.conf` so you can easily edit, remove, or add rules without changing Java code.

### Custom Filter Configuration

You can add custom filter rules by creating a configuration file named `cfr_class_filter.conf`. CFR searches for this file in the following locations (in order):

1. Current working directory
2. CFR JAR directory

Both configuration files are merged with the built-in rules.

#### Configuration File Format

The configuration file supports two sections: `[jar]` and `[class]`.

```
# This is a comment

# JAR file name prefix rules - filter entire JARs
[jar]
spring-core
spring-context
guava
commons-lang3
jackson-databind

# Class name prefix rules - filter specific classes
[class]
com.mycompany.internal
com.mycompany.thirdparty
org.mylibrary
```

**Section Details:**

- **`[jar]` section**: JAR file name prefix matching rules
  - Filters entire JAR files based on their names (without version suffixes)
  - Example: `spring-core` will match `spring-core-5.3.0.jar`, `spring-core-6.0.0.jar`
  - Matching is case-insensitive
  - Version numbers are automatically handled (e.g., `guava` matches `guava-31.1-jre.jar`)

- **`[class]` section**: Full class name prefix matching rules
  - Filters specific classes based on their fully qualified names
  - Example: `org.springframework` will match `org.springframework.core.xxx`
  - Supports both package prefixes (`org.springframework`) and dotted-boundary prefixes (`java.`, `rx.`)
  - For fat-jar/war layouts, matching automatically normalizes `BOOT-INF/classes/` and `WEB-INF/classes/` prefixes, so rules like `io.vertx.` and `javax.` still match directly

### Fully Qualified Name De-dup Cache (Optional)

Use `--enableclassnamecache` to enable an in-run cache of already decompiled fully qualified class names and avoid duplicate decompilation.

Activation requirements:

- `--flatoutput` and `--flatnojardir` must both be enabled
- If either one is missing, `--enableclassnamecache` is ignored (with a console message)

Behavior:

- The cache stores names in a hierarchical structure (`com -> test1 -> test2 -> ... -> Test`)
- CFR checks the cache before decompilation; cache hits are skipped, misses are decompiled and then recorded
- Cache size is bounded; eviction prioritizes entries with lower hit counts and deeper hierarchy to control memory use

### Extended Filter Rules

The extended filter rules in `cfr_class_filter.conf` cover **390 class prefixes** and **210 JAR name prefixes**, including:

**Popular Libraries:**
- Spring Framework (spring-*, org.springframework.*)
- Apache Commons (commons-*, org.apache.*)
- Jackson JSON (jackson-*, com.fasterxml.jackson.*)
- SLF4J/Logback (org.slf4j.*, ch.qos.logback.*)
- Netty (io.netty.*)
- Bouncy Castle (org.bouncycastle.*)

**Database/ORM:**
- MySQL, PostgreSQL, Oracle, SQLite, MongoDB, Cassandra
- Hibernate, MyBatis, JPA

**Message Queues & Networking:**
- Apache Kafka, RabbitMQ, Redis clients
- Netty, OkHttp, OkIo
- SNMP4J, SSH libraries

**Code Analysis & Generation:**
- ASM, Javassist, cglib, ByteBuddy
- ANTLR, Groovy

**Testing:**
- JUnit, Mockito, EasyMock, PowerMock, Hamcrest

**And many more third-party libraries...**

### Rule De-duplication

Class rules are now globally minimized by prefix coverage:

- If a broader rule exists, covered specific rules are removed.
- Example: when `log4j` exists, `log4j-core` is redundant and removed.
- This keeps rules concise while preserving effective filter behavior.

### Generate Class Rules From JAR Scan

Use `scan_jar_packages.py` to scan jars and update `[class]` rules:

```bash
python scan_jar_packages.py --scan-dir "D:\WorkDirQiax\VulnDiscov" --config-file "cfr_class_filter.conf" --min-count 10
```

Notes:

- The script recursively scans all `.jar` files under `--scan-dir`.
- Extracted prefixes are merged into `[class]`.
- Exact rules covered by broader prefixes are automatically dropped.

**Legacy Format (Backward Compatible):**

Lines without a section header are treated as class name prefix rules:

```
# Legacy format - treated as class rules
com.mycompany.internal
org.mylibrary
```

**File Format Rules:**

- Each line should contain one rule
- Lines starting with `#` are treated as comments
- Empty lines are ignored
- Section headers (`[jar]`, `[class]`) are case-insensitive

### Example Usage

```bash
# Decompile a JAR with class filtering enabled
java -jar cfr.jar myapp.jar --enableclassfilter --outputdir ./output

# Decompile all class/jar files found under a directory recursively
java -jar cfr.jar ./input-dir --outputdir ./output

# Recursively scan a directory and apply class/jar filtering at the same time
java -jar cfr.jar ./input-dir --enableclassfilter --outputdir ./output

# Flat output (recursive directory input) - by default still keeps each JAR's relative directory prefix
java -jar cfr.jar ./input-dir --outputdir ./output --flatoutput

# Flat output + no JAR directory prefix (won't create jar-name folders like test/)
java -jar cfr.jar ./input-dir --outputdir ./output --flatoutput --flatnojardir

# Enable fully qualified class-name de-dup cache in flat output mode
java -jar cfr.jar ./input-dir --outputdir ./output --flatoutput --flatnojardir --enableclassnamecache

# Enable class filtering and print skipped JARs at the end
java -jar cfr.jar ./input-dir --enableclassfilter --showskippedjars --outputdir ./output

# Show CFR version comment header in each decompiled file (disabled by default)
java -jar cfr.jar myapp.jar --showversion

# Show decompiler status comments in output (disabled by default)
java -jar cfr.jar myapp.jar --comments

# View help for the filter option
java -jar cfr.jar --help enableclassfilter

# View help for the class-name cache flag
java -jar cfr.jar --help enableclassnamecache
```

# Getting CFR

The main site for CFR is <a href="https://www.benf.org/other/cfr">benf.org/other/cfr</a>, where releases are available with a bunch of rambling musings from the author.

Since 0.145, Binaries are published on github along with release tags.

You can also download CFR from your favourite <a href="https://mvnrepository.com/artifact/org.benf/cfr">maven</a> repo, though releases are published a few days late usually, to allow for release regret.

# Issues

If you have an issue, please **_DO NOT_** include copyright materials.  I will have to delete your issue.

# Building CFR

Dead easy!

Just ensure you have [Maven](https://maven.apache.org/) installed. Then `mvn compile` in the root directory of this project will get you what you need.

Note: If you encounter a `maven-compiler-plugin...: Compilation failure` error while trying to compile the project then your `JAVA_HOME` environment variable is probably pointing to a JDK version that doesn't support `6` for the `source` or `target` compile options.
Fix this by pointing `JAVA_HOME` to a JDK version that still supports compiling to Java 1.6 such as JDK 11. Also note, the version of Java on your `JAVA` may need to be greater than 1.6 if you are using Maven version `>=3.3.1` which requires Java 1.7. The best solution is to use JDK 8, 9, 10 or 11 for both your `PATH` and `JAVA_HOME`.

The main class is `org.benf.cfr.reader.Main`, so once you've built, you can test it out (from `target/classes`)
```
java org.benf.cfr.reader.Main java.lang.Object
```
to get CFR to decompile `java.lang.Object`.


## Decompilation tests

As part of the Maven build automatic decompilation tests are performed. They verify that the current decompiled output of CFR matches the expected previous output. The test data (Java class and JAR files) are part of a separate Git repository; it is therefore necessary to clone this repository with `git clone --recurse-submodules`. The expected output and CFR test configuration is however part of this repository to allow altering it without having to modify the corresponding test data. The test data is in the `decompilation-test/test-data` directory, and the respective expected data and custom configuration is in the `decompilation-test/test-data-expected-output` directory (with a similar directory structure, see [Expected data structure](#expected-data-structure) below).

The decompilation tests are also performed by the GitHub workflow, and in case of test failures the unified diff is available in a [workflow artifact](https://docs.github.com/en/actions/managing-workflow-runs/downloading-workflow-artifacts) called "decompilation-test-failures-diff".

**The expected output is not the gold standard**, it merely describes the currently expected output. There is nothing wrong with adjusting the expected output, if the changes to the decompilation results are reasonable.

The test class is [`org.benf.cfr.test.DecompilationTest`](decompilation-test/src/org/benf/cfr/test/DecompilationTest.java). It can be modified to adjust the test directories, or to ignore certain class files or JARs. Additionally it is possible to directly execute the tests there from the IDE. This usually gives better output than what is shown by Maven, and allows using the built-in IDE functionality for showing differences between the expected and the actual data.

### Options file

The decompilation process can be customized by adding an options file. Each line of it specifies a CFR option, with key and value separated by a space. Empty lines and lines starting with `#` are ignored and can be used for comments.

Example:
```
# Enable identifier renaming
renameillegalidents true
```

See [Expected data structure](#expected-data-structure) below for how to name the file and where to place it.

### Expected data structure

#### Class files

For class files the expected data and custom configuration is in the same respective location under `test-data-expected-output`, with the file names being based on the class file name.

For example, for the class file `test-data/classes/subdir/MyClass.class` the following files can be used:

- `test-data-expected-output/classes/subdir/`
    - `MyClass.expected.java`  
      Contains the expected decompiled Java output, optionally with [decompilation notes](#decompilation-note-comments).
    - `MyClass.options`  
      An optional [options file](#options-file) customizing decompilation.
    - `MyClass.expected.summary`  
      Contains the expected summary reported by the CFR API. Can be omitted when no summary is produced.
    - `MyClass.expected.exceptions`  
      Contains the expected exceptions reported by the CFR API. Can be omitted when no exception is reported.

#### JAR files

For JAR files the expected data and custom configuration is inside a directory with the name of the JAR file in the respective location under `test-data-expected-output`. Expected Java output files include the package name in their file name, for example `mypackage.MyClass.java`. The options file and the expected summary and exceptions file use the "file name" `_`. For [multi-release JARs](https://openjdk.java.net/jeps/238) the directory contains subdirectories for version specific classes. The directory name has the form `java-<version>`.

For example, for the multi-release JAR file `test-data/jars/subdir/MyJar.jar` the following files can be used:
- `test-data-expected-output/jars/subdir/MyJar/`
    - `mypackage.MyClass.java`  
      Contains the expected decompiled Java output for the class `mypackage.MyClass`, optionally with [decompilation notes](#decompilation-note-comments).
    - `java-11/mypackage.MyClass.java`  
      Contains the expected decompiled Java output for a class file specific to Java 11 and higher (for multi-release JARs).
    - `_.options`  
      An optional [options file](#options-file) customizing decompilation.
    - `_.expected.summary`  
      Contains the expected summary reported by the CFR API. Can be omitted when no summary is produced.
    - `_.expected.exceptions`  
      Contains the expected exceptions reported by the CFR API. Can be omitted when no exception is reported.

### Decompilation note comments

The expected Java output files support comments representing _decompilation notes_. They are ignored during comparison with the actual Java output and can for example be used to indicate incorrect or improvable CFR output. There are two kinds of decompilation notes:

- Line: Start with `//#` (optionally prefixed with whitespace)
- Inline: `/*# ... #*/`

Line decompilation notes should be used sparingly, especially in large files, because they shift line numbers for the diff files (due to being removed during comparison), which can be confusing.

Example:
```java
//# Line decompilation note
public class MyClass {
    public static void main(String[] stringArray/*# Inline decompilation note #*/) {
        ...
    }
}
```

### Updating / creating expected data

When adding a lot of new classes or JAR files for the decompilation tests or when a change to CFR affects the output for a lot of classes or JAR files, manually creating or updating the expected output can be rather cumbersome. For these cases the following system properties exist which help with this. They can be set with `-D<system-property>` when running tests. However, when these system properties are set, the respective tests will still fail (but the expected data is updated) to prevent accidentally using them for regular test execution.

- `cfr.decompilation-test.create-expected`  
Generates all missing expected test data based on the current CFR output.
- `cfr.decompilation-test.update-expected`  
Updates the expected test data to match the actual data produced by CFR. Note that this does not work for expected Java output using [decompilation notes](#decompilation-note-comments) because those comments would get lost. The affected tests have to be updated manually.
