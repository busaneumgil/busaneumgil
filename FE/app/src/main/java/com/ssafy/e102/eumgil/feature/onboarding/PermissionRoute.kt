package com.ssafy.e102.eumgil.feature.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * 온보딩 완료 직전 런타임 권한을 일괄 요청하는 Route.
 *
 * 별도 UI 없이 화면 진입 즉시 RECORD_AUDIO · ACCESS_FINE_LOCATION ·
 * ACCESS_COARSE_LOCATION 세 권한을 요청한다.
 * 허용/거부 결과에 관계없이 [onPermissionHandled]를 호출해 다음 화면으로 진행한다.
 */
@Composable
fun PermissionRoute(onPermissionHandled: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        onPermissionHandled()
    }

    LaunchedEffect(launcher) {
        launcher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }
}
