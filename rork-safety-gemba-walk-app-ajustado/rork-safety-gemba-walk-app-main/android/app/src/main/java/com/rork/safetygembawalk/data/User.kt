package com.rork.safetygembawalk.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val area: String,
    val isAdmin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$firstName $lastName"
}

val PREDEFINED_AREAS = listOf(
    "JC2",
    "TAB", 
    "J1",
    "Logistica",
    "Eletrica",
    "Producao",
    "Manutencao",
    "Qualidade",
    "Seguranca",
    "Meio Ambiente"
)

// Default admin user - will be created on first run
val DEFAULT_ADMIN_USER = User(
    id = "admin_default",
    firstName = "Administrador",
    lastName = "Ahlstrom",
    email = "admin@ahlstrom.com",
    password = "admin123",
    area = "Seguranca",
    isAdmin = true
)
