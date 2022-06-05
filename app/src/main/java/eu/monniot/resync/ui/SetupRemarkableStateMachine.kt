package eu.monniot.resync.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.monniot.resync.rmcloud.Tokens
import eu.monniot.resync.rmcloud.exchangeCodeForDeviceToken
import eu.monniot.resync.rmcloud.saveTokens
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

// TODO Rename SetupRemarkableAccount when switching to Account from legacy preferences
@Composable
fun SetupRemarkableScreen(onDone: () -> Unit) {
    val (state, setState) = remember { mutableStateOf<SetupState>(SetupState.Init) }
    val content = LocalContext.current

    LaunchedEffect("setupRemarkableScreen") {
        setupLogic(content, setState)

        onDone()
    }

    when (state) {
        is SetupState.AccountSetUp -> AccountSetUp(onContinueClick = state.onContinueClick)
        is SetupState.EnterCode -> EnterCode(
            invalidCode = state.invalidCode,
            onCodeEntered = state.onCodeEntered
        )
        SetupState.ExchangingToken -> ExchangingToken()
        SetupState.Init -> {
            // The logic needs to set the callback, so what we do is that we use
            // this init state originally to render the screen and then swap it up
            // with the screen correctly configured. Hopefully that swap will
            // happen fast enough that the user will not be able to reach the dummy
            // callback below (nor will they enter values as they would be discarded)
            EnterCode(invalidCode = false, onCodeEntered = {})
        }
    }
}

sealed interface SetupState {
    object Init : SetupState
    data class EnterCode(
        val invalidCode: Boolean,
        val onCodeEntered: (String) -> Unit,
    ) : SetupState

    object ExchangingToken : SetupState
    data class AccountSetUp(val onContinueClick: () -> Unit) : SetupState
}

// TODO Extract Context out of the function so that testing it become easy
suspend fun setupLogic(
    context: Context,
    setState: (SetupState) -> Unit
) {
    var token: String? = null
    var invalidCode = false

    // Repeat steps 1 & 2 until we get a token
    // Note that the loop body is suspended, which is why this doesn't
    // loop between screen at lightning speed.
    while (token == null) {

        // 1. Get code from user
        val (codeEntered, onCodeEntered) = callbackToDeferred<String>()
        setState(SetupState.EnterCode(invalidCode, onCodeEntered))
        val code = codeEntered.await()

        // 2. Exchange code
        setState(SetupState.ExchangingToken)
        try {
            token = exchangeCodeForDeviceToken(code)
        } catch (e: Throwable) {
            // This will restart the loop and ask the user for a code, again
            invalidCode = true
            println("Exchanging code failed with exception: $e")
        }
    }

    // 3. Saving the token on the device
    setState(SetupState.ExchangingToken)
    val tokens = Tokens(token, user = null)
    saveTokens(context, tokens)

    // 4. Present the success screen
    val (continueClick, onContinueClick) = callbackToDeferredUnit()
    setState(SetupState.AccountSetUp(onContinueClick))
    continueClick.await()
}

// Is there a way to be generic on the number of parameters ? As it stands, that's
// the only difference with `callbackToDeferred`.
fun callbackToDeferredUnit(): Pair<Deferred<Unit>, () -> Unit> {
    val d = CompletableDeferred<Unit>()
    val c = { d.complete(Unit); Unit }

    return d to c
}

fun <T> callbackToDeferred(): Pair<Deferred<T>, (T) -> Unit> {
    val d = CompletableDeferred<T>()
    val c = { t: T -> d.complete(t); Unit }

    return d to c
}


@Composable
fun EnterCode(
    invalidCode: Boolean,
    onCodeEntered: (String) -> Unit,
) {

    var code by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.padding(16.dp, 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            "Connect a reMarkable account",
            style = MaterialTheme.typography.h5,
            textAlign = TextAlign.Center,
        )
        Text(
            "This application create epub files and upload them to your reMarkable account. To do so, you need to connect an account.",
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Column {
            Text(
                "Go to: my.remarkable.com",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://my.remarkable.com/device/mobile/connect")
                }
            )
            Text(
                "Use a web browser to access your one-time code",
                style = MaterialTheme.typography.body2
            )
        }

        Column {
            Text(
                "Enter code",
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )
            Text("Enter your one-time code here", style = MaterialTheme.typography.body2)

            val focusManager = LocalFocusManager.current
            TextField(
                value = code,
                onValueChange = {
                    when (it.length) {
                        in 0..7 -> {
                            code = it
                        }
                        8 -> {
                            code = it
                        }
                        else -> {
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // Hide the soft keyboard
                        focusManager.clearFocus()

                        onCodeEntered(code)
                    }
                )
            )
        }

        if (invalidCode) {
            Text(
                "The code entered previously was invalid. Please try again",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.error)
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Enter Code"
)
@Composable
fun EnterCodePreview() {
    ReSyncTheme {
        EnterCode(invalidCode = false, onCodeEntered = {})
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Enter Invalid Code"
)
@Composable
fun EnterCodeInvalidPreview() {
    ReSyncTheme {
        EnterCode(invalidCode = true, onCodeEntered = {})
    }
}

@Composable
fun ExchangingToken() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Connecting...", style = MaterialTheme.typography.h5)
        // TODO Add an indefinite linear progress bar
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Exchanging Code"
)
@Composable
fun ExchangingCodePreview() {
    ReSyncTheme {
        ExchangingToken()
    }
}

@Composable
fun AccountSetUp(onContinueClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(16.dp, 32.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text("Success!", style = MaterialTheme.typography.h5)
            Text(
                "You can now upload documents to your account.",
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
            )
        }

        Button(onClick = onContinueClick) {
            Text("Continue", style = MaterialTheme.typography.button)
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Account set up"
)
@Composable
fun AccountSetUpPreview() {
    ReSyncTheme {
        AccountSetUp {}
    }
}
