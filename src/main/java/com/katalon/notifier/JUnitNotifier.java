package com.katalon.notifier;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.listeners.RunListener;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;

import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.dom4j.io.SAXReader;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.swing.text.Document;
import javax.swing.text.Element;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class JUnitNotifier extends Notifier {
    private final String local;

    @DataBoundConstructor
    public JUnitNotifier(String local)
    {
        this.local = local;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        try{

            listener.getLogger().println();


            //listener.getLogger().println(hudson.model.Hudson.getInstance().getItems());

            listener.getLogger().println("Parsing the test result1");

            TestResultAction resultAction = build.getAction(TestResultAction.class);
            List<? extends Action> actions = build.getAllActions();

            for(Action i : actions)
            {
                listener.getLogger().println(i.getDisplayName());
            }

            List<TestResult> testResults = new ArrayList<>();

            if(resultAction != null)
            {
                testResults.add(resultAction.getResult());
            } else
            {
                AggregatedTestResultAction aggregatedTestResultAction = build.getAction(AggregatedTestResultAction.class);
                if (aggregatedTestResultAction != null)
                {
                    List<AggregatedTestResultAction.ChildReport> childReports = aggregatedTestResultAction.getResult();
                    if (childReports != null)
                    {
                        for (AggregatedTestResultAction.ChildReport childReport: childReports)
                        {
                            if(childReport.result instanceof TestResult)
                            {
                                testResults.add((TestResult) childReport.result);
                            }
                        }
                    }
                }
            }
            if(!testResults.isEmpty())
            {
                listener.getLogger().println("Submitting test");
                //listener.getLogger().println("Submitting test results to + "on behalf of" + username);
                //new JiraLogService().submitTestLogs(jiraUrl, username, password, build, testResults);
            }
        }
        catch (Exception e) {
            listener.getLogger().println("Failed to submit test results " + e);
        }
        return true;
        //return super.perform(build, launcher, listener);
    }


    @Override
    public BuildStepDescriptor getDescriptor() {
        return super.getDescriptor();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> { // Publisher because Notifiers are a type of publisher
        private String global;

        @Override
        public String getDisplayName() {
            return "JUnitNotifierKatalon"; // What people will see as the plugin name in the configs
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true; // We are always OK with someone adding this as a build step for their job
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException{
            this.global = formData.getString("global");
            save();
            return super.configure(req, formData);
        }
    }
}


