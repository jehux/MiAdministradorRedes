package com.example.miadministradorredes.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.snmp4j.CommandResponder
import org.snmp4j.CommandResponderEvent
import org.snmp4j.Snmp
import org.snmp4j.TransportMapping
import org.snmp4j.smi.Address
import org.snmp4j.smi.GenericAddress
import org.snmp4j.smi.UdpAddress
import org.snmp4j.transport.DefaultUdpTransportMapping

class SnmpTrapReceiver {

    private lateinit var snmp: Snmp

    fun startListening() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Dirección para escuchar traps (puerto 162 por defecto)
                val address: Address = GenericAddress.parse("udp:192.168.1.74/1162")
                val transport: TransportMapping<UdpAddress> =
                    DefaultUdpTransportMapping(address as UdpAddress)
                snmp = Snmp(transport)

                // Crear una instancia de CommandResponder para manejar traps
                snmp.addCommandResponder(object : CommandResponder {
                    override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
                        val pdu = event?.pdu
                        if (pdu != null) {
                            println("Trap recibido desde: ${event.peerAddress}")
                            println("Información del trap: ${pdu.variableBindings}")
                        }
                    }
                })

                transport.listen()
                Log.e("snmp", "Esperando traps SNMP en puerto 1162...")
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("snmp", "Error al iniciar el receptor SNMP: ${e.message}")
            }
        }
    }

    fun stopListening() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                snmp.close()
                Log.e("snmp", "Receptor SNMP detenido.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
