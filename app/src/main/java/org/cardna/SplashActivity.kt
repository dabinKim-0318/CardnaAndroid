package org.cardna

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, SPLASH_VIEW_TIME)
    }

    companion object {
        const val SPLASH_VIEW_TIME = 2000L
    }
}