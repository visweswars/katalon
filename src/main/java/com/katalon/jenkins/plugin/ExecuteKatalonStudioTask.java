package com.katalon.jenkins.plugin;

import com.google.common.base.Throwables;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

public class ExecuteKatalonStudioTask extends Builder {

    private String version;

    private String location;

    private String executeArgs;

    @DataBoundConstructor
    public ExecuteKatalonStudioTask(String version, String location, String executeArgs) {
        this.version = version;
        this.location = location;
        this.executeArgs = executeArgs;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getExecuteArgs() {
        return executeArgs;
    }

    public void setExecuteArgs(String executeArgs) {
        this.executeArgs = executeArgs;
    }

    private void executeKatalon(String katalonExecutableFile, String workSpace, BuildListener buildListener) throws IOException {
        File file = new File(katalonExecutableFile);
        if (!file.exists()) {
            file = new File(katalonExecutableFile + ".exe");
        }
        if (file.exists()) {
            file.setExecutable(true);
        }
        if (katalonExecutableFile.contains(" ")) {
            katalonExecutableFile = "\"" + katalonExecutableFile + "\"";
        }
        String command = katalonExecutableFile +
                " -noSplash " +
                " -runMode=console " +
                " -projectPath=\"" + workSpace + "\" " +
                this.executeArgs;

        OsUtils.runCommand(buildListener, command);
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
        try {
            FilePath workspace = abstractBuild.getWorkspace();

            if (workspace != null) {
                String workspaceLocation = workspace.getRemote();

                if (workspaceLocation != null) {

                    String katalonDirPath;

                    if (StringUtils.isBlank(this.location)) {
                        File katalonDir = KatalonUtils.getKatalonPackage(buildListener, this.version);
                        katalonDirPath = katalonDir.getAbsolutePath();
                    } else {
                        katalonDirPath = this.location;
                    }

                    LogUtils.log(buildListener, "Using Katalon Studio at " + katalonDirPath);
                    String katalonExecutableFile;
                    String os = OsUtils.getOSVersion(buildListener);
                    if (os.contains("macos")) {
                        katalonExecutableFile = Paths.get(katalonDirPath, "Contents", "MacOS", "katalon")
                            .toAbsolutePath()
                            .toString();
                    } else {
                        katalonExecutableFile = Paths.get(katalonDirPath, "katalon")
                            .toAbsolutePath()
                            .toString();
                    }
                    executeKatalon(katalonExecutableFile, workspaceLocation, buildListener);

                }
            }

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
