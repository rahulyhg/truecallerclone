package com.innovations.djnig.truecallerclone.Fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.tamir7.contacts.Contact;
import com.github.tamir7.contacts.Contacts;
import com.gordonwong.materialsheetfab.MaterialSheetFab;
import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.FirebaseHelper;
import com.innovations.djnig.truecallerclone.Utils.FragmentsUtil;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;
import com.innovations.djnig.truecallerclone.Utils.SmsDateComparator;
import com.innovations.djnig.truecallerclone.customView.Fab;
import com.innovations.djnig.truecallerclone.listeners.OnCompleteListener;
import com.innovations.djnig.truecallerclone.models.ContactModel;
import com.innovations.djnig.truecallerclone.models.Sms;
import com.innovations.djnig.truecallerclone.models.SmsDialog;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;
import com.trello.rxlifecycle2.components.support.RxFragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class SmsDialogsFragment extends RxFragment implements OnCompleteListener{

    private int selectedItemsCount = 0;

    private MaterialSearchBar searchBar;

    @BindView(R.id.dialogsList)
    DialogsList dialogsList;

    @BindView(R.id.new_message)
    Fab newMessage;

   /* @BindView(R.id.fab_sheet)
    View sheetView;

    @BindView(R.id.overlay)
    View overlay;

    @BindView(R.id.fab_sheet_new_number)
    View enterNewNumber;

    @BindView(R.id.fab_sheet_from_contacts)
    View fromContact;*/

    private List<Contact> contactList;
    private Disposable subscribe;
    private DrawerActivity drawerActivity;
    private List<SmsDialog> smsDialogs;
    private DialogsListAdapter<SmsDialog> dialogAdapter;
    private ArrayList<ContactModel> contactModelArrayList;
    private MaterialSheetFab<Fab> materialSheetFab;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sms_dialogs, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setTitle("Confirmation");
            alertDialog.setMessage("Do you really want to delete?");
            alertDialog.setPositiveButton("Yes", (dialogInterface, which) -> {
                for (SmsDialog dialog: smsDialogs){
                    if(dialog.isSelected()) {
                        for (Sms sms: dialog.getSmsList()) {
                            sms.delete(getContext());
                        }
                        drawerActivity.getSmsDialogList().remove(dialog);
                    }
                }
                drawerActivity.updateSmsList();
                initAdapter();
                resetActionBar();
                Toast.makeText(getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                System.out.println("DELETE");
            });
            alertDialog.setNegativeButton("No", (dialog, which) -> {
                initAdapter();
                resetActionBar();
            });
            alertDialog.setCancelable(true);
            alertDialog.setOnCancelListener(dialog -> {
                initAdapter();
                resetActionBar();
            });
            alertDialog.show();
        }
        else if(item.getItemId() == R.id.action_block) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setTitle("Confirmation");
            alertDialog.setMessage("Do you really want to block contact?");
            alertDialog.setPositiveButton("Yes", (dialogInterface, which) -> {
                for (SmsDialog dialog: smsDialogs) {
                    if(dialog.isSelected()) {
                        if(dialog.getContactNumber().equals(dialog.getDialogName())) {
                            PrefsUtil.blockUnknownNumber(getContext(),ContactNumberUtil.getCleanNumber(dialog.getContactNumber()));
                            FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(dialog.getContactNumber()));
                        }
                        else {
                            FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(dialog.getContactNumber()));
                            PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(dialog.getContactNumber()), 1);
                        }
                        drawerActivity.blockedContacts.add(dialog.getContactNumber());
                        drawerActivity.getSmsDialogList().remove(dialog);
                    }
                }
                System.out.println(drawerActivity.blockedContacts);
                drawerActivity.updateSmsList();
                initAdapter();
                resetActionBar();
                Toast.makeText(getContext(), "Blocked", Toast.LENGTH_SHORT).show();
                System.out.println("Block contact");
            });
            alertDialog.setNegativeButton("No", (dialog, which) -> {
                initAdapter();
                resetActionBar();
            });
            alertDialog.setCancelable(true);
            alertDialog.setOnCancelListener(dialog -> {
                initAdapter();
                resetActionBar();
            });
            alertDialog.show();
        }
        return true;
    }

    private void resetActionBar() {
        selectedItemsCount = 0;
        drawerActivity.isSelectMode = false;
        for (SmsDialog smsDialog: smsDialogs)
            smsDialog.setSelected(false);
        drawerActivity.getSupportActionBar().hide();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        drawerActivity = (DrawerActivity) getActivity();
        drawerActivity.setSearchBarListener();
        drawerActivity.toolbar.setNavigationOnClickListener(v -> {
            drawerActivity.getSupportActionBar().hide();
            initAdapter();
            resetActionBar();
        });

        initAdapter();

        /*int sheetColor = getResources().getColor(R.color.background_card);
        int fabColor = getResources().getColor(R.color.colorAccent);*/

        /*materialSheetFab = new MaterialSheetFab<>(newMessage, sheetView, overlay, sheetColor, fabColor);

        if(drawerActivity != null){
            drawerActivity.materialSheetFab = this.materialSheetFab;
        }


        enterNewNumber.setOnClickListener(v1 ->{

            SmsListFragment fragment = new SmsListFragment();
            Bundle bundleArgs = new Bundle();
            bundleArgs.putBoolean("newNumber", true);
            fragment.setArguments(bundleArgs);
            materialSheetFab.hideSheet();
            FragmentsUtil.attachFragment(getActivity(), fragment, R.id.fragment_container, true);
        });

        fromContact.setOnClickListener(view1 -> {
            Contacts.initialize(getActivity());
            if(contactList == null || contactModelArrayList == null){
                contactList = Contacts.getQuery().find();

                contactModelArrayList = new ArrayList<>(contactList.size());
                for(Contact contact: contactList){
                    try {

                        String contNum = contact.getPhoneNumbers().get(0).getNormalizedNumber();

                        if(contNum == null || contNum.isEmpty()){
                            contNum = contact.getPhoneNumbers().get(0).getNumber();
                        }

                        contactModelArrayList.add(new ContactModel(contact.getDisplayName(), contNum));
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }

            materialSheetFab.hideSheet();

            if(drawerActivity != null){
                drawerActivity.openSearchView(new ArrayList<>(contactModelArrayList), contactModelArrayList);
            }
        });*/
        newMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Contacts.initialize(getActivity());
                if(contactList == null || contactModelArrayList == null){
                    contactList = Contacts.getQuery().find();

                    contactModelArrayList = new ArrayList<>(contactList.size());
                    for(Contact contact: contactList){
                        try {

                            String contNum = contact.getPhoneNumbers().get(0).getNormalizedNumber();

                            if(contNum == null || contNum.isEmpty()){
                                contNum = contact.getPhoneNumbers().get(0).getNumber();
                            }

                            contactModelArrayList.add(new ContactModel(contact.getDisplayName(), contNum));
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }


                if(drawerActivity != null){
                    drawerActivity.openSearchView(new ArrayList<>(contactModelArrayList), contactModelArrayList);
                }
            }
        });
    }

    private void initAdapter(){
        //dialogAdapter = new DialogsListAdapter<>((imageView, url) -> Picasso.with(getActivity()).load(R.drawable.profile_pictures).into(imageView));
        /*dialogAdapter = new DialogsListAdapter<>(drawerActivity.findViewById(R.layout.dialogs_list),(imageView, url) -> {
            Picasso.with(getActivity()).load(url).into(imageView);
        });*/
        dialogAdapter = new DialogsListAdapter<>(R.layout.item_custom_dialogs_list, new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url) {
                Picasso.with(getActivity()).load(url).into(imageView);
            }
        });
        /*dialogAdapter = new DialogsListAdapter<>(new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url) {
                imageView.setImageBitmap(ContactsUtils.retrieveContactPhoto(getContext(),"2355553332"));
            }
        });*/

        dialogAdapter.setDatesFormatter(date -> {
            if (DateFormatter.isToday(date)) {
                return DateFormatter.format(date, DateFormatter.Template.TIME);
            } else if (DateFormatter.isYesterday(date)) {
                return getString(R.string.date_header_yesterday);
            } else {
                return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH);
            }
        });

        dialogAdapter.setOnDialogViewClickListener((view, dialog) -> {
            if(!drawerActivity.isSelectMode) {
                drawerActivity.setVisibilityOfFAB(false, true);
                drawerActivity.setVisibilityOfBottomPanel(false);
                FragmentsUtil.attachFragment(getActivity(), new SmsListFragment(),
                        R.id.fragment_container, true);

                if(subscribe != null && !subscribe.isDisposed()){
                    subscribe.dispose();
                }

                subscribe = Observable.fromCallable(dialog::getSmsList)
                        .subscribeOn(Schedulers.io())
                        .flatMapIterable(list -> list)
                        .toSortedList(new SmsDateComparator())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(list -> {
                            if (drawerActivity.onSmsListReady != null) {
                                drawerActivity.onSmsListReady.onReady(list);
                            }
                        });
            }
            else if(drawerActivity.isSelectMode) {
                selectListItem(view, dialog);
            }

        });
        dialogAdapter.setOnDialogViewLongClickListener((view, dialog) -> {

         /*   String number = ContactNumberUtil.normalizePhoneNumber(getContext(),dialog.getContactNumber());*/

            System.out.println("Show action bar clicked!");
            drawerActivity.getSupportActionBar().show();
            drawerActivity.isSelectMode = true;
            selectListItem(view, dialog);
        });
        dialogsList.setAdapter(dialogAdapter);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true);
        mLayoutManager.setStackFromEnd(true);
        dialogsList.setLayoutManager(mLayoutManager);
        if(drawerActivity.isMessagesRead)
            getSms();
    }

   /* @Override
    public void onCompleteReading(boolean isSpam) {
        System.out.println("Method called");
        System.out.println(isSpam);
    }*/



    private void selectListItem(View view, SmsDialog dialog) {
        int all = dialogAdapter.getItemCount();
        if(dialog.isSelected()) {
            dialog.setSelected(false);
            selectedItemsCount--;
            view.setBackgroundColor(Color.WHITE);
            if(selectedItemsCount == 0)
                resetActionBar();
        }
        else if(!dialog.isSelected()){
            dialog.setSelected(true);
            selectedItemsCount++;
            view.setBackgroundColor(Color.LTGRAY);
        }
        drawerActivity.getSupportActionBar().setTitle(selectedItemsCount + "/" + all);
    }

    private void removeNotification() {
        /*NotificationManager mNotifyMgr = (android.app.NotificationManager)getContext().getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= 23) {
            StatusBarNotification[] notifications = mNotifyMgr.getActiveNotifications();
            for (StatusBarNotification notification: notifications) {
                if(notification.getTag().equals(phoneNumber)) {
                    mNotifyMgr.cancel(notification.getId());
                    break;
                }
            }
        }*/
    }

    /*private void markAllAsRead(List<Sms> smsList) {
        for(Sms s: smsList){

            SmsUtils.markMessageRead(getActivity(), s.getId());

        }
    }*/

    private void getSms() {


        List<SmsDialog> d = new ArrayList<>(drawerActivity.getSmsDialogList());
        if(d !=null){
            smsDialogs = d;
            dialogAdapter.setItems(smsDialogs);
            if( dialogAdapter.getItemCount() == 0) {
                getActivity().findViewById(R.id.no_sms).setVisibility(View.VISIBLE);
            }
            else
                getActivity().findViewById(R.id.no_sms).setVisibility(View.GONE);
        }


        //checkNetworkNames();
    }

