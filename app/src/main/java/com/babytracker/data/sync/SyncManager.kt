package com.babytracker.data.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.babytracker.data.db.dao.BabyEventDao
import com.babytracker.data.db.dao.SyncTombstoneDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncState {
    object Idle : SyncState()
    object Searching : SyncState()
    object Syncing : SyncState()
    data class Success(val added: Int) : SyncState()
    data class Error(val message: String) : SyncState()
    object NoDeviceFound : SyncState()
}

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: BabyEventDao,
    private val tombstoneDao: SyncTombstoneDao
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val SERVICE_TYPE = "_babytracker._tcp."
        private const val PORT = 47654
        private const val DISCOVERY_TIMEOUT_MS = 8000L
        private const val RESULT_DISPLAY_MS = 3000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    val deviceId: String by lazy {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("device_id", it).apply()
        }
    }

    private var serverSocket: ServerSocket? = null
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    @Volatile private var registeredServiceName: String? = null
    @Volatile private var discoveredHost: String? = null
    @Volatile private var discoveredPort: Int = PORT

    @Volatile private var isStarted = false

    fun start() {
        if (isStarted) return
        isStarted = true
        startServer()
        startNsd()
    }

    // -------------------------------------------------------------------------
    // TCP server
    // -------------------------------------------------------------------------

    private fun startServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "TCP server started on port $PORT")
                while (true) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleIncomingConnection(client) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
            }
        }
    }

    private suspend fun handleIncomingConnection(socket: Socket) {
        try {
            socket.use {
                val reader = BufferedReader(InputStreamReader(it.inputStream))
                val writer = PrintWriter(it.outputStream, true)

                val json = reader.readLine() ?: return
                val received = SyncMessage.fromJson(json)
                Log.d(TAG, "Server: received ${received.events.size} events, ${received.tombstones.size} tombstones from ${received.deviceId}")

                // Merge their data, then send back our current state (post-merge)
                mergeReceivedData(received)

                val ourEvents = dao.getAllEventsSync()
                val ourTombstones = tombstoneDao.getAllTombstones()
                writer.println(SyncMessage(deviceId, ourEvents, ourTombstones).toJson())

                Log.d(TAG, "Server: sync complete, sent ${ourEvents.size} events back")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming connection: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // NSD (mDNS service discovery)
    // -------------------------------------------------------------------------

    private fun startNsd() {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {
                Log.e(TAG, "NSD registration failed: $code")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {}
            override fun onServiceRegistered(info: NsdServiceInfo) {
                registeredServiceName = info.serviceName
                Log.d(TAG, "NSD registered as: ${info.serviceName}")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
        }

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) {
                Log.d(TAG, "NSD discovery started")
            }
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, code: Int) {
                Log.e(TAG, "NSD discovery start failed: $code")
            }
            override fun onStopDiscoveryFailed(type: String, code: Int) {}

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceName == registeredServiceName) return // skip ourselves
                Log.d(TAG, "Found service: ${service.serviceName}")
                nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, code: Int) {
                        Log.e(TAG, "Resolve failed: $code")
                    }
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        Log.d(TAG, "Resolved: ${info.host.hostAddress}:${info.port}")
                        discoveredHost = info.host.hostAddress
                        discoveredPort = info.port
                    }
                })
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                if (service.serviceName != registeredServiceName) {
                    Log.d(TAG, "Service lost: ${service.serviceName}")
                    discoveredHost = null
                }
            }
        }

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "BabyTracker"
            serviceType = SERVICE_TYPE
            port = PORT
        }

        try {
            nsdManager!!.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            nsdManager!!.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "NSD setup error: ${e.message}")
        }
    }

    // -------------------------------------------------------------------------
    // Client-initiated sync
    // -------------------------------------------------------------------------

    suspend fun syncNow() {
        val current = _syncState.value
        if (current is SyncState.Syncing || current is SyncState.Searching) return

        _syncState.value = SyncState.Searching

        val host = withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) {
            while (discoveredHost == null) {
                delay(200)
            }
            discoveredHost
        }

        if (host == null) {
            _syncState.value = SyncState.NoDeviceFound
            delay(RESULT_DISPLAY_MS)
            _syncState.value = SyncState.Idle
            return
        }

        _syncState.value = SyncState.Syncing

        try {
            val changes = withContext(Dispatchers.IO) {
                Socket(host, discoveredPort).use { socket ->
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))
                    val writer = PrintWriter(socket.outputStream, true)

                    val ourEvents = dao.getAllEventsSync()
                    val ourTombstones = tombstoneDao.getAllTombstones()
                    writer.println(SyncMessage(deviceId, ourEvents, ourTombstones).toJson())

                    val json = reader.readLine() ?: return@use 0
                    val received = SyncMessage.fromJson(json)
                    Log.d(TAG, "Client: received ${received.events.size} events, ${received.tombstones.size} tombstones")

                    mergeReceivedData(received)
                }
            }
            _syncState.value = SyncState.Success(changes)
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}")
            _syncState.value = SyncState.Error(e.message ?: "Błąd połączenia")
        }

        delay(RESULT_DISPLAY_MS)
        _syncState.value = SyncState.Idle
    }

    // -------------------------------------------------------------------------
    // Merge logic
    // -------------------------------------------------------------------------

    /**
     * Merges received data into local DB.
     * Rules:
     * 1. Tombstones (deletions) are applied first — deletion always wins.
     * 2. For events: insert if new, update if the received version is newer (updatedAt).
     *    Skip if the event is locally tombstoned (we deleted it).
     *
     * Returns count of local DB rows changed.
     */
    private suspend fun mergeReceivedData(received: SyncMessage): Int {
        var changes = 0

        // Step 1: apply tombstones
        received.tombstones.forEach { tombstone ->
            tombstoneDao.insertTombstone(tombstone)
            val deleted = dao.deleteEventBySyncId(tombstone.syncId)
            if (deleted > 0) changes++
        }

        // Step 2: apply events (skip if we tombstoned them locally)
        received.events.forEach { event ->
            if (tombstoneDao.countBySyncId(event.syncId) > 0) return@forEach

            val existing = dao.getEventBySyncId(event.syncId)
            when {
                existing == null -> {
                    val result = dao.insertEventIgnore(event.copy(id = 0))
                    if (result != -1L) changes++
                }
                event.updatedAt > existing.updatedAt -> {
                    dao.updateEvent(event.copy(id = existing.id))
                    changes++
                }
                // else: our version is equal or newer — keep it
            }
        }

        return changes
    }
}
