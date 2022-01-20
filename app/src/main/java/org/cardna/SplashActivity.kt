package org.cardna

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import org.cardna.base.baseutil.BaseViewUtil
import org.cardna.databinding.ActivitySplashBinding

class SplashActivity : BaseViewUtil.BaseAppCompatActivity<ActivitySplashBinding>(R.layout.activity_splash) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun initView() {
        initSplash()
    }

    private fun initSplash() {
        val handler = Handler()
        handler.postDelayed({ var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }, SPLASH_VIEW_TIME)
    }

    companion object {
        const val SPLASH_VIEW_TIME = 2000L
    }
}