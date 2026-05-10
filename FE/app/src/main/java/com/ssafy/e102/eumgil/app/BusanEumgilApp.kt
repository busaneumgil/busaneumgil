package com.ssafy.e102.eumgil.app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.kakao.vectormap.KakaoMapSdk
import com.navercorp.nid.NaverIdLoginSDK
import com.ssafy.e102.eumgil.BuildConfig

class BusanEumgilApp : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(ForegroundActivityProvider)
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isNotBlank()) {
            KakaoMapSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
            KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
        if (isNaverLoginConfigured()) {
            NaverIdLoginSDK.initialize(
                context = this,
                clientId = BuildConfig.NAVER_CLIENT_ID,
                clientSecret = BuildConfig.NAVER_CLIENT_SECRET,
                clientName = BuildConfig.NAVER_CLIENT_NAME,
            )
        }
        appContainer = AppContainer(context = this)
    }

    private fun isNaverLoginConfigured(): Boolean =
        BuildConfig.NAVER_CLIENT_ID.isNotBlank() &&
            BuildConfig.NAVER_CLIENT_SECRET.isNotBlank() &&
            BuildConfig.NAVER_CLIENT_NAME.isNotBlank()
}
