package com.innovations.djnig.truecallerclone.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.innovations.djnig.truecallerclone.R;

/**
 * @author Artur Romasiuk
 */

public class ContactNumberUtil {

    public static void searchNameForUnknownNumber(String number, final TextView textView) {
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        DatabaseReference mRootRef = mDatabase.getReference();
        mRootRef.child("contactBase").child(number).child("names").orderByValue().limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Log.i("dbString", String.valueOf(dataSnapshot.getValue()));
                        if (dataSnapshot.getValue() != null) {
                            for (DataSnapshot nameSnapshot : dataSnapshot.getChildren()) {
                                Log.i("dbString", String.valueOf(nameSnapshot.getValue()));
                                String nameKey = nameSnapshot.getKey();
                                Log.i("dbString", nameKey);
                                textView.setText(nameKey);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.i("dbString", String.valueOf(databaseError));
                    }
                });
    }

    public static String normalizePhoneNumber(Context context, String phoneNumber){
        SharedPreferences sharedPref = context.getSharedPreferences(context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        String countryIso = sharedPref.getString(context.getString(R.string.country_iso),null);
        if (countryIso != null) {
            PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
            try {
                Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phoneNumber, countryIso);
                boolean isValidNumberForRegion = phoneUtil.isValidNumberForRegion(numberProto, countryIso);
                Log.i("countryIso", String.valueOf(isValidNumberForRegion));
                if (isValidNumberForRegion) {
                    boolean isValid = phoneUtil.isValidNumber(numberProto);
                    Log.i("countryIso", String.valueOf(isValid));
                    if (isValid) {
                        Log.i("countryIso", phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164));
                        phoneNumber = phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164);
                    }
                } else {
                    try {
                        // phone must begin with '+'
                        Phonenumber.PhoneNumber numberProtoWithAnotherIso = phoneUtil.parse(phoneNumber, "");
                        int countryCodeNew = numberProtoWithAnotherIso.getCountryCode();
                        Log.i("countryIso", phoneUtil.format(numberProtoWithAnotherIso, PhoneNumberUtil.PhoneNumberFormat.E164));
                        phoneNumber = phoneUtil.format(numberProtoWithAnotherIso, PhoneNumberUtil.PhoneNumberFormat.E164);
                    } catch (NumberParseException e) {
                        Log.i("countryIso", e.toString());
                    }
                }
            } catch (NumberParseException e) {
                Log.i("countryIso", e.toString());
            }
        }
        return phoneNumber;
    }

    public static String getCleanNumber(String number) {
        number = number.replace(".", "");
        number = number.replace("#", "");
        number = number.replace("$", "");
        number = number.replace("[", "");
        number = number.replace("]", "");
        number = number.replace("-", "");
        number = number.replace("(", "");
        number = number.replace(")", "");
        //number = number.replace("+", "");
        number = number.replace(" ", "");
        return number;
    }
}
