package com.innovations.djnig.truecallerclone.models;

import java.util.List;

/**
 * Created by dell on 09.02.2018.
 */

public class BlockedContact {
    private String displayName;
    private List<String> phoneNumbers;
    private String photoUri;

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPhoneNumbers(List<String> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }
}
