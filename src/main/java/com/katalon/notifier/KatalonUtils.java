package com.katalon.notifier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.BuildListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class KatalonUtils {

    private static final String RELEASES_LIST =
            "https://github.com/katalon-studio/katalon-studio/blob/master/releases.json";

    private static KatalonVersion getVersionInfo(BuildListener buildListener, String versionNumber) throws IOException {

        URL url = new URL(RELEASES_LIST);

        String os = OsUtils.getOSVersion(buildListener);

        ObjectMapper objectMapper = new ObjectMapper();
        List<KatalonVersion> versions = objectMapper.readValue(url, new TypeReference<List<KatalonVersion>>() {
        });

        for (KatalonVersion version : versions) {
            if ((version.getVersion().equals(versionNumber)) && (version.getOs().equals(os))) {
                String containingFolder = version.getContainingFolder();
                if (containingFolder == null) {
                    String fileName = version.getFilename();
                    String fileExtension = "";
                    if (fileName.endsWith(".zip")) {
                        fileExtension = ".zip";
                    } else if (fileName.endsWith(".tar.gz")) {
                        fileExtension = ".tar.gz";
                    }
                    containingFolder = fileName.replace(fileExtension, "");
                    version.setContainingFolder(containingFolder);
                }
                LogUtils.log(buildListener, "Katalon Studio is hosted at " + version.getUrl() + ".");
                return version;
            }
        }
        return null;
    }

    private static void downloadAndExtract(
            BuildListener buildListener, String versionNumber, File targetDir)
            throws IOException {

        KatalonVersion version = KatalonUtils.getVersionInfo(buildListener, versionNumber);

        URL url = new URL(version.getUrl());

        url.openStream();

        InputStream in = new BufferedInputStream(url.openStream(), 1024);
        ZipInputStream zIn = new ZipInputStream(in);

        unpackArchive(version.getContainingFolder(), zIn, targetDir);
    }

    private static void unpackArchive(
            String containingFolder, ZipInputStream inputStream, File targetDir)
            throws IOException {
        ZipEntry entry;

        while ((entry = inputStream.getNextEntry()) != null) {
            String folder = entry.getName().replace(containingFolder, "");

            File file = new File(targetDir, File.separator + folder);

            if (!OsUtils.buildDirectory(file.getParentFile())) {
                throw new IOException("Could not create directory: " + file.getParentFile());
            }

            if (!entry.isDirectory()) {
                try (OutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                    IOUtils.copy(inputStream, fileOutputStream);
                }
            } else {
                if (!OsUtils.buildDirectory(file)) {
                    throw new IOException("Could not create directory" + file);
                }
            }
        }
    }

    private static File getKatalonFolder(String version) {
        String path = System.getProperty("user.home");

        Path p = Paths.get(path, ".katalon", version);
        return p.toFile();
    }

    static File getKatalonPackage(BuildListener buildListener, String versionNumber) throws IOException {

        File katalonDir = getKatalonFolder(versionNumber);

        Path fileLog = Paths.get(katalonDir.toString(), ".katalon.done");

        if (fileLog.toFile().exists()) {
            LogUtils.log(buildListener, "Katalon Studio package has been downloaded already.");
        } else {
            FileUtils.deleteDirectory(katalonDir);

            katalonDir.mkdirs();

            KatalonUtils.downloadAndExtract(buildListener, versionNumber, katalonDir);

            fileLog.toFile().createNewFile();
        }

        return katalonDir;
    }
}
