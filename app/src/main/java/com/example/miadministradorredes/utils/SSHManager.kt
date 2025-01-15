package com.example.miadministradorredes.utils

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream

class SSHManager {
    suspend fun executeSSHCommand(
        host: String,
        port: Int,
        username: String,
        password: String,
        command: String
    ): String {
        return withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(username, host, port)

            // Configuración de autenticación
            session.setPassword(password)
            val config = java.util.Properties()
            config["StrictHostKeyChecking"] = "no" // Desactiva la verificación de claves para pruebas
            session.setConfig(config)

            var result = ""
            try {
                session.connect()
                // Usa un único canal para todos los comandos
                val channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                channel.setCommand(command)
                val input = channel.inputStream
                channel.connect()
                val output = input.bufferedReader().use { it.readText() }
                result += output
                channel.disconnect()
                session.disconnect()
                result
            } catch (e: Exception) {
                e.printStackTrace()
                session.disconnect()
                "Error: ${e.message}"
            }
        }
    }

    suspend fun executeSSHCommandsWithShell(
        host: String,
        port: Int,
        username: String,
        password: String,
        commands: List<String>
    ): String {
        return withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session: Session = jsch.getSession(username, host, port)

            // Configurar autenticación
            session.setPassword(password)
            val config = java.util.Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)

            try {
                session.connect()
                val channel = session.openChannel("shell") as com.jcraft.jsch.ChannelShell

                // Configurar entrada y salida del canal
                val outputStream = ByteArrayOutputStream()
                channel.outputStream = outputStream

                val writer: OutputStream = channel.outputStream

                channel.connect()

                // Enviar comandos secuenciales
                for (command in commands) {
                    writer.write("$command\n".toByteArray())
                    writer.flush()
                    Thread.sleep(500) // Esperar para recibir la salida completa
                }

                writer.write("exit\n".toByteArray())
                writer.flush()

                // Capturar la salida
                Thread.sleep(1000) // Esperar para asegurar que se reciba toda la salida
                val result = outputStream.toString()

                // Cerrar el canal y la sesión
                channel.disconnect()
                session.disconnect()

                result
            } catch (e: Exception) {
                e.printStackTrace()
                session.disconnect()
                "Error: ${e.message}"
            }
        }
    }
}