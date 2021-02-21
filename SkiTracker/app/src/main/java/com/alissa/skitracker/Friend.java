package com.alissa.skitracker;

public class Friend {


    private String name;

    private String code;

    private boolean online;


    public Friend(String vName, String vCode, boolean vOnline){
        name = vName;
        code = vCode;
        online = vOnline;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public boolean getOnline(){
        return online;
    }
}
