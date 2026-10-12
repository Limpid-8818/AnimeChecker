package com.animechecker.local.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.animechecker.local.data.repository.AppRepository
import com.animechecker.local.di.AppContainer

/**
 * 依赖通道：组合根在这里进入 Compose 树，ViewModel 由此取依赖。
 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer 未提供：AppRoot 需要在 CompositionLocalProvider(LocalAppContainer provides ...) 内调用")
}

/** 构造 feature ViewModel：依赖从 [LocalAppContainer] 取，Screen 不直接接触数据层。 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppRepository) -> VM,
): VM {
    // current 必须在 composable 上下文读取，不能带进 factory 的 lambda
    val repository = LocalAppContainer.current.repository
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(repository) } },
    )
}
