package eu.monniot.resync.rmcloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow


private const val TAG = "AccountManager"
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


    fun watchAccounts(): Flow<List<Account>> {
        return channelFlow {
            val listener: SharedPreferences.OnSharedPreferenceChangeListener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    trySend(listAccounts())

                    Unit
                }

            trySend(listAccounts())
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
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

    companion object {
        fun create(context: Context): PreferencesManager {

            return PreferencesManager(openSharedPrefs(context))
        }
    }
}

data class Tokens(val device: String, val user: String?) {

    companion object {
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
