package eu.monniot.resync.rmcloud

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val ACCOUNT_NUMBERS = "rmAccountNumbers"
private const val ACCOUNT_INDEX = "rmAccountIndex"
private const val LEGACY_DEVICE_TOKEN_FIELD = "deviceToken"
private const val LEGACY_USER_TOKEN_FIELD = "userToken"

private fun accountNameField(index: Int) = "accountName-${index}"
private fun deviceTokenField(index: Int) = "deviceToken-${index}"
private fun userTokenField(index: Int) = "userToken-${index}"

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

data class Tokens(val device: String, val user: String?) {

    companion object {
        internal fun fromStrings(device: String?, user: String?): Tokens? {
            return if (device == null) null
            else Tokens(device, user)
        }
    }
}

// Read the reMarkable tokens based on the current selected account.
fun readTokens(context: Context): Pair<String, Tokens?> {
    val sharedPreferences = openSharedPrefs(context)

    var index = sharedPreferences.getInt(ACCOUNT_INDEX, -1)
    println("Read tokens index = $index")

    if (index < 0) {
        // Old preferences, when we didn't had account. Let's migrate to the new keys
        val device = sharedPreferences.getString(LEGACY_DEVICE_TOKEN_FIELD, null)
        val user =
            if (device != null) sharedPreferences.getString(LEGACY_USER_TOKEN_FIELD, null) else null

        println("readTokens: legacy device = $device; legacy user = $user")

        with(sharedPreferences.edit()) {
            putInt(ACCOUNT_INDEX, 0)
            putInt(ACCOUNT_NUMBERS, 1)
            putString(accountNameField(0), "Default")
            putString(deviceTokenField(0), device)
            putString(userTokenField(0), user)

            apply()
        }

        index = 0
    }

    val name = sharedPreferences.getString(accountNameField(index), null) ?: "No name"
    val device = sharedPreferences.getString(deviceTokenField(index), null)
    val user = sharedPreferences.getString(userTokenField(index), null)


    return name to Tokens.fromStrings(device, user)
}

fun addAccount(context: Context, index: Int, name: String) {
    val sharedPreferences = openSharedPrefs(context)

    val nb = sharedPreferences.getInt(ACCOUNT_NUMBERS, 0)

}

// TODO Need to be updated to work with multi accounts
fun saveTokens(context: Context, tokens: Tokens) {

    val sharedPreferences = openSharedPrefs(context)

    with(sharedPreferences.edit()) {
        putString(LEGACY_DEVICE_TOKEN_FIELD, tokens.device)
        if (tokens.user != null) {
            putString(LEGACY_USER_TOKEN_FIELD, tokens.user)
        }

        apply()
    }

}