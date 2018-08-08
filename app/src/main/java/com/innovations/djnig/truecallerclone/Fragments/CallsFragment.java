package com.innovations.djnig.truecallerclone.Fragments;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.FirebaseHelper;
import com.innovations.djnig.truecallerclone.Utils.FragmentsUtil;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;
import com.innovations.djnig.truecallerclone.Utils.SmsDateComparator;
import com.innovations.djnig.truecallerclone.Utils.SmsUtils;
import com.innovations.djnig.truecallerclone.adapters.LogsAdapter;
import com.innovations.djnig.truecallerclone.listeners.OnCompleteListener;
import com.innovations.djnig.truecallerclone.models.CallLog;
import com.innovations.djnig.truecallerclone.models.Sms;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.trello.rxlifecycle2.components.support.RxFragment;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by djnig on 12/2/2017.
 */

public class CallsFragment extends RxFragment implements  OnCompleteListener {

    @BindView(R.id.recycler_call_log)
    RecyclerView mRecyclerCalls;

    MaterialSearchBar searchBar;
    private Disposable subscribe;

    private LogsAdapter mAdapter;
    //List<LogObject> callLogs;
    private Disposable subscription;
    private static final int RC_CALL = 125;


    private DrawerActivity drawerActivity;
    private Set<CallLog> callsListToDelete = new HashSet<>();
    private List<CallLog> newList;
    private LinearLayoutManager mLayoutManager;
    int mNotificationId;
    private short selectedItemsCount = 0;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.calls_fragment, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setHasOptionsMenu(true);
        drawerActivity = (DrawerActivity) getActivity();
        drawerActivity.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCalls();
                resetActionBar();
            }
        });
        getCalls();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setTitle("Confirmation");
            alertDialog.setMessage("Do you really want to delete?");
            alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    for(CallLog callLog: callsListToDelete) {
                        callLog.delete(getContext());
                    }
                    drawerActivity.updateCallsList();
                    getCalls();
                    resetActionBar();
                    System.out.println("DELETE");
                }
            });
            alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getCalls();
                    resetActionBar();
                }
            });
            alertDialog.setCancelable(true);
            alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    getCalls();
                    resetActionBar();
                }
            });
            alertDialog.show();
        }
        else if(id == R.id.action_block) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
            alertDialog.setTitle("Confirmation");
            alertDialog.setMessage("Do you really want to block contact?");
            alertDialog.setPositiveButton("Yes", (dialogInterface, which) -> {
                for (CallLog dialog: newList) {
                    if(dialog.isSelected()) {
                        if(dialog.getNumber().equals(dialog.getName())) {
                            PrefsUtil.blockUnknownNumber(getContext(),ContactNumberUtil.getCleanNumber(dialog.getNumber()));
                            FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(dialog.getNumber()));
                        }
                        else {
                            FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(dialog.getNumber()));
                            PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(dialog.getNumber()), 1);
                        }
                        drawerActivity.blockedContacts.add(dialog.getNumber());
                        //drawerActivity.getCallList().remove(dialog);
                    }
                }
                System.out.println(drawerActivity.blockedContacts);
                //drawerActivity.updateCallsList();
                getCalls();
                resetActionBar();
                Toast.makeText(getContext(), "Blocked", Toast.LENGTH_SHORT).show();
                System.out.println("Block contact");
            });
            alertDialog.setNegativeButton("No", (dialog, which) -> {
                getCalls();
                resetActionBar();
            });
            alertDialog.setCancelable(true);
            alertDialog.setOnCancelListener(dialog -> {
                getCalls();
                resetActionBar();
            });
            alertDialog.show();
        }
        return true;
    }

    private void resetActionBar() {  //disable select mode and reset item's views
        if(drawerActivity.isSelectMode) {
            selectedItemsCount = 0;
            drawerActivity.isSelectMode = false;
            for (CallLog callLog : newList)
                callLog.setSelected(false);
            callsListToDelete.clear();
            drawerActivity.getSupportActionBar().hide();
        }
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        drawerActivity = ((DrawerActivity)getActivity());
        drawerActivity.onCompleteListenerCalls = this;
        searchBar = drawerActivity.getSearchBar();
        searchBar.clearSuggestions();
        searchBar.setSearchIconTint(Color.BLACK);
        searchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(count == 0){
                    mAdapter = new LogsAdapter(getActivity());
                    mRecyclerCalls.setAdapter(mAdapter);
                    mAdapter.setItems(newList);
                    return;
                }
                if(before == count){
                    return;
                }

                if(subscription != null && !subscription.isDisposed()){
                    subscription.dispose();
                }

                drawerActivity.setVisibilityOfProgressBar(true);

                mAdapter.clearItems();
                mAdapter.notifyDataSetChanged();
                subscription = Observable.fromIterable(newList)
                        .filter(log -> log.getName().toLowerCase().contains(s.toString().toLowerCase()))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .compose(bindToLifecycle())
                        .doOnComplete(() -> {
                    drawerActivity.setVisibilityOfProgressBar(false);
                    mLayoutManager.scrollToPositionWithOffset(0, 0);

                })
                        .doOnDispose(() -> drawerActivity.setVisibilityOfProgressBar(false))
                        .subscribe(log -> {
                            mAdapter.addItem(log);
                            mAdapter.notifyDataSetChanged();
                        });

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        searchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

            }

            @Override
            public void onSearchConfirmed(CharSequence text) {

            }

            @Override
            public void onButtonClicked(int buttonCode) {
                switch (buttonCode){
                    case MaterialSearchBar.BUTTON_NAVIGATION:
                        ((DrawerActivity)getActivity()).getDrawer().openDrawer(GravityCompat.START);
                        break;
                    case MaterialSearchBar.BUTTON_BACK:
                        searchBar.disableSearch();
                        mAdapter = new LogsAdapter(getActivity());
                        mRecyclerCalls.setAdapter(mAdapter);
                        mAdapter.setItems(newList);
                        break;
                }
            }
        });

    }

    @Override
    public void onDetach() {
        super.onDetach();
        searchBar.setEnabled(false);
        searchBar.setSearchIconTint(Color.TRANSPARENT);
        drawerActivity.onCompleteListenerCalls = null;
    }

    private void getCalls() {
        mAdapter = new LogsAdapter(getActivity());

        mAdapter.setOnClickListener(new LogsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, CallLog item) {
                if(!drawerActivity.isSelectMode) {
                    callPhoneNumber(item.getNumber());
                    //getNotification(item);
                }
                else if(drawerActivity.isSelectMode){
                    selectListItem(view, item);
                    System.out.println(callsListToDelete.size());
                }
            }

            @Override
            public void onInnerItemClick(View view, CallLog item) {
                if(!drawerActivity.isSelectMode) {
                    switch (view.getId()) {
                    case R.id.call_info:
                        Toast.makeText(getActivity(), "INFO", Toast.LENGTH_SHORT).show();
                        break;
                    case R.id.sms_button_calls:
                        drawerActivity.setVisibilityOfFAB(false, true);
                        drawerActivity.setVisibilityOfBottomPanel(false);
                        if(subscribe != null && !subscribe.isDisposed()){
                            subscription.dispose();
                        }
                        drawerActivity.setVisibilityOfFAB(false,true);
                        String phoneNumber = item.getNumber();
                        Sms defaultSms = new Sms();
                        defaultSms.setAddress(phoneNumber);

                        FragmentsUtil.attachFragment(getActivity(), new SmsListFragment(),
                                R.id.fragment_container, true);

                        subscribe = Observable.fromCallable(() -> SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL))
                                .subscribeOn(Schedulers.io())
                                .flatMapIterable(list -> list)
                                .filter(c -> c.getAddress().equals(phoneNumber))
                                .defaultIfEmpty(defaultSms)
                                .toSortedList( new SmsDateComparator())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(list -> {
                                    if (drawerActivity.onSmsListReady != null){
                                        drawerActivity.onSmsListReady.onReady(list);
                                    }
                                });
                        break;
                    }
                }
            }
        });
        mAdapter.setOnItemLongClickListener(new LogsAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClickListener(View view, CallLog item) {
                System.out.println("Show action bar clicked!");
                drawerActivity.getSupportActionBar().show();
                drawerActivity.isSelectMode = true;
                selectListItem(view, item);
            }
        });
        mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true);
        mLayoutManager.setStackFromEnd(true);
        mRecyclerCalls.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getActivity())
                .marginResId(R.dimen.leftmargin, R.dimen.rightmargin)
                .build());
        mRecyclerCalls.setLayoutManager(mLayoutManager);
        mRecyclerCalls.setItemAnimator(new DefaultItemAnimator());
        mRecyclerCalls.setAdapter(mAdapter);
        //TODO: Fix concurent exception

        if(drawerActivity.isCallsLogRead) {
            newList = drawerActivity.getCallList();
            if (newList != null) {
                if (newList.size() == 0) {
                    getActivity().findViewById(R.id.no_calls).setVisibility(View.VISIBLE);
                } else
                    getActivity().findViewById(R.id.no_calls).setVisibility(View.INVISIBLE);

                for (CallLog callLog : newList) {
                    callLog.setNumber(ContactNumberUtil.normalizePhoneNumber(getContext(), callLog.getNumber()));
                }
                mAdapter.setItems(newList);
                searchBar.setEnabled(true);
            }
        }

    }

    private void selectListItem(View view, CallLog dialog) {
        System.out.println("There changes");
        int all = mAdapter.getItemCount();
        if(dialog.isSelected()) {
            dialog.setSelected(false);
            selectedItemsCount--;
            view.setBackgroundColor(Color.WHITE);
            callsListToDelete.remove(dialog);
            if(selectedItemsCount == 0)
                resetActionBar();
        }
        else if(!dialog.isSelected()){
            dialog.setSelected(true);
            selectedItemsCount++;
            System.out.println("ID: " + view.getId());
            view.setBackgroundColor(Color.LTGRAY);
            callsListToDelete.add(dialog);
        }
        drawerActivity.getSupportActionBar().setTitle(selectedItemsCount + "/" + all);
    }



    private void getNotification(CallLog item) {
        /*SmsListFragment smsListFragment = new SmsListFragment();

        List<Sms> allSms = SmsUtils.getAllSms(getActivity(), SmsFolder.INCOMING_SMS);
        List<Sms> smsList = new ArrayList<>();
        for(Sms sms: allSms) {
            if (sms.getAddress().equals(item.getNumber()))
                smsList.add(sms);
        }
        if(smsList.size() == 0) {
            Sms sms = new Sms();
            sms.setAddress(item.getNumber());
            smsList.add(sms);
        }

        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("sms", (ArrayList<? extends Parcelable>) smsList);
        smsListFragment.setArguments(bundle);
        FragmentsUtil.attachFragment(getActivity(), smsListFragment,
                R.id.fragment_container, true);*/

        Intent resultIntent = new Intent(getContext(), DrawerActivity.class);
        resultIntent.putExtra("PhoneNumber", "650470");


        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        getContext(),
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        Uri soundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getContext())
                        .setSmallIcon(R.drawable.ic_notification_message)
                        .setContentTitle(item.getNumber())
                        .setContentText("Notification body")
                        .setContentIntent(resultPendingIntent)
                        .setAutoCancel(true)
                        .setSound(soundUri);

        mNotificationId++;

        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getContext().getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    public void callPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, RC_CALL);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_CALL) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    drawerActivity.updateCallsList();
                    getCalls();
                }
            };
            runnable.run();

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        resetActionBar();
    }

    @Override
    public void onComplete() {
       /* newList = drawerActivity.getCallList();
        mAdapter.setItems(newList);
        searchBar.setEnabled(true);*/
        newList = drawerActivity.getCallList();
        if(newList != null){
            if(newList.size() == 0) {
                getActivity().findViewById(R.id.no_calls).setVisibility(View.VISIBLE);
            }
            else
                getActivity().findViewById(R.id.no_calls).setVisibility(View.INVISIBLE);

            for(CallLog callLog: newList) {
                callLog.setNumber(ContactNumberUtil.normalizePhoneNumber(getContext(), callLog.getNumber()));
            }
            mAdapter.setItems(newList);
            searchBar.setEnabled(true);
        }
    }
}
