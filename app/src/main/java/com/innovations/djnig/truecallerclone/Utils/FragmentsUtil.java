package com.innovations.djnig.truecallerclone.Utils;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewGroup;


/**
 * Created by djnig on 12/1/2017.
 */

public class FragmentsUtil {


        public static <T extends Fragment> void attachFragment(Context context, T fragment, @IdRes int container, boolean addToBS) {

            FragmentManager manager = ((FragmentActivity) context).getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            Fragment fr = manager.findFragmentByTag(fragment.getClass().getName());

            if(fr == null){
                transaction.replace(container, fragment, fragment.getClass().getName());
            }else {
                transaction.replace(container, fr, fragment.getClass().getName());
            }

            if(addToBS){
                transaction.addToBackStack(null);
            }
            transaction.commit();
        }



}