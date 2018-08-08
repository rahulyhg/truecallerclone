package com.innovations.djnig.truecallerclone;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SaveContactActivity extends AppCompatActivity {

    @BindView(R.id.number_to_save)
    TextView numberToSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_save_contact);
        ButterKnife.bind(this);
        numberToSave.setText("SAVE\n"+getIntent().getStringExtra("number"));
    }
}
