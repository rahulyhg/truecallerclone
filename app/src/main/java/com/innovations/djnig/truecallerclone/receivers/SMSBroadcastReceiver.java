package com.innovations.djnig.truecallerclone.receivers;

/**
 * Created by dell on 18.01.2018.
 */

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import android.util.Log;

import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.ContactsUtils;
import com.innovations.djnig.truecallerclone.Utils.FirebaseHelper;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;
import com.innovations.djnig.truecallerclone.Utils.SmsUtils;
import com.innovations.djnig.truecallerclone.interfaces.OnDataReceiveListener;
import com.innovations.djnig.truecallerclone.newClasses.MessageItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class SMSBroadcastReceiver extends BroadcastReceiver implements OnDataReceiveListener{
    private final String TAG = "MessagingReceiver";
    private static final long[] VIBRATION_SILENT = {0, 0};
    private static final long[] VIBRATION_ON = {1000, 0, 1000};

    private Context mContext;
    private SharedPreferences mPrefs;

    private String mAddress;
    private String mBody;
    private long mDate;

    private Uri mUri;



    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");
        abortBroadcast();

        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (intent.getExtras() != null) {
            Object[] pdus = (Object[]) intent.getExtras().get("pdus");
            SmsMessage[] messages = new SmsMessage[pdus.length];
            for (int i = 0; i < messages.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            }

            SmsMessage sms = messages[0];
            if (messages.length == 1 || sms.isReplace()) {
                mBody = sms.getDisplayMessageBody();
            } else {
                StringBuilder bodyText = new StringBuilder();
                for (SmsMessage message : messages) {
                    bodyText.append(message.getMessageBody());
                }
                mBody = bodyText.toString();
            }

            mAddress = sms.getDisplayOriginatingAddress();
            mDate = sms.getTimestampMillis();


            insertMessageAndNotify();
            DrawerActivity.isNewMessages = true;
            DrawerActivity.isNewMessage = true;
        }
    }

    private void insertMessageAndNotify() {
        mUri = SmsUtils.addMessageToInbox(mContext, mAddress, mBody, mDate);
        if(PrefsUtil.getSpamAction(mContext, ContactNumberUtil.getCleanNumber(mAddress)) == 0)//contact exists and wasn't blocked
            sendNotification();//if contact isn't blocked
        else if(PrefsUtil.getSpamAction(mContext, ContactNumberUtil.getCleanNumber(mAddress)) == 1) {
            System.out.println("MESSAGE IS BLOCKED");
            if(PrefsUtil.isNotifiableBlockedSms(mContext))
                sendNotificationBlocked();
            else
                System.out.println("Don't send notification");
        }
        else if(PrefsUtil.getSpamAction(mContext, ContactNumberUtil.getCleanNumber(mAddress)) == 2) {
            if(PrefsUtil.blockTopSpamers(mContext)) {
                FirebaseHelper.isSpamer(ContactNumberUtil.getCleanNumber(mAddress), this);
            }
            else
                sendNotification();
        }



       /* PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "MessagingReceiver");
        wakeLock.acquire();
        wakeLock.release();*/

    }

    private void sendNotificationBlocked() {
        HashMap<Long, ArrayList<MessageItem>> conversations = SmsUtils.getUnreadUnseenConversations(mContext);
        long threadId = (long) conversations.keySet().toArray()[0];
        int smsCount = 0;
        for(Map.Entry entry: conversations.entrySet()) {
            ArrayList<MessageItem> list = (ArrayList<MessageItem>) entry.getValue();
            if(list.get(0).mAddress.equals(mAddress)) {
                threadId = (long) entry.getKey();
                smsCount = list.size();
                break;
            }
        }


        Intent resultIntent = new Intent(mContext, DrawerActivity.class);
        resultIntent.putExtra("PhoneNumber", mAddress);

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, (int) threadId, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri soundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        //int unreadMessageCount = SmsUtils.getUnreadMessageCount(mContext);

        String msgTitle;//to replace number on contact's name if exist
        msgTitle = ContactsUtils.findNameByNumber(mContext,mAddress);
        if(msgTitle == null)
            msgTitle = mAddress;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext, mAddress)
                        .setSmallIcon(R.drawable.ic_block)
                        .setContentTitle(msgTitle)
                        .setContentText(mBody)
                        .setAutoCancel(true)
                        .setNumber(smsCount)
                        .setTicker("Blocked message from: " + msgTitle)
                        .setContentIntent(resultPendingIntent)
                        .setVibrate(PrefsUtil.isVibrationOn(mContext) ? VIBRATION_ON : VIBRATION_SILENT);

        Uri prefSound = PrefsUtil.getNotificationRingtone(mContext);//builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        if(prefSound != null)
            mBuilder.setSound(prefSound);
        else
            mBuilder.setSound(soundUri);

        // Gets an instance of the NotificationManager service
        android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager)mContext.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mAddress,(int) threadId, mBuilder.build());
    }


    private void sendNotification() {
        HashMap<Long, ArrayList<MessageItem>> conversations = SmsUtils.getUnreadUnseenConversations(mContext);
        long threadId = (long) conversations.keySet().toArray()[0];
        int smsCount = 0;
        for(Map.Entry entry: conversations.entrySet()) {
            ArrayList<MessageItem> list = (ArrayList<MessageItem>) entry.getValue();
            if(list.get(0).mAddress.equals(mAddress)) {
                threadId = (long) entry.getKey();
                smsCount = list.size();
                break;
            }
        }


        Intent resultIntent = new Intent(mContext, DrawerActivity.class);
        resultIntent.putExtra("PhoneNumber", mAddress);

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, (int) threadId, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri soundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        int unreadMessageCount = SmsUtils.getUnreadMessageCount(mContext);

        String msgTitle;//to replace number on contact's name if exist
        msgTitle = ContactsUtils.findNameByNumber(mContext,mAddress);
        if(msgTitle == null)
            msgTitle = mAddress;
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(mContext, mAddress)
                        .setSmallIcon(R.drawable.ic_new_sms)
                        .setContentTitle(msgTitle)
                        .setContentText(mBody)
                        .setAutoCancel(true)
                        .setNumber(smsCount)
                        .setTicker(msgTitle + ": " + mBody)
                        .setContentIntent(resultPendingIntent)
                        .setVibrate(PrefsUtil.isVibrationOn(mContext) ? VIBRATION_ON : VIBRATION_SILENT)
                        .setSound(soundUri);
                        //.setSound(PrefsUtil.getNotificationRingtone(mContext));//builder.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)



        // Gets an instance of the NotificationManager service
        android.app.NotificationManager mNotifyMgr = (android.app.NotificationManager)mContext.getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mAddress,(int) threadId, mBuilder.build());

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock((PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "MessagingReceiver");
        wakeLock.acquire();
        wakeLock.release();
    }

    @Override
    public void onCompleteReading(boolean isSpam) {
        if(isSpam) {
            System.out.println("TOP SPAMER'S MESSAGE IS BLOCKED");
            if(PrefsUtil.isNotifiableBlockedSms(mContext))
                sendNotificationBlocked();
            else
                System.out.println("Don't send notification");
            PrefsUtil.blockUnknownNumber(mContext, ContactNumberUtil.getCleanNumber(mAddress));
        }
        else
            sendNotification();
    }
}


