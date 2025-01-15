package com.example.miadministradorredes.utils
import org.snmp4j.*
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import org.snmp4j.security.SecurityProtocols
import org.snmp4j.event.ResponseEvent
import org.snmp4j.mp.SnmpConstants

class SNMPManager(private val targetAddress: String, private var community: String) {

    private lateinit var snmp: Snmp

    // Inicializa SNMP
    fun init() {
        try {
            val transport = DefaultUdpTransportMapping()
            snmp = Snmp(transport)
            SecurityProtocols.getInstance().addDefaultProtocols()
            snmp.listen()
            println("SNMP inicializado.")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Realiza una consulta SNMP para una OID específica
    fun getOIDValue(oid: String): String? {
        try {
            val target = CommunityTarget<UdpAddress>().apply {
                address = GenericAddress.parse("udp:$targetAddress/161") as UdpAddress?
                community = OctetString(this@SNMPManager.community)
                version = SnmpConstants.version2c
                retries = 2
                timeout = 1500
            }

            val pdu = PDU().apply {
                type = PDU.GET
                add(VariableBinding(OID(oid)))
            }

            val responseEvent: ResponseEvent<UdpAddress> = snmp.send(pdu, target)
            val response = responseEvent.response

            if (response == null) {
                println("No se recibió respuesta.")
                return null
            } else {
                return response.variableBindings[0].toValueString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun close() {
        try {
            snmp.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
