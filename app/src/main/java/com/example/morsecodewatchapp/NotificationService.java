package com.example.morsecodewatchapp;

import android.content.Context ;
import android.service.notification.NotificationListenerService ;
import android.service.notification.StatusBarNotification ;
import android.util.Log ;
import android.app.Notification;

import java.util.List;
import java.util.Map;

public class NotificationService extends NotificationListenerService {
    private String TAG = this .getClass().getSimpleName() ;
    Map<String, List<String>> notificationCategories;
    Map<String,StatusBarNotification> NotificationMap;

    Context context ;
    static NotificationListener notificationListener;
    @Override
    public void onCreate () {
        super .onCreate() ;
        context = getApplicationContext() ;

        /*notificationCategories.put("Simple alert", new List<String>{""});
        notificationCategories.put("Email",);
        notificationCategories.put("News",);
        notificationCategories.put("Incoming call",);
        notificationCategories.put("Missed call",);
        notificationCategories.put("SMS/MMS",);
        notificationCategories.put("Voice mail",);
        notificationCategories.put("Schedule",);
        notificationCategories.put("High prioritized alert",);
        notificationCategories.put("Instant message",);*/
    }
    @Override
    public void onNotificationPosted (StatusBarNotification sbn) {
        //if(!NotificationMap.containsKey(sbn.getKey())){
        //    NotificationMap.put(sbn.getKey(),sbn);
            CharSequence seqText = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
            CharSequence seqTitle = sbn.getNotification().extras.getCharSequence(Notification.EXTRA_TITLE);

            String Text = seqText != null ? seqText.toString() : "!nulltext!";
            String Title = seqTitle != null ? seqTitle.toString() : "!nulltitle!";
            String Message = Title + "   " + Text;
            Log. i ( TAG , "********** onNotificationPosted" ) ;
            Log. i ( TAG , "ID :" + sbn.getId()  + " \t " + sbn.getNotification(). tickerText + " \t " + sbn.getPackageName()) ;
            if(notificationListener != null && !sbn.getPackageName().equals("com.android.systemui")) {
                notificationListener.NewNotification( "("+ sbn.getKey() + ") " + Message, sbn.getOpPkg());
            }
        //}

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
    public void setListener (NotificationListener notificationListener) {
        NotificationService.notificationListener = notificationListener;
    }
}
