package com.innovations.djnig.truecallerclone.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by djnig on 1/18/2018.
 */

public class ContactModel implements Serializable {
    private String mContactName;
    private String mContactNumber;

    public ContactModel(String contactName, String contactNumber) {
        this.mContactName = contactName;
        this.mContactNumber = contactNumber;
    }

    public String getContactName() {
        return mContactName;
    }

    public String getContactNumber() {
        return mContactNumber;
    }

    @Override
    public String toString() {
        return mContactName +"\n" + mContactNumber;
    }


}
