package com.katalon.jenkins.plugin;

import hudson.model.BuildListener;
import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

class OsUtils {

    static String getOSVersion(BuildListener buildListener) {

        if (SystemUtils.IS_OS_WINDOWS) {

            try {
                Process p = Runtime.getRuntime().exec("wmic os get osarchitecture");

                InputStreamReader in = new InputStreamReader(p.getInputStream());
                BufferedReader reader = new BufferedReader(in);

                String line;
                boolean is32 = true;

                while ((line = reader.readLine()) != null) {
                    if (line.contains("64")) {
                        is32 = false;
                        break;
                    }
                }
                p.destroy();
                reader.close();

                if (is32) {
                    return "windows 32";
                } else {
                    return "windows 64";
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
            cmdarray = Arrays.asList("cmd", "/c", command).toArray(new String[] {});
        } else {
            cmdarray = Arrays.asList("sh", "-c", command).toArray(new String[] {});
        }
        LogUtils.log(buildListener, "Execute " + command);
        Process cmdProc = Runtime.getRuntime().exec(cmdarray);
        try (
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
                BufferedReader stderrReader = new BufferedReader(new InputStreamReader(cmdProc.getErrorStream()))
        ) {
            String line;
            while ((line = stdoutReader.readLine()) != null ||
                    (line = stderrReader.readLine()) != null) {
                LogUtils.log(buildListener, line);
            }
        }
    }
}
