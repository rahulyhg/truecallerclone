package com.innovations.djnig.truecallerclone.receivers;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hss01248.notifyutil.NotifyUtil;
import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;
import com.innovations.djnig.truecallerclone.overlay.OverlayService;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by djnig on 12/4/2017.
 */

public class CallReceiver extends PhonecallReceiver{

    Context context;

    private final static String TAG_OVERLAY = "tag_overlay";
    private final static String INC_CALL = "incoming_call";
    private final static String OUT_CALL = "outgoing_call";
    private final static String AFTERCALL = "aftercall";
    private final static String CONTACT = "contact";

    @Override
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        context = ctx;
        if (PrefsUtil.getSpamAction(ctx, ContactNumberUtil.getCleanNumber(number)) == 1) { //Check if number is blocked
            rejectCall();
        }
        if (isContactExists(number, ctx)) {
            return;
        }


        if (PrefsUtil.isBlockingHidden(context)) {
            rejectCallIfHidden(number);
            if (PrefsUtil.isNotifiableBlockedCall(context)) {
                showNotification();
            }
        }

        //showOverlay(ctx, INC_CALL, number);
        //next method onNamesGot

        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        final DatabaseReference namesReference = mRootRef.child("contactBase").child(number).child("names");
        namesReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                System.out.println("DB READING STARTED");
                if (dataSnapshot.getValue() != null) {
                    HashMap<String, Long> names = (HashMap<String, Long>) dataSnapshot.getValue();
                    if(names != null) {
                        String name = "";
                        int i = 0;
                        for (Map.Entry entry: names.entrySet()) {
                            if(Integer.parseInt(entry.getValue().toString()) > i) {
                                i = Integer.parseInt(entry.getValue().toString());
                                name = (String) entry.getKey();
                            }
                        }
                        System.out.println("Names:\n" + names);
                        showOverlay(ctx, INC_CALL, name);

                    }
                }
                else {
                    showOverlay(ctx, INC_CALL, number);
                    System.out.println("NO NAMES");
                }
                //FirebaseDatabase.getInstance().goOffline();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("dbStringSms", String.valueOf(databaseError));
            }
        });
    }

    private void showOverlay(Context ctx, String value, String contactNumber) {
        Intent myService = new Intent(ctx, OverlayService.class);
        Bundle bundle = new Bundle();
        bundle.putString(TAG_OVERLAY, value);
        bundle.putString(CONTACT, contactNumber);
        myService.putExtras(bundle);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(myService);
        } else {
           ctx.startService(myService);
        }
    }

    private void showNotification() {
        NotifyUtil.init(context);
        NotifyUtil.buildSimple(100, R.drawable.ic_block, context.getString(R.string.app_name), context.getString(R.string.call_was_blocked),
                NotifyUtil.buildIntent(DrawerActivity.class))
                .setTicker("Missed the call")
                .show();

    }

    private void rejectCallIfHidden(String phoneNumber) {

        if (Character.compare(phoneNumber.charAt(0), '-') == 0) {
            ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).setStreamMute(AudioManager.STREAM_RING, true);

            TelephonyManager telephony = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            try {
                Class c = Class.forName(telephony.getClass().getName());
                Method m = c.getDeclaredMethod("getITelephony");
                m.setAccessible(true);
                ITelephony telephonyService = (ITelephony) m.invoke(telephony);
                if (PrefsUtil.getBlockVariant(context).equals("1")) {
                    telephonyService.endCall();
                } else {
                    telephonyService.silenceRinger();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private void rejectCall() {
        TelephonyManager telephony = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(telephony.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephony);
            telephonyService.endCall();
            showSpamNotification();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSpamNotification() {
        NotifyUtil.init(context);
        NotifyUtil.buildSimple(100, R.drawable.ic_block, context.getString(R.string.app_name), context.getString(R.string.spam_call_was_blocked),
                NotifyUtil.buildIntent(DrawerActivity.class))
                .show();

    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
     /*   if (isContactExists(number, ctx)) {
            return;
        }
        Intent i = new Intent(ctx, OverlayStarter.class);
        i.putExtra("number", number);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);*/
        showOverlay(ctx, OUT_CALL, number);

    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        if(isServiceRunning(OverlayService.class, ctx)){
            hideOverlay(ctx);
        }

        if (PrefsUtil.getSpamAction(ctx, number) == 2)
            isSpam(number, ctx);

        DrawerActivity.isNewCall = true;

    }


    private boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void hideOverlay(Context ctx) {
        Intent intent = new Intent(ctx, OverlayService.class);
        ctx.stopService(intent);
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        if(isServiceRunning(OverlayService.class, ctx)){
            hideOverlay(ctx);
        }
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
        isSpam(number, ctx);
        DrawerActivity.isNewCall = true;
    }

    private void isSpam(final String number, final Context ctx) {
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        final DatabaseReference spamReference = mRootRef.child("contactBase").child(number);
        spamReference.orderByValue().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            if (!dataSnapshot.hasChild("spam")) {
                                spamReference.child("spam").child("counter").push();
                                spamReference.child("spam").child("counter").setValue(0);
                                showOverlay(ctx, OUT_CALL, number);
                                return;
                            }
                            for (DataSnapshot spamSnapshot : dataSnapshot.getChildren()) {
                                /*HashMap<String, Object> yourData = (HashMap) spamSnapshot.getValue();
                                long numberOfUsers = (long) yourData.get("counter");*/
                                //long numberOfUsers = (long) spamSnapshot.getValue();
                                long numberOfUsers = (long) spamSnapshot.child("counter").getValue();
                                Log.e("number of users", String.valueOf(numberOfUsers) + "");
                                if (numberOfUsers > 5) {
                                    Intent i = new Intent(ctx, OverlayService.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putString(TAG_OVERLAY, "spam");
                                    bundle.putString("number", number);
                                    bundle.putLong("numberOfUsers", numberOfUsers);
                                    i.putExtras(bundle);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        ctx.startForegroundService(i);
                                    } else {
                                        ctx.startService(i);
                                    }
                                } else {
                                    if (!isContactExists(number, ctx)) {
                                        //showOverlay(ctx, OUT_CALL, number);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i("dbStringSpam", String.valueOf(databaseError));
                    }
                });
    }

    private boolean isContactExists(final String phoneNumber, Context context) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null);
        if (cursor == null) {
            return false;
        }
        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        return (contactName != null);
    }
}
