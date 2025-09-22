package com.messagetrans.service.email

import android.util.Log
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.utils.RuntimeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.*
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

data class EmailResult(
    val success: Boolean,
    val errorMessage: String? = null
)

object EmailService {
    
    private const val TAG = "EmailService"
    
    // 代理控制设置 - 使用系统代理设置
    private const val USE_SYSTEM_PROXY = true
    
    suspend fun sendEmail(
        emailConfig: EmailConfig,
        subject: String,
        content: String
    ): EmailResult = withContext(Dispatchers.IO) {
        
        try {
            RuntimeLogger.logEmailSending(emailConfig.emailAddress, "主题: $subject")
            val proxyInfo = if (emailConfig.useProxy) {
                "自定义代理: ${emailConfig.proxyType}://${emailConfig.proxyHost}:${emailConfig.proxyPort}"
            } else {
                "使用系统代理: 是"
            }
            RuntimeLogger.logInfo(TAG, "邮件发送配置", "服务器: ${emailConfig.smtpServer}:${emailConfig.smtpPort}, SSL: ${emailConfig.useSSL}, $proxyInfo")
            Log.d(TAG, "Sending email via ${emailConfig.emailAddress}")
            
            val props = Properties().apply {
                put("mail.smtp.host", emailConfig.smtpServer)
                put("mail.smtp.port", emailConfig.smtpPort.toString())
                put("mail.smtp.auth", "true")
                
                if (emailConfig.useSSL) {
                    put("mail.smtp.socketFactory.port", emailConfig.smtpPort.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
                
                // 设置超时
                put("mail.smtp.connectiontimeout", "30000")
                put("mail.smtp.timeout", "30000")
                put("mail.smtp.writetimeout", "30000")
                
                // 配置代理设置
                configureProxySettings(this, emailConfig)
            }
            
            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                    return javax.mail.PasswordAuthentication(emailConfig.username, emailConfig.password)
                }
            })
            
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(emailConfig.emailAddress, emailConfig.displayName))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailConfig.emailAddress))
                setSubject(subject, "UTF-8")
                setText(content, "UTF-8")
                sentDate = Date()
            }
            
            Transport.send(message)
            
            RuntimeLogger.logEmailSuccess(emailConfig.emailAddress, "主题: $subject")
            Log.d(TAG, "Email sent successfully via ${emailConfig.emailAddress}")
            EmailResult(success = true)
            
        } catch (e: AuthenticationFailedException) {
            val error = "邮箱认证失败: ${e.message}"
            RuntimeLogger.logEmailFailure(emailConfig.emailAddress, "主题: $subject", error, e)
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
            
        } catch (e: MessagingException) {
            val error = "邮件发送失败: ${e.message}"
            RuntimeLogger.logEmailFailure(emailConfig.emailAddress, "主题: $subject", error, e)
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
            
        } catch (e: Exception) {
            val error = "未知错误: ${e.message}"
            RuntimeLogger.logEmailFailure(emailConfig.emailAddress, "主题: $subject", error, e)
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
        }
    }
    
    suspend fun testConnection(emailConfig: EmailConfig): EmailResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing email connection for ${emailConfig.emailAddress}")
            
            val props = Properties().apply {
                put("mail.smtp.host", emailConfig.smtpServer)
                put("mail.smtp.port", emailConfig.smtpPort.toString())
                put("mail.smtp.auth", "true")
                
                if (emailConfig.useSSL) {
                    put("mail.smtp.socketFactory.port", emailConfig.smtpPort.toString())
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.ssl.enable", "true")
                } else {
                    put("mail.smtp.starttls.enable", "true")
                }
                
                put("mail.smtp.connectiontimeout", "10000")
                put("mail.smtp.timeout", "10000")
                
                // 配置代理设置
                configureProxySettings(this, emailConfig)
            }
            
            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                    return javax.mail.PasswordAuthentication(emailConfig.username, emailConfig.password)
                }
            })
            
            val transport = session.getTransport("smtp")
            transport.connect()
            transport.close()
            
            Log.d(TAG, "Email connection test successful for ${emailConfig.emailAddress}")
            EmailResult(success = true)
            
        } catch (e: AuthenticationFailedException) {
            val error = "认证失败: 用户名或密码错误"
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
            
        } catch (e: MessagingException) {
            val error = "连接失败: ${e.message}"
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
            
        } catch (e: Exception) {
            val error = "连接测试失败: ${e.message}"
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
        }
    }
    
    fun formatSmsEmailContent(
        phoneNumber: String,
        content: String,
        timestamp: Long,
        simSlot: Int,
        simType: String
    ): Pair<String, String> {
        val date = Date(timestamp)
        val subject = "短信转发 - $phoneNumber"
        
        val emailContent = buildString {
            appendLine("您收到了一条新短信:")
            appendLine()
            appendLine("发送方: $phoneNumber")
            appendLine("时间: $date")
            appendLine("SIM卡: SIM${simSlot + 1} ($simType)")
            appendLine("内容:")
            appendLine("────────────────")
            appendLine(content)
            appendLine("────────────────")
            appendLine()
            appendLine("此邮件由短信转发器自动发送")
        }
        
        return Pair(subject, emailContent)
    }
    
    /**
     * 测试代理连接
     */
    suspend fun testProxyConnection(emailConfig: EmailConfig): EmailResult = withContext(Dispatchers.IO) {
        if (!emailConfig.useProxy || emailConfig.proxyHost.isEmpty()) {
            return@withContext EmailResult(success = true, errorMessage = "未配置代理或使用系统代理")
        }
        
        try {
            Log.d(TAG, "Testing proxy connection: ${emailConfig.proxyType}://${emailConfig.proxyHost}:${emailConfig.proxyPort}")
            RuntimeLogger.logInfo(TAG, "代理连接测试", "测试${emailConfig.proxyType}代理: ${emailConfig.proxyHost}:${emailConfig.proxyPort}")
            
            when (emailConfig.proxyType.uppercase()) {
                "HTTP" -> {
                    testHttpProxy(emailConfig)
                }
                "SOCKS" -> {
                    testSocksProxy(emailConfig)
                }
                else -> {
                    EmailResult(success = false, errorMessage = "不支持的代理类型: ${emailConfig.proxyType}")
                }
            }
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "代理连接测试失败", e)
            Log.e(TAG, "Proxy connection test failed", e)
            EmailResult(success = false, errorMessage = "代理连接测试失败: ${e.message}")
        }
    }
    
    /**
     * 测试HTTP代理连接
     */
    private suspend fun testHttpProxy(emailConfig: EmailConfig): EmailResult = withContext(Dispatchers.IO) {
        try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(emailConfig.proxyHost, emailConfig.proxyPort))
            
            // 如果有代理认证，设置系统属性
            if (emailConfig.proxyUsername.isNotEmpty()) {
                System.setProperty("http.proxyUser", emailConfig.proxyUsername)
                System.setProperty("http.proxyPassword", emailConfig.proxyPassword)
            }
            
            // 尝试通过代理连接到一个测试服务器
            val testUrl = URL("http://www.google.com")
            val connection = testUrl.openConnection(proxy)
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            
            RuntimeLogger.logInfo(TAG, "HTTP代理测试成功", "代理服务器响应正常")
            EmailResult(success = true, errorMessage = "HTTP代理连接成功")
            
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "HTTP代理测试失败", e)
            EmailResult(success = false, errorMessage = "HTTP代理连接失败: ${e.message}")
        }
    }
    
    /**
     * 测试SOCKS代理连接
     */
    private suspend fun testSocksProxy(emailConfig: EmailConfig): EmailResult = withContext(Dispatchers.IO) {
        try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(emailConfig.proxyHost, emailConfig.proxyPort))
            
            // 创建Socket通过SOCKS代理连接测试
            val socket = Socket(proxy)
            socket.soTimeout = 5000
            socket.connect(InetSocketAddress("8.8.8.8", 53), 5000)
            socket.close()
            
            RuntimeLogger.logInfo(TAG, "SOCKS代理测试成功", "代理服务器响应正常")
            EmailResult(success = true, errorMessage = "SOCKS代理连接成功")
            
        } catch (e: Exception) {
            RuntimeLogger.logError(TAG, "SOCKS代理测试失败", e)
            EmailResult(success = false, errorMessage = "SOCKS代理连接失败: ${e.message}")
        }
    }
    
    /**
     * 配置邮件发送的代理设置
     * 支持自定义代理或使用系统代理设置
     */
    private fun configureProxySettings(props: Properties, emailConfig: EmailConfig) {
        if (emailConfig.useProxy && emailConfig.proxyHost.isNotEmpty()) {
            // 使用自定义代理设置
            Log.d(TAG, "Using custom proxy: ${emailConfig.proxyType}://${emailConfig.proxyHost}:${emailConfig.proxyPort}")
            
            when (emailConfig.proxyType.uppercase()) {
                "HTTP" -> {
                    // HTTP代理设置
                    props.apply {
                        put("mail.smtp.proxy.host", emailConfig.proxyHost)
                        put("mail.smtp.proxy.port", emailConfig.proxyPort.toString())
                        
                        if (emailConfig.proxyUsername.isNotEmpty()) {
                            put("mail.smtp.proxy.user", emailConfig.proxyUsername)
                            put("mail.smtp.proxy.password", emailConfig.proxyPassword)
                        }
                    }
                    
                    // 设置系统属性（某些情况下JavaMail需要）
                    System.setProperty("http.proxyHost", emailConfig.proxyHost)
                    System.setProperty("http.proxyPort", emailConfig.proxyPort.toString())
                    System.setProperty("https.proxyHost", emailConfig.proxyHost)
                    System.setProperty("https.proxyPort", emailConfig.proxyPort.toString())
                }
                "SOCKS" -> {
                    // SOCKS代理设置
                    props.apply {
                        put("mail.smtp.socks.host", emailConfig.proxyHost)
                        put("mail.smtp.socks.port", emailConfig.proxyPort.toString())
                        
                        if (emailConfig.proxyUsername.isNotEmpty()) {
                            put("java.net.socks.username", emailConfig.proxyUsername)
                            put("java.net.socks.password", emailConfig.proxyPassword)
                        }
                    }
                    
                    // 设置系统属性
                    System.setProperty("socksProxyHost", emailConfig.proxyHost)
                    System.setProperty("socksProxyPort", emailConfig.proxyPort.toString())
                }
            }
            
            RuntimeLogger.logInfo(TAG, "自定义代理配置", 
                "类型: ${emailConfig.proxyType}, 服务器: ${emailConfig.proxyHost}:${emailConfig.proxyPort}, 认证: ${emailConfig.proxyUsername.isNotEmpty()}")
                
        } else if (USE_SYSTEM_PROXY) {
            // 使用系统代理设置
            Log.d(TAG, "Using system proxy settings for email sending")
            
            // 启用系统属性支持
            props.put("java.net.useSystemProxies", "true")
            
            // 记录系统代理状态到日志
            val httpProxyHost = System.getProperty("http.proxyHost", "未设置")
            val httpProxyPort = System.getProperty("http.proxyPort", "未设置")
            val httpsProxyHost = System.getProperty("https.proxyHost", "未设置")
            val httpsProxyPort = System.getProperty("https.proxyPort", "未设置")
            
            RuntimeLogger.logInfo(TAG, "系统代理状态", 
                "HTTP代理: $httpProxyHost:$httpProxyPort, HTTPS代理: $httpsProxyHost:$httpsProxyPort")
        } else {
            // 不使用代理
            Log.d(TAG, "No proxy configured for email sending")
            RuntimeLogger.logInfo(TAG, "代理配置", "不使用代理")
        }
    }
}