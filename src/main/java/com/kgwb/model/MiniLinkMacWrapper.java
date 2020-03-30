package com.kgwb.model;

public class MiniLinkMacWrapper {
    private String nearName;
    private String nearIpAddress;
    private String summary;

    public MiniLinkMacWrapper(String nearName, String nearIpAddress, String summary) {
        this.nearName = nearName;
        this.nearIpAddress = nearIpAddress;
        this.summary = summary;
    }

    public String getNearName() {
        return nearName;
    }

    public void setNearName(String nearName) {
        this.nearName = nearName;
    }

    public String getNearIpAddress() {
        return nearIpAddress;
    }

    public void setNearIpAddress(String nearIpAddress) {
        this.nearIpAddress = nearIpAddress;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
