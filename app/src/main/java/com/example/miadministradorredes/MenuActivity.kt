package com.example.miadministradorredes

import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.miadministradorredes.utils.DhcpBindingParser
import com.example.miadministradorredes.utils.Loader
import com.example.miadministradorredes.utils.MailSMTP
import com.example.miadministradorredes.utils.SSHManager
import com.example.miadministradorredes.utils.SnmpTrapReceiver
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MenuActivity : AppCompatActivity() {
    var BANDWIDTH_ALERT = 8000f
    var host = ""
    var port = 0
    var user = ""
    var password = ""
    var outputDhcp = ""

    val ipRouter2 = "192.168.163.196"
    val ipRouter3 = "192.168.163.195"
    val ipSwitch = "192.168.163.197"

    val parser = DhcpBindingParser()
    val sshManager = SSHManager()
    val loader = Loader(this)
    val smtp = MailSMTP()

    var wasBelowThreshold = true

    private lateinit var lineChart: LineChart
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var snmpTrapReceiver: SnmpTrapReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Recuperar los valores del intent
        host = intent.getStringExtra("HOST") ?: "Valor no recibido"
        port = intent.getIntExtra("PORT",0)
        user = intent.getStringExtra("USER") ?: "Valor no recibido"
        password = intent.getStringExtra("PASSWORD") ?: "Valor no recibido"

        // Inicia el receptor SNMP
        snmpTrapReceiver = SnmpTrapReceiver()
        snmpTrapReceiver.startListening()

        val buttonDhcp: Button = findViewById(R.id.button_dhcp)
        val buttonDhcpDetail: Button = findViewById(R.id.button_dhcp_detail)
        val buttonVlan: Button = findViewById(R.id.button_vlan)
        val switchAcl = findViewById<Switch>(R.id.switch_acl)
        val buttonSettings: Button = findViewById(R.id.button_setings)
        val tvConsole: TextView = findViewById(R.id.textView_console)

        buttonDhcp.setOnClickListener {
            loader.showLoader("configurando servidor DHCP")
            CoroutineScope(Dispatchers.Main).launch {
                val commands = listOf("configure terminal",
                    "ip dhcp excluded-address 192.168.10.1 192.168.10.10",
                    "ip dhcp pool RED10",
                    "network 192.168.10.0 255.255.255.0",
                    "default-router 192.168.10.1",
                    "dns-server 8.8.8.8 ",
                    "exit",
                    "",
                    "ip dhcp excluded-address 192.168.20.1 192.168.20.10",
                    "ip dhcp pool RED20",
                    "network 192.168.20.0 255.255.255.0",
                    "default-router 192.168.20.1",
                    "dns-server 8.8.8.8 ",
                    "exit",
                    "",
                    "ip dhcp excluded-address 192.168.30.1 192.168.30.10",
                    "ip dhcp pool RED30",
                    "network 192.168.30.0 255.255.255.0",
                    "default-router 192.168.30.1",
                    "dns-server 8.8.8.8 ",
                    "exit",
                    "",
                    "ip dhcp excluded-address 192.168.31.1 192.168.31.10",
                    "ip dhcp pool RED31",
                    "network 192.168.31.0 255.255.255.0",
                    "default-router 192.168.31.1",
                    "dns-server 8.8.8.8 ",
                    "exit"
                    )
                outputDhcp = sshManager.executeSSHCommandsWithShell(host, port, user,password, commands)
                withContext(Dispatchers.Main) {
                    Log.d("Conexion ssh","Resultado del comando:\n$outputDhcp")
                    tvConsole.text = outputDhcp
                    loader.dismissLoader()
                }
            }
        }
        buttonDhcpDetail.setOnClickListener{
            loader.showLoader("Obteniendo detalles del pool...")
            CoroutineScope(Dispatchers.Main).launch {
                val commands = listOf("terminal length 0","show ip dhcp binding", "show ip dhcp pool", "show running-config | section dhcp")
                outputDhcp = sshManager.executeSSHCommandsWithShell(host, port, user,password, commands)
                withContext(Dispatchers.Main) {
                    Log.d("Conexion ssh","Resultado del comando:\n$outputDhcp")
                    tvConsole.text = outputDhcp
                    loader.dismissLoader()
                }
            }
        }
        buttonVlan.setOnClickListener{
            loader.showLoader("Configurando VLAN...")
            CoroutineScope(Dispatchers.Main).launch {
                outputDhcp = configurateVLAN()
                withContext(Dispatchers.Main) {
                    Log.d("Conexion ssh","Resultado del comando:\n$outputDhcp")
                    tvConsole.text = outputDhcp
                    loader.dismissLoader()
                }
            }
        }
        switchAcl.setOnCheckedChangeListener { _, isChecked ->
            loader.showLoader(if (isChecked) "Encendiendo ACL..." else "Apagando ACL...")
            CoroutineScope(Dispatchers.Main).launch {
                val commands: List<String>
                if (isChecked) {
                    commands = listOf(
                        "configure terminal",
                        "ip access-list extended BLOCK_PING",
                        "deny icmp 192.168.10.0 0.0.0.255 192.168.20.0 0.0.0.255",
                        "deny icmp 192.168.20.0 0.0.0.255 192.168.10.0 0.0.0.255",
                        "permit ip any any",
                        "exit",
                        "interface FastEthernet1/0",
                        "ip access-group BLOCK_PING out",
                        "interface FastEthernet2/0",
                        "ip access-group BLOCK_PING out",
                        "exit",
                        "exit",
                        "show access-lists BLOCK_PING"
                    )
                } else {
                    commands = listOf(
                        "configure terminal",
                        "interface FastEthernet1/0",
                        "no ip access-group BLOCK_PING out",
                        "interface FastEthernet2/0",
                        "no ip access-group BLOCK_PING out",
                        "exit",
                        "no ip access-list extended BLOCK_PING",
                        "exit",
                        "show access-lists\n"
                    )
                }
                outputDhcp = sshManager.executeSSHCommandsWithShell(host, port, user, password, commands)
                withContext(Dispatchers.Main) {
                    Log.d("Conexion ssh", "Resultado del comando:\n$outputDhcp")
                    tvConsole.text = outputDhcp
                    loader.dismissLoader()
                }
            }
        }
        buttonSettings.setOnClickListener{
            loader.showLoader("Obteniendo informacion de los routers...")
            CoroutineScope(Dispatchers.Main).launch {
                val result = fetchAndConcatenateStrings()
                tvConsole.text = result // Mostrar el resultado en el TextView
                val path = saveStringToExternalFile(result)
                Toast.makeText(baseContext, path, Toast.LENGTH_SHORT).show()
                loader.dismissLoader()
            }

        }

        lineChart = findViewById(R.id.lineChart)
        setupChart()
        initializeChartData()
        startUpdatingChart()

    }

    private fun setupChart() {
        lineChart.apply {
            description.isEnabled = false

            // Desactivar las líneas de cuadrícula de fondo
            xAxis.setDrawGridLines(true)
            axisLeft.setDrawGridLines(true)
            axisRight.setDrawGridLines(true)

            // Desactivar el eje derecho
            axisRight.isEnabled = false

            // Configurar el eje izquierdo para un estilo limpio
            //axisLeft.setDrawAxisLine(false) // Oculta la línea del eje Y
            //axisLeft.setDrawLabels(false)  // Oculta las etiquetas del eje Y

            // Configurar el eje X para un estilo limpio
            xAxis.setDrawAxisLine(false) // Oculta la línea del eje X
            xAxis.setDrawLabels(false)  // Oculta las etiquetas del eje X

            // Desactivar la leyenda
            legend.isEnabled = false

            // Habilitar interacciones
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setPinchZoom(true)

            // Fondo transparente
            setDrawGridBackground(false)
        }
    }

    private fun initializeChartData() {
        val lineData = LineData()
        lineChart.data = lineData
    }

    private fun startUpdatingChart() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                addEntry()
                handler.postDelayed(this, 10_000) // Actualizar cada 10 segundos
            }
        }, 10_000)
    }

    private fun addEntry() {
        val data = lineChart.data

        if (data != null) {
            var dataSet = data.getDataSetByIndex(0) as? LineDataSet

            if (dataSet == null) {
                dataSet = createSet()
                data.addDataSet(dataSet)
            }
            CoroutineScope(Dispatchers.Main).launch {
                val command =  "show interfaces FastEthernet0/0 | include minute input rate"
                val rate = sshManager.executeSSHCommand(host,port,user,password,command)
                val floatRate = parser.extractRate(rate)
                withContext(Dispatchers.Main) {
                    parser.extractRate(rate)
                    // Agregar un nuevo valor al gráfico (valor del ancho de banda usado)
                    Log.e("Ancho de banda", "ancho de banda usado $floatRate bits")
                    data.addEntry(Entry(dataSet.entryCount.toFloat(), floatRate), 0)
                    data.notifyDataChanged()
                    lineChart.notifyDataSetChanged()

                    lineChart.moveViewToX(data.entryCount.toFloat())
                    if (floatRate > BANDWIDTH_ALERT && wasBelowThreshold) {
                        val messageHTML = smtp.loadHtmlTemplate(baseContext, "Router-1", "Superó el 10% del ancho de banda")
                        smtp.sendMail("jehujuegos@gmail.com", "Alerta de uso de ancho de banda", messageHTML)
                        Toast.makeText(baseContext, "Has superado el ancho debanda predefinido", Toast.LENGTH_SHORT).show()
                        wasBelowThreshold = false
                    } else if (floatRate <= 8000) {
                        wasBelowThreshold = true
                    }
                }
            }
        }
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Activity Monitor")
        set.apply {
            axisDependency = lineChart.axisLeft.axisDependency
            color = resources.getColor(android.R.color.holo_green_dark, theme)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)

            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
        }
        return set
    }

    fun saveStringToExternalFile(content: String): String {
        val fileName = "detalles_routers.txt"
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { output ->
                output.write(content.toByteArray())
            }
            Log.e("detalles_routers.txt", file.absolutePath)
            "Archivo guardado en: ${file.absolutePath}" // Devuelve la ruta del archivo

        } catch (e: Exception) {
            e.printStackTrace()
            "Error al guardar el archivo: ${e.message}"
        }
    }

    private suspend fun fetchAndConcatenateStrings(): String {
        return coroutineScope {
            val deferred1 = async { sshManager.executeSSHCommandsWithShell(ipRouter3,
                port,
                user,
                password,
                listOf("terminal length 0","show running-config"))
            }
            val deferred2 = async { sshManager.executeSSHCommandsWithShell(ipRouter2,
                port,
                user,
                password,
                listOf("terminal length 0","show running-config"))
            }
            val deferred3 = async { sshManager.executeSSHCommandsWithShell(host,
                port,
                user,
                password,
                listOf("terminal length 0","show running-config"))
            }
            listOf(deferred1.await(), deferred2.await(), deferred3.await()).joinToString(separator = "\n------------\n")
        }
    }

    private suspend fun configurateVLAN(): String {
        return coroutineScope {
            val deferred1 = async { sshManager.executeSSHCommandsWithShell(host,
                port,
                user,
                password,
                listOf("terminal length 0",
                    "Configure terminal",
                    "Interface f3/0",
                    "no ip address",
                    "Interface f3/0.11",
                    "encapsulation dot1Q 11",
                    "ip add 192.168.30.1 255.255.255.0",
                    "Interface f3/0.12",
                    "encapsulation dot1Q 12",
                    "ip add 192.168.31.1 255.255.255.0",
                    "exit",
                    "Interface f3/0",
                    "no shutdown",
                    "exit",
                    "exit",
                    "show running-config | section interface"
                ))
            }
            val deferred2 = async { sshManager.executeSSHCommandsWithShell(ipSwitch,
                port,
                user,
                password,
                listOf("terminal length 0",
                    "vlan database",
                    "vlan 11 name ventas",
                    "vlan 12 name rh",
                    "exit",
                    "show vlan-switch brief",
                    "configure t",
                    "interface f1/1",
                    "switchport mode acces",
                    "switchport access vlan 11",
                    "int f1/2",
                    "switchport mode acces",
                    "switchport access vlan 12",
                    "exit",
                    "exit",
                    "show vlan-switch brief",
                    "configure t",
                    "int f1/0",
                    "switchport trunk encapsulation dot1q",
                    "switchport trunk allowed vlan all",
                    "switchport mode trunk",
                    "exit",
                    "exit"
                    ))
            }
            listOf(deferred1.await(), deferred2.await()).joinToString(separator = "\n=====================\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        snmpTrapReceiver.stopListening()
    }

}