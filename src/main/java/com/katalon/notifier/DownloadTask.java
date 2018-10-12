package com.katalon.notifier;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.acegisecurity.Authentication;
import org.apache.commons.io.IOUtils;
import org.jvnet.hudson.test.TestBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadTask extends Builder {
    private File file;
    private URL url;
    private String version;

    @DataBoundConstructor
    public DownloadTask(String version){
        //String link = "https://download.katalon.com/5.8.0/Katalon_Studio_Windows_64-5.8.0.zip";
        //File file = new File("D://Download1");
       System.out.println(version);
       this.version = version;
       System.out.println(version);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private static File unpackArchive(URL url, File targetDir) throws IOException{
        if(!targetDir.exists()){
            targetDir.mkdirs();
        }

        InputStream in = new BufferedInputStream(url.openStream(), 1024);
        ZipInputStream zIn = new ZipInputStream(in);

        return unpackArchive(zIn, targetDir);
    }

    private static File unpackArchive(ZipInputStream inputStream, File targetDir) throws IOException{
        ZipEntry entry;
        while((entry = inputStream.getNextEntry()) != null){
            File file = new File(targetDir, File.separator + entry.getName());

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

    private static void copyInputStream(InputStream in, OutputStream out) throws IOException{
        IOUtils.copy(in, out);
        out.close();
    }

    private static boolean buildDirectory(File file)
    {
        return file.exists() || file.mkdirs();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
//        try{
//            unpackArchive(url, file);
//        } catch (IOException e)
//        {
//            e.printStackTrace();
//        }

//        unpackArchive(url, file);
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> { // Publisher because Notifiers are a type of publisher
//        public FormValidation doCheckName(@QueryParameter String version) throws IOException, ServletException {
//
//            return FormValidation.ok();
//        }
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
