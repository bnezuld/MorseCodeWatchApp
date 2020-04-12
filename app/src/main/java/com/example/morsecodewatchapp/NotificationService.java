package com.example.morsecodewatchapp;

import android.content.Context ;
import android.service.notification.NotificationListenerService ;
import android.service.notification.StatusBarNotification ;
import android.util.Log ;
import android.app.Notification;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NotificationService extends NotificationListenerService {
    private String TAG = this .getClass().getSimpleName() ;
    enum NotificationType {
        SIMPLE_ALERT,
        EMAIL,
        NEWS,
        INCOMING_CALL,
        MISSED_CALL,
        SMS,
        VOICE_MAIL,
        SCHEDULE,
        HIGH_PRIORITY_ALERT,
        INSTANT_MESSAGE,
        IGNORE
    }
    Map<String, NotificationType> packageNameNotificationType;


    Context context ;
    static NotificationListener notificationListener;
    @Override
    public void onCreate () {
        super .onCreate() ;
        context = getApplicationContext() ;
        packageNameNotificationType  = new HashMap<String, NotificationType>();

        //load from settings the package names and their notification tpe and add to the list
        //packageNameNotificationType.put();
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
        if(notificationListener != null && !sbn.getPackageName().equals("com.android.systemui")) {
            notificationListener.NewNotification( Message,
                                                    packageNameNotificationType.getOrDefault(sbn.getOpPkg(), NotificationType.IGNORE));
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
    public void setListener (NotificationListener notificationListener) {
        NotificationService.notificationListener = notificationListener;
    }
}
