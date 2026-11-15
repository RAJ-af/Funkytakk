package com.example

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.chat.ChatPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SavedVocabWord(
    val word: String,
    val translation: String,
    val language: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LocalGroupChat(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val avatarUrl: String = "",
    val memberCount: Int = 1,
    val memberNames: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageText: String = "Group created",
    val lastMessageTime: Long = System.currentTimeMillis()
)

object ChatPreferencesManager {
    private const val KEY_MUTED_CHATS = "muted_chat_ids"
    private const val KEY_PINNED_CHATS = "pinned_chat_ids"
    private const val KEY_BLOCKED_USERS = "blocked_user_ids"
    private const val KEY_DELETED_CHATS = "deleted_chat_ids"
    private const val KEY_UNREAD_OVERRIDE_CHATS = "unread_override_chat_ids"
    private const val KEY_GROUPS_JSON = "custom_groups_json"
    private const val KEY_MINIMALIST_MODE = "is_minimalist_mode_enabled"
    private const val KEY_CHAT_PRESET = "chat_appearance_preset"
    private const val KEY_CHAT_TEXT_SCALE = "chat_appearance_text_scale"
    private const val KEY_CHAT_DARK_THEME = "chat_appearance_dark_theme" // -1 = system, 0 = light, 1 = dark
    private const val KEY_VOCAB_JSON = "saved_vocab_json"
    private const val KEY_PINNED_MESSAGES_JSON = "pinned_messages_json"

    private val _mutedChats = MutableStateFlow<Set<String>>(emptySet())
    val mutedChats: StateFlow<Set<String>> = _mutedChats.asStateFlow()

    private val _pinnedChats = MutableStateFlow<Set<String>>(emptySet())
    val pinnedChats: StateFlow<Set<String>> = _pinnedChats.asStateFlow()

    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()

    private val _deletedChats = MutableStateFlow<Set<String>>(emptySet())
    val deletedChats: StateFlow<Set<String>> = _deletedChats.asStateFlow()

    private val _unreadOverrideChats = MutableStateFlow<Set<String>>(emptySet())
    val unreadOverrideChats: StateFlow<Set<String>> = _unreadOverrideChats.asStateFlow()

    private val _isMinimalistMode = MutableStateFlow<Boolean>(false)
    val isMinimalistMode: StateFlow<Boolean> = _isMinimalistMode.asStateFlow()

    private val _chatPreset = MutableStateFlow<ChatPreset>(ChatPreset.Butter)
    val chatPreset: StateFlow<ChatPreset> = _chatPreset.asStateFlow()

    private val _chatTextScale = MutableStateFlow<Float>(1f)
    val chatTextScale: StateFlow<Float> = _chatTextScale.asStateFlow()

    private val _chatDarkThemeOverride = MutableStateFlow<Boolean?>(null)
    val chatDarkThemeOverride: StateFlow<Boolean?> = _chatDarkThemeOverride.asStateFlow()

    private val _savedVocab = MutableStateFlow<List<SavedVocabWord>>(emptyList())
    val savedVocab: StateFlow<List<SavedVocabWord>> = _savedVocab.asStateFlow()

    private val _pinnedMessages = MutableStateFlow<Map<String, String>>(emptyMap())
    val pinnedMessages: StateFlow<Map<String, String>> = _pinnedMessages.asStateFlow()

    private val _groups = MutableStateFlow<List<LocalGroupChat>>(emptyList())
    val groups: StateFlow<List<LocalGroupChat>> = _groups.asStateFlow()

    @Volatile
    private var initializedUid: String? = null

    private fun getPrefsForActiveUser(context: Context): SharedPreferences? {
        val uid = ActiveUserManager.getActiveUid(context) ?: return null
        return context.applicationContext.getSharedPreferences(
            ActiveUserManager.chatPrefsName(uid),
            Context.MODE_PRIVATE
        )
    }

    fun init(context: Context) {
        val uid = ActiveUserManager.getActiveUid(context)
        if (uid == null) {
            resetForLogout()
            return
        }
        if (initializedUid == uid) return
        synchronized(this) {
            if (initializedUid == uid) return
            initializedUid = uid

            val prefs = context.applicationContext.getSharedPreferences(
                ActiveUserManager.chatPrefsName(uid),
                Context.MODE_PRIVATE
            )
            _mutedChats.value = prefs.getStringSet(KEY_MUTED_CHATS, emptySet())?.toSet() ?: emptySet()
            _pinnedChats.value = prefs.getStringSet(KEY_PINNED_CHATS, emptySet())?.toSet() ?: emptySet()
            _blockedUsers.value = prefs.getStringSet(KEY_BLOCKED_USERS, emptySet())?.toSet() ?: emptySet()
            _deletedChats.value = prefs.getStringSet(KEY_DELETED_CHATS, emptySet())?.toSet() ?: emptySet()
            _unreadOverrideChats.value = prefs.getStringSet(KEY_UNREAD_OVERRIDE_CHATS, emptySet())?.toSet() ?: emptySet()
            _isMinimalistMode.value = prefs.getBoolean(KEY_MINIMALIST_MODE, false)

            val presetName = prefs.getString(KEY_CHAT_PRESET, ChatPreset.Butter.name) ?: ChatPreset.Butter.name
            _chatPreset.value = runCatching { ChatPreset.valueOf(presetName) }.getOrDefault(ChatPreset.Butter)
            _chatTextScale.value = prefs.getFloat(KEY_CHAT_TEXT_SCALE, 1f).coerceIn(0.85f, 1.4f)
            val darkPref = prefs.getInt(KEY_CHAT_DARK_THEME, -1)
            _chatDarkThemeOverride.value = when (darkPref) {
                0 -> false
                1 -> true
                else -> null
            }

            val vocabJson = prefs.getString(KEY_VOCAB_JSON, null)
            _savedVocab.value = if (!vocabJson.isNullOrBlank()) {
                try {
                    val arr = JSONArray(vocabJson)
                    val list = mutableListOf<SavedVocabWord>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            SavedVocabWord(
                                word = obj.getString("word"),
                                translation = obj.getString("translation"),
                                language = obj.optString("language", "English"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }

            val pinnedJson = prefs.getString(KEY_PINNED_MESSAGES_JSON, null)
            _pinnedMessages.value = if (!pinnedJson.isNullOrBlank()) {
                try {
                    val obj = JSONObject(pinnedJson)
                    val map = mutableMapOf<String, String>()
                    obj.keys().forEach { k ->
                        map[k] = obj.getString(k)
                    }
                    map
                } catch (_: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val groupsJson = prefs.getString(KEY_GROUPS_JSON, null)
            _groups.value = if (!groupsJson.isNullOrBlank()) {
                try {
                    val array = JSONArray(groupsJson)
                    val list = mutableListOf<LocalGroupChat>()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val memberNamesList = mutableListOf<String>()
                        val membersArr = obj.optJSONArray("members")
                        if (membersArr != null) {
                            for (j in 0 until membersArr.length()) {
                                memberNamesList.add(membersArr.getString(j))
                            }
                        }
                        list.add(
                            LocalGroupChat(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                description = obj.optString("desc", ""),
                                avatarUrl = obj.optString("avatar", ""),
                                memberCount = obj.optInt("count", memberNamesList.size.coerceAtLeast(1)),
                                memberNames = memberNamesList,
                                createdAt = obj.optLong("created", System.currentTimeMillis()),
                                lastMessageText = obj.optString("lastMsg", "Group created"),
                                lastMessageTime = obj.optLong("lastTime", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                } catch (e: Exception) {
                    android.util.Log.e("ChatPreferencesManager", "Error parsing groups json", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    fun resetForLogout() {
        synchronized(this) {
            initializedUid = null
            _mutedChats.value = emptySet()
            _pinnedChats.value = emptySet()
            _blockedUsers.value = emptySet()
            _deletedChats.value = emptySet()
            _unreadOverrideChats.value = emptySet()
            _isMinimalistMode.value = false
            _chatPreset.value = ChatPreset.Butter
            _chatTextScale.value = 1f
            _chatDarkThemeOverride.value = null
            _savedVocab.value = emptyList()
            _pinnedMessages.value = emptyMap()
            _groups.value = emptyList()
        }
    }

    fun wipeForUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        if (initializedUid == uid) {
            resetForLogout()
        }
        LegacySecurityMigration.deleteSharedPrefsCompat(
            context.applicationContext,
            ActiveUserManager.chatPrefsName(uid)
        )
    }

    fun isChatMuted(chatId: String): Boolean = _mutedChats.value.contains(chatId)

    fun toggleMuteChat(context: Context, chatId: String): Boolean {
        init(context)
        val current = _mutedChats.value.toMutableSet()
        val newStatus = if (current.contains(chatId)) {
            current.remove(chatId)
            false
        } else {
            current.add(chatId)
            true
        }
        _mutedChats.value = current
        saveStringSet(context, KEY_MUTED_CHATS, current)
        return newStatus
    }

    fun isChatPinned(chatId: String): Boolean = _pinnedChats.value.contains(chatId)

    fun togglePinChat(context: Context, chatId: String): Boolean {
        init(context)
        val current = _pinnedChats.value.toMutableSet()
        val newStatus = if (current.contains(chatId)) {
            current.remove(chatId)
            false
        } else {
            current.add(chatId)
            true
        }
        _pinnedChats.value = current
        saveStringSet(context, KEY_PINNED_CHATS, current)
        return newStatus
    }

    fun isUserBlocked(userId: String): Boolean = _blockedUsers.value.contains(userId)

    fun blockUser(context: Context, userId: String) {
        init(context)
        val current = _blockedUsers.value.toMutableSet()
        current.add(userId)
        _blockedUsers.value = current
        saveStringSet(context, KEY_BLOCKED_USERS, current)
    }

    fun unblockUser(context: Context, userId: String) {
        init(context)
        val current = _blockedUsers.value.toMutableSet()
        current.remove(userId)
        _blockedUsers.value = current
        saveStringSet(context, KEY_BLOCKED_USERS, current)
        unmarkChatDeleted(context, userId)
    }

    fun isChatDeleted(chatId: String): Boolean = _deletedChats.value.contains(chatId)

    fun markChatDeleted(context: Context, chatId: String) {
        init(context)
        val current = _deletedChats.value.toMutableSet()
        current.add(chatId)
        _deletedChats.value = current
        saveStringSet(context, KEY_DELETED_CHATS, current)
    }

    fun unmarkChatDeleted(context: Context, chatId: String) {
        init(context)
        val current = _deletedChats.value.toMutableSet()
        if (current.remove(chatId)) {
            _deletedChats.value = current
            saveStringSet(context, KEY_DELETED_CHATS, current)
        }
    }

    fun toggleUnreadOverride(context: Context, chatId: String): Boolean {
        init(context)
        val current = _unreadOverrideChats.value.toMutableSet()
        val isNowUnread = if (current.contains(chatId)) {
            current.remove(chatId)
            false
        } else {
            current.add(chatId)
            true
        }
        _unreadOverrideChats.value = current
        saveStringSet(context, KEY_UNREAD_OVERRIDE_CHATS, current)
        return isNowUnread
    }

    fun clearUnreadOverride(context: Context, chatId: String) {
        init(context)
        val current = _unreadOverrideChats.value.toMutableSet()
        if (current.remove(chatId)) {
            _unreadOverrideChats.value = current
            saveStringSet(context, KEY_UNREAD_OVERRIDE_CHATS, current)
        }
    }

    fun toggleMinimalistMode(context: Context): Boolean {
        init(context)
        val newState = !_isMinimalistMode.value
        _isMinimalistMode.value = newState
        getPrefsForActiveUser(context)?.edit()?.putBoolean(KEY_MINIMALIST_MODE, newState)?.apply()
        return newState
    }

    fun setMinimalistMode(context: Context, enabled: Boolean) {
        init(context)
        _isMinimalistMode.value = enabled
        getPrefsForActiveUser(context)?.edit()?.putBoolean(KEY_MINIMALIST_MODE, enabled)?.apply()
    }

    fun createGroup(
        context: Context,
        name: String,
        description: String = "",
        memberNames: List<String> = emptyList(),
        avatarUrl: String = "",
        groupId: String? = null
    ): LocalGroupChat {
        init(context)
        val newGroup = LocalGroupChat(
            id = groupId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            name = name,
            description = description,
            memberCount = (memberNames.size + 1).coerceAtLeast(2),
            memberNames = memberNames,
            avatarUrl = avatarUrl.ifBlank {
                "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200"
            }
        )
        val updated = listOf(newGroup) + _groups.value
        _groups.value = updated
        saveGroups(context, updated)
        return newGroup
    }

    fun deleteGroup(context: Context, groupId: String) {
        init(context)
        val updated = _groups.value.filterNot { it.id == groupId }
        _groups.value = updated
        saveGroups(context, updated)
    }

    private fun saveGroups(context: Context, groupsList: List<LocalGroupChat>) {
        try {
            val array = JSONArray()
            groupsList.forEach { grp ->
                val obj = JSONObject()
                obj.put("id", grp.id)
                obj.put("name", grp.name)
                obj.put("desc", grp.description)
                obj.put("avatar", grp.avatarUrl)
                obj.put("count", grp.memberCount)
                obj.put("created", grp.createdAt)
                obj.put("lastMsg", grp.lastMessageText)
                obj.put("lastTime", grp.lastMessageTime)
                val mArr = JSONArray()
                grp.memberNames.forEach { mArr.put(it) }
                obj.put("members", mArr)
                array.put(obj)
            }
            getPrefsForActiveUser(context)?.edit()?.putString(KEY_GROUPS_JSON, array.toString())?.apply()
        } catch (e: Exception) {
            android.util.Log.e("ChatPreferencesManager", "Error saving groups", e)
        }
    }

    private fun saveStringSet(context: Context, key: String, set: Set<String>) {
        getPrefsForActiveUser(context)?.edit()?.putStringSet(key, set)?.apply()
    }

    fun setChatPreset(context: Context, preset: ChatPreset) {
        init(context)
        _chatPreset.value = preset
        getPrefsForActiveUser(context)
            ?.edit()
            ?.putString(KEY_CHAT_PRESET, preset.name)
            ?.apply()
    }

    fun setChatTextScale(context: Context, scale: Float) {
        init(context)
        val clamped = scale.coerceIn(0.85f, 1.4f)
        _chatTextScale.value = clamped
        getPrefsForActiveUser(context)
            ?.edit()
            ?.putFloat(KEY_CHAT_TEXT_SCALE, clamped)
            ?.apply()
    }

    fun setChatDarkThemeOverride(context: Context, dark: Boolean?) {
        init(context)
        _chatDarkThemeOverride.value = dark
        val code = when (dark) {
            false -> 0
            true -> 1
            null -> -1
        }
        getPrefsForActiveUser(context)
            ?.edit()
            ?.putInt(KEY_CHAT_DARK_THEME, code)
            ?.apply()
    }

    fun saveVocabWord(context: Context, word: String, translation: String, language: String) {
        init(context)
        val clean = word.trim()
        if (clean.isEmpty()) return
        val updated = listOf(SavedVocabWord(clean, translation, language)) +
            _savedVocab.value.filterNot { it.word.equals(clean, ignoreCase = true) }
        _savedVocab.value = updated
        persistVocab(context, updated)
    }

    fun removeVocabWord(context: Context, word: String) {
        init(context)
        val updated = _savedVocab.value.filterNot { it.word.equals(word, ignoreCase = true) }
        _savedVocab.value = updated
        persistVocab(context, updated)
    }

    private fun persistVocab(context: Context, list: List<SavedVocabWord>) {
        try {
            val arr = JSONArray()
            list.forEach { item ->
                val obj = JSONObject()
                obj.put("word", item.word)
                obj.put("translation", item.translation)
                obj.put("language", item.language)
                obj.put("timestamp", item.timestamp)
                arr.put(obj)
            }
            getPrefsForActiveUser(context)
                ?.edit()
                ?.putString(KEY_VOCAB_JSON, arr.toString())
                ?.apply()
        } catch (_: Exception) {}
    }

    fun togglePinMessage(context: Context, chatId: String, messageId: String): Boolean {
        init(context)
        val current = _pinnedMessages.value.toMutableMap()
        val isNowPinned = if (current[chatId] == messageId) {
            current.remove(chatId)
            false
        } else {
            current[chatId] = messageId
            true
        }
        _pinnedMessages.value = current
        try {
            val obj = JSONObject()
            current.forEach { (k, v) -> obj.put(k, v) }
            getPrefsForActiveUser(context)
                ?.edit()
                ?.putString(KEY_PINNED_MESSAGES_JSON, obj.toString())
                ?.apply()
        } catch (_: Exception) {}
        return isNowPinned
    }
}
