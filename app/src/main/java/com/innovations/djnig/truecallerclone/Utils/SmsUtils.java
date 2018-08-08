package com.innovations.djnig.truecallerclone.Utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.google.android.mms.MmsException;
import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.models.Sms;
import com.innovations.djnig.truecallerclone.newClasses.Conversation;
import com.innovations.djnig.truecallerclone.newClasses.Message;
import com.innovations.djnig.truecallerclone.newClasses.MessageColumns;
import com.innovations.djnig.truecallerclone.newClasses.MessageItem;
import com.innovations.djnig.truecallerclone.newClasses.SmsHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by djnig on 12/14/2017.
 */

public class SmsUtils {
    public static final Uri RECEIVED_MESSAGE_CONTENT_PROVIDER = Uri.parse("content://sms/inbox");
    public static final String COLUMN_READ = "read";
    public static final byte UNREAD = 0;

    public static final String UNREAD_SELECTION = COLUMN_READ + " = " + UNREAD;

    public enum Type {
        ALL, INCOMING, OUTGOING
    }

    public static List<Sms> getAllSms(Activity mActivity, Type type) {
        String folder;

        switch (type){
            case INCOMING:
                folder = "inbox";
                break;
            case OUTGOING:
                folder = "sent";
                break;
            default:
                folder = "";
        }

        List<Sms> lstSms = new ArrayList<>();
        Sms objSms = new Sms();
        Uri message = Uri.parse("content://sms/"+folder);
        ContentResolver cr = mActivity.getContentResolver();

        Cursor c = cr.query(message, null, null, null, null);
        mActivity.startManagingCursor(c);
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {

                objSms = new Sms();
                objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSms.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSms.setReadState(c.getString(c.getColumnIndex("read")));
                objSms.setTime(c.getString(c.getColumnIndexOrThrow("date")));
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setIncoming(true);
                } else {
                    objSms.setIncoming(false);
                }

                lstSms.add(objSms);
                c.moveToNext();
            }
        }

        ((DrawerActivity)mActivity).mCursorForClose = c;

        return lstSms;
    }

    public static void markMessageRead(Context context, String id) {
        ContentValues values = new ContentValues();
        values.put("READ", 1);
        try{
            context.getContentResolver().update(Uri.parse("content://sms/"),values, "_id="+id, null);
        }catch (NullPointerException e){
            e.printStackTrace();
        }

    }

    public static void markAllMessagesAsRead(Context context, List<Sms> smsList) {
        for(Sms s: smsList){

            SmsUtils.markMessageRead(context, s.getId());

        }
    }

    /**
     * Add incoming SMS to inbox
     *
     * @param context
     * @param address Address of sender
     * @param body    Body of incoming SMS message
     * @param time    Time that incoming SMS message was sent at
     */
    public static Uri addMessageToInbox(Context context, String address, String body, long time) {

        ContentResolver contentResolver = context.getContentResolver();
        ContentValues cv = new ContentValues();

        cv.put("address", address);
        cv.put("body", body);
        cv.put("date_sent", time);

        return contentResolver.insert(RECEIVED_MESSAGE_CONTENT_PROVIDER, cv);
    }

    public static int getUnreadMessageCount(Context context) {
        int result = 0;

        // Create a cursor for the conversation list
        Cursor conversationCursor = context.getContentResolver().query(
                SmsHelper.CONVERSATIONS_CONTENT_PROVIDER, Conversation.ALL_THREADS_PROJECTION,
                SmsHelper.UNREAD_SELECTION, null, SmsHelper.sortDateAsc);

        if (conversationCursor.moveToFirst()) {
            do {
                Uri threadUri = Uri.withAppendedPath(Message.MMS_SMS_CONTENT_PROVIDER, conversationCursor.getString(Conversation.ID));
                Cursor messageCursor = context.getContentResolver().query(threadUri, MessageColumns.PROJECTION, SmsHelper.UNREAD_SELECTION, null, SmsHelper.sortDateDesc);
                if (messageCursor != null) {
                    result += messageCursor.getCount();
                    messageCursor.close();
                }
            } while (conversationCursor.moveToNext());
        }

        conversationCursor.close();

        return result;
    }

    public static HashMap<Long, ArrayList<MessageItem>> getUnreadUnseenConversations(Context context) {
        HashMap<Long, ArrayList<MessageItem>> result = new HashMap<>();

        String selection = SmsHelper.UNSEEN_SELECTION + " AND " + SmsHelper.UNREAD_SELECTION;

        // Create a cursor for the conversation list
        Cursor conversationCursor = context.getContentResolver().query(
                SmsHelper.CONVERSATIONS_CONTENT_PROVIDER, Conversation.ALL_THREADS_PROJECTION,
                SmsHelper.UNREAD_SELECTION, null, SmsHelper.sortDateAsc);

        if (conversationCursor != null && conversationCursor.moveToFirst()) {
            do {
                ArrayList<MessageItem> messages = new ArrayList<>();
                long threadId = conversationCursor.getLong(Conversation.ID);
                Uri threadUri = Uri.withAppendedPath(Message.MMS_SMS_CONTENT_PROVIDER, Long.toString(threadId));
                Cursor messageCursor = context.getContentResolver().query(threadUri, MessageColumns.PROJECTION, selection, null, SmsHelper.sortDateAsc);

                if (messageCursor != null && messageCursor.moveToFirst()) {
                    do {
                        MessageColumns.ColumnsMap columnsMap = new MessageColumns.ColumnsMap(messageCursor);
                        MessageItem message = null;
                        try {
                            message = new MessageItem(context, messageCursor.getString(columnsMap.mColumnMsgType), messageCursor, columnsMap, null, true);
                            message.mBody = messageCursor.getString(columnsMap.mColumnSmsBody);
                            message.mDate = messageCursor.getLong(columnsMap.mColumnSmsDate);
                        } catch (MmsException e) {
                            e.printStackTrace();
                        }
                        messages.add(message);
                    } while (messageCursor.moveToNext());
                    messageCursor.close();
                    result.put(threadId, messages);
                }

            } while (conversationCursor.moveToNext());
            conversationCursor.close();
        }

        return result;
    }

    public static ArrayList<Message> getUnreadMessagesLegacy(Context context, Uri threadUri) {
        ArrayList<Message> result = new ArrayList<>();

        if (threadUri != null) {
            Cursor messageCursor = context.getContentResolver().query(threadUri, MessageColumns.PROJECTION, UNREAD_SELECTION, null, SmsHelper.sortDateAsc);
            MessageColumns.ColumnsMap columnsMap = new MessageColumns.ColumnsMap(messageCursor);

            if (messageCursor.moveToFirst()) {
                do {
                    try {
                        Message message = new Message(context, messageCursor.getLong(columnsMap.mColumnMsgId));
                        result.add(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (messageCursor.moveToNext());
            }

            messageCursor.close();
        }

        return result;
    }
}
