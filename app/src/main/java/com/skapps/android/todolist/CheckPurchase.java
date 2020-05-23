package com.skapps.android.todolist;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Syed Umair on 23/05/2020.
 */
public class CheckPurchase {
    private Context context;

    private final String ISPURCHASED = "IsPurchased";

    public CheckPurchase(Context context){
        this.context = context;
    }

    public void setUserPurchased(boolean isPurchase){
        if(context != null){
            SharedPreferences pref = context.getSharedPreferences("inAppPurchase", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean(ISPURCHASED, isPurchase);
            editor.apply();
        }
    }

    public boolean isUserPurchased(){
        if(context != null){
            SharedPreferences pref = context.getSharedPreferences("inAppPurchase", Context.MODE_PRIVATE);
            return pref.getBoolean(ISPURCHASED, false );
        }else {
            return false;
        }
    }
}
