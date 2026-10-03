package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
    private const val PREFS_NAME = "funkytalk_chat_prefs"
    private const val KEY_MUTED_CHATS = "muted_chat_ids"
    private const val KEY_PINNED_CHATS = "pinned_chat_ids"
    private const val KEY_BLOCKED_USERS = "blocked_user_ids"
    private const val KEY_DELETED_CHATS = "deleted_chat_ids"
    private const val KEY_UNREAD_OVERRIDE_CHATS = "unread_override_chat_ids"
    private const val KEY_GROUPS_JSON = "custom_groups_json"

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

    private val _groups = MutableStateFlow<List<LocalGroupChat>>(emptyList())
    val groups: StateFlow<List<LocalGroupChat>> = _groups.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _mutedChats.value = prefs.getStringSet(KEY_MUTED_CHATS, emptySet()) ?: emptySet()
        _pinnedChats.value = prefs.getStringSet(KEY_PINNED_CHATS, emptySet()) ?: emptySet()
        _blockedUsers.value = prefs.getStringSet(KEY_BLOCKED_USERS, emptySet()) ?: emptySet()
        _deletedChats.value = prefs.getStringSet(KEY_DELETED_CHATS, emptySet()) ?: emptySet()
        _unreadOverrideChats.value = prefs.getStringSet(KEY_UNREAD_OVERRIDE_CHATS, emptySet()) ?: emptySet()

        val groupsJson = prefs.getString(KEY_GROUPS_JSON, null)
        if (!groupsJson.isNullOrBlank()) {
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
                _groups.value = list
            } catch (e: Exception) {
                android.util.Log.e("ChatPreferencesManager", "Error parsing groups json", e)
            }
        }
    }

    fun isChatMuted(chatId: String): Boolean = _mutedChats.value.contains(chatId)

    fun toggleMuteChat(context: Context, chatId: String): Boolean {
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
        val current = _blockedUsers.value.toMutableSet()
        current.add(userId)
        _blockedUsers.value = current
        saveStringSet(context, KEY_BLOCKED_USERS, current)
    }

    fun unblockUser(context: Context, userId: String) {
        val current = _blockedUsers.value.toMutableSet()
        current.remove(userId)
        _blockedUsers.value = current
        saveStringSet(context, KEY_BLOCKED_USERS, current)
        unmarkChatDeleted(context, userId)
    }

    fun isChatDeleted(chatId: String): Boolean = _deletedChats.value.contains(chatId)

    fun markChatDeleted(context: Context, chatId: String) {
        val current = _deletedChats.value.toMutableSet()
        current.add(chatId)
        _deletedChats.value = current
        saveStringSet(context, KEY_DELETED_CHATS, current)
    }

    fun unmarkChatDeleted(context: Context, chatId: String) {
        val current = _deletedChats.value.toMutableSet()
        if (current.remove(chatId)) {
            _deletedChats.value = current
            saveStringSet(context, KEY_DELETED_CHATS, current)
        }
    }

    fun toggleUnreadOverride(context: Context, chatId: String): Boolean {
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
        val current = _unreadOverrideChats.value.toMutableSet()
        if (current.remove(chatId)) {
            _unreadOverrideChats.value = current
            saveStringSet(context, KEY_UNREAD_OVERRIDE_CHATS, current)
        }
    }

    fun createGroup(
        context: Context,
        name: String,
        description: String = "",
        memberNames: List<String> = emptyList(),
        avatarUrl: String = "",
        groupId: String? = null
    ): LocalGroupChat {
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
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_GROUPS_JSON, array.toString()).apply()
        } catch (e: Exception) {
            android.util.Log.e("ChatPreferencesManager", "Error saving groups", e)
        }
    }

    private fun saveStringSet(context: Context, key: String, set: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(key, set).apply()
    }
}
