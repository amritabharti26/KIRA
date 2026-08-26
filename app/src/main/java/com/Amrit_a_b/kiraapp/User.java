package com.Amrit_a_b.kiraapp;

public class User {

    public String name;
    public String email;

    @SuppressWarnings("unused")
    public User(){} // Required by Firebase Realtime Database

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }
}