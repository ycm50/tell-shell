package com.tellshell.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyDataStore: DataStore<Preferences> by preferencesDataStore(name = "history")

class HistoryStore(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("history_items")
        private const val MAX_ITEMS = 200
    }

    private val gson = Gson()

    /** 所有历史记录 */
    val historyItems: Flow<List<HistoryItem>> = context.historyDataStore.data.map { prefs ->
        val json = prefs[KEY_HISTORY] ?: "[]"
        val type = object : TypeToken<List<HistoryItem>>() {}.type
        val items: List<HistoryItem> = gson.fromJson(json, type) ?: emptyList()
        items.sortedByDescending { it.timestamp }
    }

    /** 添加一条历史记录 */
    suspend fun addItem(item: HistoryItem) {
        context.historyDataStore.edit { prefs ->
            val json = prefs[KEY_HISTORY] ?: "[]"
            val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
            val items: MutableList<HistoryItem> = gson.fromJson(json, type) ?: mutableListOf()
            items.add(0, item) // 插到最前面
            // 限制最大条目数
            if (items.size > MAX_ITEMS) {
                val excess = items.size - MAX_ITEMS
                for (i in 0 until excess) {
                    items.removeAt(items.lastIndex)
                }
            }
            prefs[KEY_HISTORY] = gson.toJson(items)
        }
    }

    /** 更新已有条目（例如执行完更新输出） */
    suspend fun updateItem(updated: HistoryItem) {
        context.historyDataStore.edit { prefs ->
            val json = prefs[KEY_HISTORY] ?: "[]"
            val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
            val items: MutableList<HistoryItem> = gson.fromJson(json, type) ?: mutableListOf()
            val index = items.indexOfFirst { it.id == updated.id }
            if (index != -1) {
                items[index] = updated
                prefs[KEY_HISTORY] = gson.toJson(items)
            }
        }
    }

    /** 删除指定条目 */
    suspend fun deleteItems(ids: Set<String>) {
        context.historyDataStore.edit { prefs ->
            val json = prefs[KEY_HISTORY] ?: "[]"
            val type = object : TypeToken<MutableList<HistoryItem>>() {}.type
            val items: MutableList<HistoryItem> = gson.fromJson(json, type) ?: mutableListOf()
            items.removeAll { it.id in ids }
            prefs[KEY_HISTORY] = gson.toJson(items)
        }
    }

    /** 清空所有历史 */
    suspend fun clearAll() {
        context.historyDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = "[]"
        }
    }
}
