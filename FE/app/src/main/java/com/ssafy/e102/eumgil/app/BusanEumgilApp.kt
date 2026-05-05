package com.ssafy.e102.eumgil.app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.ssafy.e102.eumgil.BuildConfig

class BusanEumgilApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
        appContainer = AppContainer(context = this)
    }
}
