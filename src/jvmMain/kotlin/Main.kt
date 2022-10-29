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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.*
import org.apache.commons.lang3.SystemUtils
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.timer

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun App(callback: ApplicationCallback) {
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
    var exception by remember { mutableStateOf<Exception?>(null) }

    var darkMode by remember { mutableStateOf(callback.getDarkModeEnabled()) }

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
                launch {
                    scaffoldState.snackbarHostState.showSnackbar(
                        result.type.uiName,
                        actionLabel = result.exception?.let { "详细信息" }
                    ).let {
                        if (it == SnackbarResult.ActionPerformed) {
                            exception = result.exception
                        }
                    }
                }

                if (result.type == ResultType.SUCCESS) {
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

    callback.setInterfaceStyleChangeListener {
        darkMode = it
    }

    MaterialTheme(if (darkMode) darkColors() else lightColors()) {
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
            },
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(24.dp)
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

                    Spacer(Modifier.height(12.dp))

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

                    Spacer(Modifier.height(12.dp))

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

                Box(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
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

            run {
                val ex = exception
                if (ex != null) {
                    AlertDialog(
                        onDismissRequest = { exception = null },
                        title = { Text("内部错误") },
                        text = { Text(ex.stackTraceToString()) },
                        confirmButton = {
                            Button(onClick = {
                                exception = null
                            }) {
                                Text("关闭")
                            }
                        }
                    )
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
    var colorModeListener by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    var visibilityListener by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val callback = object : ApplicationCallback {
        override fun setInterfaceStyleChangeListener(l: (dark: Boolean) -> Unit) {
            colorModeListener = l
        }

        override fun getDarkModeEnabled() = currentOS.isDarkModeEnabled()
    }

    var visible by remember { mutableStateOf(true) }
    var onTop by remember { mutableStateOf(false) }
    val trayState = rememberTrayState()

    var activeInterval by remember { mutableStateOf(Handler.preferences.intervalIndex) }
    Tray(
        state = trayState,
        icon = painterResource("tray.svg"),
        menu = {
            designedIntervals.forEachIndexed { index, interval ->
                CheckboxItem(
                    text = "${interval}分钟",
                    checked = activeInterval == index,
                    onCheckedChange = { checked ->
                        if (checked && activeInterval != index) {
                            activeInterval = index
                            Handler.preferences.intervalIndex = index
                            updateInterval(interval, trayState)
                        }
                    }
                )
            }

            Separator()

            if (!visible)
                Item(
                    text = "显示主界面",
                    onClick = {
                        visible = true
                        onTop = true
                        onTop = false
                        visibilityListener?.invoke(true)
                    }
                )

            Item(
                text = "退出",
                onClick = {
                    handleClose()
                }
            )
        }
    )

    Window(
        onCloseRequest = {
            visible = false
            visibilityListener?.invoke(false)
            if (!Handler.preferences.notified) {
                Handler.preferences.notified = true
                val notification =
                    Notification("NUISTin正在后台运行", "点击图标调整以设置", Notification.Type.Info)
                trayState.sendNotification(notification)
            }
        },
        title = "NUISTin",
        state = WindowState(size = DpSize(500.dp, 400.dp)),
        visible = visible,
        alwaysOnTop = onTop,
        onKeyEvent = { ev ->
            if (ev.key.nativeKeyCode == KeyEvent.VK_W) {
                if (SystemUtils.IS_OS_MAC_OSX && ev.isMetaPressed) {
                    visible = false
                    visibilityListener?.invoke(false)
                    return@Window true
                } else if (ev.isCtrlPressed) {
                    visible = false
                    visibilityListener?.invoke(false)
                    return@Window true
                }
            }
            false
        }
    ) {
        window.minimumSize = Dimension(400, 400)
        App(callback)
    }

    LaunchedEffect(Unit) {
        val idleMsg = "从休眠中恢复连接"
        fun newJobWithoutUI(): Timer {
            val detector = IdleDetect(10000L)
            return timer("idle detection", period = 10000L) {
                if (detector.hasIdled()) {
                    detector.reset()
                    updateDaemon.launch {
                        loginRecent(trayState, retry = 10, idleMsg)
                    }
                }
            }
        }

        fun newJob(): Timer {
            var enabled = currentOS.isDarkModeEnabled()
            val detector = IdleDetect(500L)
            return fixedRateTimer("ui mode detection", period = 500L) { // detect changes in dark mode settings and idle
                if (currentOS.isDarkModeEnabled() != enabled) {
                    enabled = !enabled
                    colorModeListener?.invoke(enabled)
                }
                if (detector.hasIdled()) {
                    updateDaemon.launch {
                        loginRecent(trayState, retry = 10, idleMsg)
                    }
                }
            }
        }

        var currentJob = newJob()
        visibilityListener = { visible ->
            currentJob.cancel()
            currentJob = if (visible) {
                newJob()
            } else {
                newJobWithoutUI()
            }
        }
        updateInterval(designedIntervals[Handler.preferences.intervalIndex], trayState)
    }
}

fun ApplicationScope.handleClose() {
    Handler.close()
    updateDaemon.cancel()
    exitApplication()
}

val Carrier.uiName
    get() = when (this) {
        Carrier.MOBILE -> "移动"
        Carrier.TELECOM -> "电信"
        Carrier.UNICOM -> "联通"
    }

val ResultType.uiName
    get() = when (this) {
        ResultType.IP_FAILURE -> "无法获取IP地址"
        ResultType.LOGIN_FAILURE -> "登录服务器拒绝了我们的请求"
        ResultType.TIMEOUT -> "请求超时"
        ResultType.SUCCESS -> "成功登录"
        ResultType.EXCEPTION -> "内部错误"
    }

val updateDaemon = CoroutineScope(Dispatchers.Default)

val designedIntervals = listOf(5, 10, 20, 30)
var timer: Timer? = null
fun updateInterval(interval: Int, trayState: TrayState) {
    timer?.cancel()
    val delay = interval * 60000L
    timer = timer(initialDelay = delay, period = delay) {
        updateDaemon.launch {
            loginRecent(trayState)
        }
    }
}

suspend fun loginRecent(tray: TrayState, retry: Int = 0, successMsg: String? = null) {
    Handler.account(Handler.preferences.recentAccount)
        ?.let {
            var result = Handler.login(it)
            var trial = 0
            while (trial < retry && result.type != ResultType.SUCCESS) {
                result = Handler.login(it)
                trial++
                delay(1000L) // don't hurry
            }
            if (result.type != ResultType.SUCCESS) {
                val notification = Notification("登录失败", result.type.uiName, Notification.Type.Warning)
                tray.sendNotification(notification)
            } else if (!successMsg.isNullOrEmpty()) {
                val notification = Notification("登录成功", successMsg, Notification.Type.Info)
                tray.sendNotification(notification)
            }
        }
}


interface ApplicationCallback {
    fun setInterfaceStyleChangeListener(l: (dark: Boolean) -> Unit)

    fun getDarkModeEnabled(): Boolean
}
