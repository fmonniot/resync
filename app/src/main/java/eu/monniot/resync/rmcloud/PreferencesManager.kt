package eu.monniot.resync.rmcloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.*


private const val TAG = "AccountManager"
private const val UPLOAD_METHOD = "rmUploadMethod"
private const val ACCOUNT_NUMBERS = "rmAccountNumbers"
private const val ACCOUNT_INDEX = "rmAccountIndex"

private fun accountNameField(index: Int) = "accountName-${index}"
private fun deviceTokenField(index: Int) = "deviceToken-${index}"
private fun userTokenField(index: Int) = "userToken-${index}"

private fun openSharedPrefs(context: Context): SharedPreferences {
    val mainKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    return EncryptedSharedPreferences.create(context,
        "rmcloud",
        mainKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
}

class PreferencesManager private constructor(private val sharedPreferences: SharedPreferences) {

    // TODO watchPreferences
    fun watchPreferences(): Flow<Preferences> {
        return channelFlow {
            val listener: SharedPreferences.OnSharedPreferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    trySend(readPreferences())

                    Unit
                }

            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
    }

    fun readPreferences(): Preferences {
        val accounts = listAccounts()
        val uploadMethod = readUploadMethod()

        return Preferences(uploadMethod, accounts)
    }

    private fun listAccounts(): List<Account> {
        val nb = sharedPreferences.getInt(ACCOUNT_NUMBERS, 0)
        val selectedIndex = sharedPreferences.getInt(ACCOUNT_INDEX, -1)

        Log.d(TAG, "ACCOUNT_NUMBERS = $nb; ACCOUNT_INDEX = $selectedIndex")

        // Special casing when there is no accounts because the range below include
        // nb (eg. nb = 0 would result in 0 being read, but we start at 1)
        if (nb < 1) {
            return emptyList()
        }

        // Accounts id/indices are 1-based
        return (1..nb).map { idx ->
            val name = sharedPreferences.getString(accountNameField(idx), null)
            val device = sharedPreferences.getString(deviceTokenField(idx), null)
            val user = sharedPreferences.getString(userTokenField(idx), null)

            Log.d(TAG,
                "Reading accounts: idx=$idx; name=$name; device=${device?.let { "redacted" }}; user=${user?.let { "redacted" }}")

            Account(name ?: "No name",
                AccountId(idx),
                Tokens.fromStrings(device, user),
                idx == selectedIndex)
        }
    }

    // Used for the consolidate screen, where documents are based on the current account
    fun watchCurrentAccount(): Flow<Account> {
        TODO()
    }

    fun readCurrentAccount(): Account {
        val index = sharedPreferences.getInt(ACCOUNT_INDEX, 0)
        Log.d(TAG, "current account index is $index")

        val name = sharedPreferences.getString(accountNameField(index), null) ?: "No name"
        val device = sharedPreferences.getString(deviceTokenField(index), null)
        val user = sharedPreferences.getString(userTokenField(index), null)

        return Account(name, AccountId(index), Tokens.fromStrings(device, user), true)
    }

    fun addAccount(name: String, tokens: Tokens): Account {
        val accountId = with(sharedPreferences.edit()) {
            val index = sharedPreferences.getInt(ACCOUNT_NUMBERS, 0)
            val next = index + 1

            putInt(ACCOUNT_NUMBERS, next)
            putString(accountNameField(next), name)
            putString(deviceTokenField(next), tokens.device)
            putString(userTokenField(next), tokens.user)
            apply()

            next
        }

        return Account(name, AccountId(accountId), null, false)
    }

    fun changeCurrentAccount(id: AccountId) {
        with(sharedPreferences.edit()) {
            putInt(ACCOUNT_INDEX, id.raw)

            apply()
        }
    }

    fun renameAccount(id: AccountId, name: String) {
        with(sharedPreferences.edit()) {
            putString(accountNameField(id.raw), name)

            apply()
        }
    }

    fun readUploadMethod(): UploadMethod {
        return when (val raw = sharedPreferences.getString(UPLOAD_METHOD, "share")) {
            "share" -> UploadMethod.Share
            "direct" -> UploadMethod.Direct
            else -> throw java.lang.IllegalStateException("Unknown method $raw")
        }
    }

    fun changeUploadMethod(method: UploadMethod) {
        val raw = when (method) {
            UploadMethod.Direct -> "direct"
            UploadMethod.Share -> "share"
        }

        with(sharedPreferences.edit()) {
            putString(UPLOAD_METHOD, raw)
            apply()
        }
    }

    companion object {
        enum class UploadMethod { Direct, Share }

        data class Preferences(val uploadMethod: UploadMethod, val accounts: List<Account>)

        fun create(context: Context): PreferencesManager {

            return PreferencesManager(openSharedPrefs(context))
        }
    }
}

data class Tokens(val device: String, val user: String?) {

    val scopes by lazy {
        if (user != null) {

            val parts = user.split(".")
            val payload: ByteArray = Base64.getDecoder().decode(parts[1])
            val str = String(payload)

            // From my experience, fromJson does NOT return null but instead throw…
            // So don't think you are safe because you have handled a null young padawan…
            val scopes = Moshi.Builder()
                .build()
                .adapter(JwtPayload::class.java)
                .fromJson(str)
                ?.scopes

            scopes?.split(" ") ?: emptyList()
        } else {
            null
        }
    }

    /**
     * Parse the user token, look at the claims' scopes and see if one
     * sync:fox, sync:tortoise or sync:hare is present. If yes, that's
     * protocol 1.5.
     */
    fun is15Account() =
        scopes?.any { it == "sync:fox" || it == "sync:tortoise" || it == "sync:hare" } ?: false

    companion object {
        internal data class JwtPayload(val scopes: String)

        internal fun fromStrings(device: String?, user: String?): Tokens? {
            return if (device == null) null
            else Tokens(device, user)
        }
    }
}

data class Account(val name: String, val id: AccountId, val tokens: Tokens?, val active: Boolean) {

    companion object {
        val samples = listOf(Account("Personal Account", AccountId(1), null, true),
            Account("Test Account", AccountId(2), null, false))
    }
}

// New type over the account index. Only the account manager should see
// the raw int and −more importantly− be able to create such ids.
@JvmInline
value class AccountId internal constructor(internal val raw: Int)
