package com.example.newsreader

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.example.newsreader.NewsList.MainActivity
import kotlinx.android.synthetic.main.activity_intro.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IntroActivity : AppCompatActivity() {

    var runnable = Runnable {
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        AlarmTool.setAlarm(applicationContext)
        CacheDataUpdate.filesdir = filesDir
        imageCenter.setBackgroundResource(R.drawable.icon_newspaper)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LOW_PROFILE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    override fun onResume() {
        super.onResume()
        layout_intro.postDelayed(runnable, 1300)
    }

    override fun onPause() {
        super.onPause()
        layout_intro.removeCallbacks(runnable)
    }
}
