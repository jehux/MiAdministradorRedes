package com.example.miadministradorredes

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.miadministradorredes.utils.SNMPManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val buttonLogin: Button = findViewById(R.id.button_login)
        val etIp: EditText = findViewById(R.id.editText_ip)
        val etPort: EditText = findViewById(R.id.editText_port)
        val etUser: EditText = findViewById(R.id.editText_user)
        val etPassword: EditText = findViewById(R.id.editTextTextPassword)
        buttonLogin.setOnClickListener {
            val host = etIp.text.toString()
            val port = etPort.text.toString().toInt()
            val username = etUser.text.toString()
            val password = etPassword.text.toString()

            goToMenu(host,port,username,password)
        }
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
        getiFInOctets()
    }

    fun goToMenu(host: String, port: Int, username: String, password: String) {
        val intent = Intent(this, MenuActivity::class.java)
        intent.putExtra("HOST", host)
        intent.putExtra("PORT", port)
        intent.putExtra("USER", username)
        intent.putExtra("PASSWORD", password)
        startActivity(intent)
    }

    fun getiFInOctets() {
        // Dirección del dispositivo SNMP y comunidad
        val targetAddress = "192.168.1.101"
        val community = "public"

        // Inicializa el manager SNMP
        val snmpManager = SNMPManager(targetAddress, community)
        // Obtén los valores en segundo plano
        CoroutineScope(Dispatchers.IO).launch {
            try {
                snmpManager.init()
                val inOctetsOID = "1.3.6.1.2.1.2.2.1.10.1" // ifInOctets para índice 1
                val outOctetsOID = "1.3.6.1.2.1.2.2.1.16.1" // ifOutOctets para índice 1

                val inOctets = snmpManager.getOIDValue(inOctetsOID)
                val outOctets = snmpManager.getOIDValue(outOctetsOID)

                // Muestra los valores en los logs
                Log.e("snmp","ifInOctets: $inOctets")
                Log.e("snmp","ifOutOctets: $outOctets")
            } finally {
                snmpManager.close()
            }
        }
    }

}