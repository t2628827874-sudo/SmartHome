package com.example.smarthome.Model;

public class DeviceCardModel {
    private int iconId;//图片
    private String title;//标题
    private String subtitle;//副标题

    public DeviceCardModel(String subtitle, String title, int iconId) {
        this.subtitle = subtitle;
        this.title = title;
        this.iconId = iconId;
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}
