package org.apache.maven.plugin.surefire.report;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.report.ReportEntry;

public class TestReportHandler {

    protected static final Map<String, Set<String>> classNames = synchronizedMap(new HashMap<>());
    protected static final Map<String, List<WrappedReportEntry>> classEntries = synchronizedMap(new HashMap<>());
    protected static final Map<String, List<WrappedReportEntry>> testEntries = synchronizedMap(new HashMap<>());
    protected static final int $ = 36;

    private final ReportEntry report;
    private final TestSetStats testSetStats;
    private final String sourceRootName;
    private final ConsoleLogger logger;

    public TestReportHandler(ConsoleLogger consoleLogger, ReportEntry report, TestSetStats testSetStats) {
        this.report = report;
        this.testSetStats = testSetStats;
        this.sourceRootName = getSourceRootName();
        logger= consoleLogger;
    }

    public TestReportHandler(ConsoleLogger consoleLogger, ReportEntry report) {
        this(consoleLogger, report, null);
    }

    public void prepare() {
        if (hasNestedTests()) {
            markClassNamesForNestedTests();
        }
    }

    public void print(BiFunction<List<WrappedReportEntry>,List<WrappedReportEntry>, TreePrinter> getTreePrinter) {
    	// FIXME: aquí parece estar el problema
        if (isMarkedAsNestedTest()) {
        	logger.info("Printing nested: "+report);
        	logger.info("Class names: "+classNames);
        	logger.info("Class entries: "+classEntries);
        	logger.info("Test entries: "+testEntries);
        	
            prepareEntriesForNestedTests();
            if (isNestedTestReadyToPrint()) {
            	logger.info("Printing nested ready: "+testSetStats);
                printNestedTests(getTreePrinter);
            }
        } else {
        	logger.info("Printing un-nested: "+report);
            printTests(getTreePrinter);
        }
    }

    private boolean isMarkedAsNestedTest() {
        return classNames.containsKey(sourceRootName);
    }

    private void prepareClassEntriesForNestedTest() {
        classEntries.putIfAbsent(sourceRootName, new ArrayList<>());
        classEntries.computeIfPresent(sourceRootName, addToCollection((WrappedReportEntry) report));
    }

    private List<WrappedReportEntry> getClassEntryList() {
        return classEntries.get(sourceRootName);
    }

    private void markClassNamesForNestedTests() {
        classNames.putIfAbsent(sourceRootName, new HashSet<>(singleton(sourceRootName)));
        classNames.computeIfPresent(sourceRootName, addToCollection(report.getSourceName()));
    }

    private Set<String> getClassNameList() {
        return classNames.get(sourceRootName);
    }

    private void prepareTestEntriesForNestedTest() {
        testEntries.putIfAbsent(sourceRootName, new ArrayList<>());
        testEntries.computeIfPresent(sourceRootName, (k,v)->{
        	v.addAll(testSetStats.getReportEntries());
        	return v;
        });
    }

    private List<WrappedReportEntry> getTestEntryList() {
        return testEntries.get(sourceRootName);
    }

    private void cleanEntries() {
        classNames.remove(sourceRootName);
        classEntries.remove(sourceRootName);
        testEntries.remove(sourceRootName);
    }

    private void sortClassEntryList() {
        getClassEntryList()
                .sort(Comparator.comparing(ReportEntry::getSourceName));
    }

    private void prepareEntriesForNestedTests() {
        if (hasNestedTests()) {
            prepareTestEntriesForNestedTest();
        }
        prepareClassEntriesForNestedTest();
    }

    private boolean isNestedTestReadyToPrint() {
        return getClassEntryList().size() == getClassNameList().size();
    }

    private void printNestedTests(BiFunction<List<WrappedReportEntry>,List<WrappedReportEntry>, TreePrinter> getTreePrinter) {
        sortClassEntryList();
        getTreePrinter
                .apply(getClassEntryList(), getTestEntryList())
                .printTests();
        cleanEntries();
    }

    private void printTests(BiFunction<List<WrappedReportEntry>,List<WrappedReportEntry>, TreePrinter> getTreePrinter) {
        getTreePrinter
                .apply(singletonList((WrappedReportEntry) report), new ArrayList<>(testSetStats.getReportEntries()))
                .printTests();
    }

    private <J, K, V extends Collection<K>> BiFunction<J, V, V> addToCollection(K obj) {
        return (k, v) -> {
            v.add(obj);
            return v;
        };
    }

    private String getSourceRootName() {
        return report.getSourceName().split("\\$", -1)[0];
    }

    private boolean hasNestedTests() {
        if (this.testSetStats == null) {
            return hasNestedTests(report);
        } else {
            return hasNestedTests(testSetStats);
        }
    }

    private boolean hasNestedTests(TestSetStats testSetStats) {
        return testSetStats.getReportEntries()
                .stream()
                .anyMatch(this::hasNestedTests);
    }

    private boolean hasNestedTests(ReportEntry reportEntry) {
        return reportEntry.getSourceName()
                .chars()
                .filter(c -> c == $)
                .count() > 0;
    }
}
