package com.nhanh.lich;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
    Reminders.ensureChannel(c);
    int id = intent.getIntExtra("id", 0);
    String title = intent.getStringExtra("title");
    String body = intent.getStringExtra("body");
    Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    Notification n = new Notification.Builder(c, Reminders.CHANNEL)
        .setSmallIcon(R.drawable.ic_stat_lich)
        .setColor(0xFFFFD27A)
        .setContentTitle(title == null ? "Lịch Nhanh" : title)
        .setContentText(body == null ? "" : body)
        .setStyle(new Notification.BigTextStyle().bigText(body == null ? "" : body))
        .setCategory(Notification.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setContentIntent(pi)
        .build();
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm != null) nm.notify(id, n);
  }
}