//    private void prepareDialogs() {
//        for (int i = 0; i < smsDialogs.size(); i++) {
//            smsDialogs.get(i).prepare();
//            if (PhoneNumberUtils.isGlobalPhoneNumber(smsDialogs.get(i).getLastMessage().getAddress())) {
//                Log.e("filtered unknowns", smsDialogs.get(i).getLastMessage().getAddress());
//                searchNameForUnknownNumber(smsDialogs.get(i), i);
//            }
//        }
//    }

//    private void checkNetworkNames() {
//        for (int i = 0; i < smsDialogs.size(); i++) {
//            if (PhoneNumberUtils.isGlobalPhoneNumber(smsDialogs.get(i).getLastMessage().getAddress())) {
//                Log.e("filtered unknowns", smsDialogs.get(i).getLastMessage().getAddress());
//                searchNameForUnknownNumber(smsDialogs.get(i), i);
//            }
//        }
//    }

//    private void sortSms() {
//        smsDialogs.clear();
//
//
//
//
//
//        for (int i = 0; i < mSmsList.size(); i++) {
//            Sms smsItem = mSmsList.get(i);
//            if (smsDialogs.isEmpty()) {
//                SmsDialog dialog = new SmsDialog(getActivity());
//                dialog.addSms(smsItem);
//                smsDialogs.add(dialog);
//                continue;
//            }
//            boolean added = false;
//            for (SmsDialog dialog : smsDialogs) {
//                if (dialog.getContactNumber().equals(smsItem.getAddress())) {
//                    dialog.addSms(smsItem);
//                    added = true;
//                }
//            }
//            if (!added) {
//                SmsDialog smsDialog = new SmsDialog(getActivity());
//                smsDialog.addSms(smsItem);
//                smsDialogs.add(smsDialog);
//            }
//        }
//    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        drawerActivity = ((DrawerActivity) getActivity());
        drawerActivity.onCompleteListenerSms = this;
        searchBar = drawerActivity.getSearchBar();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        drawerActivity.onCompleteListenerSms = null;
        drawerActivity.materialSheetFab = null;
    }

