package com.nhanh.dich;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.*;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.*;
import android.view.accessibility.*;
import java.io.*;
import java.util.*;

public class DichService extends AccessibilityService {
  static final String V = "com.xiaomi.aiasst.vision";
  static final String VA = "com.miui.voiceassist";
  static volatile long cho = 0;      // thời điểm bắt đầu chờ
  static volatile int buoc = 0;      // 1: chờ ô Dịch màn hình, 2: chờ nút Start
  static DichService ban;
  final Handler h = new Handler(Looper.getMainLooper());

  static final String[] O_DICH = {"Dịch màn hình", "Screen translation", "Translate screen", "屏幕翻译"};
  static final String[] NUT_START = {"Start", "Bắt đầu", "开始", "開始"};

  static void batDau(Context c) {
    cho = SystemClock.uptimeMillis(); buoc = 1;
    ghi(c, "batDau, service=" + (ban != null));
    Intent i = new Intent().setComponent(new ComponentName(V, V + ".ui.MainActivity"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    c.startActivity(i);
    if (ban != null) ban.quetLai(0);
  }

  @Override protected void onServiceConnected() { ban = this; ghi(this, "service ket noi"); }
  @Override public boolean onUnbind(Intent i) { if (ban == this) ban = null; return super.onUnbind(i); }
  @Override public void onInterrupt() {}

  @Override public void onAccessibilityEvent(AccessibilityEvent e) {
    if (buoc == 0) return;
    if (SystemClock.uptimeMillis() - cho > 12000) { ghi(this, "het gio o buoc " + buoc); buoc = 0; return; }
    quet();
  }

  void quetLai(long tre) {
    h.postDelayed(() -> { if (buoc != 0 && SystemClock.uptimeMillis() - cho < 12000) { quet(); quetLai(250); } }, tre == 0 ? 250 : tre);
  }

  void quet() {
    List<AccessibilityWindowInfo> ws = getWindows();
    for (AccessibilityWindowInfo w : ws) {
      AccessibilityNodeInfo r = w.getRoot();
      if (r == null || r.getPackageName() == null) continue;
      String pk = r.getPackageName().toString();
      if (buoc == 1 && pk.equals(V)) {
        AccessibilityNodeInfo n = tim(r, O_DICH);
        if (n != null) { bam(n); buoc = 2; cho = SystemClock.uptimeMillis(); ghi(this, "bam o dich man hinh"); return; }
      }
      if (buoc == 2 && pk.equals(VA)) {
        AccessibilityNodeInfo n = tim(r, NUT_START);
        if (n != null) { bam(n); buoc = 0; ghi(this, "bam Start - xong"); return; }
      }
    }
  }

  AccessibilityNodeInfo tim(AccessibilityNodeInfo r, String[] chu) {
    for (String s : chu) {
      List<AccessibilityNodeInfo> l = r.findAccessibilityNodeInfosByText(s);
      for (AccessibilityNodeInfo n : l) {
        CharSequence t = n.getText() != null ? n.getText() : n.getContentDescription();
        if (t != null && t.toString().trim().equalsIgnoreCase(s)) return n;
      }
    }
    return null;
  }

  void bam(AccessibilityNodeInfo n) {
    AccessibilityNodeInfo p = n;
    while (p != null && !p.isClickable()) p = p.getParent();
    if (p != null && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
    Rect b = new Rect(); n.getBoundsInScreen(b);
    Path path = new Path(); path.moveTo(b.centerX(), b.centerY());
    dispatchGesture(new GestureDescription.Builder()
        .addStroke(new GestureDescription.StrokeDescription(path, 0, 60)).build(), null, null);
  }

  static void ghi(Context c, String s) {
    try (PrintWriter o = new PrintWriter(new FileWriter(new File(c.getExternalFilesDir(null), "ketqua.txt"), true))) {
      o.println(new Date() + " " + s);
    } catch (Exception ignored) {}
  }
}
