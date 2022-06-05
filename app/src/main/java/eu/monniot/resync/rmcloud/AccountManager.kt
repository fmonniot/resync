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

class AccountManager private constructor(private val sharedPreferences: SharedPreferences) {


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

        // That might not work very well when there is no accounts set up.
        // I'm ok with it for now, mainly because LauncherActivity requires to set up at least
        // one account. I do need to improve the Account management in the future, it's too
        // brittle right now and works only by accident (or rather, by following the exact same
        // steps I have followed on my phone).
        return (0..nb).map { idx ->
            val name = sharedPreferences.getString(accountNameField(idx), null) ?: "No name"
            val device = sharedPreferences.getString(deviceTokenField(idx), null)
            val user = sharedPreferences.getString(userTokenField(idx), null)

            Account(
                name,
                AccountId(idx),
                Tokens.fromStrings(device, user),
                idx == selectedIndex
            )
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
            apply()

            next
        }

        return Account(name, AccountId(accountId), null, false)
    }

    fun changeCurrentAccount(id: AccountId) {

    }

    fun renameAccount(id: AccountId, name: String) {
        with(sharedPreferences.edit()) {
            putString(accountNameField(id.raw), name)

            apply()
        }
    }

    fun updateTokens(id: AccountId, tokens: Tokens): Account {
        TODO()
    }

    companion object {
        fun create(context: Context): AccountManager {

            return AccountManager(openSharedPrefs(context))
        }
    }
}

data class Account(val name: String, val id: AccountId, val tokens: Tokens?, val active: Boolean) {

    companion object {
        val samples = listOf(
            Account("Personal Account", AccountId(1), null, true),
            Account("Test Account", AccountId(2), null, false)
        )
    }
}

// New type over the account index. Only the account manager should see
// the raw int and −more importantly− be able to create such ids.
@JvmInline
value class AccountId internal constructor(internal val raw: Int)
