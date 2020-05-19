package com.example.morsecodewatchapp;

import androidx.core.app.NotificationCompat;

public interface NotificationListener {
    void NewNotification(String Notification, char notificationType, NotificationCompat.Action action) ;
}