package com.silvered.ecodrive.util

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.Socket

data class Client(val ip: String, val port: Int) {

    interface ClientCallback {
        fun onMessage(message: String)
        fun onConnect(socket: Socket)
        fun onDisconnect(socket: Socket, message: String)
        fun onConnectError(socket: Socket?, message: String)
    }

    private lateinit var socket: Socket
    private lateinit var socketOutput: OutputStream
    private lateinit var socketInput: BufferedReader

    private val TAG = "Client"

    private var listener: ClientCallback? = null

    fun connect(listener: ClientCallback) {

        Thread {

            try {

                this.listener = listener

                socket = Socket(ip, port)
                socketOutput = socket.getOutputStream()
                socketInput = BufferedReader(InputStreamReader(socket.getInputStream()))


                Thread {

                    var charsRead = 0
                    val buffer = CharArray(1024)
                    var message: String? = null

                    try {

                        while (true) {
                            charsRead = socketInput.read(buffer)
                            message = String(buffer).substring(0, charsRead)
                            if (message != null && listener != null) {
                                listener.onMessage(message)
                            }
                            message = null
                        }
                    } catch (e: Exception) {
                        Log.d(TAG,"Il client ")
                        e.message?.let { listener?.onDisconnect(socket, it) }
                    }

                }.start()

                listener.onConnect(socket)

            } catch (e: Exception) {
                e.message?.let { listener?.onConnectError(null, it) }
            }

        }.start()

    }

    fun disconnect() {
        try {
            if (this::socket.isInitialized && socket.isConnected) {
                socket.close()
            }
        } catch (e: Exception) {
            e.message?.let { listener?.onDisconnect(socket, it) }
        }finally {
            removeClientCallBack()
        }
    }

    fun send(message: String) {
        try {
            socketOutput.write(message.toByteArray())
        } catch (e: Exception) {
            e.message?.let { listener?.onDisconnect(socket, it) }
        }
    }

    fun removeClientCallBack() {
        this.listener = null
    }



}