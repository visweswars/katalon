package com.katalon.notifier;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.List;


public class JUnitNotifier extends Notifier {

    private final String local;

    @DataBoundConstructor
    public JUnitNotifier(String local) {
        this.local = local;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener buildListener) {

        LogUtils.log(buildListener, "Parsing the test result");

        TestResultAction resultAction = build.getAction(TestResultAction.class);
        List<? extends Action> actions = build.getAllActions();

        for (Action action : actions) {
            LogUtils.log(buildListener, action.getDisplayName());
        }

        List<TestResult> testResults = new ArrayList<>();

        if (resultAction != null) {
            testResults.add(resultAction.getResult());
        } else {
            AggregatedTestResultAction aggregatedTestResultAction = build.getAction(AggregatedTestResultAction.class);
            if (aggregatedTestResultAction != null) {
                List<AggregatedTestResultAction.ChildReport> childReports = aggregatedTestResultAction.getResult();
                if (childReports != null) {
                    for (AggregatedTestResultAction.ChildReport childReport : childReports) {
                        if (childReport.result instanceof TestResult) {
                            testResults.add((TestResult) childReport.result);
                        }
                    }
                }
            }
        }
        return true;
    }


    @Override
    public BuildStepDescriptor getDescriptor() {
        return super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
}


