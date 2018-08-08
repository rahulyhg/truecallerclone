package com.innovations.djnig.truecallerclone.models;

import android.content.Context;

import com.innovations.djnig.truecallerclone.Utils.ContactsUtils;
import com.stfalcon.chatkit.commons.models.IDialog;
import com.stfalcon.chatkit.commons.models.IUser;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by djnig on 12/16/2017.
 */

public class SmsDialog implements IDialog<Sms> {

    private boolean isSelected = false;
    private List<Sms> mSmsList;
    private Context context;
    private String address;
    private Sms lastSms;
    private String photo;


    public SmsDialog(Context context) {
        mSmsList = new ArrayList<>();
        this.context = context;
    }

    @Override
    public String getId() {
        return address;
    }

    @Override
    public String getDialogPhoto() {
        return photo;
    }


    @Override
    public String getDialogName() {
        return ContactsUtils.findNameByNumber(context, address);
    }

    @Override
    public List<? extends IUser> getUsers() {
            List<SmsSubscriber> users = new ArrayList<>();
            //users.add(new SmsSubscriber(address, ContactsUtils.findNameByNumber(context, address), "avatar"));
            //users.add(new SmsSubscriber(address, ContactsUtils.findNameByNumber(context, address), "android.resource://" + context.getPackageName() + "/drawable/profile_pictures"));
            users.add(new SmsSubscriber(address, ContactsUtils.findNameByNumber(context, address), photo));
            //users.add(new SmsSubscriber("0", "Me", null));
            return users;
    }

    @Override
    public Sms getLastMessage() {
        return lastSms;
    }

    @Override
    public void setLastMessage(Sms message) {

    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }


    public int getUnreadCount() {
        int count = 0;
        for(Sms s : mSmsList){
            if(s.getReadState().equals("0")){
                count++;
            }
        }
        return count;
    }
    public void addSms(Sms sms){
        mSmsList.add(sms);
    }

    public void setSmsList(List<Sms> smsList){
        this.mSmsList = smsList;
        this.lastSms = mSmsList.get(0);
        this.address = lastSms.getAddress();
    }


    public String getContactNumber() {
       return address;
    }



    public List<Sms> getSmsList() {
        return mSmsList;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
