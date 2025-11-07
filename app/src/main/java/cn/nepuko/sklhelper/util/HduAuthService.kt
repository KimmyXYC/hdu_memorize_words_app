package cn.nepuko.sklhelper.util

import android.annotation.SuppressLint
import android.util.Base64
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.*
import io.ktor.http.Parameters
import org.jsoup.Jsoup
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

sealed class LoginResult {
    data class Success(val token: String) : LoginResult()
    data class Failure(val errorType: ErrorType, val message: String) : LoginResult()

    enum class ErrorType {
        NETWORK_ERROR,           // 网络连接失败
        LOGIN_PAGE_UNAVAILABLE,  // 无法获取登录页面
        INVALID_CREDENTIALS,     // 账号或密码错误
        TOKEN_EXTRACTION_FAILED, // 登录成功但无法获取Token
        SESSION_ERROR,           // 会话错误
        UNKNOWN_ERROR            // 未知错误
    }
}

object HduAuthService {

    private const val TAG = "HduAuthService"
    private const val LOGIN_URL = "https://sso.hdu.edu.cn/login"
    private const val BASE_SERVICE_URL = "https://skl.hdu.edu.cn/api/cas/login"

    private var cookieStorage = AcceptAllCookiesStorage()
    private var client = createClient()

    private fun createClient() = HttpClient(Android) {
        followRedirects = false
        engine {
            connectTimeout = 10_000
            socketTimeout = 10_000
        }
        install(HttpCookies) {
            storage = cookieStorage
        }
        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (Linux; U; Android 12; zh-CN; M2102J2SC Build/SKQ1.211006.001) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/69.0.3497.100 UWS/3.22.1.210 Mobile Safari/537.36 AliApp(DingTalk/6.5.20) com.alibaba.android.rimet/24646881 Channel/700159 language/zh-CN abi/64 UT4Aplus/0.2.25 colorScheme/light")
        }
    }

    private fun clearAllCookiesAndSession() {
        try {
            // Close and clear cookie storage
            cookieStorage.close()

            // Close existing client
            client.close()

            Log.d(TAG, "已关闭旧的HTTP客户端")

            // Create new cookie storage and client for fresh session
            cookieStorage = AcceptAllCookiesStorage()
            client = createClient()

            Log.d(TAG, "已创建新的HTTP客户端和Cookie存储")
        } catch (e: Exception) {
            Log.e(TAG, "清除会话时发生异常", e)
        }
    }

    suspend fun login(username: String, password: String): LoginResult {
        return loginAndGetToken(username, password)
    }

    suspend fun loginAndGetToken(username: String, password: String): LoginResult {
        return try {
            Log.d(TAG, "开始登录流程，用户名: $username")

            // Clear all cookies and create fresh HTTP client to ensure a completely new session
            try {
                clearAllCookiesAndSession()
            } catch (e: Exception) {
                Log.e(TAG, "清除会话失败", e)
                return LoginResult.Failure(
                    LoginResult.ErrorType.SESSION_ERROR,
                    "会话清除失败，请重试"
                )
            }

            // Generate state token
            val stateToken = generateStateToken()
            val serviceUrl = "$BASE_SERVICE_URL?state=$stateToken&index="

            val (cryptoKey, execution, fullLoginUrl) = fetchLoginTokens(serviceUrl)
            if (cryptoKey == null || execution == null) {
                Log.e(TAG, "获取登录令牌失败")
                return LoginResult.Failure(
                    LoginResult.ErrorType.LOGIN_PAGE_UNAVAILABLE,
                    "无法访问登录页面，请检查网络连接"
                )
            }

            Log.d(TAG, "成功获取登录令牌")

            val encryptedPassword = try {
                encryptPassword(cryptoKey, password)
            } catch (e: Exception) {
                Log.e(TAG, "密码加密失败", e)
                return LoginResult.Failure(
                    LoginResult.ErrorType.UNKNOWN_ERROR,
                    "密码加密失败，请重试"
                )
            }
            Log.d(TAG, "密码加密完成")

            val ticketUrl = postLoginForm(username, encryptedPassword, cryptoKey, execution, fullLoginUrl ?: LOGIN_URL)
            if (ticketUrl == null) {
                Log.e(TAG, "登录失败")
                return LoginResult.Failure(
                    LoginResult.ErrorType.INVALID_CREDENTIALS,
                    "账号或密码错误"
                )
            }

            Log.d(TAG, "登录成功，获取到ticket URL")

            // Exchange ticket for token
            val token = exchangeTicketForToken(ticketUrl, fullLoginUrl ?: LOGIN_URL)
            if (token != null) {
                Log.d(TAG, "成功获取 X-Auth-Token")
                return LoginResult.Success(token)
            } else {
                Log.e(TAG, "无法获取 X-Auth-Token")
                return LoginResult.Failure(
                    LoginResult.ErrorType.TOKEN_EXTRACTION_FAILED,
                    "登录成功但无法获取认证令牌"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "登录过程发生异常", e)
            e.printStackTrace()
            return LoginResult.Failure(
                LoginResult.ErrorType.NETWORK_ERROR,
                "网络连接失败: ${e.message}"
            )
        }
    }

    private fun generateStateToken(): String {
        val chars = "0123456789abcdef"
        return (1..24)
            .map { chars.random() }
            .joinToString("")
    }

    private suspend fun fetchLoginTokens(serviceUrl: String): Triple<String?, String?, String?> {
        try {
            Log.d(TAG, "正在获取登录页面")
            val response: HttpResponse = client.get(LOGIN_URL) {
                parameter("service", serviceUrl)
            }
            Log.d(TAG, "登录页面响应状态: ${response.status.value}")

            val responseBody = response.bodyAsText()
            val fullUrl = response.call.request.url.toString()

            val doc = Jsoup.parse(responseBody)
            val cryptoKey = doc.select("p#login-croypto").text()
            val execution = doc.select("p#login-page-flowkey").text()

            Log.d(TAG, "解析结果 - cryptoKey长度: ${cryptoKey.length}, execution: ${execution.take(20)}...")
            return Triple(cryptoKey, execution, fullUrl)
        } catch (e: Exception) {
            Log.e(TAG, "获取登录令牌时发生异常", e)
            return Triple(null, null, null)
        }
    }

    private suspend fun postLoginForm(
        username: String,
        encryptedPassword: String,
        cryptoKey: String,
        execution: String,
        referer: String
    ): String? {
        try {
            Log.d(TAG, "正在提交登录表单")
            val response: HttpResponse = client.submitForm(
                url = LOGIN_URL,
                formParameters = Parameters.build {
                    append("username", username)
                    append("type", "UsernamePassword")
                    append("_eventId", "submit")
                    append("geolocation", "")
                    append("execution", execution)
                    append("password", encryptedPassword)
                    append("croypto", cryptoKey)
                    append("captcha_code", "")
                    append("captcha_payload", "")
                }
            ) {
                header("Referer", referer)
            }

            Log.d(TAG, "登录表单响应状态: ${response.status.value}")

            if (response.status.value == 302) {
                val location = response.headers["Location"]
                Log.d(TAG, "重定向到: $location")
                return location
            } else {
                val responseBody = response.bodyAsText()
                Log.d(TAG, "登录失败，响应内容: ${responseBody.take(500)}")
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "提交登录表单时发生异常", e)
            return null
        }
    }

    private suspend fun exchangeTicketForToken(ticketUrl: String, referer: String): String? {
        try {
            var currentUrl = ticketUrl
            var currentReferer = referer
            val maxRedirects = 10

            for (i in 0 until maxRedirects) {
                Log.d(TAG, "跟踪重定向 $i: $currentUrl")
                val response: HttpResponse = client.get(currentUrl) {
                    header("Referer", currentReferer)
                }

                // Check for token in URL fragment
                if ("token=" in currentUrl) {
                    val fragment = currentUrl.substringAfter("#", "")
                    if (fragment.contains("token=")) {
                        val token = fragment.substringAfter("token=").substringBefore("&")
                        if (token.isNotEmpty()) {
                            Log.d(TAG, "从URL片段中找到token")
                            return token
                        }
                    }
                }

                if (response.status.value == 302) {
                    val location = response.headers["Location"]
                    if (location != null) {
                        currentReferer = currentUrl
                        currentUrl = location
                        continue
                    }
                }

                // Check cookies for X-Auth-Token
                val cookies = client.cookies(response.call.request.url)
                for (cookie in cookies) {
                    if (cookie.name == "X-Auth-Token") {
                        Log.d(TAG, "从Cookie中找到token")
                        return cookie.value
                    }
                }

                break
            }

            Log.e(TAG, "无法找到 X-Auth-Token")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "交换ticket时发生异常", e)
            return null
        }
    }

    @SuppressLint("GetInstance")
    private fun encryptPassword(keyB64: String, plainText: String): String {
        try {
            val key = Base64.decode(keyB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding")
            val secretKeySpec = SecretKeySpec(key, "AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "密码加密时发生异常", e)
            throw e
        }
    }
}
