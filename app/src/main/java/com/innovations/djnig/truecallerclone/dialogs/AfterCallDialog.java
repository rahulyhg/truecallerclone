package com.innovations.djnig.truecallerclone.dialogs;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.innovations.djnig.truecallerclone.R;
import com.innovations.djnig.truecallerclone.SaveContactActivity;
import com.innovations.djnig.truecallerclone.Utils.ContactNumberUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by djnig on 12/5/2017.
 */

public class AfterCallDialog extends DialogFragment {


    @BindView(R.id.save_contact)
    Button saveContact;

    @BindView(R.id.unknown_number)
    TextView unknownNumber;

    @BindView(R.id.markSpam)
    LinearLayout markSpam;

    private String number;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        number = getArguments().getString("number");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.aftercall_dialog, null);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        unknownNumber.setText(number);
        ContactNumberUtil.searchNameForUnknownNumber(number, unknownNumber);
        saveContact.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SaveContactActivity.class);
            intent.putExtra("number", number);
            startActivity(intent);
            dismiss();
        });
        markSpam.setOnClickListener(v -> {
            //TODO: make spam in shared
            addOneToSpam(number);
        });

    }

    private void addOneToSpam(final String number) {
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        //final DatabaseReference spamReference = mRootRef.child("contactBase").child(number).child("spam");
        final DatabaseReference spamReference = mRootRef.child("contactBase").child(number);
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

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

}