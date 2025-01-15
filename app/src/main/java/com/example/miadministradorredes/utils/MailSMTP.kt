package com.example.miadministradorredes.utils
import android.content.Context
import android.util.Log
import com.example.miadministradorredes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class MailSMTP {
    fun sendMail(destinatario: String, asunto: String, mensajeHTML: String) {
        val remitente = "tu-email"
        val contraseña = "tu-email-password"

        val propiedades = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com") // Servidor SMTP de Gmail
            put("mail.smtp.port", "587")           // Puerto SMTP
            put("mail.smtp.auth", "true")          // Autenticación requerida
            put("mail.smtp.starttls.enable", "true") // TLS habilitado
        }

        val session = Session.getInstance(propiedades, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(remitente, contraseña)
            }
        })

        try {
            CoroutineScope(Dispatchers.IO).launch {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(remitente))
                    addRecipient(Message.RecipientType.TO, InternetAddress(destinatario))
                    subject = asunto
                    setContent(mensajeHTML, "text/html")
                }

                Transport.send(message)
                Log.d("Correo", "Correo enviado exitosamente.")
            }
        } catch (e: MessagingException) {
            e.printStackTrace()
            Log.e("Correo","Error al enviar el correo: ${e.message}")
        }
    }

    fun loadHtmlTemplate(context: Context, router: String, status: String): String {
        // Leer la plantilla desde res/raw
        val inputStream = context.resources.openRawResource(R.raw.template)
        val htmlTemplate = inputStream.bufferedReader().use { it.readText() }

        return htmlTemplate
            .replace("{ROUTER}", router)
            .replace("{STATUS}", status)
            .replace("{DATE}", getCurrentDateTime())
    }

    private fun getCurrentDateTime(): String {
        // Obtener la fecha y hora actuales
        val currentDateTime = LocalDateTime.now()

        // Formatear la fecha y hora
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // Formato deseado
        return currentDateTime.format(formatter)
    }

}