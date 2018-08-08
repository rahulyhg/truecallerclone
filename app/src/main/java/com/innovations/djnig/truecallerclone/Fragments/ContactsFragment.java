package com.innovations.djnig.truecallerclone.Fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.tamir7.contacts.Contact;
import com.github.tamir7.contacts.Contacts;
import com.github.tamir7.contacts.PhoneNumber;
import com.github.tamir7.contacts.Query;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.FragmentsUtil;
import com.innovations.djnig.truecallerclone.Utils.SmsDateComparator;
import com.innovations.djnig.truecallerclone.Utils.SmsUtils;
import com.innovations.djnig.truecallerclone.adapters.ContactsAdapter;
import com.innovations.djnig.truecallerclone.models.Sms;
import com.trello.rxlifecycle2.components.support.RxFragment;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by djnig on 12/1/2017.
 */

public class ContactsFragment extends RxFragment implements EasyPermissions.PermissionCallbacks {

    private static final int RC_CONTACTS = 123;
    private static final int RC_CONTACT_DETAILS = 124;
    private static final int RC_ADD_CONTACT = 125;
    private static final int RC_CALL = 125;

    @BindView(R.id.recycler_contacts)
    RecyclerView mRecyclerContacts;

    @BindView(R.id.new_contact)
    FloatingActionButton newContact;

    private ContactsAdapter mAdapter;
    private SharedPreferences sharedPref;
    private Disposable subscribe;

    private DrawerActivity drawerActivity;



