package com.nhanh.lich;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Calendar;

/** Widget màn hình chính: ngày dương, ngày âm, can chi; tự đổi lúc nửa đêm. */
public class LichWidget extends AppWidgetProvider {
  private static final String[] DOW = {"Chủ nhật", "Thứ hai", "Thứ ba", "Thứ tư", "Thứ năm", "Thứ sáu", "Thứ bảy"};

  @Override
  public void onUpdate(Context c, AppWidgetManager mgr, int[] ids) {
    RemoteViews v = build(c);
    for (int id : ids) mgr.updateAppWidget(id, v);
    scheduleMidnight(c);
  }

  static RemoteViews build(Context c) {
    Calendar cal = Calendar.getInstance();
    int d = cal.get(Calendar.DAY_OF_MONTH), m = cal.get(Calendar.MONTH) + 1, y = cal.get(Calendar.YEAR);
    int dow = cal.get(Calendar.DAY_OF_WEEK) - 1;
    int[] l = LunarCalc.toLunar(d, m, y);
    RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_lich);
    v.setTextViewText(R.id.w_dow, DOW[dow].toUpperCase());
    v.setTextViewText(R.id.w_day, String.valueOf(d));
    v.setTextViewText(R.id.w_month, "Tháng " + m + ", " + y);
    v.setTextViewText(R.id.w_lunar, l[0] + "/" + l[1] + (l[3] == 1 ? " nhuận" : "") + " âm lịch");
    v.setTextViewText(R.id.w_can, "Ngày " + LunarCalc.dayName(d, m, y) + " · Năm " + LunarCalc.yearName(l[2]));
    v.setTextViewText(R.id.w_hol, holiday(d, m, l));
    Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    v.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
    return v;
  }

  private static String holiday(int d, int m, int[] l) {
    if (l[3] == 0) {
      if (l[0] == 1 && l[1] == 1) return "Tết Nguyên Đán";
      if (l[0] == 15 && l[1] == 1) return "Rằm tháng Giêng";
      if (l[0] == 10 && l[1] == 3) return "Giỗ Tổ Hùng Vương";
      if (l[0] == 15 && l[1] == 7) return "Vu Lan";
      if (l[0] == 15 && l[1] == 8) return "Tết Trung Thu";
      if (l[0] == 23 && l[1] == 12) return "Ông Công Ông Táo";
      if (l[0] == 1) return "Mùng 1";
      if (l[0] == 15) return "Ngày rằm";
    }
    if (d == 1 && m == 1) return "Tết Dương lịch";
    if (d == 2 && m == 9) return "Quốc khánh";
    return "";
  }

  static void updateAll(Context c) {
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    int[] ids = m.getAppWidgetIds(new ComponentName(c, LichWidget.class));
    if (ids == null || ids.length == 0) return;
    RemoteViews v = build(c);
    for (int id : ids) m.updateAppWidget(id, v);
    scheduleMidnight(c);
  }

  static void scheduleMidnight(Context c) {
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    if (am == null) return;
    Calendar n = Calendar.getInstance();
    n.add(Calendar.DAY_OF_MONTH, 1);
    n.set(Calendar.HOUR_OF_DAY, 0);
    n.set(Calendar.MINUTE, 0);
    n.set(Calendar.SECOND, 20);
    Intent i = new Intent(c, LichWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c, LichWidget.class)));
    PendingIntent pi = PendingIntent.getBroadcast(c, 9001, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    am.setAndAllowWhileIdle(AlarmManager.RTC, n.getTimeInMillis(), pi);
  }
}
