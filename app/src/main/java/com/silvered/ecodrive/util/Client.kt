package com.silvered.ecodrive.util

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.StringBuilder
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

                    try {

                        val builder = StringBuilder()
                        while (true) {

                            builder.append(socketInput.read().toChar())
                            val message = builder.toString()
                            if (isValidJson(message)) {
                                builder.clear()
                                listener?.onMessage(message)
                            }
                        }

                    } catch (e: Exception) {
                        e.message?.let { listener?.onDisconnect(socket, it) }
                    }

                }.start()

                listener.onConnect(socket)

            } catch (e: Exception) {
                e.message?.let { listener?.onConnectError(null, it) }
            }

        }.start()

    }

    private fun isValidJson(message: String): Boolean {

        try {
            JSONObject(message)
        } catch (ex: Exception) {
            return false
        }

        return true
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