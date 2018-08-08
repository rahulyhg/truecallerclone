package com.innovations.djnig.truecallerclone.Fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;
import com.innovations.djnig.truecallerclone.DrawerActivity;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.ContactsUtils;
import com.innovations.djnig.truecallerclone.Utils.FirebaseHelper;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;
import com.innovations.djnig.truecallerclone.Utils.SmsUtils;
import com.innovations.djnig.truecallerclone.listeners.OnBackPressedListener;
import com.innovations.djnig.truecallerclone.listeners.OnSmsListReady;
import com.innovations.djnig.truecallerclone.models.Sms;
import com.innovations.djnig.truecallerclone.newClasses.MessageItem;
import com.klinker.android.send_message.Message;
import com.klinker.android.send_message.Settings;
import com.klinker.android.send_message.Transaction;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.stfalcon.chatkit.utils.DateFormatter;
import com.trello.rxlifecycle2.components.support.RxFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;
import github.ankushsachdeva.emojicon.EmojiconGridView;
import github.ankushsachdeva.emojicon.EmojiconsPopup;
import github.ankushsachdeva.emojicon.emoji.Emojicon;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by djnig on 12/14/2017.
 */

public class SmsListFragment extends RxFragment implements OnBackPressedListener, OnSmsListReady {

    private static final int RC_ADD_CONTACT = 125;
    private static final int RC_CALL = 124;
    private static final int RC_CONTACT_DETAILS = 123;
    @BindView(R.id.messagesList)
    MessagesList messagesList;

    @BindView(R.id.messageInput)
    EditText editText;

    @BindView(R.id.emoji_button)
    ImageView emojiButton;

    @BindView(R.id.input_message)
    MessageInput input;

    @BindView(R.id.progress_sms_list)
    ProgressBar progressBar;

    private DrawerActivity mDrawerActivity;
    private MessagesListAdapter<Sms> adapter;
    private List<Sms> smsList;
    private String mAddressToSendTo;
    private MaterialSearchBar mActionBar;

    private boolean isNewNumber;
    private String newPhoneNumber;

    private AppBarLayout appBarLayout;
    private Toolbar alternativeToolBar;
    private CardView cardView;
    private TextView cardViewNewContact;
    private TextView cardViewBlock;
    private TextView cardViewDeleteConv;

    private HashSet<Sms> selectedSms = new HashSet<>();
    private LinearLayout personalInfo;
    private Timer onNewSmsListener;

    private int smsCount = 0;
    private CircleImageView contactsPhoto;

    private boolean hasPhoto = false;
    private boolean isBlocked = false;
    private TextView blockedTextView;

    private EmojiconsPopup popup;
    private boolean isEmojiKeyboardActive = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDrawerActivity = (DrawerActivity) getActivity();
        mDrawerActivity.onSmsListReady = this;
        mDrawerActivity.setVisibilityOfFAB(false, true);
        mDrawerActivity.setVisibilityOfBottomPanel(false);
        mActionBar = mDrawerActivity.getSearchBar();
        mActionBar.setVisibility(View.INVISIBLE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sms_list, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);
        Bundle bundle = getArguments();

        if(bundle != null){
            isNewNumber = bundle.getBoolean("newNumber", false);
        }
        adapter = new MessagesListAdapter<>("0", new ImageLoader() {
            @Override
            public void loadImage(ImageView imageView, String url) {
                Picasso.with(SmsListFragment.this.getContext()).load(url).into(imageView);
            }
        });

