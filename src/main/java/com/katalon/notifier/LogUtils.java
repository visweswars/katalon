package com.katalon.notifier;

import hudson.model.BuildListener;

class LogUtils {

    static void log(BuildListener buildListener, String message) {
        buildListener.getLogger().println(message);
    }
}
