package com.katalon.notifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadTask extends Builder {

    private static String linkFileConfig = "https://katalon-analytics-local.s3-ap-southeast-1.amazonaws.com/Jenkin-plugin/config.json";

    private String version;
    private String execute;
    private String fileNameKatalon;

    @DataBoundConstructor
    public DownloadTask(String version, String execute){
        this.version = version;
        this.execute = execute;

        this.fileNameKatalon = "";
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

    private File downloadAndExtract(File targetDir) throws IOException {
        String link = readFileJSon();

        URL url = new URL(link);

        url.openStream();

        InputStream in = new BufferedInputStream(url.openStream(), 1024);
        ZipInputStream zIn = new ZipInputStream(in);

        return unpackArchive(zIn, targetDir);
    }

    private File unpackArchive(ZipInputStream inputStream, File targetDir) throws IOException{
        ZipEntry entry;

        while((entry = inputStream.getNextEntry()) != null){
            String folder = entry.getName().replace(this.fileNameKatalon, "");

            File file = new File(targetDir, File.separator + folder);

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

    private void copyInputStream(InputStream in, OutputStream out) throws IOException {
        IOUtils.copy(in, out);
        out.close();
    }

    private boolean buildDirectory(File file)
    {
        return file.exists() || file.mkdirs();
    }

    private File getKatalonFolder(){
        String path = System.getProperty("user.home");

        Path p = Paths.get(path,".katalon", this.version);
        return p.toFile();
    }

    private void runExcutebyCmd(String katalonExecuteDir, String workSpace, BuildListener buildListener) throws IOException {
        String configExecute = katalonExecuteDir + " " +this.execute + " -projectPath=\"" + workSpace + "\"";

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

    private String getOSVersion() throws IOException {
        if(SystemUtils.IS_OS_WINDOWS){
            Process p = Runtime.getRuntime().exec("wmic os get osarchitecture");

            InputStreamReader in = new InputStreamReader(p.getInputStream());
            BufferedReader reader = new BufferedReader(in);

            String line;
            Boolean is32 = true;

            while ((line = reader.readLine()) != null){
                if(line.indexOf("64") != -1){
                    is32 = false;
                    break;
                }
            }
            p.destroy();
            reader.close();

            if(is32)
                return "windows 32";
            else
                return "windows 64";

        } else if(SystemUtils.IS_OS_MAC){
            return "mac";
        } else if(SystemUtils.IS_OS_LINUX){
            return "linux";
        }
        return "";
    }

    private String readFileJSon() throws IOException {
        URL url = new URL(linkFileConfig);

        String os = getOSVersion();

        ObjectMapper objectMapper = new ObjectMapper();
        List<KatalonVersion> listVersion = objectMapper.readValue(url, new TypeReference<List<KatalonVersion>>(){});

        for(KatalonVersion ver : listVersion){
            if((ver.getVersion().equals(this.version)) && (ver.getOs().equals(os))){
                String temp = ver.getFilename();
                this.fileNameKatalon = temp.substring(0,temp.lastIndexOf("."));

                return ver.getUrl();
            }
        }
        return "";
    }
    @Override
    public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
        try {
            String workSpace = abstractBuild.getProject().getSomeWorkspace().getRemote();

            File katalonDir = getKatalonFolder();

            Path fileLog = Paths.get(katalonDir.toString(), ".katalon.done");

            if(fileLog.toFile().exists()){
                System.out.println("Exists");
            } else
            {
                FileUtils.deleteDirectory(katalonDir);

                katalonDir.mkdirs();

                downloadAndExtract(katalonDir);

                fileLog.toFile().createNewFile();
            }

            runExcutebyCmd(Paths.get(katalonDir.toString(), "katalon").toString(), workSpace, buildListener);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> { // Publisher because Notifiers are a type of publisher
        @Override
        public String getDisplayName() {
            return "Execute Task"; // What people will see as the plugin name in the configs
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true; // We are always OK with someone adding this  as a build step for their job
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException{
            save();
            return super.configure(req, formData);
        }
    }
}
