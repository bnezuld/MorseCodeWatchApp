package com.example.morsecodewatchapp;

import android.content.Context ;
import android.service.notification.NotificationListenerService ;
import android.service.notification.StatusBarNotification ;
import android.util.Log ;
import android.app.Notification;

public class NotificationService extends NotificationListenerService {
    private String TAG = this .getClass().getSimpleName() ;
    Context context ;
    static MyListener myListener ;
    @Override
    public void onCreate () {
        super .onCreate() ;
        context = getApplicationContext() ;
    }
    @Override
    public void onNotificationPosted (StatusBarNotification sbn) {

        CharSequence seqText = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence seqTitle = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);

        String Text = seqText != null ? seqText.toString() : "!nulltext!";
        String Title = seqTitle != null ? seqTitle.toString() : "!nulltitle!";
        String Message = Title + "   " + Text;
        Log. i ( TAG , "********** onNotificationPosted" ) ;
        Log. i ( TAG , "ID :" + sbn.getId()  + " \t " + sbn.getNotification(). tickerText + " \t " + sbn.getPackageName()) ;
        if(myListener != null && !sbn.getPackageName().equals("com.android.systemui")) {
            myListener.setValue(Message);
        }
    }
    @Override
    public void onNotificationRemoved (StatusBarNotification sbn) {
        CharSequence seqText = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence seqTitle = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);

        String Text = seqText != null ? seqText.toString() : "!nulltext!";
        String Title = seqTitle != null ? seqTitle.toString() : "!nulltitle!";
        String Message = Title;
        Log. i ( TAG , "********** onNotificationRemoved" ) ;
        Log. i ( TAG , "ID :" + sbn.getId() + " \t " + sbn.getNotification(). tickerText + " \t " + sbn.getPackageName()) ;
        //if(myListener != null)
        //    myListener.setValue("R " + sbn.getId() +  " " + Message) ;
    }
    public void setListener (MyListener myListener) {
        NotificationService. myListener = myListener ;
    }
}
