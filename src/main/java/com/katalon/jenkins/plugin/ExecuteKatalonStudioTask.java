package com.katalon.jenkins.plugin;

import com.google.common.base.Throwables;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class ExecuteKatalonStudioTask extends Builder {

    private String version;

    private String executeArgs;

    @DataBoundConstructor
    public ExecuteKatalonStudioTask(String version, String executeArgs) {
        this.version = version;
        this.executeArgs = executeArgs;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExecuteArgs() {
        return executeArgs;
    }

    public void setExecuteArgs(String executeArgs) {
        this.executeArgs = executeArgs;
    }

    private void executeKatalon(String katalonExecutableFile, String workSpace, BuildListener buildListener) throws IOException {
        String command = katalonExecutableFile +
                " -runMode=console " +
                " -projectPath=\"" + workSpace + "\" " +
                this.executeArgs;

        OsUtils.runCommand(buildListener, command);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
        try {
            String workSpace = abstractBuild.getWorkspace().getRemote();

            File katalonDir = KatalonUtils.getKatalonPackage(buildListener, this.version);

            LogUtils.log(buildListener, katalonDir.getAbsolutePath());

            String katalonExecutableFile = Paths.get(katalonDir.getAbsolutePath(), "katalon")
                    .toAbsolutePath()
                    .toString();

            executeKatalon(katalonExecutableFile, workSpace, buildListener);

        } catch (Exception e) {
            String stackTrace = Throwables.getStackTraceAsString(e);
            LogUtils.log(buildListener, stackTrace);
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> { // Publisher because Notifiers are a type of publisher
        @Override
        public String getDisplayName() {
            return "Execute Katalon Studio Tests"; // What people will see as the plugin name in the configs
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true; // We are always OK with someone adding this  as a build step for their job
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
    }
}
