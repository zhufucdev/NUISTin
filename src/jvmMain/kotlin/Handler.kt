import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.apache.commons.lang3.SystemUtils
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths

@OptIn(ExperimentalSerializationApi::class)
object Handler {
    /* Data store */
    private val accountsDir by lazy { File(dataDir, "accounts") }
    private val preferencesFile by lazy { File(dataDir, "preferences.json") }

    private val dataDir by lazy {
        if (SystemUtils.IS_OS_WINDOWS) {
            Paths.get(System.getenv("APPDATA"), "NUISTin").toFile()
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            Paths.get(SystemUtils.USER_HOME, "Library", "Application Support", "NUISTin")
                .toFile()
        } else if (SystemUtils.IS_OS_LINUX) {
            File(SystemUtils.getUserHome(), ".nuistin")
        } else {
            File(SystemUtils.getUserHome(), "nuistin")
        }
    }

    /* Runtime */
    private val ktor = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    private val accounts by lazy {
        buildSet<Account> {
            accountsDir.listFiles()?.forEach {
                kotlin.runCatching {
                    if (!it.isHidden && it.extension == "json") {
                        it.inputStream().use { s ->
                            add(Json.decodeFromStream(s))
                        }
                    }
                }
            }
        }.toMutableSet()
    }

    val preferences: Preferences by lazy {
        if (!preferencesFile.exists()) {
            Preferences.default
        } else {
            preferencesFile.inputStream().use { Json.decodeFromStream(it) }
        }
    }

    fun store(account: Account) {
        if (!accountsDir.exists()) {
            accountsDir.mkdirs()
        }
        val acc = if (!account.remember) {
            account.copy(password = "")
        } else {
            account
        }
        accounts.add(acc)

        val file = File(accountsDir, "${acc.id}.json")
        if (!file.exists()) {
            file.createNewFile()
        }
        file.outputStream().use {
            Json.encodeToStream(acc, it)
        }
    }

    suspend fun login(account: Account): LoginResult {
        preferences.recentAccount = account.id
        val addr: String
        try {
            val res = ktor.get("http://10.255.255.34/api/v1/ip")
            if (!res.status.isSuccess()) {
                return LoginResult.IP_FAILURE
            }

            val ip = res.body<IpQueryResponse>()
            if (ip.code != 200) {
                return LoginResult.IP_FAILURE
            }
            addr = ip.data!!
        } catch (e: HttpRequestTimeoutException) {
            return LoginResult.TIMEOUT
        } catch (e: Exception) {
            e.printStackTrace()
            return LoginResult.EXCEPTION
        }

        try {
            val res = ktor.post {
                url("http://10.255.255.34/api/v1/login")
                header("connection", "keep-alive")
                contentType(ContentType.Application.Json.withCharset(Charset.forName("GBK")))
                setBody(
                    LoginRequest(
                        account.id,
                        account.password,
                        account.carrier.channel.toString(),
                        addr
                    )
                )
            }
            if (!res.status.isSuccess()) {
                return LoginResult.LOGIN_FAILURE
            }
            val body = Json.decodeFromString<JsonObject>(res.bodyAsText())
            if (body["code"]!!.jsonPrimitive.int != 200) {
                return LoginResult.LOGIN_FAILURE
            }
        } catch (e: HttpRequestTimeoutException) {
            return LoginResult.TIMEOUT
        } catch (e: Exception) {
            e.printStackTrace()
            return LoginResult.EXCEPTION
        }

        return LoginResult.SUCCESS
    }

    fun list() = accounts.toList()

    fun account(id: String) = accounts.firstOrNull { it.id == id }

    fun close() {
        preferences.let { p ->
            preferencesFile.outputStream().use {
                Json.encodeToStream(p, it)
            }
        }
    }
}

@Serializable
data class IpQueryResponse(val code: Int, val data: String?)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val channel: String,
    val usripadd: String,
    val pagesign: String = "secondauth",
    val ifautologin: String = "0"
)

enum class LoginResult {
    IP_FAILURE,
    LOGIN_FAILURE,
    TIMEOUT,
    SUCCESS,
    EXCEPTION
}