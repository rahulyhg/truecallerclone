package com.innovations.djnig.truecallerclone.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;
import com.innovations.djnig.truecallerclone.Utils.FirebaseHelper;
import com.innovations.djnig.truecallerclone.Utils.PrefsUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by karasinboots on 11.01.2018.
 */

public class SpamDialog extends DialogFragment {
    @BindView(R.id.spamNumber)
    TextView spamNumber;
    @BindView(R.id.numberOfThinkers)
    TextView thinkersNumber;
    @BindView(R.id.blockButton)
    Button blockButton;
    @BindView(R.id.allowButton)
    Button allowButton;

    private String number;
    private long numberOfUsers;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        number = getArguments().getString("number");
        numberOfUsers = getArguments().getLong("numberOfUsers", 0);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.spam_dialog, null);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spamNumber.setText(number);
        String message = numberOfUsers + " " + thinkersNumber.getText();
        thinkersNumber.setText(message);
        blockButton.setOnClickListener(v -> {
            //PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(number), 1);
            PrefsUtil.blockUnknownNumber(getContext(), ContactNumberUtil.getCleanNumber(number));
            addOneToSpam(number);
        });
        allowButton.setOnClickListener(v -> {
            PrefsUtil.setSpamAction(getContext(), ContactNumberUtil.getCleanNumber(number), 0);
            dismiss();
            getActivity().finish();
        });
    }

    @Override
    public void onResume() {
        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = getResources().getDimensionPixelSize(R.dimen.popup_width);
        params.height = getResources().getDimensionPixelSize(R.dimen.popup_height);
        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        super.onResume();
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
                                dismiss();
                                getActivity().finish();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i("dbStringSms", String.valueOf(databaseError));
                    }
                });*/
    }
}
