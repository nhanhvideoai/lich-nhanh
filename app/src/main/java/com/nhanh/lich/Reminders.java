package com.nhanh.lich;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

/** Lưu danh sách nhắc việc do giao diện tính sẵn và đặt báo thức cho từng mục. */
public final class Reminders {
  static final String CHANNEL = "nhac-viec";
  private static final String PREF = "reminders";

  private Reminders() {}

  static void ensureChannel(Context c) {
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (nm == null || nm.getNotificationChannel(CHANNEL) != null) return;
    NotificationChannel ch = new NotificationChannel(CHANNEL, "Nhắc việc", NotificationManager.IMPORTANCE_HIGH);
    ch.setDescription("Thông báo nhắc sự kiện trong lịch");
    ch.enableVibration(true);
    nm.createNotificationChannel(ch);
  }

  static void save(Context c, String json) {
    prefs(c).edit().putString("list", json == null ? "[]" : json).apply();
  }

  static void scheduleAll(Context c) {
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    if (am == null) return;
    SharedPreferences p = prefs(c);
    // huỷ các báo thức cũ
    try {
      JSONArray old = new JSONArray(p.getString("ids", "[]"));
      for (int i = 0; i < old.length(); i++) am.cancel(pending(c, old.getInt(i), null, null));
    } catch (Exception ignored) {}

    JSONArray ids = new JSONArray();
    long now = System.currentTimeMillis();
    boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
    try {
      JSONArray list = new JSONArray(p.getString("list", "[]"));
      for (int i = 0; i < list.length(); i++) {
        JSONObject o = list.getJSONObject(i);
        long at = o.getLong("at");
        if (at <= now) continue;
        int id = o.getInt("id");
        PendingIntent pi = pending(c, id, o.optString("title"), o.optString("body"));
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
        ids.put(id);
      }
    } catch (Exception ignored) {}
    p.edit().putString("ids", ids.toString()).apply();
  }

  private static PendingIntent pending(Context c, int id, String title, String body) {
    Intent i = new Intent(c, ReminderReceiver.class);
    i.putExtra("id", id);
    if (title != null) i.putExtra("title", title);
    if (body != null) i.putExtra("body", body);
    return PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREF, Context.MODE_PRIVATE); }
}
