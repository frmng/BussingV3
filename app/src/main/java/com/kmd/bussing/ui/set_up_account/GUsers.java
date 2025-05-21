package com.kmd.bussing.ui.set_up_account;

public class GUsers {

    String userId;
    String name;
    String profile;

    public GUsers(String userId, String profile, String name) {
        this.userId = userId;
        this.profile = profile;
        this.name = name;
    }

    public GUsers() {}

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }
}