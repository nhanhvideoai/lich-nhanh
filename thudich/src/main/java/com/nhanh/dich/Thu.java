package com.nhanh.dich;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.io.*;
import java.util.*;

public class Thu extends Activity {
  static final String V = "com.xiaomi.aiasst.vision";
  PrintWriter out;

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    SharedPreferences sp = getSharedPreferences("c", 0);
    Intent it = getIntent();
    if (it.hasExtra("dat")) sp.edit().putInt("cach", it.getIntExtra("dat", 1)).apply();
    int cach = it.hasExtra("cach") ? it.getIntExtra("cach", 0) : sp.getInt("cach", 1);
    try { out = new PrintWriter(new FileWriter(new File(getExternalFilesDir(null), "ketqua.txt"), true)); }
    catch (Exception e) { out = new PrintWriter(new StringWriter()); }
    out.println("=== cach " + cach + " action=" + it.getAction() + " " + new Date());
    try { chay(cach); out.println("OK"); }
    catch (Throwable e) { out.println("LOI " + e); }
    out.flush(); out.close();
    finish();
    overridePendingTransition(0, 0);
  }

  Intent a(String action) { return new Intent(action).setPackage(V).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); }
  ComponentName c(String cls) { return new ComponentName(V, V + cls); }

  void chay(int cach) throws Exception {
    switch (cach) {
      case 0: liet(); break;
      case 1: startActivity(a("miui.intent.action.IntentAiTranslateServiceActivity")); break;
      case 2: startActivity(a("com.xiaomi.aiasst.vision.ACTION_AI_TRANSLATE")); break;
      case 3: startForegroundService(new Intent().setComponent(c(".control.translation.AiTranslateService"))); break;
      case 4: startService(new Intent().setComponent(c(".control.translation.AiTranslateService"))); break;
      case 5: startActivity(new Intent(Intent.ACTION_VIEW).setComponent(c(".intent.DesktopShortcutRouterActivity"))
                .putExtra("from", "desktop_shortcut").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); break;
      case 6: sendBroadcast(new Intent("com.xiaomi.aiasst.vision.ACTION_AI_TRANSLATE").setPackage(V)); break;
      case 7: startService(new Intent("com.xiaomi.aiasst.vision.ACTION_TRANSLATION_SERVICE").setPackage(V)); break;
      case 8: startService(new Intent("com.xiaomi.aiasst.vision.ACTION_TRANSLATE_SERVICE").setPackage(V)); break;
      case 9: startActivity(a("miui.intent.action.ACTION_START_AI_SUBTITLE")); break;
      case 10: startActivity(a("miui.intent.action.AITranslateDetailsActivity")); break;
      case 11: startActivity(new Intent().setComponent(c(".intent.IntentAiTranslateServiceActivity"))
                .putExtra("from", "desktop_shortcut").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); break;
    }
  }

  void liet() throws Exception {
    PackageManager pm = getPackageManager();
    PackageInfo p = pm.getPackageInfo(V, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES
        | PackageManager.GET_RECEIVERS | PackageManager.GET_PROVIDERS);
    out.println("version " + p.versionName);
    if (p.activities != null) for (ActivityInfo x : p.activities)
      if (x.exported) out.println("ACT " + x.name + " perm=" + x.permission);
    if (p.services != null) for (ServiceInfo x : p.services)
      if (x.exported) out.println("SVC " + x.name + " perm=" + x.permission);
    if (p.receivers != null) for (ActivityInfo x : p.receivers)
      if (x.exported) out.println("RCV " + x.name + " perm=" + x.permission);
    if (p.providers != null) for (ProviderInfo x : p.providers)
      if (x.exported) out.println("PRV " + x.name + " auth=" + x.authority + " perm=" + x.readPermission);
  }
}
