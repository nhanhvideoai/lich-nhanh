package com.nhanh.lich;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Đặt lại nhắc việc và widget sau khi khởi động máy hoặc cập nhật app. */
public class BootReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent intent) {
    Reminders.ensureChannel(c);
    Reminders.scheduleAll(c);
    LichWidget.updateAll(c);
    CapNhat.lenLich(c);
  }
}
