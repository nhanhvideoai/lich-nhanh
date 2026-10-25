package com.nhanh.lich;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.webkit.JavascriptInterface;

/** Cầu nối JS ↔ Android: rung, nhắc việc, bộ nhớ tạm, widget. */
public class Bridge {
  private final Activity act;
  private final Context c;

  Bridge(Activity a) { act = a; c = a.getApplicationContext(); }

  @JavascriptInterface public void tick() {
    Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    if (v == null || !v.hasVibrator()) return;
    if (Build.VERSION.SDK_INT >= 29) v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
    else v.vibrate(VibrationEffect.createOneShot(8, 80));
  }

  @JavascriptInterface public void schedule(String json) {
    Reminders.save(c, json);
    Reminders.scheduleAll(c);
  }

  @JavascriptInterface public boolean canNotify() {
    return Build.VERSION.SDK_INT < 33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
  }

  @JavascriptInterface public void askNotify() {
    if (Build.VERSION.SDK_INT >= 33)
      act.runOnUiThread(() -> act.requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 5));
  }

  @JavascriptInterface public boolean canExact() {
    if (Build.VERSION.SDK_INT < 31) return true;
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    return am != null && am.canScheduleExactAlarms();
  }

  @JavascriptInterface public void askExact() {
    if (Build.VERSION.SDK_INT < 31) return;
    Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + c.getPackageName()));
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try { c.startActivity(i); } catch (Exception ignored) {}
  }

  @JavascriptInterface public void copy(String text) {
    ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Lịch Nhanh", text));
  }

  @JavascriptInterface public String paste() {
    ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
    if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip().getItemCount() == 0) return "";
    CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(c);
    return t == null ? "" : t.toString();
  }

  @JavascriptInterface public void refreshWidget() { LichWidget.updateAll(c); }

  @JavascriptInterface public String version() {
    try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; }
    catch (Exception e) { return ""; }
  }
}
