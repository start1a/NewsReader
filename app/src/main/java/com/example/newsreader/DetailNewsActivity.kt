package com.example.newsreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_detail_news.*

class DetailNewsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_news)

        val link = intent.getStringExtra("link")
        // 자바스크립트 허용
        newsWebView.settings.javaScriptEnabled = true

        newsWebView.loadUrl(link)
        newsWebView.webChromeClient = WebChromeClient()
        newsWebView.webViewClient = WebViewClientClass()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 백버튼 클릭 시 페이지 뒤로 가기 여부 확인
        if ((keyCode == KeyEvent.KEYCODE_BACK) && newsWebView.canGoBack()) {
            newsWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // 새로운 페이지를 띄울 경우 새 창이 아닌 기존 창에서 실행
    private class WebViewClientClass : WebViewClient() {
        //페이지 이동
        override fun shouldOverrideUrlLoading(
            view: WebView,
            url: String
        ): Boolean {
            Log.d("check URL", url)
            view.loadUrl(url)
            return true
        }
    }
}
