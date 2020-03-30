package com.kgwb.model;

import javafx.beans.property.SimpleStringProperty;

public class MacModel {
    private SimpleStringProperty nearName;
    private SimpleStringProperty nearIpAddress;
    private SimpleStringProperty summary;

    public MacModel() { }

    public MacModel(MiniLinkMacWrapper data) {
        this.nearName = new SimpleStringProperty(data.getNearName());
        this.nearIpAddress = new SimpleStringProperty(data.getNearIpAddress());
        this.summary = new SimpleStringProperty(data.getSummary());
    }

    public String getNearName() {
        return nearName.get();
    }

    public SimpleStringProperty nearNameProperty() {
        return nearName;
    }

    public void setNearName(String nearName) {
        this.nearName.set(nearName);
    }

    public String getNearIpAddress() {
        return nearIpAddress.get();
    }

    public SimpleStringProperty nearIpAddressProperty() {
        return nearIpAddress;
    }

    public void setNearIpAddress(String nearIpAddress) {
        this.nearIpAddress.set(nearIpAddress);
    }

    public String getSummary() {
        return summary.get();
    }

    public SimpleStringProperty summaryProperty() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary.set(summary);
    }
}
