package com.rork.safetygembawalk.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rork.safetygembawalk.data.User
import com.rork.safetygembawalk.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val user: User? = null,
    val errorMessage: String? = null,
    val isFirstRun: Boolean = false
)

sealed interface AuthAction {
    data class Login(val email: String, val password: String) : AuthAction
    data class Register(
        val firstName: String,
        val lastName: String,
        val email: String,
        val password: String,
        val area: String
    ) : AuthAction
    data object Logout : AuthAction
    data object CheckAuthStatus : AuthAction
    data object ClearError : AuthAction
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository(application)
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkAuthStatus()
    }
    
    fun onAction(action: AuthAction) {
        when (action) {
            is AuthAction.Login -> login(action.email, action.password)
            is AuthAction.Register -> register(
                action.firstName,
                action.lastName,
                action.email,
                action.password,
                action.area
            )
            is AuthAction.Logout -> logout()
            is AuthAction.CheckAuthStatus -> checkAuthStatus()
            is AuthAction.ClearError -> clearError()
        }
    }
    
    private fun login(email: String, password: String) {
        _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
        
        repository.login(email, password)
            .onSuccess { user ->
                _authState.value = AuthState(
                    isLoading = false,
                    isAuthenticated = true,
                    user = user
                )
            }
            .onFailure { error ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Erro ao fazer login"
                )
            }
    }
    
    private fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        area: String
    ) {
        _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)
        
        // Validate inputs
        if (firstName.isBlank() || lastName.isBlank()) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                errorMessage = "Nome e sobrenome são obrigatórios"
            )
            return
        }
        
        if (email.isBlank() || !email.contains("@")) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                errorMessage = "Email inválido"
            )
            return
        }
        
        if (password.length < 6) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                errorMessage = "Senha deve ter no mínimo 6 caracteres"
            )
            return
        }
        
        if (area.isBlank()) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                errorMessage = "Selecione uma área"
            )
            return
        }
        
        val user = User(
            id = UUID.randomUUID().toString(),
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email.trim().lowercase(),
            password = password,
            area = area
        )
        
        repository.register(user)
            .onSuccess { registeredUser ->
                _authState.value = AuthState(
                    isLoading = false,
                    isAuthenticated = true,
                    user = registeredUser
                )
            }
            .onFailure { error ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Erro ao criar conta"
                )
            }
    }
    
    private fun logout() {
        repository.logout()
        _authState.value = AuthState()
    }
    
    private fun checkAuthStatus() {
        val user = repository.getCurrentUser()
        _authState.value = AuthState(
            isAuthenticated = user != null,
            user = user
        )
    }
    
    private fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
}
