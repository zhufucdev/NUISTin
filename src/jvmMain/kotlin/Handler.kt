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
import java.io.File
import java.nio.charset.Charset


private const val ISP_SERVER = "http://a.nuist.edu.cn/"

@OptIn(ExperimentalSerializationApi::class)
object Handler {
    /* Data store */
    private val accountsDir by lazy { File(dataDir, "accounts") }
    private val preferencesFile by lazy { File(dataDir, "preferences.json") }
    private val dataDir get() = currentOS.dataDir

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
            preferencesFile.inputStream().use {
                try {
                    Json.decodeFromStream(it)
                } catch (e: Exception) {
                    preferencesFile.delete()
                    Preferences.default
                }
            }
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
            val res = ktor.get("$ISP_SERVER/api/v1/ip")
            if (!res.status.isSuccess()) {
                return ResultType.IP_FAILURE.bundle()
            }

            val ip = res.body<IpQueryResponse>()
            if (ip.code != 200) {
                return ResultType.IP_FAILURE.bundle()
            }
            addr = ip.data!!
        } catch (e: HttpRequestTimeoutException) {
            return ResultType.TIMEOUT.bundle()
        } catch (e: Exception) {
            e.printStackTrace()
            return ResultType.EXCEPTION.bundle(e)
        }

        try {
            val res = ktor.post {
                url("$ISP_SERVER/api/v1/login")
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
                return ResultType.LOGIN_FAILURE.bundle()
            }
            val body = Json.decodeFromString<JsonObject>(res.bodyAsText())
            if (body["code"]!!.jsonPrimitive.int != 200) {
                return ResultType.LOGIN_FAILURE.bundle()
            }
        } catch (e: HttpRequestTimeoutException) {
            return ResultType.TIMEOUT.bundle()
        } catch (e: Exception) {
            e.printStackTrace()
            return ResultType.EXCEPTION.bundle(e)
        }

        return ResultType.SUCCESS.bundle()
    }

    fun list() = accounts.toList()

    fun account(id: String) = accounts.firstOrNull { it.id == id }

    fun close() {
        preferences.let { p ->
            if (!preferencesFile.exists()) {
                dataDir.mkdirs()
                preferencesFile.createNewFile()
            }
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

data class LoginResult(val type: ResultType, val exception: Exception? = null)

enum class ResultType {
    IP_FAILURE,
    LOGIN_FAILURE,
    TIMEOUT,
    SUCCESS,
    EXCEPTION
}

fun ResultType.bundle(e: Exception? = null) = LoginResult(this, e)