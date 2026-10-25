package com.nhanh.lich;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewClientCompat;

public class MainActivity extends Activity {
  private static final int REQ_FILE = 11;
  private WebView web;
  private ValueCallback<Uri[]> fileCb;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    web = new WebView(this);
    web.setBackgroundColor(0xFF2A2522);
    web.setOverScrollMode(View.OVER_SCROLL_NEVER);
    setContentView(web);

    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setAllowFileAccess(false);
    s.setAllowContentAccess(true);
    s.setTextZoom(100);

    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
        .build();
    web.setWebViewClient(new WebViewClientCompat() {
      @Override
      public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return loader.shouldInterceptRequest(request.getUrl());
      }
    });
    web.setWebChromeClient(new WebChromeClient() {
      @Override
      public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
        if (fileCb != null) fileCb.onReceiveValue(null);
        fileCb = cb;
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        try {
          startActivityForResult(Intent.createChooser(i, "Chọn ảnh nền"), REQ_FILE);
        } catch (Exception e) {
          fileCb = null;
          return false;
        }
        return true;
      }
    });
    web.addJavascriptInterface(new Bridge(this), "Native");
    Reminders.ensureChannel(this);
    web.loadUrl("https://appassets.androidplatform.net/assets/www/index.html");
  }

  @Override
  protected void onActivityResult(int req, int res, Intent data) {
    super.onActivityResult(req, res, data);
    if (req == REQ_FILE && fileCb != null) {
      Uri[] r = (res == RESULT_OK && data != null && data.getData() != null) ? new Uri[] {data.getData()} : null;
      fileCb.onReceiveValue(r);
      fileCb = null;
    }
  }

  @Override
  public void onBackPressed() {
    web.evaluateJavascript("(window.onNativeBack && window.onNativeBack()) ? '1' : '0'", v -> {
      if (!"\"1\"".equals(v)) moveTaskToBack(true);
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (web != null) web.evaluateJavascript("window.onNativeResume && window.onNativeResume()", null);
    LichWidget.updateAll(this);
  }
}
