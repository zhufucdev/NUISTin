import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.awt.Dimension

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()

    var id by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remember by remember { mutableStateOf(true) }
    var autologin by remember { mutableStateOf(false) }
    var carrier by remember { mutableStateOf(Carrier.MOBILE) }

    var carrierExpended by remember { mutableStateOf(false) }
    var idExpended by remember { mutableStateOf(false) }
    var idInputError by remember { mutableStateOf("") }
    var pwdInputError by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val scaffoldState = rememberScaffoldState()
    var autoLoginExecuted by remember { mutableStateOf(false) }

    fun currentAccount() = Account(id, password, carrier, remember, autologin && remember)

    fun validateInput(): Boolean {
        var valid = true
        if (password.isEmpty()) {
            pwdInputError = "密码不能为空"
            valid = false
        }
        if (id.isEmpty()) {
            idInputError = "ID不能为空"
            valid = false
        }
        return valid
    }

    fun useState(account: Account) {
        id = account.id
        password = account.password
        carrier = account.carrier
        autologin = account.autostart
        remember = account.remember
    }

    val loginHandler: () -> Unit = {
        if (validateInput()) {
            working = true
            val account = currentAccount()
            scope.launch {
                val result = Handler.login(account)
                working = false
                scaffoldState.snackbarHostState.showSnackbar(
                    when (result) {
                        LoginResult.IP_FAILURE -> "无法获取IP地址"
                        LoginResult.LOGIN_FAILURE -> "登录服务器拒绝了我们的请求"
                        LoginResult.TIMEOUT -> "请求超时"
                        LoginResult.SUCCESS -> "成功登录"
                        LoginResult.EXCEPTION -> "内部错误"
                    }
                )

                if (result == LoginResult.SUCCESS) {
                    Handler.store(account)
                    Handler.preferences.recentAccount = account.id
                }
            }
        }
    }

    if (!autoLoginExecuted) {
        Handler.account(Handler.preferences.recentAccount)
            ?.let {
                if (it.autostart) {
                    useState(it)
                    loginHandler()
                }
            }
        autoLoginExecuted = true
    }

    MaterialTheme {
        Scaffold(
            scaffoldState = scaffoldState,
            floatingActionButton = {
                FloatingActionButton(
                    backgroundColor =
                    if (working) MaterialTheme.colors.secondaryVariant
                    else MaterialTheme.colors.secondary,
                    onClick = loginHandler
                ) {
                    if (working) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, "login")
                    }
                }
            }
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(12.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Box {
                        OutlinedTextFieldWithError(
                            value = id,
                            enabled = !working,
                            onValueChange = {
                                idExpended = true
                                id = it
                                idInputError = ""
                            },
                            label = {
                                Text("ID")
                            },
                            leadingIcon = {
                                Icon(Icons.Default.AccountCircle, "account id icon")
                            },
                            errorMessage = idInputError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = idExpended,
                            onDismissRequest = { idExpended = false },
                            focusable = false
                        ) {
                            Handler.list().forEach {
                                if (id.isEmpty() || it.id.startsWith(id)) {
                                    DropdownMenuItem(onClick = {
                                        useState(it)
                                        idExpended = false
                                    }) {
                                        Text(it.id)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextFieldWithError(
                        value = password,
                        enabled = !working,
                        onValueChange = {
                            password = it
                            pwdInputError = ""
                        },
                        errorMessage = pwdInputError,
                        label = {
                            Text("密码")
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, "account password icon")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Box {
                        OutlinedTextField(
                            value = carrier.uiName,
                            enabled = !working,
                            label = { Text("运营商") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, "carrier icon")
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        carrierExpended = true
                                    },
                                    modifier = Modifier.size(50.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        "carrier dropdown"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = carrierExpended,
                            onDismissRequest = { carrierExpended = false },
                        ) {
                            Carrier.values().forEach { c ->
                                DropdownMenuItem(
                                    onClick = {
                                        carrier = c
                                        carrierExpended = false
                                    }
                                ) { Text(c.uiName) }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.BottomStart)) {
                    Column {
                        LabelledCheckbox(
                            checked = remember,
                            enabled = !working,
                            content = {
                                Text("记住密码")
                            },
                            onCheckedChange = {
                                remember = it
                            }
                        )

                        LabelledCheckbox(
                            checked = autologin,
                            enabled = !working && remember,
                            content = {
                                Text("自动登录")
                            },
                            onCheckedChange = {
                                autologin = it
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LabelledCheckbox(
    content: @Composable () -> Unit,
    enabled: Boolean = true,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange, enabled = enabled)
        content()
    }
}

@Composable
fun OutlinedTextFieldWithError(
    value: String,
    enabled: Boolean = true,
    errorMessage: String,
    label: @Composable () -> Unit,
    leadingIcon: @Composable () -> Unit,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            label = label,
            leadingIcon = leadingIcon,
            isError = errorMessage.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation
        )
        if (errorMessage.isNotEmpty()) { // helper text
            Text(
                text = errorMessage,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

    }
}

fun main() = application {
    Window(
        onCloseRequest = ::handleClose,
        title = "NUISTin",
        state = WindowState(size = DpSize(400.dp, 370.dp))
    ) {
        window.minimumSize = Dimension(400, 370)
        App()
    }
}

fun ApplicationScope.handleClose() {
    Handler.close()
    exitApplication()

}

val Carrier.uiName
    get() = when (this) {
        Carrier.MOBILE -> "移动"
        Carrier.TELECOM -> "电信"
        Carrier.UNICOM -> "联通"
    }