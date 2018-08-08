package com.innovations.djnig.truecallerclone.models;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;


/**
 * Created by djnig on 1/21/2018.
 */

public class CallLog {


    private int type;
    private String time;
    private long date;
    private String number;
    private String name;
    private String duration;

    private boolean isSelected = false;

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void delete(Context context) {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
                String queryString = android.provider.CallLog.Calls.NUMBER + "=" + "?" + " AND " + android.provider.CallLog.Calls.DATE + "=" + "?";
                String args[] = {number, String.valueOf(date)};
                context.getContentResolver().delete(android.provider.CallLog.Calls.CONTENT_URI, queryString, args);
            }
            String queryString = android.provider.CallLog.Calls.NUMBER + "=" + "?" + " AND " + android.provider.CallLog.Calls.DATE + "=" + "?";
            String args[] = {number, String.valueOf(date)};
            context.getContentResolver().delete(android.provider.CallLog.Calls.CONTENT_URI, queryString, args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
