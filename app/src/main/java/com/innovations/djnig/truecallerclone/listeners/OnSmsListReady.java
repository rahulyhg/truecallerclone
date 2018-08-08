package com.innovations.djnig.truecallerclone.listeners;

import com.innovations.djnig.truecallerclone.models.Sms;

import java.util.List;

/**
 * Created by djnig on 1/29/2018.
 */

public interface OnSmsListReady {
    void onReady(List<Sms> smsList);
}
