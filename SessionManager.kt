package com.example.letsgo

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val PREF_NAME = "LetsGoPrefs"
    private val KEY_EMAIL = "email"
    private val KEY_ROLE = "role"
    private val pref: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = pref.edit()

    fun saveLogin(email: String, role: String) {
        editor.putString(KEY_EMAIL, email)
        editor.putString(KEY_ROLE, role)
        editor.apply()
    }

    fun getEmail(): String? = pref.getString(KEY_EMAIL, null)
    fun getRole(): String? = pref.getString(KEY_ROLE, null)

    fun isLoggedIn(): Boolean = pref.contains(KEY_EMAIL)

    fun logout() {
        editor.clear()
        editor.apply()
    }
}
