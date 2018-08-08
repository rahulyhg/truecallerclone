package com.innovations.djnig.truecallerclone;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.tamir7.contacts.Contact;
import com.github.tamir7.contacts.Contacts;
import com.github.tamir7.contacts.PhoneNumber;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.innovations.djnig.truecallerclone.Fragments.BlockedContactsFragment;
import com.innovations.djnig.truecallerclone.Fragments.CallsFragment;
import com.innovations.djnig.truecallerclone.Fragments.ContactsFragment;
import com.innovations.djnig.truecallerclone.Fragments.SmsDialogsFragment;
import com.innovations.djnig.truecallerclone.Fragments.SmsListFragment;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.DateComparator;
import com.innovations.djnig.truecallerclone.Utils.FragmentsUtil;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;
import com.innovations.djnig.truecallerclone.Utils.SmsDateComparator;
import com.innovations.djnig.truecallerclone.Utils.SmsUtils;
import com.innovations.djnig.truecallerclone.adapters.SearchViewAdapter;
import com.innovations.djnig.truecallerclone.customView.Fab;
import com.innovations.djnig.truecallerclone.listeners.OnCompleteListener;
import com.innovations.djnig.truecallerclone.listeners.OnSmsListReady;
import com.innovations.djnig.truecallerclone.models.CallLog;
import com.innovations.djnig.truecallerclone.models.ContactModel;
import com.innovations.djnig.truecallerclone.models.Sms;
import com.innovations.djnig.truecallerclone.models.SmsDialog;
import com.innovations.djnig.truecallerclone.settings.SettingsActivity;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.stfalcon.chatkit.utils.DateFormatter;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;
import com.tuenti.smsradar.SmsRadar;
import com.wickerlabs.logmanager.LogObject;
import com.wickerlabs.logmanager.LogsManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindAnim;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import im.dlg.dialer.DialpadActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.EasyPermissions;

import static im.dlg.dialer.DialpadFragment.EXTRA_REGION_CODE;

