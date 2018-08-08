package com.innovations.djnig.truecallerclone.newClasses;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

import com.google.android.mms.MmsException;

import java.util.ArrayList;
import java.util.HashMap;

public class SmsHelper {

    public static final Uri SMS_CONTENT_PROVIDER = Uri.parse("content://sms/");
    public static final Uri MMS_CONTENT_PROVIDER = Uri.parse("content://mms/");
    public static final Uri MMS_SMS_CONTENT_PROVIDER = Uri.parse("content://mms-sms/conversations/");
    public static final Uri SENT_MESSAGE_CONTENT_PROVIDER = Uri.parse("content://sms/sent");
    public static final Uri DRAFTS_CONTENT_PROVIDER = Uri.parse("content://sms/draft");
    public static final Uri PENDING_MESSAGE_CONTENT_PROVIDER = Uri.parse("content://sms/outbox");
    public static final Uri RECEIVED_MESSAGE_CONTENT_PROVIDER = Uri.parse("content://sms/inbox");
    public static final Uri CONVERSATIONS_CONTENT_PROVIDER = Uri.parse("content://mms-sms/conversations?simple=true");
    public static final Uri ADDRESSES_CONTENT_PROVIDER = Uri.parse("content://mms-sms/canonical-addresses");

    public static final String MAX_MMS_ATTACHMENT_SIZE_UNLIMITED = "unlimited";
    public static final String MAX_MMS_ATTACHMENT_SIZE_300KB = "300kb";
    public static final String MAX_MMS_ATTACHMENT_SIZE_600KB = "600kb";
    public static final String MAX_MMS_ATTACHMENT_SIZE_1MB = "1mb";

    public static final String sortDateDesc = "date DESC";
    public static final String sortDateAsc = "date ASC";

    public static final byte UNREAD = 0;
    public static final byte READ = 1;

    // Attachment types
    public static final int TEXT = 0;
    public static final int IMAGE = 1;
    public static final int VIDEO = 2;
    public static final int AUDIO = 3;
    public static final int SLIDESHOW = 4;

    // Columns for SMS content providers
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_THREAD_ID = "thread_id";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_RECIPIENT = "recipient_ids";
    public static final String COLUMN_PERSON = "person";
    public static final String COLUMN_SNIPPET = "snippet";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_DATE_NORMALIZED = "normalized_date";
    public static final String COLUMN_DATE_SENT = "date_sent";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_ERROR = "error";
    public static final String COLUMN_READ = "read";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_MMS = "ct_t";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_SUB = "sub";
    public static final String COLUMN_MSG_BOX = "msg_box";
    public static final String COLUMN_SUBJECT = "subject";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_SEEN = "seen";

    public static final String READ_SELECTION = COLUMN_READ + " = " + READ;
    public static final String UNREAD_SELECTION = COLUMN_READ + " = " + UNREAD;
    public static final String UNSEEN_SELECTION = COLUMN_SEEN + " = " + UNREAD;

    public static final int ADDRESSES_ADDRESS = 1;

    private static final String TAG = "SMSHelper";
    private static SmsManager sms;


    public SmsHelper() {

    }

    /**
     * The quality parameter which is used to compress JPEG images.
     */
    public static final int IMAGE_COMPRESSION_QUALITY = 95;
    /**
     * The minimum quality parameter which is used to compress JPEG images.
     */
    public static final int MINIMUM_IMAGE_COMPRESSION_QUALITY = 50;

    /**
     * Message type: all messages.
     */
    public static final int MESSAGE_TYPE_ALL = 0;

    /**
     * Message type: inbox.
     */
    public static final int MESSAGE_TYPE_INBOX = 1;

    /**
     * Message type: sent messages.
     */
    public static final int MESSAGE_TYPE_SENT = 2;

    /**
     * Message type: drafts.
     */
    public static final int MESSAGE_TYPE_DRAFT = 3;

    /**
     * Message type: outbox.
     */
    public static final int MESSAGE_TYPE_OUTBOX = 4;

    /**
     * Message type: failed outgoing message.
     */
    public static final int MESSAGE_TYPE_FAILED = 5;

    /**
     * Message type: queued to send later.
     */
    public static final int MESSAGE_TYPE_QUEUED = 6;

    /**
     * MMS address parsing data structures
     */
    // allowable phone number separators
    private static final char[] NUMERIC_CHARS_SUGAR = {
            '-', '.', ',', '(', ')', ' ', '/', '\\', '*', '#', '+'
    };

    private static String[] sNoSubjectStrings;


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

    /**
     * List of messages grouped by thread id, used for showing notifications
     */
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


    public static int getUnseenSMSCount(Context context, long threadId) {
        Cursor cursor = null;
        int count = 0;
        String selection = UNSEEN_SELECTION + " AND " + UNREAD_SELECTION + (threadId == 0 ? "" : " AND " + COLUMN_THREAD_ID + " = " + threadId);

        try {
            cursor = context.getContentResolver().query(RECEIVED_MESSAGE_CONTENT_PROVIDER, new String[]{COLUMN_ID}, selection, null, null);
            cursor.moveToFirst();
            count = cursor.getCount();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }


}
