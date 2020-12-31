package eu.monniot.resync.rmcloud

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private val DEVICE_TOKEN_FIELD = "deviceToken"
private val USER_TOKEN_FIELD = "userToken"

private fun openSharedPrefs(context: Context): SharedPreferences {
    val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    return EncryptedSharedPreferences.create(
        context,
        "rmcloud",
        mainKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

data class Tokens(val device: String, val user: String?) {}

fun readTokens(context: Context): Tokens? {
    val sharedPreferences = openSharedPrefs(context)

    val device = sharedPreferences.getString(DEVICE_TOKEN_FIELD, null)

    return if (device == null) {
        null
    } else {
        val user = sharedPreferences.getString(USER_TOKEN_FIELD, null)

        Tokens(device, user)
    }
}

fun saveTokens(context: Context, tokens: Tokens) {

    val sharedPreferences = openSharedPrefs(context)

    with(sharedPreferences.edit()) {
        putString(DEVICE_TOKEN_FIELD, tokens.device)
        if (tokens.user != null) {
            putString(USER_TOKEN_FIELD, tokens.user)
        }

        apply()
    }

}