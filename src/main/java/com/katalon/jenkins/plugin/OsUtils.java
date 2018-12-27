package com.katalon.jenkins.plugin;

import hudson.model.BuildListener;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class OsUtils {

    static String getOSVersion(BuildListener buildListener) {

        if (SystemUtils.IS_OS_WINDOWS) {

            try {
                Process p = Runtime.getRuntime().exec("wmic os get osarchitecture");
                try (InputStream inputStream = p.getInputStream()) {
                    String output = IOUtils.toString(inputStream);
                    p.destroy();

                    if (output.contains("64")) {
                        return "windows 64";
                    } else {
                        return "windows 32";
                    }
                }
            } catch (Exception e) {
                LogUtils.log(buildListener, "Cannot detect the OS architecture. Assume it is x64.");
                LogUtils.log(buildListener, "Reason: " + e.getMessage() + ".");
                return "windows 64";
            }

        } else if (SystemUtils.IS_OS_MAC) {
            return "mac";
        } else if (SystemUtils.IS_OS_LINUX) {
            return "linux";
        }
        return "";
    }

    static void runCommand(BuildListener buildListener, String command) throws IOException {

        String[] cmdarray;
        if (SystemUtils.IS_OS_WINDOWS) {
            cmdarray = Arrays.asList("cmd", "/c", command).toArray(new String[]{});
        } else {
            cmdarray = Arrays.asList("sh", "-c", command).toArray(new String[]{});
        }
        LogUtils.log(buildListener, "Execute " + command);
        Process cmdProc = Runtime.getRuntime().exec(cmdarray);
        try (
                BufferedReader stdoutReader = new BufferedReader(
                        new InputStreamReader(
                                cmdProc.getInputStream(), StandardCharsets.UTF_8));
                BufferedReader stderrReader = new BufferedReader(
                        new InputStreamReader(
                                cmdProc.getErrorStream(), StandardCharsets.UTF_8))
        ) {
            String line;
            while ((line = stdoutReader.readLine()) != null ||
                    (line = stderrReader.readLine()) != null) {
                LogUtils.log(buildListener, line);
            }
        }
    }
}
