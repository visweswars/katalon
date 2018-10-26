package com.katalon.notifier;

public class KatalonVersion {
    private String version = null;
    private String filename = null;
    private String os = null;
    private String url = null;

    KatalonVersion(){

    }

    public String getOs() {
        return os;
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
