package com.alissa.skitracker;

public class Friend {


    private String name;

    private String code;

    private boolean online;

    private boolean canBeJoined;


    public Friend(String vName, String vCode, boolean vOnline, boolean vCanBeJoined){
        name = vName;
        code = vCode;
        online = vOnline;
        canBeJoined = vCanBeJoined;
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

    public boolean getcanBeJoined(){
        return canBeJoined;
    }
}
