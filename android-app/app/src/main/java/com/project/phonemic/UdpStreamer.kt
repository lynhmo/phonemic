package com.project.phonemic

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UdpStreamer(private val host: String, private val port: Int) {

    private var socket: DatagramSocket? = null
    private var address: InetAddress? = null

    fun start() {
        socket = DatagramSocket()
        address = InetAddress.getByName(host) // resolve once
    }

    fun send(data: ByteArray, length: Int) {
        val s = socket ?: return
        val a = address ?: return
        try {
            s.send(DatagramPacket(data, length, a, port))
        } catch (_: Exception) {}
    }

    fun stop() {
        socket?.close()
        socket = null
    }
}
