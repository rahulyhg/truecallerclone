package com.innovations.djnig.truecallerclone.Utils;

import com.innovations.djnig.truecallerclone.models.Sms;
import com.innovations.djnig.truecallerclone.models.SmsDialog;

import java.util.Comparator;
import java.util.List;

/**
 * Created by djnig on 12/21/2017.
 */

public class DateComparator implements Comparator<SmsDialog> {
    @Override
    public int compare(SmsDialog o1, SmsDialog o2) {
        return o1.getLastMessage().getCreatedAt().compareTo(o2.getLastMessage().getCreatedAt());
    }
}
