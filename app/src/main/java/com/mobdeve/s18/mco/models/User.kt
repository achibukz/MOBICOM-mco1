package com.mobdeve.s18.mco.models

data class User(
    val id: String,
    var username: String,
    var passwordPlain: String,
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var mobile: String = "",
    var profileImageUri: String? = null
)
