package com.messagetrans.service.email

import android.util.Log
import com.messagetrans.data.database.entities.EmailConfig
import com.messagetrans.utils.RuntimeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    
    suspend fun sendEmail(
        emailConfig: EmailConfig,
        subject: String,
        content: String
    ): EmailResult = withContext(Dispatchers.IO) {
        
        try {
            RuntimeLogger.logEmailSending(emailConfig.emailAddress, "主题: $subject")
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
            }
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(emailConfig.username, emailConfig.password)
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
            RuntimeLogger.logEmailFailure(emailConfig.emailAddress, error, e)
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
            
        } catch (e: MessagingException) {
            val error = "邮件发送失败: ${e.message}"
            RuntimeLogger.logEmailFailure(emailConfig.emailAddress, error, e)
            Log.e(TAG, error, e)
            EmailResult(success = false, errorMessage = error)
            
        } catch (e: Exception) {
            val error = "未知错误: ${e.message}"
            RuntimeLogger.logEmailFailure(emailConfig.emailAddress, error, e)
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
            }
            
            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(emailConfig.username, emailConfig.password)
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
}