//    private void searchNameForUnknownNumber(final SmsDialog item, final int position) {
//        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
//        DatabaseReference mRootRef = mDatabase.getReference();
//        mRootRef.child("contactBase").child(item.getContactNumber()).child("names").orderByValue().limitToLast(1)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(DataSnapshot dataSnapshot) {
//                        if (dataSnapshot.getValue() != null) {
//                            for (DataSnapshot nameSnapshot : dataSnapshot.getChildren()) {
//                                String nameKey = nameSnapshot.getKey();
//                                if (nameKey != null) {
//                                    Log.e("dbStringSms", nameKey);
//                                    for (Sms sms : item.getSmsList()) {
//                                        sms.setAddress(nameKey);
//                                    }
//                                    item.prepare();
//                                    dialogAdapter.notifyItemChanged(position, item);
//                                }
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(DatabaseError databaseError) {
//                        Log.i("dbStringSms", String.valueOf(databaseError));
//                    }
//                });
//    }

    @Override
    public void onComplete() {
        smsDialogs = drawerActivity.getSmsDialogList();
        dialogAdapter.setItems(smsDialogs);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetActionBar();
        selectedItemsCount = 0;
        drawerActivity.isSelectMode = false;

        for (SmsDialog smsDialog: smsDialogs)
            smsDialog.setSelected(false);
        ActionBar supportActionBar = drawerActivity.getSupportActionBar();

        if(supportActionBar != null)
        supportActionBar.hide();
    }
}
