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
  static long thay = 0, lanBam = 0; static int soLan = 0;
  final Handler h = new Handler(Looper.getMainLooper());

  static final String SW = "com.miui.voiceassist:id/cl_trans_sw";
  static final String[] O_DICH = {"Dịch màn hình", "Screen translation", "Translate screen", "屏幕翻译"};
  static final String[] NUT_START = {"Start", "Bắt đầu", "开始", "開始"};

  static void batDau(Context c) {
    ghi(c, "batDau, service=" + (ban != null));
    if (ban != null) {
      AccessibilityNodeInfo sw = ban.timId(SW);
      if (sw != null) { ban.bam(sw); ghi(c, "da co thanh dich ben canh -> bat/tat"); return; }
    }
    cho = SystemClock.uptimeMillis(); buoc = 1; thay = 0; lanBam = 0; soLan = 0;
    Intent i = new Intent().setComponent(new ComponentName(V, V + ".ui.MainActivity"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    c.startActivity(i);
    if (ban != null) ban.quetLai(0);
  }

  @Override protected void onServiceConnected() { ban = this; soSuKien = 0; ghi(this, "service ket noi"); }
  @Override public boolean onUnbind(Intent i) { if (ban == this) ban = null; return super.onUnbind(i); }
  @Override public void onInterrupt() {}

  static long lanChan = 0; static int soSuKien = 0;

  // Khi AI Xiaomi bật lên (giữ thanh dưới) -> đóng nó và bật dịch màn hình
  boolean chanAiXiaomi() {
    long now = SystemClock.uptimeMillis();
    if (now - lanChan < 4000) return false;
    for (AccessibilityWindowInfo w : getWindows()) {
      AccessibilityNodeInfo r = w.getRoot();
      if (r == null || r.getPackageName() == null || !r.getPackageName().toString().equals(VA)) continue;
      Rect b = new Rect(); w.getBoundsInScreen(b);
      if (soSuKien < 40) ghi(this, "  win VA " + b.toShortString() + " type=" + w.getType());
      if (coIdChua(r, "trans", 0) || coIdChua(r, "loading", 0)) continue;
      boolean nghe = !r.findAccessibilityNodeInfosByText("我在听").isEmpty() || !r.findAccessibilityNodeInfosByText("松手发送").isEmpty()
          || !r.findAccessibilityNodeInfosByText("Đang nghe").isEmpty() || !r.findAccessibilityNodeInfosByText("listening").isEmpty();
      if (b.height() < 900 && !nghe) continue;
      lanChan = now;
      StringBuilder sb = new StringBuilder("chan AI Xiaomi: win " + b.toShortString() + "\n");
      cay(r, 0, sb);
      ghi(this, sb.length() > 3000 ? sb.substring(0, 3000) : sb.toString());
      performGlobalAction(GLOBAL_ACTION_BACK);
      h.postDelayed(() -> batDau(this), 450);
      return true;
    }
    return false;
  }

  static boolean coIdChua(AccessibilityNodeInfo n, String s, int d) {
    if (n == null || d > 12) return false;
    String id = n.getViewIdResourceName();
    if (id != null && id.toLowerCase().contains(s)) return true;
    for (int i = 0; i < n.getChildCount(); i++) if (coIdChua(n.getChild(i), s, d + 1)) return true;
    return false;
  }

  @Override public void onAccessibilityEvent(AccessibilityEvent e) {
    if (buoc == 0) {
      if (e.getPackageName() != null && !"com.android.chrome".contentEquals(e.getPackageName()) && soSuKien++ < 80) ghi(this, "sk pkg=" + e.getPackageName() + " type=" + e.getEventType() + " cls=" + e.getClassName() + " txt=" + e.getText());
      if ((e.getPackageName() != null && VA.contentEquals(e.getPackageName())) || e.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED) chanAiXiaomi();
      return;
    }
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
        long now = SystemClock.uptimeMillis();
        if (n != null) {
          if (thay == 0) thay = now;
          if (now - thay >= 500 && now - lanBam >= 1200) {
            if (soLan >= 4) { buoc = 0; ghi(this, "bam Start 4 lan khong an"); return; }
            Rect b = new Rect(); n.getBoundsInScreen(b);
            AccessibilityNodeInfo p = n; while (p != null && !p.isClickable()) p = p.getParent();
            if (p != null) p.getBoundsInScreen(b);
            cham(b.centerX(), b.centerY());
            lanBam = now; soLan++;
            ghi(this, "cham Start lan " + soLan + " tai " + b.toShortString());
          }
          return;
        } else if (soLan > 0) { buoc = 0; ghi(this, "Start bien mat - dang dich"); return; }
        else if (now - cho > 1500) {
          List<AccessibilityNodeInfo> l = r.findAccessibilityNodeInfosByViewId(SW);
          if (!l.isEmpty()) { bam(l.get(0)); buoc = 0; ghi(this, "thanh thu gon -> bam nut dich"); return; }
        }
      }
    }
  }

  static void xemCay(Context c) {
    if (ban == null) { ghi(c, "xemCay: service chua chay"); return; }
    StringBuilder sb = new StringBuilder();
    for (AccessibilityWindowInfo w : ban.getWindows()) {
      AccessibilityNodeInfo r = w.getRoot();
      sb.append("## win type=").append(w.getType()).append(" layer=").append(w.getLayer())
        .append(" pkg=").append(r == null ? null : r.getPackageName()).append('\n');
      if (r != null && r.getPackageName() != null && !r.getPackageName().toString().equals("com.android.chrome")) cay(r, 0, sb);
    }
    ghi(c, sb.toString());
  }

  static void cay(AccessibilityNodeInfo n, int d, StringBuilder sb) {
    if (n == null || d > 25) return;
    Rect b = new Rect(); n.getBoundsInScreen(b);
    for (int i = 0; i < d; i++) sb.append(' ');
    sb.append(n.getClassName()).append(" id=").append(n.getViewIdResourceName())
      .append(" t=").append(n.getText()).append(" d=").append(n.getContentDescription())
      .append(n.isClickable() ? " CLICK" : "").append(' ').append(b.toShortString()).append('\n');
    for (int i = 0; i < n.getChildCount(); i++) cay(n.getChild(i), d + 1, sb);
  }

  AccessibilityNodeInfo timId(String id) {
    for (AccessibilityWindowInfo w : getWindows()) {
      AccessibilityNodeInfo r = w.getRoot();
      if (r == null || r.getPackageName() == null || !r.getPackageName().toString().equals(VA)) continue;
      List<AccessibilityNodeInfo> l = r.findAccessibilityNodeInfosByViewId(id);
      if (!l.isEmpty()) return l.get(0);
    }
    return null;
  }

  void cham(int x, int y) {
    Path path = new Path(); path.moveTo(x, y);
    dispatchGesture(new GestureDescription.Builder()
        .addStroke(new GestureDescription.StrokeDescription(path, 0, 50)).build(), null, null);
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
