package com.innovations.djnig.truecallerclone.Utils;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.innovations.djnig.truecallerclone.interfaces.OnDataReceiveListener;

import java.util.HashMap;

/**
 * Created by dell on 13.02.2018.
 */

public class FirebaseHelper {
    static boolean isSpamer = false;
    static boolean flagMethodCalled = false;

    public static boolean isSpamer(String number, OnDataReceiveListener onDataReceiveListener) {
        flagMethodCalled = true;
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        mRootRef.child("contactBase").child(ContactNumberUtil.getCleanNumber(number)).child("spam")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if(flagMethodCalled) {
                            Long spamCounter = (Long) dataSnapshot.getValue();
                            if (spamCounter != null && spamCounter > 100)
                                onDataReceiveListener.onCompleteReading(true);
                            else
                                onDataReceiveListener.onCompleteReading(false);
                            flagMethodCalled = false;
                        }
                    }


                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        flagMethodCalled = false;
                    }
                });



        return isSpamer;
    }

    public static void addOneToSpam(String cleanNumber) {
        flagMethodCalled = true;
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        final DatabaseReference spamReference = mRootRef.child("contactBase").child(cleanNumber).child("spam");
        spamReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(flagMethodCalled) {
                    if (dataSnapshot.getValue() != null) {
                        long numberOfUsers = (long) dataSnapshot.getValue();
                        spamReference.setValue(numberOfUsers + 1);

                    } else
                        spamReference.setValue(1);
                    flagMethodCalled = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("dbStringSms", String.valueOf(databaseError));
                flagMethodCalled = false;
            }
        });
    }

    public static void minusOneFromSpam(String cleanNumber) {
        flagMethodCalled = true;
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        final DatabaseReference spamReference = mRootRef.child("contactBase").child(cleanNumber).child("spam");
        spamReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(flagMethodCalled) {
                    if (dataSnapshot.getValue() != null) {
                        long numberOfUsers = (long) dataSnapshot.getValue();
                        if (numberOfUsers > 0)
                            spamReference.setValue(numberOfUsers - 1);

                    } else
                        spamReference.setValue(0);
                    flagMethodCalled = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("dbStringSms", String.valueOf(databaseError));
                flagMethodCalled = false;
            }
        });
    }

    public static void getNames(String number, OnDataReceiveListener onDataReceiveListener) {
        flagMethodCalled = true;
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        final DatabaseReference spamReference = mRootRef.child("contactBase").child(number).child("names");
        spamReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(flagMethodCalled) {
                    if (dataSnapshot.getValue() != null) {
                        HashMap<String, Long> names = (HashMap<String, Long>) dataSnapshot.getValue();
                        if(names != null) {

                        }


                    } else
                        spamReference.setValue(0);
                    flagMethodCalled = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("dbStringSms", String.valueOf(databaseError));
                flagMethodCalled = false;
            }
        });
    }
}
