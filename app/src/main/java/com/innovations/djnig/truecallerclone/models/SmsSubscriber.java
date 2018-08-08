package com.innovations.djnig.truecallerclone.models;

import com.stfalcon.chatkit.commons.models.IUser;

/**
 * Created by djnig on 12/16/2017.
 */

public class SmsSubscriber implements IUser {

    private String id;
    private String name;
    private String avatar;

    public SmsSubscriber(String id, String name, String avatar) {
        this.id = id;
        this.name = name;
        this.avatar = avatar;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }
}
