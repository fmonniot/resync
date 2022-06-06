package eu.monniot.resync.ui.launcher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.monniot.resync.rmcloud.Account
import eu.monniot.resync.rmcloud.AccountId
import eu.monniot.resync.rmcloud.PreferencesManager
import eu.monniot.resync.rmcloud.PreferencesManager.Companion.Preferences
import eu.monniot.resync.rmcloud.PreferencesManager.Companion.UploadMethod


@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val manager = remember { PreferencesManager.create(context) }

    val preferences by manager.watchPreferences()
        .collectAsState(initial = manager.readPreferences())

    SettingsView(
        preferences,
        uploadViaShareChange = {
            val method = if (it) UploadMethod.Share else UploadMethod.Direct
            manager.changeUploadMethod(method)
        },
        accountSelect = manager::changeCurrentAccount,
        accountRename = manager::renameAccount,
        addAccount = {
            /*
             TODO Display the SetupRemarkableState

             Improve the Setup screen by asking for an account name at the end.
             Then make sure that the `onDone` callback do return the name and
             tokens. Once done, we can call the manager's addAccount method
             and we now have a new account ready to be used.

             Idea: Make accounts entirely optional. Consolidation wouldn't be
             possible, and epub upload would be through the rm app (via android
             share mechanism, which should work for pdf & epub).

             Sounds like a good idea actually. And would save me the trouble of
             debugging rm new upload flow. Need to put a disclaimer before
             upload that 1.5 is untested and should be used at the user risks.
             */
        }
    )
}

// TODO Understand how adding a new account will work
@Composable
fun SettingsView(
    preferences: Preferences,
    uploadViaShareChange: (Boolean) -> Unit,
    accountSelect: (AccountId) -> Unit,
    accountRename: (AccountId, String) -> Unit,
    addAccount: () -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {

        SettingsMenuLine(
            title = { Text("Upload via share") },
            subtitle = {
                val txt =
                    if (preferences.uploadMethod == UploadMethod.Share)
                        "Upload use rM app via Android Share"
                    else
                        "Upload will use Cloud directly"

                Text(txt)
            },
            action = {
                Checkbox(checked = preferences.uploadMethod == UploadMethod.Share,
                    onCheckedChange = uploadViaShareChange)
            },
            onClick = {}
        )

        SettingsGroup(title = "reMarkable Account") {

            val (editAccount, setEditAccount) = remember { mutableStateOf<Account?>(null) }

            if (editAccount != null) {
                AccountEditDialog(account = editAccount,
                    onDismiss = { setEditAccount(null) },
                    onConfirm = { id, name -> accountRename(id, name); setEditAccount(null) })
            }

            preferences.accounts.forEach { account ->
                SettingsMenuLine(title = { Text(account.name) },
                    subtitle = if (account.tokens == null) {
                        { Text("Not set up") }
                    } else {
                        {}
                    },
                    action = {
                        RadioButton(selected = account.active,
                            onClick = { accountSelect(account.id) })
                    },
                    onClick = {
                        setEditAccount(account)
                    })
            }

            SettingsMenuLine(
                title = { Text("Add another account (TODO)") },
                onClick = addAccount
            )
        }
    }
}

@Composable
fun AccountEditDialog(
    account: Account,
    onDismiss: () -> Unit,
    onConfirm: (AccountId, String) -> Unit,
) {

    val (name, updateName) = remember { mutableStateOf(account.name) }

    // TODO Account deletion
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {
        TextButton(onClick = { onConfirm(account.id, name) }) {
            Text("Confirm")
        }
    }, dismissButton = {
        TextButton(onClick = onDismiss) {
            Text("Cancel")
        }
    }, title = { Text("Account edition") }, text = {
        TextField(
            label = { Text("Account Name") }, value = name, onValueChange = updateName
        )
    })
}

@Composable
fun SettingsGroup(
    title: String, content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Group title

        Divider(Modifier.padding(top=8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(start = (16 + 40 + 8).dp, 8.dp, 16.dp, 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {


            val primary = MaterialTheme.colors.primary
            val titleStyle = MaterialTheme.typography.subtitle2.copy(color = primary)
            Text(style = titleStyle, text = title)
        }

        // Content
        content()
    }
}

@Composable
fun SettingsMenuLine(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsTileTexts(
                title = title,
                subtitle = subtitle,
                modifier = Modifier
                    .padding(start = (16 + 40 + 8).dp)
                    .fillMaxHeight()
            )
        }
        if (action != null) {

            SettingsTileAction {
                action.invoke()
            }
        }
    }
}

@Composable
internal fun SettingsTileTitle(title: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.subtitle1) {
        title()
    }
}

@Composable
internal fun SettingsTileSubtitle(subtitle: @Composable () -> Unit) {
    ProvideTextStyle(value = MaterialTheme.typography.caption) {
        CompositionLocalProvider(
            LocalContentAlpha provides ContentAlpha.medium, content = subtitle
        )
    }
}

@Composable
internal fun RowScope.SettingsTileTexts(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    subtitle: @Composable (() -> Unit)?,
) {
    Column(
        modifier = modifier.weight(1f),
        verticalArrangement = Arrangement.Center,
    ) {
        SettingsTileTitle(title)
        if (subtitle != null) {
            Spacer(modifier = Modifier.size(2.dp))
            SettingsTileSubtitle(subtitle)
        }
    }
}

@Composable
internal fun SettingsTileAction(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
)
@Composable
internal fun SettingsScreenPreview() {
    val prefs = Preferences(UploadMethod.Share, Account.samples)

    MaterialTheme {
        SettingsView(prefs, {}, {}, { _, _ -> }, {})
    }
}