        adapter.enableSelectionMode(new MessagesListAdapter.SelectionListener() {

            @Override
            public void onSelectionChanged(int count) {
                if(!mDrawerActivity.isSelectMode) {
                    mDrawerActivity.toolbar.getMenu().clear();
                    mDrawerActivity.toolbar.inflateMenu(R.menu.drawer);
                    hideToolBar();
                    mDrawerActivity.getSupportActionBar().show();
                    mDrawerActivity.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(mDrawerActivity.isSelectMode) { //this is need, because  adapter.unselectAllItems(); not allow to change toolbar
                                adapter.unselectAllItems();
                                mDrawerActivity.isSelectMode = false;
                                initToolBar();
                                onCreateOptionsMenu(mDrawerActivity.toolbar.getMenu(), mDrawerActivity.getMenuInflater());

                                //selectedSms.clear();
                            /*adapter.clear();
                            adapter = new MessagesListAdapter<>("0", null);
                            adapter.addToEnd(smsList, false);*/
                            }
                        }
                    });

                    mDrawerActivity.getSupportActionBar().setTitle(count + " / " + smsCount);
                    mDrawerActivity.isSelectMode = true;
                    onCreateOptionsMenu(mDrawerActivity.toolbar.getMenu(), mDrawerActivity.getMenuInflater());
                }
                else if(mDrawerActivity.isSelectMode) {
                    mDrawerActivity.getSupportActionBar().setTitle(adapter.getSelectedMessages().size() + " / " + smsCount);
                    if(adapter.getSelectedMessages().size() == 0) {
                        mDrawerActivity.isSelectMode = false;
                        initToolBar();
                        onCreateOptionsMenu(mDrawerActivity.toolbar.getMenu(), mDrawerActivity.getMenuInflater());
                    }
                }
            }
        });
        adapter.setDateHeadersFormatter(date -> {
            if (DateFormatter.isToday(date)) {
                return getString(R.string.today);
            } else if (DateFormatter.isYesterday(date)) {
                return getString(R.string.date_header_yesterday);
            } else {
                return DateFormatter.format(date, DateFormatter.Template.STRING_DAY_MONTH_YEAR);
            }
        });


        /* First way without enableSelectMode
        adapter.setOnMessageViewClickListener(new MessagesListAdapter.OnMessageViewClickListener<Sms>() {
            @Override
            public void onMessageViewClick(View view, Sms message) {
                if(mDrawerActivity.isSelectMode) {
                    selectSmsItem(view, message);
                }
            }
        });
        adapter.setOnMessageViewLongClickListener(new MessagesListAdapter.OnMessageViewLongClickListener<Sms>() {
            @Override
            public void onMessageViewLongClick(View view, Sms message) {
                if(!mDrawerActivity.isSelectMode) {
                    view.setBackgroundColor(Color.LTGRAY);
                    mDrawerActivity.toolbar.getMenu().clear();
                    mDrawerActivity.toolbar.inflateMenu(R.menu.drawer);
                    hideToolBar();
                    mDrawerActivity.getSupportActionBar().show();
                    mDrawerActivity.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mDrawerActivity.isSelectMode = false;
                            initToolBar();
                            onCreateOptionsMenu(mDrawerActivity.toolbar.getMenu(), mDrawerActivity.getMenuInflater());
                            selectedSms.clear();
                            adapter.clear();
                            adapter = new MessagesListAdapter<>("0", null);
                            adapter.addToEnd(smsList, false);
                        }
                    });

                    mDrawerActivity.isSelectMode = true;
                    onCreateOptionsMenu(mDrawerActivity.toolbar.getMenu(), mDrawerActivity.getMenuInflater());
                    selectSmsItem(view, message);
                   *//* mDrawerActivity.setTitle(adapter.getSelectedMessages().size());*//*
                }

            }
        });*/
        messagesList.setAdapter(adapter);

        editText.setPaddingRelative(0,0,50,0);

        input.setInputListener(input -> {
            if(isNewNumber){
                if(newPhoneNumber == null || newPhoneNumber.isEmpty()){
                    Toast.makeText(getActivity(), "Enter phone number first!", Toast.LENGTH_SHORT).show();
                    return false;
                }

            }

            sendSms(input);
            Sms sms = new Sms();
            sms.setId(isNewNumber ? newPhoneNumber : mAddressToSendTo);
            sms.setReadState("1");
            sms.setTime(Long.toString(new Date().getTime()));
            sms.setMsg(input.toString());
            sms.setAddress(isNewNumber ? newPhoneNumber : mAddressToSendTo);
            sms.setIncoming(false);
            adapter.addToStart(sms, true);
            //smsList.add(0, sms);
            mDrawerActivity.updateSmsList();
            return true;
        });

        emojiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!popup.isShowing()){

                    //If keyboard is visible, simply show the emoji popup
                    if(popup.isKeyBoardOpen()){
                        popup.showAtBottom();
                    }

                    //else, open the text keyboard first and immediately after that show the emoji popup
                    else{
                        editText.setFocusableInTouchMode(true);
                        editText.requestFocus();
                        popup.showAtBottomPending();
                        final InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                    }
                }

                //If popup is showing, simply dismiss it to show the undelying text keyboard
                else{
                    popup.dismiss();
                }

                changeEmojiKeyboardIcon();

            }
        });

        input.setAttachmentsListener(() -> {
            /*System.out.println("Clicked");
            Sms sms = new Sms();
            sms.setId(mAddressToSendTo);
            sms.setAddress(mAddressToSendTo);
            sms.setIncoming(true);
            sms.setImage("https://habrastorage.org/getpro/habr/post_images/e4b/067/b17/e4b067b17a3e414083f7420351db272b.jpg");
            //sms.setMsg("Hello");
            sms.setTime(Long.toString(new Date().getTime()));
            adapter.addToStart(sms,true);*/

            ImagePicker.create(this)
                    //.returnMode(ReturnMode.ALL) // set whether pick and / or camera action should return immediate result or not.
                    .folderMode(true) // folder mode (false by default)
                    .toolbarFolderTitle("Folder") // folder selection title
                    .toolbarImageTitle("Tap to select") // image selection title
                    .toolbarArrowColor(Color.BLACK) // Toolbar 'up' arrow color
                    .single()
                    .limit(10) // max images can be selected (99 by default)
                    .showCamera(true) // show camera or not (true by default)
                    .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                    .enableLog(false) // disabling log
                    .start(); // start image picker activity with request code
        });

