package com.example.users.model;

public class SaveToken {
    private static String token;
    private Long adminID;

    public Long getAdminID() {
        return adminID;
    }

    public void setAdminID(Long adminID) {
        this.adminID = adminID;
    }

    private SaveToken(){

    }

    public static SaveToken getInstance() {
        return new SaveToken();
    }
    public  String getToken() {
        return token;
    }

    public void setToken(String token) {
        SaveToken.token = token;
    }

}
