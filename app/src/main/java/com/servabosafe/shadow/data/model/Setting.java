package com.servabosafe.shadow.data.model;


import com.servabosafe.shadow.utility.command.Command;

/**
 * Created by brandon.burton on 6/16/14.
 */
public class Setting implements BaseHolder {

    public static final int NO_RESOURCE = -1;

    private int mImageResource;
    private String mTitle;
    private String mSubtitle = "";
    private Command mCommand;

    private boolean mIsHeader = false;

    public boolean isHeader() {
        return mIsHeader;
    }

    public void setIsHeader(boolean mIsHeader) {
        this.mIsHeader = mIsHeader;
    }

    public Setting()
    {
        mImageResource = NO_RESOURCE;
        mTitle = "";
        mSubtitle = "";
        mCommand = null;
    }

    public Setting(String header) {

        mTitle=header;
        mIsHeader = true;

    }

    public Setting(String title, String subtitle, Command command) {
        mImageResource = NO_RESOURCE;
        mTitle = title;
        mSubtitle = subtitle;
        mCommand = command;
    }

    public Setting(int resource, String title, String subtitle, Command command) {
        mImageResource = resource;
        mTitle = title;
        mSubtitle = subtitle;
        mCommand = command;
    }

    public static Setting createHeader(String title) {
        return new Setting("header");
    }

    public int getmImageResource() {
        return mImageResource;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getSubtitle() {
        return mSubtitle;
    }

    public void executeCommand() {
        mCommand.execute(null);
    }

    public Command getCommand() {
        return mCommand;
    }


}
