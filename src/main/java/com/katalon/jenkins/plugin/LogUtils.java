package com.katalon.jenkins.plugin;

import hudson.model.BuildListener;

class LogUtils {

    static void log(BuildListener buildListener, String message) {
        buildListener.getLogger().println(message);
    }
}
