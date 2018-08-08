package com.innovations.djnig.truecallerclone.Utils;

import com.innovations.djnig.truecallerclone.models.Sms;

import java.util.Comparator;

/**
 * Created by djnig on 1/16/2018.
 */

public class SmsDateComparator implements Comparator<Sms> {
    @Override
    public int compare(Sms o1, Sms o2) {
        return o2.getCreatedAt().compareTo(o1.getCreatedAt());
    }
}