/*
        if(isNewNumber){
            progressBar.setVisibility(View.GONE);
            mActionBar.clearSuggestions();
            mActionBar.setEnabled(true);
            mActionBar.setPlaceHolder("Enter phone number");
            mActionBar.setHint("Enter phone number");
            mActionBar.enableSearch();

            mActionBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
                @Override
                public void onSearchStateChanged(boolean enabled) {
                    if(enabled){
                        if(newPhoneNumber !=null){
                            mActionBar.setText(newPhoneNumber);
                        }
                    }

                }

                @Override
                public void onSearchConfirmed(CharSequence text) {
                    newPhoneNumber = text.toString();
                    mActionBar.setPlaceHolder(newPhoneNumber);
                }

                @Override
                public void onButtonClicked(int buttonCode) {
                    switch (buttonCode){
                    case MaterialSearchBar.BUTTON_NAVIGATION:
                    mDrawerActivity.getDrawer().openDrawer(GravityCompat.START);
                    break;
                    case MaterialSearchBar.BUTTON_BACK:
                        newPhoneNumber = mActionBar.getText();
                        if(newPhoneNumber.isEmpty()){
                            mActionBar.setPlaceHolder("Enter phone number");
                        }else {
                            mActionBar.setPlaceHolder(newPhoneNumber);
                        }

                    mActionBar.disableSearch();
                    break;
                }
                }
            });
        }*//*else {
        mDrawerActivity.setVisibilityOfBottomPanel(false);
        mDrawerActivity.setVisibilityOfFAB(false, false);
        smsList = getArguments().getParcelableArrayList("sms");
        mAddressToSendTo = smsList.get(0).getAddress();
        if (smsList.size() == 1 && smsList.get(0).getId() == null)//if there no messages we create one empty sms before calling fragment
            smsList.remove(0);

            if(mActionBar != null){
                mActionBar.setPlaceHolder(ContactsUtils.findNameByNumber(getActivity(), mAddressToSendTo));
            }
            adapter.addToEnd(smsList, false);
        }*/
        initToolBar();
        initEmojiKeyboard();
        //initCardView();

        //TODO:сделать приложение по дефолту для работы следуйщего метода
        //markAllAsRead();
    }

    private void changeEmojiKeyboardIcon(){
        if(isEmojiKeyboardActive) {
            emojiButton.setImageResource(R.drawable.ic_action_emoji);
            isEmojiKeyboardActive = false;
        }
        else {
            emojiButton.setImageResource(R.drawable.ic_action_keyboard);
            isEmojiKeyboardActive = true;
        }
    }

    private void initEmojiKeyboard() {
        View root = getActivity().findViewById(R.id.sms_list_fragment);
        popup = new EmojiconsPopup(root, getContext());
        popup.setSizeForSoftKeyboard();

        //If the text keyboard closes, also dismiss the emoji popup
        popup.setOnSoftKeyboardOpenCloseListener(new EmojiconsPopup.OnSoftKeyboardOpenCloseListener() {

            @Override
            public void onKeyboardOpen(int keyBoardHeight) {

            }

            @Override
            public void onKeyboardClose() {
                if(popup.isShowing()) {
                    popup.dismiss();
                    isEmojiKeyboardActive = false;
                    emojiButton.setImageResource(R.drawable.ic_action_emoji);//reset button's image
                }
            }
        });

        popup.setOnEmojiconClickedListener(new EmojiconGridView.OnEmojiconClickedListener() {

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                if(editText == null)
                    return;
                int start = editText.getSelectionStart();
                int end = editText.getSelectionEnd();
                if (start < 0) {
                    editText.append(emojicon.getEmoji());
                } else {
                    editText.getText().replace(Math.min(start, end),
                            Math.max(start, end), emojicon.getEmoji(), 0,
                            emojicon.getEmoji().length());
                }
            }
        });
        popup.setOnEmojiconBackspaceClickedListener(new EmojiconsPopup.OnEmojiconBackspaceClickedListener() {

            @Override
            public void onEmojiconBackspaceClicked(View v) {
                KeyEvent event = new KeyEvent(
                        0, 0, 0, KeyEvent.KEYCODE_DEL, 0, 0, 0, 0, KeyEvent.KEYCODE_ENDCALL);
                editText.dispatchKeyEvent(event);
            }
        });
    }

    private void initOnNewSmsListener() {
        if(!isBlocked) {
            onNewSmsListener = new Timer();
            onNewSmsListener.scheduleAtFixedRate(new TimerTask() {

                @Override
                public void run() {
                    if (mDrawerActivity.isNewMessage) {
                        mDrawerActivity.isNewMessage = false;
                        System.out.println("YOU GOT NEW MESSAGE");
                        List<MessageItem> messages = new ArrayList<>();
                        HashMap<Long, ArrayList<MessageItem>> list = SmsUtils.getUnreadUnseenConversations(getContext());
                        for (ArrayList<MessageItem> convers : list.values()) {
                            if (convers.get(0).mAddress.equals(mAddressToSendTo)) {
                                messages = convers;
                                break;
                            }
                        }
                        if (!messages.isEmpty()) {
                            ArrayList<Sms> newSms = new ArrayList<>();
                            for (MessageItem message : messages) {
                                Sms sms = new Sms();
                                sms.setId(String.valueOf(message.mMsgId));
                                sms.setAddress(message.mAddress);
                                sms.setIncoming(true);
                                sms.setTime(Long.toString(message.mDate));
                                sms.setReadState("1");
                                sms.setMsg(message.mBody);
                                newSms.add(sms);
                                System.out.println(sms);
                                //smsList.add(sms);
                                smsCount++;
                                mDrawerActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.addToStart(sms, true);
                                    }
                                });
                                Thread readSms = new Thread(() -> SmsUtils.markAllMessagesAsRead(getContext(), newSms));
                                readSms.start();


                            }
                        }
                        //Collections.sort(smsList, new SmsDateComparator());
                    }
                }

            }, 0, 1000);
        }
    }

    private void initContactInfo() {
        contactsPhoto = getActivity().findViewById(R.id.my_custom_icon);
        System.out.println("Address: " + mAddressToSendTo);
        String imageUri = mDrawerActivity.getContactImages().get(mAddressToSendTo);
        if(imageUri != null) {
            System.out.println("Image is set");
            contactsPhoto.setImageURI(Uri.parse(imageUri));
            hasPhoto = true;
        }
        else if(isBlocked) {
            contactsPhoto.setImageResource(R.drawable.profile_pictures_blocked);
        }
        else
            contactsPhoto.setImageResource(R.drawable.profile_pictures);
        personalInfo = getActivity().findViewById(R.id.app_bar_personal_info);
        personalInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(ContactsUtils.findContactByNumber(getContext(), mAddressToSendTo)));
                intent.setData(uri);
                startActivityForResult(intent, RC_CONTACT_DETAILS);
            }
        });
    }

    private void selectSmsItem(View view, Sms message) {
        if(!message.isSelected()) {
            view.setBackgroundColor(Color.LTGRAY);
            message.setSelected(true);
            selectedSms.add(message);
            mDrawerActivity.getSupportActionBar().setTitle(selectedSms.size() + "/" + smsCount);
        }
        else {
            view.setBackgroundColor(Color.WHITE);
            message.setSelected(false);
            selectedSms.remove(message);
            mDrawerActivity.getSupportActionBar().setTitle(selectedSms.size() + "/" + smsCount);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        if(!mDrawerActivity.isSelectMode) {
            inflater.inflate(R.menu.conversation_menu, menu);
        }
        else
            inflater.inflate(R.menu.conversation_select_sms_menu, menu);

    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println("INSIDE OPTIONS");
        int id = item.getItemId();

        //for main menu
        if (id == R.id.action_more) {
            System.out.println("MENU MORE CLICKED");
            cardView = getActivity().findViewById(R.id.menu_more);
            cardView.setVisibility(View.VISIBLE);
            cardView.setFocusable(true);
            cardViewNewContact = getActivity().findViewById(R.id.fab_sheet_new_contact);
            if(mAddressToSendTo.equals(ContactsUtils.findNameByNumber(getActivity(), mAddressToSendTo))) {
                cardViewNewContact.setVisibility(View.VISIBLE);
            }
            else
                cardViewNewContact.setVisibility(View.GONE);
        }

        //for select menu
        if(id == R.id.sms_action_delete) {
            if(adapter.getSelectedMessages().size() > 0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                alertDialog.setTitle("Confirmation");
                alertDialog.setMessage("Do you really want to delete?");
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        for (Sms sms :adapter.getSelectedMessages()) {
                            sms.delete(getContext());
                            smsCount--;
                            //smsList.remove(sms);
                        }
                        adapter.deleteSelectedMessages();
                        mDrawerActivity.isSelectMode = false;
                        mDrawerActivity.updateSmsList();
                        initToolBar();


                        System.out.println("DELETE SMS");
                    }
                });
                alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.unselectAllItems();
                        mDrawerActivity.isSelectMode = false;
                        initToolBar();
                    }
                });
                alertDialog.setCancelable(true);
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        adapter.unselectAllItems();
                        mDrawerActivity.isSelectMode = false;
                        initToolBar();
                    }
                });
                alertDialog.show();
            }
            /*for(Sms sms: selectedSms) {
                sms.delete(getContext());
            }
            selectedSms.clear();
            List<Sms> allSms = SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL);
            List<Sms> smsList = new ArrayList<>();
            for(Sms sms: allSms) {
                if (sms.getAddress().equals(mAddressToSendTo))
                    smsList.add(sms);
            }*/
        }
        else if(id == R.id.action_conversation_call) {
            callPhoneNumber();
        }
        return super.onOptionsItemSelected(item);
    }

    public void callPhoneNumber() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + mAddressToSendTo));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivityForResult(intent, RC_CALL);
        }
    }




    private void initCardView() {
        cardViewNewContact = getActivity().findViewById(R.id.fab_sheet_new_contact);
        cardViewNewContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_INSERT);
                intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
                intent.putExtra(ContactsContract.Intents.Insert.PHONE, mAddressToSendTo);
                startActivityForResult(intent, RC_ADD_CONTACT);
            }
        });
        cardViewBlock = getActivity().findViewById(R.id.fab_sheet_block);
        if(isBlocked)
            cardViewBlock.setText(R.string.unlock);
        else
            cardViewBlock.setText(R.string.block);
        cardViewBlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isBlocked) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                    alertDialog.setTitle("Confirmation");
                    alertDialog.setMessage("Do you really want to block contact?");
                    alertDialog.setPositiveButton("Block", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            if(isUnknownNumber()) {
                                PrefsUtil.blockUnknownNumber(mDrawerActivity, ContactNumberUtil.getCleanNumber(mAddressToSendTo));
                                FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(mAddressToSendTo));
                            }
                            else {
                                FirebaseHelper.addOneToSpam(ContactNumberUtil.getCleanNumber(mAddressToSendTo));
                                PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(mAddressToSendTo), 1);
                            }
                            if (!hasPhoto)
                                contactsPhoto.setImageResource(R.drawable.profile_pictures_blocked);

                            if(onNewSmsListener != null) {
                                onNewSmsListener.cancel();
                                onNewSmsListener = null;
                            }
                            isBlocked = true;
                            cardView.setVisibility(View.INVISIBLE);
                            cardViewBlock.setText(R.string.unlock);
                            blockedTextView.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(), "Blocked", Toast.LENGTH_SHORT).show();
                        }
                    });
                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cardView.setVisibility(View.INVISIBLE);
                        }
                    });
                    alertDialog.setCancelable(true);
                    alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            cardView.setVisibility(View.INVISIBLE);
                        }
                    });
                    alertDialog.show();
                }
                else {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                    alertDialog.setTitle("Confirmation");
                    alertDialog.setMessage("Do you really want to unlock contact?");
                    alertDialog.setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int which) {
                            if(isUnknownNumber()) {
                                PrefsUtil.unlockUnknownNumber(mDrawerActivity, ContactNumberUtil.getCleanNumber(mAddressToSendTo));
                                FirebaseHelper.minusOneFromSpam(ContactNumberUtil.getCleanNumber(mAddressToSendTo));
                            }
                            else {
                                FirebaseHelper.minusOneFromSpam(ContactNumberUtil.getCleanNumber(mAddressToSendTo));
                                PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(mAddressToSendTo), 0);
                            }
                            isBlocked = false;
                            if (!hasPhoto)
                                contactsPhoto.setImageResource(R.drawable.profile_pictures);
                            cardViewBlock.setText(R.string.block);
                            blockedTextView.setVisibility(View.GONE);
                            initOnNewSmsListener();
                            cardView.setVisibility(View.INVISIBLE);
                            Toast.makeText(getContext(), "Unlocked", Toast.LENGTH_SHORT).show();

                        }
                    });
                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            cardView.setVisibility(View.INVISIBLE);
                        }
                    });
                    alertDialog.setCancelable(true);
                    alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            cardView.setVisibility(View.INVISIBLE);
                        }
                    });
                    alertDialog.show();
                }


            }
        });

        cardViewDeleteConv = getActivity().findViewById(R.id.fab_sheet_delete_conversation);
        cardViewDeleteConv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext());
                alertDialog.setTitle("Confirmation");
                alertDialog.setMessage("Do you really want to delete conversation?");
                alertDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        Context context = getContext();
                        Disposable subscribe = Observable.fromCallable(() -> SmsUtils.getAllSms(getActivity(), SmsUtils.Type.ALL))
                                .subscribeOn(Schedulers.io())
                                .flatMapIterable(list -> list)
                                .filter(c -> c.getAddress().equals(mAddressToSendTo))
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnComplete(() -> {
                                    mDrawerActivity.updateSmsList();
                                    mDrawerActivity.onBackPressed();
                                })
                                .subscribe(sms -> {
                                    System.out.println("delete");
                                    System.out.println(sms);
                                    sms.delete(context);
                                });
                        cardView.setVisibility(View.INVISIBLE);
                    }
                });
                alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        cardView.setVisibility(View.INVISIBLE);
                    }
                });
                alertDialog.setCancelable(true);
                alertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        cardView.setVisibility(View.INVISIBLE);
                    }
                });
                alertDialog.show();
            }
        });
    }

    private boolean isUnknownNumber() {
        TextView title = getActivity().findViewById(R.id.textView_title);
        if(mAddressToSendTo.equals(title.getText()))
            return true;
        else
            return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            System.out.println("Images picked: ");
            // Get a list of picked images
            //List<Image> images = ImagePicker.getImages(data);
            // or get a single image only
            Image image = ImagePicker.getFirstImageOrNull(data);
            View layout = getActivity().findViewById(R.id.images);
            layout.setVisibility(View.VISIBLE);
            ImageView selectedImage = getActivity().findViewById(R.id.selected_image);
            selectedImage.setImageURI(Uri.parse(image.getPath()));
            ImageButton closeButton = getActivity().findViewById(R.id.button_close);
            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    layout.setVisibility(View.GONE);
                    selectedImage.setImageBitmap(null);
                }
            });
        }
        if(requestCode == RC_ADD_CONTACT) {
            mDrawerActivity.updateCallsList();
            mDrawerActivity.updateSmsList();
            mDrawerActivity.onBackPressed();
        }
        else if(requestCode == RC_CALL) {
            Runnable runnable = () -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mDrawerActivity.updateCallsList();
            };
            runnable.run();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void hideToolBar() {
        appBarLayout.setVisibility(View.INVISIBLE);
        //mActionBar.setVisibility(View.VISIBLE);
        mDrawerActivity.getSupportActionBar().hide();
        mDrawerActivity.initToolbar();
    }

    private void initToolBar() {
        appBarLayout = getActivity().findViewById(R.id.app_bar_alternative);
        appBarLayout.setVisibility(View.VISIBLE);
        alternativeToolBar = getActivity().findViewById(R.id.toolbar_alternative);
        alternativeToolBar.getMenu().clear();
        mDrawerActivity.setSupportActionBar(alternativeToolBar);
        mDrawerActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        mDrawerActivity.getSupportActionBar().show();
        TextView title = getActivity().findViewById(R.id.textView_title);
        title.setText(ContactsUtils.findNameByNumber(getActivity(), mAddressToSendTo));
        blockedTextView = getActivity().findViewById(R.id.text_blocked);
        if(isBlocked)
            blockedTextView.setVisibility(View.VISIBLE);
        else
            blockedTextView.setVisibility(View.GONE);
    }

    private void sendSms(CharSequence textToSend) {
        Settings sendSettings = new Settings();
        Transaction sendTransaction = new Transaction(getActivity(), sendSettings);
        Message message = new Message(textToSend.toString(), isNewNumber ? newPhoneNumber : mAddressToSendTo);
        //message.setImage(getScaledBitmap("https://habrastorage.org/getpro/habr/post_images/e4b/067/b17/e4b067b17a3e414083f7420351db272b.jpg")); to send message
        sendTransaction.sendNewMessage(message, Transaction.NO_THREAD_ID);
    }

    private Bitmap getScaledBitmap(String picturePath) {
        BitmapFactory.Options sizeOptions = new BitmapFactory.Options();
        sizeOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath, sizeOptions);


        sizeOptions.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(picturePath, sizeOptions);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDrawerActivity.setVisibilityOfBottomPanel(true);
        mDrawerActivity.onSmsListReady = null;
        if(mActionBar != null) {
            mActionBar.setPlaceHolder(getString(R.string.app_name));
            mActionBar.setHint("Search");
            mActionBar.setText("");
        }
        appBarLayout.setVisibility(View.INVISIBLE);
        mActionBar.setVisibility(View.VISIBLE);
        mDrawerActivity.getSupportActionBar().hide();
        mDrawerActivity.initToolbar();
        mDrawerActivity.updateSmsList();
        if(!isBlocked)
            onNewSmsListener.cancel();
    }

    @Override
    public void onBackPressed() {//Для перехода во вкладку СМС
        System.out.println("BACK PRESSED!");
//        FragmentsUtil.attachFragment(getActivity(), new SmsDialogsFragment(),
//                R.id.fragment_container, false);

    }

    @Override
    public void onReady(List<Sms> smsList) {
        mAddressToSendTo = smsList.get(0).getAddress();
        System.out.println(mAddressToSendTo);

        if(ContactsUtils.findNameByNumber(getActivity(), mAddressToSendTo).equals(mAddressToSendTo)) {
            Set<String> blockedNumbers = PrefsUtil.getBlockedUnknownNumbers(getContext());
            System.out.println("Blocked numbers");
            System.out.println(blockedNumbers);
            String tmpCleanNumber = ContactNumberUtil.getCleanNumber(mAddressToSendTo);
            if(blockedNumbers != null) {
                for (String blockedNumber : blockedNumbers) {
                    if (blockedNumber.equals(tmpCleanNumber)) {
                        isBlocked = true;
                        break;
                    }
                }
            }
        }
        else {
            if (PrefsUtil.getSpamAction(getContext(), ContactNumberUtil.getCleanNumber(mAddressToSendTo)) == 1) {
                isBlocked = true;
            } else
                isBlocked = false;
        }

        if (smsList.size() == 1 && smsList.get(0).getId() == null)
            smsList.remove(0);

        if (mActionBar != null) {
            mActionBar.setPlaceHolder(ContactsUtils.findNameByNumber(getActivity(), mAddressToSendTo));
        }

        progressBar.setVisibility(View.GONE);
        adapter.addToEnd(smsList, false);

        smsCount = smsList.size();
        initToolBar();
        initContactInfo();//init layout on app bar to view contact's info by clicking
        initCardView();
        initOnNewSmsListener();

        //MARK AS READ
        Observable.fromIterable(smsList)
                .subscribeOn(Schedulers.io())
                .map(Sms::getId)
                .observeOn(Schedulers.io())
                .subscribe(id -> SmsUtils.markMessageRead(getActivity(), id));
    }
}
