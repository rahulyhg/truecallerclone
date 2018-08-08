package com.innovations.djnig.truecallerclone.overlay;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.ContactsUtils;
import com.innovations.djnig.truecallerclone.Utils.FirebaseHelper;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;

/**
 * Created by djnig on 1/15/2018.
 */

public class OverlayService extends Service {

    private View view;
    private final static String TAG_OVERLAY = "tag_overlay";
    private final static String INC_CALL = "incoming_call";
    private final static String OUT_CALL = "outgoing_call";
    private final static String AFTERCALL = "aftercall";
    private final static String CONTACT = "contact";
    private final static String SPAM = "spam";

    private WindowManager wm;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (intent != null && intent.getExtras() != null) {
            String value = intent.getExtras().getString(TAG_OVERLAY);

            String contactNumber = intent.getExtras().getString(CONTACT);
            switch (value) {
                case OUT_CALL:
                    createOutgoingCallOverlay(contactNumber);
                    break;
                case INC_CALL:
                    createIncomingCallOverlay(contactNumber);
                    break;
                case AFTERCALL:
                    createAfterCallOverlay();
                    break;
                case SPAM:
                    String number = intent.getExtras().getString("number");
                    long numberOfUsers = intent.getExtras().getLong("numberOfUsers", 0);
                    createSpamOverlay(number, numberOfUsers);
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createSpamOverlay(String number, long numberOfUsers) {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.spam_dialog, null);
        view.findViewById(R.id.blockButton).setOnClickListener(v -> {
            PrefsUtil.setSpamAction(OverlayService.this, ContactNumberUtil.getCleanNumber(number), 1);
            addOneToSpam(number);
        });
        view.findViewById(R.id.allowButton).setOnClickListener(v -> {
            PrefsUtil.setSpamAction(OverlayService.this, ContactNumberUtil.getCleanNumber(number), 0);
            stopSelf();
        });
        ((TextView) view.findViewById(R.id.spamNumber)).setText(number);
        TextView thinkersNumber = view.findViewById(R.id.numberOfThinkers);
        String message = numberOfUsers + " " + thinkersNumber.getText();
        thinkersNumber.setText(message);

        final float scale = getResources().getDisplayMetrics().density;

        int pixelsHeight = (int) (250 * scale + 0.5f);
        int pixelsWidth = (int) (350 * scale + 0.5f);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                pixelsWidth, pixelsHeight,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        wm.addView(view, params);
    }


    private void createAfterCallOverlay() {

    }

    private void createIncomingCallOverlay(String contactNumber) {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.overlay_outgoing_call, null);
        view.findViewById(R.id.close_overlay).setOnClickListener(v -> stopSelf());


        final float scale = getResources().getDisplayMetrics().density;
        final int pixelsWidth = getResources().getDisplayMetrics().widthPixels - 100; //100 is margin


        //130 is dp
        int pixelsHeight = (int) (150 * scale + 0.5f);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                pixelsWidth, pixelsHeight,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        wm.addView(view, params);
        ((TextView) view.findViewById(R.id.contact_number_overlay)).setText(contactNumber);
        ((TextView) view.findViewById(R.id.contact_name_overlay)).setText(ContactsUtils.findNameByNumber(this, contactNumber));

        view.findViewById(R.id.overlay_call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + contactNumber));
                if (intent.resolveActivity(getApplication().getPackageManager()) != null) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }
                stopSelf();
            }
        });

        view.findViewById(R.id.overlay_sms).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DrawerActivity.class);
                intent.putExtra("PhoneNumber", contactNumber);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                stopSelf();
            }
        });

        view.findViewById(R.id.overlay_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, contactNumber);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                stopSelf();
            }
        });
        view.findViewById(R.id.overlay_block).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefsUtil.blockUnknownNumber(getApplicationContext(), ContactNumberUtil.getCleanNumber(contactNumber));
                FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(contactNumber));
                stopSelf();
            }
        });


    }

    private void createOutgoingCallOverlay(String contactNumber) {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.overlay_outgoing_call, null);
        view.findViewById(R.id.close_overlay).setOnClickListener(v -> stopSelf());


        final float scale = getResources().getDisplayMetrics().density;
        final int pixelsWidth = getResources().getDisplayMetrics().widthPixels - 100; //100 is margin


        //130 is dp
        int pixelsHeight = (int) (130 * scale + 0.5f);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
               pixelsWidth, pixelsHeight,
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER;
        params.x = 0;
        params.y = 0;
        wm.addView(view, params);
        ((TextView)view.findViewById(R.id.contact_number_overlay)).setText(contactNumber);
        ((TextView)view.findViewById(R.id.contact_name_overlay)).setText(ContactsUtils.findNameByNumber(this, contactNumber));
    }

    private void addOneToSpam(final String number) {
        FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(number));

        /*final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        final DatabaseReference spamReference = mRootRef.child("contactBase").child(number).child("spam");
        spamReference.orderByValue().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.getValue() != null) {
                            for (DataSnapshot spamSnapshot : dataSnapshot.getChildren()) {
                                long numberOfUsers = (long) spamSnapshot.getValue();
                                spamReference.child("counter").setValue(numberOfUsers + 1);
                                stopSelf();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i("dbStringSms", String.valueOf(databaseError));
                    }
                });*/
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(view!=null){
            wm.removeView(view);
            view = null;
        }
    }
}
