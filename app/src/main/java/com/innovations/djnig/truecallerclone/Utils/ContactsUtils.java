package com.innovations.djnig.truecallerclone.Utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.ContactsContract;

import com.github.tamir7.contacts.Contact;
import com.github.tamir7.contacts.Contacts;
import com.github.tamir7.contacts.PhoneNumber;
import com.innovations.djnig.truecallerclone.R;

import java.io.InputStream;
import java.util.List;

/**
 * Created by djnig on 12/15/2017.
 */

public class ContactsUtils {

     public static String findNameByNumber(Context context, String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{"display_name"}, (String)null, new String[]{"display_name"}, (String)null);
        if(cursor == null) {
            return null;
        } else {
            String contactName = null;
            if(cursor.moveToFirst()) {
                contactName = cursor.getString(cursor.getColumnIndex("display_name"));
            }

            if(!cursor.isClosed()) {
                cursor.close();
            }

            return contactName == null ? phoneNumber : contactName;
        }
    }

    public static long findContactByNumber(Context context, String numb) {
        /*String contactid26 = null;

        ContentResolver contentResolver = context.getContentResolver();

        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phonenumintolist));

        Cursor cursor =
                contentResolver.query(
                        uri,
                        new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID},
                        null,
                        null,
                        null);

        if(cursor!=null) {
            while(cursor.moveToNext()){
                String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME));
                contactid26 = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));

            }
            cursor.close();
        }
        if (contactid26 == null)
        {
            Toast.makeText(DetailedCallHistory.this, "No contact found associated with this number", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Intent intent_contacts = new Intent(Intent.ACTION_VIEW, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, String.valueOf(contactid26)));
            //Intent intent_contacts = new Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/" + contactid26));
            context.startActivity(intent_contacts);
        }*/
        Contacts.initialize(context);
        List<Contact> contacts = Contacts.getQuery().find();
        for (Contact contact: contacts) {
            System.out.println();
            for (PhoneNumber number: contact.getPhoneNumbers()) {
                String normNumber = ContactNumberUtil.normalizePhoneNumber(context, number.getNumber());
                if (normNumber.equals(numb)) {
                    System.out.println("FOUND WITH ID " + contact.getId());
                    return contact.getId();
                }
            }
        }
        return (long) (Math.random()*1000);
    }

    //Return contact's photo if it exists or standard image
    public static Bitmap retrieveContactPhoto(Context context, String number) {
        ContentResolver contentResolver = context.getContentResolver();
        String contactId = null;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID};

        Cursor cursor =
                contentResolver.query(
                        uri,
                        projection,
                        null,
                        null,
                        null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID));
            }
            cursor.close();
        }

        Bitmap photo = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.profile_pictures);

        if(contactId != null) {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(context.getContentResolver(),
                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, Long.valueOf(contactId)));
            if (inputStream != null) {
                photo = BitmapFactory.decodeStream(inputStream);
            }
        }

        return photo;
    }
}
