package com.katalon.notifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadTask extends Builder {
    private String version;
    private String execute;

    @DataBoundConstructor
    public DownloadTask(String version, String execute){

        this.version = version;
        this.execute = execute;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getExecute() {
        return execute;
    }

    public void setExecute(String execute) {
        this.execute = execute;
    }

    private static File downloadAndExtract(String link, File targetDir) throws IOException{
        String nameFileKatalon = getNameKatalonFoler(link);

        URL url = new URL(link);

        url.openStream();

        InputStream in = new BufferedInputStream(url.openStream(), 1024);
        ZipInputStream zIn = new ZipInputStream(in);

        return unpackArchive(zIn, targetDir, nameFileKatalon);
    }

    private static File unpackArchive(ZipInputStream inputStream, File targetDir, String nameKatalonFolder) throws IOException{
        ZipEntry entry;

        while((entry = inputStream.getNextEntry()) != null){
            String folder = entry.getName().replace(nameKatalonFolder, "");

            File file = new File(targetDir, File.separator + folder);

            //File file = new File(targetDir, File.separator + entry.getName());

            if(!buildDirectory(file.getParentFile())){
                throw new IOException("Could not create directory: " + file.getParentFile());
            }

            if(!entry.isDirectory()){
                copyInputStream(inputStream, new BufferedOutputStream(new FileOutputStream(file)));
            } else{
                if(!buildDirectory(file))
                {
                    throw new IOException("Could not create directory" + file);
                }
            }
        }
        return targetDir;
    }

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        IOUtils.copy(in, out);
        out.close();
    }

    private static boolean buildDirectory(File file)
    {
        return file.exists() || file.mkdirs();
    }

    private File getKatalonFolder(){
        //Get user home, but failed. path = "C:\\windows\\system32\\config\\systemprofile\\.katalon\\5.8.0";
//        String path = System.getProperty("user.home");

        String path = "C:\\Users\\tuananhtran";
        Path p = Paths.get(path,".katalon", this.version);
        return p.toFile();
    }

    private static String getNameKatalonFoler(String link){
        int lastIndexof = link.lastIndexOf("/");
        int endNameFolder = link.lastIndexOf(".zip");
        return link.substring(lastIndexof + 1, endNameFolder );
    }

    private void runExcutebyCmd(String katalonExecuteDir, BuildListener buildListener) throws IOException {

        //Remove -consolelog in this.execute to show in console jenkins
        String configExecute = katalonExecuteDir + " " +this.execute;

        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

        Process cmdProc;

        if (isWindows){
            cmdProc = Runtime.getRuntime().exec("cmd /c " + configExecute);
        } else{
            cmdProc = Runtime.getRuntime().exec("sh -c " + configExecute);
        }

        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
        String line;

        while ((line = stdoutReader.readLine()) != null) {
            buildListener.getLogger().println(line);
        }
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
        //String link = "https://download.katalon.com/5.8.0/Katalon_Studio_Windows_64-5.8.0.zip";
        String link = "https://download.katalon.com/" + this.version + "/Katalon_Studio_Windows_64-" + this.version + ".zip";
        File katalonDir = getKatalonFolder();

        //Get direction workspace
        //abstractBuild.getProject().getSomeWorkspace();
        //String dirWorkspace = abstractBuild.getEnvironment().get("WORKSPACE");

        try {
            Path fileLog = Paths.get(katalonDir.toString(), ".katalon.done");

            if(fileLog.toFile().exists()){
                System.out.println("Exists");
            } else
            {
                FileUtils.deleteDirectory(katalonDir);

                katalonDir.mkdirs();

                downloadAndExtract(link, katalonDir);

                fileLog.toFile().createNewFile();
            }

            runExcutebyCmd(Paths.get(katalonDir.toString(), "katalon").toString(), buildListener);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> { // Publisher because Notifiers are a type of publisher
        @Override
        public String getDisplayName() {
            return "Katalon Download Task"; // What people will see as the plugin name in the configs
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true; // We are always OK with someone adding this as a build step for their job
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException{
            save();
            return super.configure(req, formData);
        }
    }
}