public class DrawerActivity extends RxAppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MaterialSearchBar.OnSearchActionListener,
        EasyPermissions.PermissionCallbacks {

    @BindView(R.id.fab_dialpad)
    FloatingActionButton fabDialpad;

    @BindView(R.id.progressBarMain)
    ProgressBar progressBar;

    @BindAnim(R.anim.zoom_in)
    Animation zoomIn;

    @BindAnim(R.anim.zoom_out)
    Animation zoomOut;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.searchBar)
    MaterialSearchBar searchBar;

    SearchViewAdapter suggestionsAdapter;

    @BindView(R.id.navigation)
    BottomNavigationView navigation;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.toolbar_main)
    public Toolbar toolbar;

    @BindView(R.id.menu_more)
    public CardView cardViewMenu;

    @BindView(R.id.blocked_contacts_menu_more)
    public CardView cardViewBlockedContacts;

    @BindView(R.id.show_blocked_contacts_btn)
    public ImageButton btnBlockedContact;

    public static boolean isCallsLogRead = false;
    public static boolean isMessagesRead = false;
    public static boolean isMethodCalled = false;
    public static boolean isNewMessages = false;
    public static boolean isNewMessage = false; // variable for situation when you inside SmsListFragment
    public static boolean isNewCall = false;
    public static boolean isSelectMode = false;
    private Timer onNewSmsAndCallsListener;//checker on new sms to update sms list when you are in app

    public Cursor mCursorForClose;
    public OnCompleteListener onCompleteListenerCalls;
    public OnCompleteListener onCompleteListenerSms;
    public OnSmsListReady onSmsListReady;

    private int RC_DIALPAD = 123;
    private int RC_OVERLAY_PERMISSION = 111;
    private int RC_CONTACTS_AND_CALLS = 1234;


    private AlertDialog overlayReqDialog;
    private List<CallLog> newList;
    private List<SmsDialog> smsList;
    public List<String> blockedContacts = new ArrayList<>();
    private HashMap<String, String> contactsImages; //<PhoneNumber, ImageUrl>
    public MaterialSheetFab<Fab> materialSheetFab;

    private Handler handler;


    String[] perms = {
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS
            //Manifest.permission.MODIFY_PHONE_STATE
    };

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = (MenuItem item) -> {
        final float scale = getResources().getDisplayMetrics().density;
        switch (item.getItemId()) {

            case R.id.navigation_calls:
                FragmentsUtil.attachFragment(DrawerActivity.this, new CallsFragment(),
                        R.id.fragment_container, false);
                setVisibilityOfFAB(true, true);
                CoordinatorLayout.LayoutParams params = new CoordinatorLayout.LayoutParams(btnBlockedContact.getLayoutParams());
                params.gravity = Gravity.END;
                params.setMargins(0, (int) (17*scale), (int) (25*2*scale), 0);
                btnBlockedContact.setLayoutParams(params);
                return true;

            case R.id.navigation_sms:
                FragmentsUtil.attachFragment(DrawerActivity.this, new SmsDialogsFragment(),
                        R.id.fragment_container, false);
                setVisibilityOfFAB(false,true);
                params = params = new CoordinatorLayout.LayoutParams(btnBlockedContact.getLayoutParams());
                params.gravity = Gravity.END;
                params.setMargins(0, (int) (17*scale), (int) (20*scale), 0);
                btnBlockedContact.setLayoutParams(params);

                return true;

            case R.id.navigation_contacts:
                FragmentsUtil.attachFragment(DrawerActivity.this, new ContactsFragment(),
                        R.id.fragment_container, false);
                setVisibilityOfFAB(false, true);
                params = params = new CoordinatorLayout.LayoutParams(btnBlockedContact.getLayoutParams());
                params.gravity = Gravity.END;
                params.setMargins(0, (int) (17*scale), (int) (20*scale), 0);
                btnBlockedContact.setLayoutParams(params);
                return true;
        }
        return false;
    };

    public void setVisibilityOfFAB(boolean newVisibility, boolean withAnim) {

        if(newVisibility == (fabDialpad.getVisibility() == View.VISIBLE)){
            return;
        }
        if(!withAnim){
            fabDialpad.setVisibility(newVisibility ? View.VISIBLE : View.INVISIBLE);
        }else {
            if(newVisibility){
                fabDialpad.startAnimation(zoomIn);
            }else {
                fabDialpad.startAnimation(zoomOut);
            }
        }
    }

    public void setVisibilityOfProgressBar(boolean visibility){
        progressBar.setVisibility(visibility ? View.VISIBLE: View.INVISIBLE);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && cardViewMenu.getVisibility() == View.VISIBLE) {
            Rect viewRect = new Rect();
            cardViewMenu.getGlobalVisibleRect(viewRect);
            if (!viewRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                cardViewMenu.setVisibility(View.INVISIBLE);
            }
        }
        else if(ev.getAction() == MotionEvent.ACTION_DOWN && cardViewBlockedContacts.getVisibility() == View.VISIBLE) {
            Rect viewRect = new Rect();
            cardViewBlockedContacts.getGlobalVisibleRect(viewRect);
            if (!viewRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                cardViewBlockedContacts.setVisibility(View.INVISIBLE);
            }
        }
        return super.dispatchTouchEvent(ev);
    }

   /* @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissionOverlay();
        }
    }*/


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void getPermissionOverlay() {
        overlayReqDialog = new AlertDialog.Builder(this)
                .setPositiveButton("Got it!", (d, v) -> {
                    Intent intentSettings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intentSettings.setData(Uri.parse("package:" + DrawerActivity.this.getPackageName()));
                    startActivityForResult(intentSettings, RC_OVERLAY_PERMISSION);
                })
                .setCancelable(false)
                .setMessage("This app needs permission for drawing on top")
                .create();
        overlayReqDialog.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initContactsImages();
        System.out.println("BLOCKED LIST");
        System.out.println(blockedContacts);
        setAppDefault();
        setContentView(R.layout.activity_drawer);
        ButterKnife.bind(this);
        initToolbar();
        setSmsCallsListener();
        setAnimationListeners();

        if (EasyPermissions.hasPermissions(this, perms))  {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(Settings.canDrawOverlays(this)){
                    getSmsList();
                    getCallsLog();
                }else {
                    getPermissionOverlay();
                }

            }else {
                getSmsList();
                getCallsLog();
            }

        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.contacts_rationale),
                    RC_CONTACTS_AND_CALLS, perms);
        }


        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_calls);

        searchBar.setPlaceHolder(getString(R.string.app_name));
        searchBar.setEnabled(false);
        searchBar.setSearchIconTint(Color.TRANSPARENT);

        /*btnBlockedContact = findViewById(R.id.show_blocked_contacts_btn);*/
        btnBlockedContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(DrawerActivity.this, "Clicked", Toast.LENGTH_SHORT).show();
                navigation.setSelected(false);
                navigation.setSelectedItemId(R.id.navigation_sms);
                FragmentsUtil.attachFragment(DrawerActivity.this, new BlockedContactsFragment(),
                        R.id.fragment_container, true);
            }
        });
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        suggestionsAdapter = new SearchViewAdapter(inflater);

        setSearchBarListener();

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {

            switch (navigation.getSelectedItemId()){
                case R.id.navigation_calls:
                    fabDialpad.setVisibility(View.VISIBLE);
                    break;
                default:
                    fabDialpad.setVisibility(View.INVISIBLE);
                    break;
            }

        });

        navigationView.setNavigationItemSelectedListener(this);

       /* if(getSupportActionBar() != null)
            getSupportActionBar().setTitle("1/2");*/

        //when activity opened via notification
        String phoneNumber = getIntent().getStringExtra("PhoneNumber");
        if(phoneNumber != null) {
            setVisibilityOfFAB(false,true);
            openConversationFragment(phoneNumber);
        }
    }

    private void initContactsImages() {
        contactsImages = new HashMap<>();
        Contacts.initialize(this);
        List<Contact> contacts = Contacts.getQuery().find();
        for (Contact contact: contacts) {
            for (PhoneNumber phoneNumber : contact.getPhoneNumbers()) {
                //contactsImages.put(ContactNumberUtil.getCleanNumber(phoneNumber.getNumber()), contact.getPhotoUri());
                //contactsImages.put(ContactNumberUtil.normalizePhoneNumber(this, phoneNumber.getNumber()), contact.getPhotoUri());
                contactsImages.put(ContactNumberUtil.getCleanNumber(phoneNumber.getNumber()), contact.getPhotoUri());
                if(PrefsUtil.getSpamAction(this, ContactNumberUtil.getCleanNumber(phoneNumber.getNumber())) == 1) {
                    blockedContacts.add(ContactNumberUtil.getCleanNumber(phoneNumber.getNumber()));
                }
            }
        }
        System.out.println(contactsImages.size());
        System.out.println(contactsImages);
    }


    public void setSearchBarListener() {
        searchBar.setOnSearchActionListener(this);
    }

    public void initToolbar() {
        toolbar.setTitle("New title");
        toolbar.setNavigationIcon(R.drawable.ic_action_close);



        setSupportActionBar(toolbar);
        getSupportActionBar().hide();
        toolbar.setNavigationOnClickListener(v -> {
            //toolbar.setVisibility(View.INVISIBLE);
            System.out.println("Hide clicked!");
            getSupportActionBar().hide();
            isSelectMode = false;
            updateSmsList();
        });

    }

    private void openConversationFragment(String phoneNumber) {
        System.out.println(phoneNumber);
        SmsListFragment smsListFragment = new SmsListFragment();

        Sms defaultSms = new Sms();
        defaultSms.setAddress(phoneNumber);

        FragmentsUtil.attachFragment(DrawerActivity.this, new SmsListFragment(),
                R.id.fragment_container, true);

        Observable.fromCallable(() -> SmsUtils.getAllSms(DrawerActivity.this, SmsUtils.Type.ALL))
                .subscribeOn(Schedulers.io())
                .flatMapIterable(list -> list)
                .filter(c -> c.getAddress().equals(phoneNumber))
                .defaultIfEmpty(defaultSms)
                .toSortedList(new SmsDateComparator())
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    updateSmsList();
                    if(onSmsListReady != null){
                        onSmsListReady.onReady(list);
                    }
                });

    }

    public List<SmsDialog> getSmsDialogList(){
        return smsList;
    }

    private void getSmsList(){
        isMessagesRead = false;
        Set<String> blockedNumbers = PrefsUtil.getBlockedUnknownNumbers(this);
        System.out.println("Blocked numbers");
        System.out.println(blockedNumbers);

        Observable.fromCallable(() -> SmsUtils.getAllSms(this, SmsUtils.Type.ALL))
                .subscribeOn(Schedulers.io())
                .flatMapIterable(list -> list)
                .filter(sms->
                        PrefsUtil.getSpamAction(this, ContactNumberUtil.getCleanNumber(sms.getAddress())) != 1)
                //.filter(sms -> !blockedNumbers.contains(ContactNumberUtil.getCleanNumber(sms.getAddress())))
                .groupBy(Sms::getAddress)
                .flatMapSingle(Observable::toList)
                .map(list -> {
                    SmsDialog d = new SmsDialog(this);
                    d.setSmsList(list);
                    System.out.println(list.get(0).getAddress());
                    String img = contactsImages.get(list.get(0).getAddress());
                    if(img != null) {
                        d.setPhoto(contactsImages.get(list.get(0).getAddress()));
                        System.out.println("IMAGE ADDED");
                    }
                    else {
                        System.out.println("IMAGE WASN'T ADDED");
                        d.setPhoto("android.resource://" + this.getPackageName() + "/drawable/profile_pictures");
                        //d.setPhoto( this.getApplicationContext().getResources().getDrawable(R.drawable.profile_pictures).toString());
                    }

                    return d;
                }).toSortedList(new DateComparator())
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    smsList = list;
                    if(onCompleteListenerSms != null)
                        onCompleteListenerSms.onComplete();
                    isMessagesRead = true;
                });
    }

    public void updateSmsList() {
        getSmsList();
    }

    public List<CallLog> getCallList(){
        return newList;
    }

    private void getCallsLog() {
        isCallsLogRead = false;
        newList = new ArrayList<>();
        setVisibilityOfProgressBar(true);

        Observable.fromCallable(() -> {
            LogsManager logsManager = new LogsManager(this);
            return logsManager.getLogs(LogsManager.ALL_CALLS);
        })
                .subscribeOn(Schedulers.io())
                .flatMapIterable(list -> list)
                .map(this::transformType)
                .toList()
                .compose(bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list ->{
                    setVisibilityOfProgressBar(false);
                    newList = list;
                    if(onCompleteListenerCalls != null){
                        onCompleteListenerCalls.onComplete();
                        isCallsLogRead = true;
                    }
                });

    }

    public void updateCallsList() {
        getCallsLog();
    }

    DateFormatter.Formatter formatter = date -> {
        if (DateFormatter.isToday(date)) {
            return DateFormatter.format(date, DateFormatter.Template.TIME);
        } else if (DateFormatter.isYesterday(date)) {
            return getString(R.string.date_header_yesterday);
        } else {
            return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH);
        }
    };

    private CallLog transformType(LogObject log) {
        CallLog callLog = new CallLog();
        try {
            callLog.setType(log.getType());
            callLog.setNumber(log.getNumber());
            callLog.setTime(formatter.format(new Date(log.getDate())));
            callLog.setDate(log.getDate());
            callLog.setDuration(log.getCoolDuration());
            callLog.setName(log.getContactName());
        } catch (Exception e) {
            e.printStackTrace();
            callLog.setName("Private");
        }
        newList.add(callLog);
        return callLog;
    }

    private void setAnimationListeners() {
        zoomIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                fabDialpad.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        zoomOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fabDialpad.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        if(isSelectMode)
            isSelectMode = false;
        if(cardViewMenu.getVisibility() == View.VISIBLE)
            cardViewMenu.setVisibility(View.INVISIBLE);

        getSearchBar().setEnabled(false);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if(materialSheetFab != null && materialSheetFab.isSheetVisible()){
            materialSheetFab.hideSheet();
        }else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

        /*DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout); Черновик, для перехода во вкладку СМС
        FragmentManager fm = getSupportFragmentManager();
        OnBackPressedListener backPressedListener = null;
        for (Fragment fragment: fm.getFragments()) {
            if (fragment instanceof  OnBackPressedListener) {
                backPressedListener = (OnBackPressedListener) fragment;
                break;
            }
        }

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if (backPressedListener != null) {
            backPressedListener.onBackPressed();
            setVisibilityOfFAB(false);
            fabDialpad.setVisibility(View.INVISIBLE);
        } else {
            super.onBackPressed();
        }*/

    }

    private void setSmsCallsListener() {
        onNewSmsAndCallsListener = new Timer();
        onNewSmsAndCallsListener.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                if(isNewMessages) {
                    System.out.println("YOU GOT NEW MESSAGE");
                    updateSmsList();
                    isNewMessages = false;
                }
                else if(isNewCall) {
                    isNewCall = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateCallsList();
                        }
                    });
                }
            }

        }, 0, 1000);
    }

    @OnClick(R.id.fab_dialpad)
    void openDialerpad() {
        Intent intent = new Intent(DrawerActivity.this, DialpadActivity.class);
        intent.putExtra(EXTRA_REGION_CODE, "+38");
        startActivityForResult(intent, RC_DIALPAD);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer, menu);
        return true;
    }

    public MaterialSearchBar getSearchBar (){
        return searchBar;
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            Intent intent = new Intent(DrawerActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            Intent intent = new Intent(DrawerActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void callPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);

        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivity(intent);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_CONTACTS_AND_CALLS) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(Settings.canDrawOverlays(this)){
                    getSmsList();
                    getCallsLog();
                }else {
                    getPermissionOverlay();
                }

            }else {
                getSmsList();
                getCallsLog();
            }
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == RC_DIALPAD) {
            String raw = data.getStringExtra(DialpadActivity.EXTRA_RESULT_RAW);
            callPhoneNumber(raw);
        }
        if( requestCode == RC_OVERLAY_PERMISSION){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    getSmsList();
                    getCallsLog();
                }
            }
        }
    }



    public void setVisibilityOfBottomPanel(boolean isVisible){
        navigation.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        fabDialpad.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursorForClose != null){
            mCursorForClose.close();
        }

        SmsRadar.stopSmsRadarService(this);
    }

    public void setAppDefault() {
        final String myPackageName = getPackageName();
        if (!Telephony.Sms.getDefaultSmsPackage(this).equals(myPackageName)) {
            // App is not default.
            // Show the "not currently set as the default SMS app" interface
           /* View viewGroup = findViewById(R.id.not_default_app);
            viewGroup.setVisibility(View.VISIBLE);

            // Set up a button that allows the user to change the default SMS app
            Button button = (Button) findViewById(R.id.change_default_app);
            button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent =
                            new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
                    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                            myPackageName);
                    startActivity(intent);
                }
            });*/

            Intent intent =
                    new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME,
                    myPackageName);
            startActivity(intent);
            Toast.makeText(this, "SET AS DEFAULT", Toast.LENGTH_SHORT);
            System.out.println("SET DEFAULT!!!!!!");
        } else {
            // App is the default.
            // Hide the "not currently set as the default SMS app" interface
            //View viewGroup = findViewById(R.id.not_default_app);
            //viewGroup.setVisibility(View.GONE);
        }
    }

    public HashMap<String, String> getContactImages() {
        return contactsImages;
    }

    public void openSearchView(List<ContactModel> contactList, final List<ContactModel> finalList){
        getSearchBar().setEnabled(true);
        suggestionsAdapter.setSuggestions(contactList);
        searchBar.setCustomSuggestionAdapter(suggestionsAdapter);
        setListenerForSearch(suggestionsAdapter);
        searchBar.enableSearch();
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(start == 0 && count == 0){
                    suggestionsAdapter.setSuggestions(new ArrayList<>(finalList));
                    return;
                }
                if(before == count){
                    return;
                }
                suggestionsAdapter.clearSuggestions();
                for(ContactModel contact : finalList){
                    try {
                        if(contact.getContactName().toLowerCase().contains(s.toString().toLowerCase())
                                || contact.getContactNumber().contains(s.toString())){
                            suggestionsAdapter.addSuggestion(contact);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                StringBuilder builder = new StringBuilder();
                for(char c: s.toString().toCharArray()) {
                    if(Character.isDigit(c))
                        builder.append(c);
                }
                if(builder.length() > 0) {
                    ContactModel newNumber = new ContactModel(builder.toString(), builder.toString());
                    suggestionsAdapter.addSuggestion(newNumber);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void setListenerForSearch(SearchViewAdapter adapter) {
        adapter.setOnItemClickListener(contact -> {

            String contactNumber = contact.getContactNumber();
            Sms defaultSms = new Sms();
            defaultSms.setAddress(contactNumber);

            getSearchBar().disableSearch();
            getSearchBar().setEnabled(false);
            FragmentsUtil.attachFragment(DrawerActivity.this, new SmsListFragment(),
                    R.id.fragment_container, true);

            Observable.fromCallable(() -> SmsUtils.getAllSms(DrawerActivity.this, SmsUtils.Type.ALL))
                    .subscribeOn(Schedulers.io())
                    .flatMapIterable(list -> list)
                    .filter(c -> c.getAddress().equals(contactNumber))
                    .defaultIfEmpty(defaultSms)
                    .toSortedList(new SmsDateComparator())
                    .compose(bindToLifecycle())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(list -> {
                        if(onSmsListReady != null){
                            onSmsListReady.onReady(list);
                        }
                    });
        });
    }


    @Override
    public void onSearchStateChanged(boolean enabled) {

    }

    public DrawerLayout getDrawer() {
        return drawer;
    }

    @Override
    public void onSearchConfirmed(CharSequence text) {

        //make some additional stuff
//        if(suggestionsAdapter.getSuggestions().size() == 1){
//            suggestionsAdapter.getSuggestions().get(0);
//        }

    }

    @Override
    public void onButtonClicked(int buttonCode) {
        switch (buttonCode){
            case MaterialSearchBar.BUTTON_NAVIGATION:
                drawer.openDrawer(GravityCompat.START);
                break;
            case MaterialSearchBar.BUTTON_SPEECH:
                break;
            case MaterialSearchBar.BUTTON_BACK:
                searchBar.disableSearch();
                searchBar.setEnabled(false);
                break;
        }
    }
}
