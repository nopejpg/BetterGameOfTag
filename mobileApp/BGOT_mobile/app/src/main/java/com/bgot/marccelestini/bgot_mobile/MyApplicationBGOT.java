package com.bgot.marccelestini.bgot_mobile;

import android.app.Application;

public class MyApplicationBGOT extends Application {

    private String podStatus = "~~~";

    public String getPodStatus() {
        return podStatus;
    }

    public void setPodStatus(String newPodStatus) {
        this.podStatus = newPodStatus;
    }
}
