package com.innovations.djnig.truecallerclone.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by djnig on 1/11/2018.
 */

public class PrefsUtil {

    public static boolean isNotifiableBlockedCall(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("is_notif_for_blocked_calls", true);
    }

    public static boolean isNotifiableBlockedSms(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("is_notif_for_blocked_sms", true);
    }

    public static boolean blockTopSpamers(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("is_block_top_spammers", true);
    }

    public static String getBlockVariant(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("how_block_calls", "1");
    }

    public static boolean isBlockingHidden(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("is_block_hidden_numbers", false);
    }

    public static boolean isVibrationOn(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("notifications_new_message_vibrate", true);
    }

    public static Uri getNotificationRingtone(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Uri defaultRingtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context.getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
        return Uri.parse(prefs.getString("notifications_new_message_ringtone", defaultRingtoneUri.toString()));
    }

    public static int getSpamAction(Context context, String number) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(number, 2);//0 - not blocked, 1 - blocked, 2 - there are no number in the list
    }

    public static void setSpamAction(Context context, String number, int value) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(number, value);
        editor.apply();
    }

    public static void blockUnknownNumber(Context context, String number) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> values = prefs.getStringSet("blocked_numbers", null);
        if(values == null) {
            values = new HashSet<>();
        }
        values.add(number);
        editor.putStringSet("blocked_numbers", values);
        int prefNum = prefs.getInt(number, 2);
        if(prefNum != 2)
            editor.remove(number);
        editor.apply();
    }

    public static void unlockUnknownNumber(Context context, String number) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> values = prefs.getStringSet("blocked_numbers", null);
        if(values != null)
            values.remove(number);
        editor.putStringSet("blocked_numbers", values);
        editor.putInt(number, 0);//it need for top spammers. If we unlock number and won't save it's state - new messages will block it in sms receiver
        editor.apply();
    }

    public static Set<String> getBlockedUnknownNumbers(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getStringSet("blocked_numbers", null);//0 - not blocked, 1 - blocked, 2 - there are no number in the list
    }
}
