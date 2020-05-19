package com.example.morsecodewatchapp;

import android.app.PendingIntent;
import android.content.Context ;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService ;
import android.service.notification.StatusBarNotification ;
import android.text.TextUtils;
import android.util.Log ;
import android.app.Notification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.RemoteInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NotificationService extends NotificationListenerService {
    private String TAG = this .getClass().getSimpleName() ;
    static char SIMPLE_ALERT = 0x0;
    static char EMAIL = 0x1;
    static char NEWS = 0x2;
    static char INCOMING_CALL = 0x3;
    static char MISSED_CALL = 0x4;
    static char SMS = 0x5;
    static char VOICE_MAIL = 0x6;
    static char SCHEDULE = 0x7;
    static char HIGH_PRIORITY_ALERT = 0x8;
    static char INSTANT_MESSAGE = 0x9;



    private static final String[] REPLY_KEYWORDS = {"reply", "android.intent.extra.text"};
    private static final CharSequence REPLY_KEYWORD = "reply";
    private static final CharSequence INPUT_KEYWORD = "input";


    Context context ;
    static NotificationListener notificationListener;
    @Override
    public void onCreate () {
        super .onCreate() ;
        context = getApplicationContext() ;

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
        String packageName = sbn.getPackageName();
        Log. i ( TAG , "********** onNotificationPosted" ) ;
        Log. i ( TAG , "ID :" + sbn.getId()  + " \t " + sbn.getNotification(). tickerText + " \t " + packageName) ;
        if(notificationListener != null && !packageName.equals("com.android.systemui") && packageName == "com.samsung.android.messaging") {
            NotificationCompat.Action action = getQuickReplyAction(sbn.getNotification(), sbn.getPackageName());
            if(action == null) {
                notificationListener.NewNotification(Message, SIMPLE_ALERT, action);
            }else{
                /*try {
                    sendReply(action, this.context, "reply");
                }catch(PendingIntent.CanceledException e)
                {

                }*/
                notificationListener.NewNotification(Message, SMS, action);
            }
        }
    }

    public static NotificationCompat.Action getQuickReplyAction(Notification n, String packageName) {
        NotificationCompat.Action action = null;
        if(Build.VERSION.SDK_INT >= 24)
            action = getQuickReplyAction(n);
        if(action == null)
            action = getWearReplyAction(n);
        if(action == null)
            return null;
        return new NotificationCompat.Action.Builder(action).build();
    }

    private static NotificationCompat.Action getQuickReplyAction(Notification n) {
        for(int i = 0; i < NotificationCompat.getActionCount(n); i++) {
            NotificationCompat.Action action = NotificationCompat.getAction(n, i);
            if(action.getRemoteInputs() != null) {
                for (int x = 0; x < action.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = action.getRemoteInputs()[x];
                    if (isKnownReplyKey(remoteInput.getResultKey()))
                        return action;
                }
            }
        }
        return null;
    }

    public static void sendReply(NotificationCompat.Action action, Context context, String msg) throws PendingIntent.CanceledException {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        ArrayList<RemoteInput> actualInputs = new ArrayList<>();

        for (RemoteInput input : action.getRemoteInputs()) {
            Log.i("", "RemoteInput: " + input.getLabel());
            bundle.putCharSequence(input.getResultKey(), msg);
            RemoteInput.Builder builder = new RemoteInput.Builder(input.getResultKey());
            builder.setLabel(input.getLabel());
            builder.setChoices(input.getChoices());
            builder.setAllowFreeFormInput(input.getAllowFreeFormInput());
            builder.addExtras(input.getExtras());
            actualInputs.add(builder.build());
        }

        RemoteInput[] inputs = actualInputs.toArray(new RemoteInput[actualInputs.size()]);
        RemoteInput.addResultsToIntent(inputs, intent, bundle);
        action.getActionIntent().send(context, 0, intent);
    }

    private static NotificationCompat.Action getWearReplyAction(Notification n) {
        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(n);
        for (NotificationCompat.Action action : wearableExtender.getActions()) {
            if(action.getRemoteInputs() != null) {
                for (int x = 0; x < action.getRemoteInputs().length; x++) {
                    RemoteInput remoteInput = action.getRemoteInputs()[x];
                    if (isKnownReplyKey(remoteInput.getResultKey()))
                        return action;
                    else if (remoteInput.getResultKey().toLowerCase().contains(INPUT_KEYWORD))
                        return action;
                }
            }
        }
        return null;
    }

    private static boolean isKnownReplyKey(String resultKey) {
        if(TextUtils.isEmpty(resultKey))
            return false;

        resultKey = resultKey.toLowerCase();
        for(String keyword : REPLY_KEYWORDS)
            if(resultKey.contains(keyword))
                return true;

        return false;
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