/*private void sendNotificationNew() {
        HashMap<Long, ArrayList<MessageItem>> conversations = SmsHelper.getUnreadUnseenConversations(mContext);
        long threadId = (long) conversations.keySet().toArray()[0];
        int smsCount = 0;
        for(Map.Entry entry: conversations.entrySet()) {
            ArrayList<MessageItem> list = (ArrayList<MessageItem>) entry.getValue();
            if(list.get(0).mAddress.equals(mAddress)) {
                threadId = (long) entry.getKey();
                smsCount = list.size();
                break;
            }
        }


        Intent resultIntent = new Intent(mContext, DrawerActivity.class);
        resultIntent.putExtra("PhoneNumber", mAddress);

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent = PendingIntent.getActivity(mContext, (int) threadId, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri soundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        int unreadMessageCount = SmsUtils.getUnreadMessageCount(mContext);


        *//*NotifyUtil.buildSimple((int) threadId, R.drawable.ic_notification_message, mAddress, mBody, resultPendingIntent)
                .setHeadup()
                .setTicker(mAddress + ": " + mBody)
                .show();*//*
        NotifyUtil.init(this.mContext);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        inboxStyle.setSummaryText("New messages: ");
        NotifyUtil.buildMailBox((int) threadId, R.drawable.ic_notification_message, mAddress)
                .addMsg(mBody)
                .setContentIntent(resultPendingIntent)
                .setSummaryText("New messages: ")
                .setSubtext("New messages: ")
                .setContentText(mBody)
                .setTicker(mAddress + ": " + mBody)
                .show();
    }*/