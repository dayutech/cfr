package org.benf.cfr.reader;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.state.ClassFileSourceChained;
import org.benf.cfr.reader.state.ClassFileSourceImpl;
import org.benf.cfr.reader.state.ClassFileSourceWrapper;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.collections.ListFactory;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.benf.cfr.reader.util.output.DumperFactory;
import org.benf.cfr.reader.util.output.InternalDumperFactoryImpl;
import org.benf.cfr.reader.util.output.SinkDumperFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CfrDriverImpl implements CfrDriver {
    private final Options options;
    private final ClassFileSource2 classFileSource;
    private final OutputSinkFactory outputSinkFactory;

    public CfrDriverImpl(ClassFileSource source, OutputSinkFactory outputSinkFactory, Options options, boolean fallbackToDefaultSource) {
        if (options == null) {
            options = new OptionsImpl(new HashMap<String, String>());
        }
        ClassFileSource2 tmpSource;
        if (source == null) {
            tmpSource = new ClassFileSourceImpl(options);
        } else {
            tmpSource = source instanceof ClassFileSource2 ? (ClassFileSource2)source : new ClassFileSourceWrapper(source);
            if (fallbackToDefaultSource) {
                tmpSource = new ClassFileSourceChained(Arrays.asList(tmpSource, new ClassFileSourceImpl(options)));
            }
        }
        this.outputSinkFactory = outputSinkFactory;
        this.options = options;
        this.classFileSource = tmpSource;
    }

    @Override
    public void analyse(List<String> toAnalyse) {
        List<AnalysisTarget> analysisTargets = expandTargets(toAnalyse);
        /*
         * There's an interesting question here - do we want to skip inner classes, if we've been given a wildcard?
         * (or a wildcard expanded by the operating system).
         *
         * Assume yes.
         */
        boolean skipInnerClass = analysisTargets.size() > 1 && options.getOption(OptionsImpl.SKIP_BATCH_INNER_CLASSES);

        // Can't sort a 1.6 singleton list.
        analysisTargets = ListFactory.newList(analysisTargets);
        Collections.sort(analysisTargets);
        for (AnalysisTarget analysisTarget : analysisTargets) {
            String path = analysisTarget.path;
            // TODO : We shouldn't have to discard state here.  But we do, because
            // it causes test fails.  (used class name table retains useful symbols).
            classFileSource.informAnalysisRelativePathDetail(null, null);
            // Note - both of these need to be reset, as they have caches.
            DCCommonState dcCommonState = new DCCommonState(options, classFileSource);
            DumperFactory dumperFactory = outputSinkFactory != null ?
                    new SinkDumperFactory(outputSinkFactory, options) :
                    new InternalDumperFactoryImpl(options);

            AnalysisType type = options.getOption(OptionsImpl.ANALYSE_AS);
            if (type == null || type == AnalysisType.DETECT) {
                type = dcCommonState.detectClsJar(path);
            }

            if (type == AnalysisType.JAR || type == AnalysisType.WAR) {
                Driver.doJar(dcCommonState, path, type, dumperFactory, analysisTarget.outputPrefix);
            } else if (type == AnalysisType.CLASS) {
                Driver.doClass(dcCommonState, path, skipInnerClass, dumperFactory, analysisTarget.outputPrefix, analysisTarget.relativePath);
            }
        }
    }

    private List<AnalysisTarget> expandTargets(List<String> toAnalyse) {
        Set<AnalysisTarget> expanded = new LinkedHashSet<AnalysisTarget>();
        for (String path : toAnalyse) {
            collectTarget(new File(path), path, null, expanded);
        }
        return ListFactory.newList(expanded);
    }

    private void collectTarget(File file, String originalPath, File scanRoot, Set<AnalysisTarget> expanded) {
        if (!file.exists() || !file.isDirectory()) {
            expanded.add(new AnalysisTarget(originalPath, null, null));
            return;
        }
        collectDirectoryTargets(file, file, expanded);
    }

    private void collectDirectoryTargets(File scanRoot, File directory, Set<AnalysisTarget> expanded) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files);
        for (File file : files) {
            if (file.isDirectory()) {
                collectDirectoryTargets(scanRoot, file, expanded);
                continue;
            }
            String lowerName = file.getName().toLowerCase();
            if (lowerName.endsWith(".class")) {
                expanded.add(new AnalysisTarget(file.getAbsolutePath(), null, getRelativePath(scanRoot, file)));
            } else if (lowerName.endsWith(".jar")) {
                String relativePath = getRelativePath(scanRoot, file);
                expanded.add(new AnalysisTarget(file.getAbsolutePath(), toOutputPrefix(removeExtension(relativePath)), relativePath));
            }
        }
    }

    private static String getRelativePath(File root, File file) {
        return root.toURI().relativize(file.toURI()).getPath();
    }

    private static String removeExtension(String path) {
        int idx = path.lastIndexOf('.');
        if (idx <= 0) {
            return path;
        }
        return path.substring(0, idx);
    }

    private static String toOutputPrefix(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        path = path.replace('/', File.separatorChar);
        if (path.charAt(path.length() - 1) == File.separatorChar) {
            path = path.substring(0, path.length() - 1);
        }
        return File.separator + path;
    }

    static class AnalysisTarget implements Comparable<AnalysisTarget> {
        private final String path;
        private final String outputPrefix;
        private final String relativePath;

        private AnalysisTarget(String path, String outputPrefix, String relativePath) {
            this.path = path;
            this.outputPrefix = outputPrefix;
            this.relativePath = relativePath;
        }

        @Override
        public int compareTo(AnalysisTarget other) {
            return path.compareTo(other.path);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof AnalysisTarget)) return false;

            AnalysisTarget that = (AnalysisTarget) o;

            return path.equals(that.path);
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }
    }
}
