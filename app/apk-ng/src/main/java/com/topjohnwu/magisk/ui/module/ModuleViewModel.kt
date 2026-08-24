package com.topjohnwu.magisk.ui.module

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.topjohnwu.magisk.arch.AsyncLoadViewModel
import com.topjohnwu.magisk.core.Const
import com.topjohnwu.magisk.core.Info
import com.topjohnwu.magisk.core.R as CoreR
import com.topjohnwu.magisk.core.download.Subject
import com.topjohnwu.magisk.core.model.module.LocalModule
import com.topjohnwu.magisk.core.model.module.OnlineModule
import com.topjohnwu.magisk.core.utils.TextHolder
import com.topjohnwu.magisk.core.utils.asText
import com.topjohnwu.magisk.ui.flash.FlashUtils
import com.topjohnwu.magisk.ui.navigation.Route
import com.topjohnwu.magisk.view.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize

class ModuleItem(val module: LocalModule) {
    val showNotice: Boolean
    val showAction: Boolean
    val noticeText: TextHolder

    init {
        val isZygisk = module.isZygisk
        val isRiru = module.isRiru
        val zygiskUnloaded = isZygisk && module.zygiskUnloaded

        showNotice = zygiskUnloaded ||
            (Info.isZygiskEnabled && isRiru) ||
            (!Info.isZygiskEnabled && isZygisk)
        showAction = module.hasAction && !showNotice
        noticeText =
            when {
                zygiskUnloaded -> CoreR.string.zygisk_module_unloaded.asText()
                isRiru -> CoreR.string.suspend_text_riru.asText(CoreR.string.zygisk.asText())
                else -> CoreR.string.suspend_text_zygisk.asText(CoreR.string.zygisk.asText())
            }
    }

    var isEnabled by mutableStateOf(module.enable)
    var isRemoved by mutableStateOf(module.remove)
    var showUpdate by mutableStateOf(module.updateInfo != null)
    val isUpdated = module.updated
    val updateReady get() = module.outdated && !isRemoved && isEnabled
}

@Parcelize
class OnlineModuleSubject(
    override val module: OnlineModule,
    override val autoLaunch: Boolean,
    override val notifyId: Int = Notifications.nextId()
) : Subject.Module() {
    override fun pendingIntent(context: Context) = FlashUtils.installIntent(context, file)
}

class ModuleViewModel : AsyncLoadViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val modules: List<ModuleItem> = emptyList(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val _sortBy = MutableStateFlow(ModuleSortBy.NAME)
    val sortBy: StateFlow<ModuleSortBy> = _sortBy.asStateFlow()
    private val _sortReverse = MutableStateFlow(false)
    val sortReverse: StateFlow<Boolean> = _sortReverse.asStateFlow()

    val filteredModules: StateFlow<List<ModuleItem>> = combine(
        _uiState, _query, _sortBy, _sortReverse
    ) { state, query, sortBy, reverse ->
        val q = query.trim()
        state.modules
            .filter { item ->
                q.isEmpty() ||
                    item.module.name.contains(q, ignoreCase = true) ||
                    item.module.id.contains(q, ignoreCase = true) ||
                    item.module.author.contains(q, ignoreCase = true) ||
                    item.module.description.contains(q, ignoreCase = true)
            }
            .sortedWith(
                when (sortBy) {
                    ModuleSortBy.NAME ->
                        if (reverse) compareByDescending<ModuleItem> { it.module.name.lowercase() }
                        else compareBy { it.module.name.lowercase() }
                    ModuleSortBy.VERSION ->
                        if (reverse) compareByDescending<ModuleItem> { it.module.versionCode }
                        else compareBy { it.module.versionCode }
                    ModuleSortBy.AUTHOR ->
                        if (reverse) compareByDescending<ModuleItem> { it.module.author.lowercase() }
                        else compareBy { it.module.author.lowercase() }
                }
            )
    }

    override suspend fun doLoadWork() {
        val isRefresh = _uiState.value.modules.isNotEmpty()
        _uiState.update { it.copy(loading = !isRefresh, refreshing = isRefresh) }
        val moduleLoaded = Info.env.isActive &&
            withContext(Dispatchers.IO) { LocalModule.loaded() }
        if (moduleLoaded) {
            val modules = withContext(Dispatchers.Default) {
                LocalModule.installed().map { ModuleItem(it) }
            }
            _uiState.update { it.copy(loading = false, refreshing = false, modules = modules) }
            loadUpdateInfo()
        } else {
            _uiState.update { it.copy(loading = false, refreshing = false) }
        }
    }

    private val networkObserver: (Boolean) -> Unit = { startLoading() }

    init {
        Info.isConnected.observeForever(networkObserver)
    }

    override fun onCleared() {
        super.onCleared()
        Info.isConnected.removeObserver(networkObserver)
    }

    private suspend fun loadUpdateInfo() {
        withContext(Dispatchers.IO) {
            _uiState.value.modules.forEach { item ->
                if (item.module.fetch()) {
                    item.showUpdate = item.module.updateInfo != null
                }
            }
        }
    }

    fun confirmLocalInstall(uri: Uri) {
        navigateTo(Route.Flash(Const.Value.FLASH_ZIP, uri.toString()))
    }

    fun runAction(id: String, name: String) {
        navigateTo(Route.Action(id, name))
    }

    fun toggleEnabled(item: ModuleItem) {
        item.isEnabled = !item.isEnabled
        item.module.enable = item.isEnabled
        showSnackbar(CoreR.string.reboot_apply_change)
    }

    fun toggleRemove(item: ModuleItem) {
        item.isRemoved = !item.isRemoved
        item.module.remove = item.isRemoved
        showSnackbar(CoreR.string.reboot_apply_change)
    }

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun setSortBy(sortBy: ModuleSortBy) {
        _sortBy.value = sortBy
    }

    fun toggleSortReverse() {
        _sortReverse.value = !_sortReverse.value
    }
}

enum class ModuleSortBy { NAME, VERSION, AUTHOR }
