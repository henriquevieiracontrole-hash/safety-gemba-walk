package com.rork.safetygembawalk.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class UserRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()
    
    init {
        // Load current user on init
        val userJson = prefs.getString(KEY_CURRENT_USER, null)
        _currentUser.value = userJson?.let { json.decodeFromString(it) }
        
        // Initialize default admin if first run
        initializeDefaultAdmin()
    }
    
    private fun initializeDefaultAdmin() {
        if (prefs.getBoolean(KEY_IS_FIRST_RUN, true)) {
            val users = mutableListOf(DEFAULT_ADMIN_USER)
            saveUsersList(users)
            prefs.edit().putBoolean(KEY_IS_FIRST_RUN, false).apply()
        }
    }
    
    fun getCurrentUser(): User? {
        return _currentUser.value
    }
    
    fun login(email: String, password: String): Result<User> {
        val users = getAllUsers()
        val user = users.find { it.email == email && it.password == password }
        
        return if (user != null) {
            saveCurrentUser(user)
            Result.success(user)
        } else {
            Result.failure(Exception("Email ou senha incorretos"))
        }
    }
    
    fun register(user: User): Result<User> {
        val users = getAllUsers().toMutableList()
        
        // Check if email already exists
        if (users.any { it.email == user.email }) {
            return Result.failure(Exception("Email já cadastrado"))
        }
        
        users.add(user)
        saveUsersList(users)
        saveCurrentUser(user)
        
        return Result.success(user)
    }
    
    fun logout() {
        prefs.edit().remove(KEY_CURRENT_USER).apply()
        _currentUser.value = null
    }
    
    private fun saveCurrentUser(user: User) {
        prefs.edit().putString(KEY_CURRENT_USER, json.encodeToString(user)).apply()
        _currentUser.value = user
    }
    
    private fun getAllUsers(): List<User> {
        val usersJson = prefs.getString(KEY_USERS_LIST, null)
        return usersJson?.let { json.decodeFromString(it) } ?: emptyList()
    }
    
    private fun saveUsersList(users: List<User>) {
        prefs.edit().putString(KEY_USERS_LIST, json.encodeToString(users)).apply()
    }
    
    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_CURRENT_USER = "current_user"
        private const val KEY_USERS_LIST = "users_list"
        private const val KEY_IS_FIRST_RUN = "is_first_run"
    }
}
