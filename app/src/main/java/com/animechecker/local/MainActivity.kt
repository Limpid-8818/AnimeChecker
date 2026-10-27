package com.animechecker.local

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.animechecker.local.ui.AppRoot
import com.animechecker.local.ui.DeepLinkRequest
import com.animechecker.local.ui.theme.AnimeCheckerTheme

class MainActivity : ComponentActivity() {

    private var pendingDeepLink by mutableStateOf<DeepLinkRequest?>(null)

    private var deepLinkSeq = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 容器由 AnimeCheckerApp 创建并持有.
        val container = (application as AnimeCheckerApp).container
        deliverDeepLink(intent)
        setContent {
            AnimeCheckerTheme {
                AppRoot(container, deepLink = pendingDeepLink)
            }
        }
    }

    // singleTop模式下栈顶存在实例时，使用onNewIntent投递深链跳转请求。
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deliverDeepLink(intent)
    }

    /**
     * 解析意图携带的深链跳转请求。
     */
    private fun deliverDeepLink(intent: Intent) {
        val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L).takeIf { it >= 0 } ?: return
        pendingDeepLink = DeepLinkRequest(subjectId, ++deepLinkSeq)
    }

    companion object {
        /** 桌面组件卡片点击携带的条目 id。 */
        const val EXTRA_SUBJECT_ID = "subjectId"
    }
}
