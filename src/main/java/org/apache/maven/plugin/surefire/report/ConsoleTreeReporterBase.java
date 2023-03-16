package org.apache.maven.plugin.surefire.report;

import org.apache.maven.plugin.surefire.log.api.ConsoleLogger;
import org.apache.maven.surefire.api.report.TestSetReportEntry;

import java.util.List;

abstract class ConsoleTreeReporterBase extends ConsoleReporter {

    public ConsoleTreeReporterBase(ConsoleLogger logger, boolean usePhrasedClassNameInRunning,
                                   boolean usePhrasedClassNameInTestCaseSummary) {
        super(logger, usePhrasedClassNameInRunning, usePhrasedClassNameInTestCaseSummary);
    }

    abstract TreePrinter getTreePrinter(List<WrappedReportEntry> classEntries, List<WrappedReportEntry> testEntries);

    @Override
    public void testSetStarting(TestSetReportEntry report) {
    	getConsoleLogger().info("HolaAAA");
        new TestReportHandler(getConsoleLogger(), report)
                .prepare();
    }

    @Override
    public void testSetCompleted(WrappedReportEntry report, TestSetStats testSetStats, List<String> testResults) {
    	getConsoleLogger().info("Adios");
        new TestReportHandler(getConsoleLogger(), report, testSetStats)
                .print(this::getTreePrinter);
    }

}
