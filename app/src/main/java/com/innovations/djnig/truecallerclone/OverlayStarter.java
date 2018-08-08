package com.innovations.djnig.truecallerclone;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.innovations.djnig.truecallerclone.dialogs.AfterCallDialog;
import com.innovations.djnig.truecallerclone.dialogs.SpamDialog;

public class OverlayStarter extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getLongExtra("numberOfUsers", 0) > 0)
            showSpamDialog(getIntent().getStringExtra("number"), getIntent().getLongExtra("numberOfUsers", 0));
        else showAfterCallDialog(getIntent().getStringExtra("number"));
    }

    public void showSpamDialog(String number, long numberOfUsers) {
        Log.e("number of users", String.valueOf(numberOfUsers));
        SpamDialog spamDialog = new SpamDialog();
        Bundle bundle = new Bundle();
        bundle.putString("number", number);
        bundle.putLong("numberOfUsers", numberOfUsers);
        spamDialog.setArguments(bundle);
        spamDialog.show(getSupportFragmentManager(), "spam");
    }

    public void showAfterCallDialog(String number) {
        AfterCallDialog dialog = new AfterCallDialog();
        Bundle bundle = new Bundle();
        bundle.putString("number", number);
        dialog.setArguments(bundle);
        dialog.show(getSupportFragmentManager(), "aftercall");
    }

}
