package com.rork.safetygembawalk.data

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UserRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }
    private val db = FirebaseFirestore.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: Flow<User?> = _currentUser.asStateFlow()

    init {
        val userJson = prefs.getString(KEY_CURRENT_USER, null)
        _currentUser.value = userJson?.let { json.decodeFromString(it) }

        initializeDefaultAdmin()

        _currentUser.value?.let {
            refreshUserRoleFromFirebase(it)
        }
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
        val user = users.find {
            it.email.equals(email, ignoreCase = true) && it.password == password
        }

        return if (user != null) {
            saveCurrentUser(user)
            refreshUserRoleFromFirebase(user)
            Result.success(user)
        } else {
            Result.failure(Exception("Email ou senha incorretos"))
        }
    }

    fun register(user: User): Result<User> {
        val users = getAllUsers().toMutableList()

        if (users.any { it.email.equals(user.email, ignoreCase = true) }) {
            return Result.failure(Exception("Email já cadastrado"))
        }

        users.add(user)
        saveUsersList(users)
        saveCurrentUser(user)
        refreshUserRoleFromFirebase(user)

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

    private fun refreshUserRoleFromFirebase(user: User) {
        db.collection("users")
            .whereEqualTo("email", user.email)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()

                if (doc != null) {
                    val role = doc.getString("role") ?: "user"
                    val area = doc.getString("area") ?: user.area
                    val fullName = doc.getString("fullName") ?: user.fullName

                    val nameParts = fullName.trim().split(" ")
                    val firstName = nameParts.firstOrNull() ?: user.firstName
                    val lastName = nameParts.drop(1).joinToString(" ")
                        .ifBlank { user.lastName }

                    val updatedUser = user.copy(
                        firstName = firstName,
                        lastName = lastName,
                        area = area,
                        isAdmin = role.equals("admin", ignoreCase = true)
                    )

                    saveCurrentUser(updatedUser)
                    updateUserInLocalList(updatedUser)
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
            }
    }

    private fun updateUserInLocalList(updatedUser: User) {
        val users = getAllUsers().toMutableList()
        val index = users.indexOfFirst {
            it.email.equals(updatedUser.email, ignoreCase = true)
        }

        if (index >= 0) {
            users[index] = updatedUser
        } else {
            users.add(updatedUser)
        }

        saveUsersList(users)
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
