package com.Amrit_a_b.kiraapp;

public class Alert {
    private String timestamp;
    private String location;
    private String status;

    public Alert() {} // Required for Firebase

    public Alert(String timestamp, String location, String status) {
        this.timestamp = timestamp;
        this.location = location;
        this.status = status;
    }

    public String getTimestamp() { return timestamp; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
}