    String[] perms = {Manifest.permission.READ_CONTACTS};

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        drawerActivity = (DrawerActivity) getActivity();
    }



    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.contacts_fragment, container, false);
        ButterKnife.bind(this, v);
        return v;
    }



    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        drawerActivity = (DrawerActivity) getActivity();

        ((DrawerActivity)getActivity()).setSearchBarListener();

        if (EasyPermissions.hasPermissions(getActivity(), perms)) {
            getContacts();
            sharedPref = getContext().getSharedPreferences(getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE);
            int defaultValue = 0;
            int startNumber = sharedPref.getInt(getContext().getString(R.string.start_number), defaultValue);
            if(startNumber<1) {
                sendAllContacts();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(getString(R.string.start_number), 1);
                editor.apply();
            }
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.contacts_rationale),
                    RC_CONTACTS, perms);
        }

        setContactAdapterListener();
        newContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
            startActivityForResult(intent, RC_ADD_CONTACT);
            //intent.putExtra(ContactsContract.Intents.Insert.NAME, person.name);
            //intent.putExtra(ContactsContract.Intents.Insert.PHONE, person.mobile);
            //intent.putExtra(ContactsContract.Intents.Insert.EMAIL, person.email);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == RC_CALL) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    drawerActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawerActivity.updateCallsList();
                        }
                    });

                }
            });
            thread.start();
            getContacts();
        }
        if (requestCode == RC_CONTACT_DETAILS) {
            System.out.println("IN ON ACTIVITY RESULT");
            drawerActivity.updateCallsList();
            drawerActivity.getSmsDialogList();
            getContacts();
            sendAllContacts();
            //new listener on adapter
            setContactAdapterListener();
        }
        else if(requestCode == RC_ADD_CONTACT) {
            List<Contact> contacts = mAdapter.getContactList();
            getContacts();
            if(contacts.size() != mAdapter.getContactList().size()) {
                //System.out.println("NEW USER!!!");
                boolean isFound = false;
                for (Contact cont: mAdapter.getContactList()) {
                    for(Contact localCont: contacts) {
                        if(cont.getDisplayName().equals(localCont.getDisplayName())) {
                            isFound = true;
                        }
                    }
                    if(!isFound) {
                        //System.out.println("FOUND");
                        System.out.println(cont.getDisplayName());
                        //sendNewContact(cont); to save traffic
                        sendAllContacts();
                    }
                    isFound = false;
                }
            }
            //new listener on adapter
            setContactAdapterListener();
        }
    }

    private void setContactAdapterListener() {
        mAdapter.setOnClickListener(new ContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Contact item) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(item.getId()));
                intent.setData(uri);
                //startActivity(intent);
                startActivityForResult(intent, RC_CONTACT_DETAILS);
                //getContacts();
                //sendAllContacts();
            }

            @Override
            public void onInnerItemClick(View view, Contact item) {
                switch (view.getId()) {
                    case R.id.call_button:
                        List<PhoneNumber> numbers = item.getPhoneNumbers();
                        if(numbers.size() > 1) {
                            ArrayList<String> list = new ArrayList<>();
                            for(PhoneNumber number: numbers) {
                                list.add(number.getNumber());
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle(R.string.select_phone_number);
                            builder.setItems((CharSequence[]) list.toArray(new CharSequence[list.size()]), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    callPhoneNumber(numbers.get(which).getNumber());
                                }
                            });

                            builder.setCancelable(true);
                            builder.show();
                        }
                        else
                            callPhoneNumber(numbers.get(0).getNumber());
                        break;
                    case R.id.sms_button:
                        /*String phoneNumber = null;
                        try {
                            phoneNumber = item.getPhoneNumbers().get(0).getNormalizedNumber();
                            if(phoneNumber == null)
                                item.getPhoneNumbers().get(0).getNumber();
                        }catch (Exception e){
                            Toast.makeText(getActivity(), "Error!", Toast.LENGTH_SHORT).show();
                            break;
                        }

                        final String finalNumber = phoneNumber;
                        Sms defaultSms = new Sms();
                        defaultSms.setAddress(finalNumber);
                        FragmentsUtil.attachFragment(getActivity(), new SmsListFragment(),
                                R.id.fragment_container, true);

                        if(subscribe != null && !subscribe.isDisposed()){
                            subscribe.dispose();
                        }

                        subscribe = Observable.fromCallable(() -> SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL))
                                .subscribeOn(Schedulers.io())
                                .flatMapIterable(list -> list)
                                .filter(c -> c.getAddress().equals(finalNumber))
                                .defaultIfEmpty(defaultSms)
                                .toSortedList(new SmsDateComparator())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(list -> {
                                    if (drawerActivity.onSmsListReady != null) {
                                        drawerActivity.onSmsListReady.onReady(list);
                                    }
                                });*/
                        List<PhoneNumber> phoneNumbers = item.getPhoneNumbers();
                        if(phoneNumbers.size() > 1) {
                            ArrayList<String> list = new ArrayList<>();
                            for(PhoneNumber number: phoneNumbers) {
                                list.add(number.getNumber());
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle(R.string.select_phone_number);
                            builder.setItems((CharSequence[]) list.toArray(new CharSequence[list.size()]), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                   //String phoneNumber = phoneNumbers.get(which).getNumber();
                                   //String phoneNumber = ContactNumberUtil.normalizePhoneNumber(drawerActivity, phoneNumbers.get(which).getNumber());
                                   String phoneNumber = ContactNumberUtil.getCleanNumber(phoneNumbers.get(which).getNumber());
                                   //phoneNumber = ContactNumberUtil.normalizePhoneNumber(drawerActivity, phoneNumber);
                                    if (subscribe != null && !subscribe.isDisposed()) {
                                        subscribe.dispose();
                                    }
                                    drawerActivity.setVisibilityOfFAB(false, true);
                                    Sms defaultSms = new Sms();
                                    defaultSms.setAddress(phoneNumber);

                                    FragmentsUtil.attachFragment(getActivity(), new SmsListFragment(),
                                            R.id.fragment_container, true);

                                    subscribe = Observable.fromCallable(() -> SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL))
                                            .subscribeOn(Schedulers.io())
                                            .flatMapIterable(list -> list)
                                            .filter(c -> c.getAddress().equals(phoneNumber))
                                            .defaultIfEmpty(defaultSms)
                                            .toSortedList(new SmsDateComparator())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(list -> {
                                                if (drawerActivity.onSmsListReady != null) {
                                                    drawerActivity.onSmsListReady.onReady(list);
                                                }
                                            });
                                }
                            });

                            builder.setCancelable(true);
                            builder.show();
                        }
                        else {
                            //String phoneNumber = ContactNumberUtil.normalizePhoneNumber(drawerActivity, phoneNumbers.get(0).getNumber());
                            String phoneNumber = ContactNumberUtil.getCleanNumber(phoneNumbers.get(0).getNumber());

                            if (subscribe != null && !subscribe.isDisposed()) {
                                subscribe.dispose();
                            }
                            drawerActivity.setVisibilityOfFAB(false, true);
                            Sms defaultSms = new Sms();
                            defaultSms.setAddress(phoneNumber);

                            FragmentsUtil.attachFragment(getActivity(), new SmsListFragment(),
                                    R.id.fragment_container, true);

                            subscribe = Observable.fromCallable(() -> SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL))
                                    .subscribeOn(Schedulers.io())
                                    .flatMapIterable(list -> list)
                                    .filter(c -> c.getAddress().equals(phoneNumber))
                                    .defaultIfEmpty(defaultSms)
                                    .toSortedList(new SmsDateComparator())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(list -> {
                                        if (drawerActivity.onSmsListReady != null) {
                                            drawerActivity.onSmsListReady.onReady(list);
                                        }
                                    });
                        }
                        break;
                }
            }
        });
    }

    private void sendNewContact(Contact cont) {
        Map<String, Object> childUpdates = new HashMap<>();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String countryIso = null;
        //this part should be transfered to MainActivity for a first start and save countryIso to  preferences

        if (tm != null) {
            if (tm.getSimCountryIso() != null)
                countryIso = tm.getSimCountryIso().toUpperCase();
            else if (tm.getNetworkCountryIso() != null)
                countryIso = tm.getNetworkCountryIso().toUpperCase();
            else if (Locale.getDefault().getCountry() != null)
                countryIso = Locale.getDefault().getCountry();
        } else if (Locale.getDefault().getCountry() != null)
            countryIso = Locale.getDefault().getCountry();


        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.country_iso), countryIso);
        editor.commit();

        if (user != null) {
            Query q = Contacts.getQuery();
            List<Contact> contacts = q.hasPhoneNumber().find();

            //mRootRef.child("contacts").child(user.getUid()).setValue(contacts);
            mRootRef.child("contacts").child(user.getUid()).push().setValue(cont);
            //for (Contact contact : contacts) {
                for (PhoneNumber phoneNumber : cont.getPhoneNumbers()) {
                    String number;
                    if (phoneNumber.getNormalizedNumber() != null) {
                        number = ContactNumberUtil.normalizePhoneNumber(getContext(), phoneNumber.getNormalizedNumber());
                    } else {
                        number = ContactNumberUtil.normalizePhoneNumber(getContext(), phoneNumber.getNumber());
                    }
                    String displayName = cont.getDisplayName();
                    displayName = displayName.replace(".","");
                    displayName = displayName.replace("#","");
                    displayName = displayName.replace("$","");
                    displayName = displayName.replace("[","");
                    displayName = displayName.replace("]","");

                    number = number.replace(".","");
                    number = number.replace("#","");
                    number = number.replace("$","");
                    number = number.replace("[","");
                    number = number.replace("]","");

                    final DatabaseReference namesReference = mRootRef.child("contactBase").child(number).child("names").
                            child(displayName);
                    namesReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue(Long.class) != null) {
                                Log.i("dblong ", String.valueOf(dataSnapshot.getValue(Long.class)));
                                namesReference.setValue(dataSnapshot.getValue(Long.class) + 1);
                            } else {
                                namesReference.setValue(1);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }

                    });
                //}
            }
        }
    }

    public void callPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, RC_CALL);
        }
    }

    private void setupRecycler(List<Contact> contacts) {
        mAdapter = new ContactsAdapter(getActivity(), contacts);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerContacts.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .marginResId(R.dimen.leftmargin, R.dimen.rightmargin)
                .build());
        mRecyclerContacts.setLayoutManager(mLayoutManager);
        mRecyclerContacts.setItemAnimator(new DefaultItemAnimator());
        mRecyclerContacts.setAdapter(mAdapter);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == RC_CONTACTS) {
            getContacts();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {


    }

    @AfterPermissionGranted(RC_CONTACTS)
    void getContacts() {

        Contacts.initialize(getActivity());
        setupRecycler(Contacts.getQuery().find());

    }

    @AfterPermissionGranted(RC_CONTACTS)
    void sendAllContacts() {
        System.out.println("SENDING CONTACTS TO DB");
        Map<String, Object> childUpdates = new HashMap<>();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
        String countryIso = null;
        //this part should be transfered to MainActivity for a first start and save countryIso to  preferences

        if (tm != null) {
            if (tm.getSimCountryIso() != null)
                countryIso = tm.getSimCountryIso().toUpperCase();
            else if (tm.getNetworkCountryIso() != null)
                countryIso = tm.getNetworkCountryIso().toUpperCase();
            else if (Locale.getDefault().getCountry() != null)
                countryIso = Locale.getDefault().getCountry();
        } else if (Locale.getDefault().getCountry() != null)
            countryIso = Locale.getDefault().getCountry();


        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.country_iso), countryIso);
        editor.apply();

        if (user != null) {
            Query q = Contacts.getQuery();
            List<Contact> contacts = q.hasPhoneNumber().find();

            mRootRef.child("contacts").child(user.getUid()).setValue(contacts);
            for (Contact contact : contacts) {
                for (PhoneNumber phoneNumber : contact.getPhoneNumbers()) {
                    String number;
                    if (phoneNumber.getNormalizedNumber() != null) {
                        number = ContactNumberUtil.normalizePhoneNumber(getContext(), phoneNumber.getNormalizedNumber());
                    } else {
                        number = ContactNumberUtil.normalizePhoneNumber(getContext(), phoneNumber.getNumber());
                    }
                    String displayName = contact.getDisplayName();
                    displayName = displayName.replace(".","");
                    displayName = displayName.replace("#","");
                    displayName = displayName.replace("$","");
                    displayName = displayName.replace("[","");
                    displayName = displayName.replace("]","");

                    number = number.replace(".","");
                    number = number.replace("#","");
                    number = number.replace("$","");
                    number = number.replace("[","");
                    number = number.replace("]","");
                    number = number.replace(" ","");

                    final DatabaseReference spamInit = mRootRef.child("contactBase").child(number).child("spam");
                    spamInit.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot.getValue() == null) {
                                spamInit.setValue(1);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                    final DatabaseReference namesReference = mRootRef.child("contactBase").child(number).child("names").
                            child(displayName);
                    namesReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.getValue(Long.class) != null) {
                                Log.i("dblong ", String.valueOf(dataSnapshot.getValue(Long.class)));
                                namesReference.setValue(dataSnapshot.getValue(Long.class) + 1);
                            } else {
                                namesReference.setValue(1);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }

                    });
                }
            }
        }

    }


}
