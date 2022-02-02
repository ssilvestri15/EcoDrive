package com.silvered.ecodrive

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdManager.DiscoveryListener
import android.net.nsd.NsdServiceInfo
import android.util.Log

/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class NsdHelper(mContext: Context) {

    interface NsdCallback {
        fun onServerFound(ip: String)
    }

    private var mNsdManager: NsdManager
    private var mResolveListener: NsdManager.ResolveListener? = null
    private var mDiscoveryListener: DiscoveryListener? = null
    private var mServiceName = "Carla"
    private var chosenServiceInfo: NsdServiceInfo? = null

    private var listener: NsdCallback? = null

    fun setNsdCallBack(listener: NsdCallback) {
        this.listener = listener
    }

    fun removeNsdCallBack() {
        this.listener = null
    }

    fun initializeNsd(listener: NsdCallback?) {

        if (this.listener == null)
            this.listener = listener


        initializeResolveListener()
        discoverServices()
    }

    fun initializeDiscoveryListener() {
        mDiscoveryListener = object : DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Service discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service discovery success$service")
                if (service.serviceType != SERVICE_TYPE) {
                    Log.d(TAG, "Unknown Service Type: " + service.serviceType)
                } else if (service.serviceName == mServiceName) {
                    Log.d(TAG, "Same machine: $mServiceName")
                } else if (service.serviceName.contains(mServiceName)) {
                    mNsdManager.resolveService(service, mResolveListener)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.e(TAG, "service lost$service")
                if (chosenServiceInfo == service) {
                    chosenServiceInfo = null
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(
                    TAG,
                    "Discovery failed: Error code:$errorCode"
                )
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(
                    TAG,
                    "Discovery failed: Error code:$errorCode"
                )
            }
        }
    }

    fun initializeResolveListener() {
        mResolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed$errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Resolve Succeeded. $serviceInfo")
                if (serviceInfo.serviceName == mServiceName) {
                    Log.d(TAG, "Same IP.")
                    return
                }
                chosenServiceInfo = serviceInfo

                if (listener == null)
                    Log.d(TAG,"NULL")

                listener?.onServerFound(chosenServiceInfo!!.host.hostAddress!!)
            }
        }
    }

    fun discoverServices() {
        stopDiscovery() // Cancel any existing discovery request
        initializeDiscoveryListener()
        mNsdManager.discoverServices(
            SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener
        )
    }

    fun stopDiscovery() {
        if (mDiscoveryListener != null) {
            try {
                mNsdManager.stopServiceDiscovery(mDiscoveryListener)
            } finally {
            }
            mDiscoveryListener = null
        }
    }

    companion object {
        const val SERVICE_TYPE = "_http._tcp."
        const val TAG = "NsdHelper"
    }

    init {
        Log.d(TAG,"Inizializzato")
        mNsdManager = mContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

}