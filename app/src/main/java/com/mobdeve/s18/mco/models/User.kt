package com.mobdeve.s18.mco.models

// 1. MAKE SURE THESE IMPORTS ARE HERE
import androidx.room.Entity
import androidx.room.PrimaryKey

// 2. ADD @ENTITY ANNOTATION
@Entity(tableName = "users")
data class User(
    // 3. ADD @PRIMARYKEY ANNOTATION
    @PrimaryKey val id: String,

    var username: String,
    var passwordPlain: String,
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var mobile: String = "",
    var profileImageUri: String? = null
)