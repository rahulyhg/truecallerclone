package com.innovations.djnig.truecallerclone.Fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.tamir7.contacts.Contact;
import com.github.tamir7.contacts.Contacts;
import com.github.tamir7.contacts.PhoneNumber;
import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.FirebaseHelper;
import com.innovations.djnig.truecallerclone.Utils.FragmentsUtil;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;
import com.innovations.djnig.truecallerclone.Utils.SmsDateComparator;
import com.innovations.djnig.truecallerclone.Utils.SmsUtils;
import com.innovations.djnig.truecallerclone.adapters.BlockedContactsAdapter;
import com.innovations.djnig.truecallerclone.models.BlockedContact;
import com.innovations.djnig.truecallerclone.models.Sms;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by dell on 17.01.2018.
 */

public class BlockedContactsFragment extends Fragment implements EasyPermissions.PermissionCallbacks {
    private static final int RC_CONTACTS = 123;

    @BindView(R.id.recycler_select_contact)
    RecyclerView mRecyclerContacts;

    private AppBarLayout appBarLayout;
    private Toolbar alternativeToolBar;

    private DrawerActivity drawerActivity;

    private BlockedContactsAdapter mAdapter;
    private SharedPreferences sharedPref;

    private TextView deleteAllConversations;
    private TextView unlockAll;




