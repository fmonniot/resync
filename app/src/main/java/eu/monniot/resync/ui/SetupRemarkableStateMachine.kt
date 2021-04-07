package eu.monniot.resync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.tapGestureFilter
import androidx.compose.ui.platform.AmbientContext
import androidx.compose.ui.platform.AmbientUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.monniot.resync.rmcloud.Tokens
import eu.monniot.resync.rmcloud.exchangeCodeForDeviceToken
import eu.monniot.resync.rmcloud.saveTokens
import java.lang.RuntimeException

@Composable
fun SetupRemarkableScreen(onDone: (Tokens) -> Unit) {
    val state: MutableState<SetupRemarkableStateMachine> = remember {
        mutableStateOf(
            SetupRemarkableStateMachine.EnterCode(invalidCode = false, onDone)
        )
    }

    val screen = state.value
    screen.View(state = state)
}

private sealed class SetupRemarkableStateMachine {

    @Composable
    abstract fun View(state: MutableState<SetupRemarkableStateMachine>)

    class EnterCode(
        private val invalidCode: Boolean,
        private val onDone: (Tokens) -> Unit,
    ) : SetupRemarkableStateMachine() {

        @Composable
        override fun View(state: MutableState<SetupRemarkableStateMachine>) {

            var code by remember { mutableStateOf("") }
            val uriHandler = AmbientUriHandler.current

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
                        modifier = Modifier.tapGestureFilter {
                            uriHandler.openUri("https://my.remarkable.com/connect/mobile")
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
                        onImeActionPerformed = { action, controller ->
                            println("debug: ime action = $action")
                            controller?.hideSoftwareKeyboard()

                            state.value = ExchangeCode(code, onDone)
                        }
                    )
                }

                if(invalidCode) {
                    Text(
                        "The code entered previously was invalid. Please try again",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.error)
                    )
                }
            }
        }
    }

    class ExchangeCode(
        private val code: String,
        private val onDone: (Tokens) -> Unit,
    ) : SetupRemarkableStateMachine() {

        @Composable
        override fun View(state: MutableState<SetupRemarkableStateMachine>) {
            val context = AmbientContext.current

            LaunchedEffect(key1 = "exchangingCode") {
                val token =
                    try {
                        exchangeCodeForDeviceToken(code)
                    } catch (e: Throwable) {
                        println("Exchanging code failed with exception: $e")
                        state.value = EnterCode(invalidCode = true, onDone)
                        return@LaunchedEffect
                    }

                val tokens = Tokens(token, user = null)

                saveTokens(context, tokens)

                state.value = TokenSaved(tokens, onDone)
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Connecting...", style = MaterialTheme.typography.h5)
            }
        }
    }

    class TokenSaved(
        private val tokens: Tokens,
        private val onDone: (Tokens) -> Unit,
    ) : SetupRemarkableStateMachine() {
        @Composable
        override fun View(state: MutableState<SetupRemarkableStateMachine>) {
            Column(
                modifier = Modifier.padding(16.dp, 32.dp).fillMaxHeight(),
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

                Button(onClick = { onDone(tokens) }) {
                    Text("Continue", style = MaterialTheme.typography.button)
                }
            }
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
    val state: MutableState<SetupRemarkableStateMachine> = remember {
        mutableStateOf(SetupRemarkableStateMachine.EnterCode(invalidCode = false) {})
    }

    state.value.View(state = state)
}


@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Exchanging Code"
)
@Composable
fun ExchangingCodePreview() {
    val state: MutableState<SetupRemarkableStateMachine> = remember {
        mutableStateOf(SetupRemarkableStateMachine.ExchangeCode("abcdefgh") {})
    }

    state.value.View(state = state)
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Account set up"
)
@Composable
fun AccountSetupPreview() {
    val state: MutableState<SetupRemarkableStateMachine> = remember {
        mutableStateOf(SetupRemarkableStateMachine.TokenSaved(Tokens("", null)) {})
    }

    state.value.View(state = state)
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3,
    showSystemUi = true,
    name = "Enter Code (Failed)"
)
@Composable
fun EnterCodeFailedPreview() {
    val state: MutableState<SetupRemarkableStateMachine> = remember {
        mutableStateOf(SetupRemarkableStateMachine.EnterCode(invalidCode = true) {})
    }

    state.value.View(state = state)
}