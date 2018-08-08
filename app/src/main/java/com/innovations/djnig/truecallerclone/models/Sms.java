package com.innovations.djnig.truecallerclone.models;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.commons.models.MessageContentType;

import java.util.Date;

public class Sms implements IMessage, Parcelable, MessageContentType.Image, MessageContentType{

    private String _id;
    private String _address;
    private String _msg;
    private String _readState; //"0" for have not read sms and "1" for have read sms
    private String _time;
    private boolean isIncoming;
    private String _image;

    private boolean isSelected = false;

    public Sms() {
    }

    public String getId(){
        return _id;
    }

    @Override
    public String getText() {
        return _msg;
    }

    @Override
    public IUser getUser() {
        if(isIncoming){
        return new SmsSubscriber(_address, _address, null);
        }
        else {
        return new SmsSubscriber("0", "Me", null);
        }
    }

    @Override
    public Date getCreatedAt() {
        return new Date(Long.parseLong(_time));
    }

    @Override
    public String getImageUrl() {
        if(_image == null) {
            System.out.println("Image for message wasn't found");
        }
        else
            System.out.println("Image: " + _image);
        return _image;
    }

    public void setImage(String image) {
        this._image = image;
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    public void setIncoming(boolean incoming) {
        isIncoming = incoming;
    }

    public String getAddress(){
        return _address;
    }
    public String getMsg(){
        return _msg;
    }
    public String getReadState(){
        return _readState;
    }
    public String getTime(){
        return _time;
    }



    public void setId(String id){
        _id = id;
    }
    public void setAddress(String address){
        _address = address;
    }
    public void setMsg(String msg){
        _msg = msg;
    }
    public void setReadState(String readState){
        _readState = readState;
    }
    public void setTime(String time){
        _time = time;
    }


    protected Sms(Parcel in) {
        _id = in.readString();
        _address = in.readString();
        _msg = in.readString();
        _readState = in.readString();
        _time = in.readString();
        isIncoming = in.readByte() != 0x00;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_id);
        dest.writeString(_address);
        dest.writeString(_msg);
        dest.writeString(_readState);
        dest.writeString(_time);
        dest.writeByte((byte) (isIncoming ? 0x01 : 0x00));
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Sms> CREATOR = new Parcelable.Creator<Sms>() {
        @Override
        public Sms createFromParcel(Parcel in) {
            return new Sms(in);
        }

        @Override
        public Sms[] newArray(int size) {
            return new Sms[size];
        }
    };

    public void delete(Context context) {
        try {
            context.getContentResolver().delete(Uri.parse("content://sms/" + getId()), null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public String toString() {
        return "Sms{" +
                "_id='" + _id + '\'' +
                ", _address='" + _address + '\'' +
                ", _msg='" + _msg + '\'' +
                ", _readState='" + _readState + '\'' +
                ", _time='" + _time + '\'' +
                ", isIncoming=" + isIncoming +
                ", isSelected=" + isSelected +
                '}';
    }
}