    String[] perms = {Manifest.permission.READ_CONTACTS};

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.select_contacts_fragment, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_more) {
            drawerActivity.cardViewBlockedContacts.setVisibility(View.VISIBLE);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        if(!drawerActivity.isSelectMode) {
            inflater.inflate(R.menu.blocked_contacts_menu, menu);
        }
        else
            inflater.inflate(R.menu.conversation_select_sms_menu, menu);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        if (EasyPermissions.hasPermissions(getActivity(), perms)) {
            getContacts();
            sharedPref = getContext().getSharedPreferences(getContext().getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE);
            int defaultValue = 0;
            int startNumber = sharedPref.getInt(getContext().getString(R.string.start_number), defaultValue);
            if(startNumber<1) {
                //sendAllContacts();
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
        drawerActivity = (DrawerActivity) getActivity();
        drawerActivity.getSearchBar().setVisibility(View.INVISIBLE);
        drawerActivity.setVisibilityOfFAB(false, true);
        initToolBar();
        initCardView();
    }

    private void initCardView() {
        unlockAll = drawerActivity.findViewById(R.id.unlock_all);
        unlockAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                alertDialog.setTitle("Confirmation");
                alertDialog.setMessage("Do you really want to unlock all contacts?");
                alertDialog.setPositiveButton("Yes", (dialogInterface, which) -> {
                    for(BlockedContact contact: mAdapter.getContactList()) {
                        if(contact.getDisplayName().equals(contact.getPhoneNumbers().get(0))) {
                            PrefsUtil.unlockUnknownNumber(getContext(), ContactNumberUtil.getCleanNumber(contact.getPhoneNumbers().get(0)));
                            FirebaseHelper.minusOneFromSpam(ContactNumberUtil.getCleanNumber(contact.getPhoneNumbers().get(0)));
                        }
                        else {
                            for (String phoneNumber : contact.getPhoneNumbers()) {
                                PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(phoneNumber), 0);
                                FirebaseHelper.minusOneFromSpam(ContactNumberUtil.getCleanNumber(phoneNumber));
                            }
                        }
                    }
                    mAdapter.getContactList().clear();
                    mRecyclerContacts.removeAllViews();
                    mRecyclerContacts.setAdapter(mAdapter);
                    drawerActivity.updateSmsList();
                    drawerActivity.cardViewBlockedContacts.setVisibility(View.INVISIBLE);
                    getActivity().findViewById(R.id.nothing_blocked_layout).setVisibility(View.VISIBLE);
                });
                alertDialog.setNegativeButton("No", (dialog, which) -> {

                });
                alertDialog.setCancelable(true);
                alertDialog.setOnCancelListener(dialog -> {

                });
                alertDialog.show();
            }
        });
        deleteAllConversations = drawerActivity.findViewById(R.id.clear_all_messages);
        deleteAllConversations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                alertDialog.setTitle("Confirmation");
                alertDialog.setMessage("Do you really want to delete all messages?");
                alertDialog.setPositiveButton("Delete", (dialogInterface, which) -> {
                    List<Sms> smsList = SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL);
                    for(Sms sms : smsList) {
                        for(BlockedContact contact : mAdapter.getContactList()) {
                            for(String phoneNumber: contact.getPhoneNumbers()) {
                                if(sms.getAddress().equals(ContactNumberUtil.normalizePhoneNumber(drawerActivity, phoneNumber))) {
                                    sms.delete(drawerActivity);
                                }
                            }
                        }
                    }
                    drawerActivity.cardViewBlockedContacts.setVisibility(View.INVISIBLE);
                    drawerActivity.updateSmsList();
                    Toast.makeText(drawerActivity, "Deleted", Toast.LENGTH_SHORT).show();
                });
                alertDialog.setNegativeButton("Cancel", (dialog, which) -> {

                });
                alertDialog.setCancelable(true);
                alertDialog.setOnCancelListener(dialog -> {

                });
                alertDialog.show();
            }
        });
    }

    private void initToolBar() {
        drawerActivity.getSupportActionBar().show();
        drawerActivity.getSupportActionBar().setTitle("Blocked list");
        Toolbar toolbar = drawerActivity.findViewById(R.id.toolbar_main);
        toolbar.setNavigationIcon(null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {

    }

    private void setContactAdapterListener() {
        mAdapter.setOnClickListener(new BlockedContactsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BlockedContact item) {
                drawerActivity.setVisibilityOfFAB(false, true);
                if(item.getPhoneNumbers().size() > 1) {
                    ArrayList<String> list = new ArrayList<>();
                    for(String number: item.getPhoneNumbers()) {
                        list.add(number);
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.select_phone_number);
                    builder.setItems((CharSequence[]) list.toArray(new CharSequence[list.size()]), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String phoneNumber = ContactNumberUtil.normalizePhoneNumber(drawerActivity, item.getPhoneNumbers().get(which));
                            Disposable subscribe;
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
                    String phoneNumber = ContactNumberUtil.normalizePhoneNumber(drawerActivity, item.getPhoneNumbers().get(0));
                    Sms defaultSms = new Sms();
                    defaultSms.setAddress(phoneNumber);
                    //TODO: Fix reading messages
                    FragmentsUtil.attachFragment(getActivity(), new SmsListFragment(),
                            R.id.fragment_container, true);
                    drawerActivity.setVisibilityOfFAB(false, true);
                    Disposable subscribe = Observable.fromCallable(() -> SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL))
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
            }

            @Override
            public void onInnerItemClick(View view, BlockedContact item) {
                if(view.getId() == R.id.unlock_button) {
                    //Toast.makeText(drawerActivity, "Unlock clicked", Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                    alertDialog.setTitle("Confirmation");
                    alertDialog.setMessage("Do you really want to unlock contact?");
                    alertDialog.setPositiveButton("Yes", (dialogInterface, which) -> {
                        if(item.getDisplayName().equals(item.getPhoneNumbers().get(0))) {
                            PrefsUtil.unlockUnknownNumber(getContext(), ContactNumberUtil.getCleanNumber(item.getPhoneNumbers().get(0)));
                            FirebaseHelper.minusOneFromSpam(ContactNumberUtil.getCleanNumber(item.getPhoneNumbers().get(0)));
                        }
                        else {
                            for (String phoneNumber : item.getPhoneNumbers()) {
                                PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(phoneNumber), 0);
                                FirebaseHelper.minusOneFromSpam(ContactNumberUtil.getCleanNumber(phoneNumber));
                            }
                        }
                        Toast.makeText(getContext(), "Unlocked", Toast.LENGTH_SHORT).show();
                        System.out.println("Unlocked");
                        drawerActivity.updateSmsList();
                        mAdapter.getContactList().remove(item);
                        mRecyclerContacts.setAdapter(mAdapter);
                        if(mAdapter.getContactList().size() == 0) {
                            getActivity().findViewById(R.id.nothing_blocked_layout).setVisibility(View.VISIBLE);
                        }

                    });
                    alertDialog.setNegativeButton("No", (dialog, which) -> {

                    });
                    alertDialog.setCancelable(true);
                    alertDialog.setOnCancelListener(dialog -> {

                    });
                    alertDialog.show();
                }
            }
        });
    }

    private void setupRecycler(List<Contact> contacts) {
        List<BlockedContact> blockedContacts = new ArrayList<>();
        for (Contact contact: contacts) {
            for(PhoneNumber number: contact.getPhoneNumbers()) {
                if (PrefsUtil.getSpamAction(getContext(), ContactNumberUtil.getCleanNumber(number.getNumber())) == 1) {
                    blockedContacts.add(createBlockedContact(contact));
                }
            }
        }

        Set<String> numbers = PrefsUtil.getBlockedUnknownNumbers(getContext());
        if(numbers != null) {
            for (String number: numbers) {
                BlockedContact blockedContact = new BlockedContact();
                blockedContact.setDisplayName(number);
                ArrayList<String> unknownNumber = new ArrayList<>();
                unknownNumber.add(number);
                blockedContact.setPhoneNumbers(unknownNumber);
                blockedContacts.add(blockedContact);
            }
        }

        if(blockedContacts.size() == 0) {
            getActivity().findViewById(R.id.nothing_blocked_layout).setVisibility(View.VISIBLE);
        }
        else
            getActivity().findViewById(R.id.nothing_blocked_layout).setVisibility(View.INVISIBLE);
        //mAdapter = new BlockedContactsAdapter(getActivity(), contacts);
        mAdapter = new BlockedContactsAdapter(getActivity(), blockedContacts);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerContacts.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .marginResId(R.dimen.leftmargin, R.dimen.rightmargin)
                .build());
        mRecyclerContacts.setLayoutManager(mLayoutManager);
        mRecyclerContacts.setItemAnimator(new DefaultItemAnimator());
        mRecyclerContacts.setAdapter(mAdapter);

    }

    private BlockedContact createBlockedContact(Contact contact) {
        BlockedContact blockedContact = new BlockedContact();
        List<String> phoneNumbers = new ArrayList<>();
        for (PhoneNumber phoneNumber : contact.getPhoneNumbers()) {
            phoneNumbers.add(phoneNumber.getNumber());
        }
        blockedContact.setDisplayName(contact.getDisplayName());
        blockedContact.setPhoneNumbers(phoneNumbers);
        blockedContact.setPhotoUri(contact.getPhotoUri());
        return blockedContact;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        drawerActivity.getSupportActionBar().hide();
        drawerActivity.initToolbar();
        drawerActivity.getSearchBar().setVisibility(View.VISIBLE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        drawerActivity.getSupportActionBar().hide();
        drawerActivity.initToolbar();
    }

   @Override
    public void onDestroy() {
        super.onDestroy();
        drawerActivity.getSupportActionBar().hide();
        drawerActivity.initToolbar();
    }
}
