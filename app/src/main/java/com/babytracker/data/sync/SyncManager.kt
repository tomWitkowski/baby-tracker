package com.babytracker.data.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.babytracker.data.db.dao.BabyEventDao
import com.babytracker.data.db.dao.SyncTombstoneDao
import com.babytracker.data.db.dao.TrustedDeviceDao
import com.babytracker.data.db.entity.DiaperSubType
import com.babytracker.data.db.entity.TrustedDevice
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.data.db.entity.EventType
import com.babytracker.data.db.entity.FeedingSubType
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
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections
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
    /** Server rejected our request — waiting for the owner to approve on their phone */
    object AwaitingApproval : SyncState()
}

/** Pending trust-approval request from an unknown incoming device */
data class TrustRequest(val deviceId: String, val deviceName: String)

@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: BabyEventDao,
    private val tombstoneDao: SyncTombstoneDao,
    private val trustedDeviceDao: TrustedDeviceDao,
    private val prefs: AppPreferences
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val SERVICE_TYPE = "_babytracker._tcp."
        private const val PORT = 47654
        private const val DISCOVERY_TIMEOUT_MS = 8000L
        private const val RESULT_DISPLAY_MS = 3000L

        // Socket timeouts
        private const val SOCKET_CONNECT_TIMEOUT_MS = 5_000
        private const val SOCKET_SO_TIMEOUT_MS = 10_000
        private const val SYNC_TOTAL_TIMEOUT_MS = 25_000L

        // Security limits — prevent OOM and bulk-deletion attacks
        private const val MAX_JSON_BYTES = 5 * 1024 * 1024        // 5 MB
        private const val MAX_EVENTS_PER_SYNC = 5_000
        private const val MAX_TOMBSTONES_PER_SYNC = 5_000
        private const val MAX_NOTE_LENGTH = 1_000

        // Valid enum values for input validation
        private val VALID_EVENT_TYPES = EventType.entries.map { it.name }.toSet()
        private val VALID_SUB_TYPES =
            FeedingSubType.entries.map { it.name }.toSet() +
            DiaperSubType.entries.map { it.name }.toSet() +
            setOf(EventType.SPIT_UP.name)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    /** Non-null when an unknown device is requesting sync and awaits user approval */
    private val _pendingTrustRequest = MutableStateFlow<TrustRequest?>(null)
    val pendingTrustRequest: StateFlow<TrustRequest?> = _pendingTrustRequest

    /**
     * Device IDs approved for this session only (not persisted to DB).
     * Thread-safe because sync operations run on IO dispatcher.
     */
    private val sessionTrustedIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    /** Human-readable name of this device sent alongside our deviceId */
    private val localDeviceName: String =
        "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(64)

    val deviceId: String by lazy {
        val syncPrefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        syncPrefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
            syncPrefs.edit().putString("device_id", it).apply()
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
            socket.soTimeout = SOCKET_SO_TIMEOUT_MS
            socket.use {
                val reader = BufferedReader(InputStreamReader(it.inputStream))
                val writer = PrintWriter(it.outputStream, true)

                val json = readLimitedLine(reader) ?: return
                val received = SyncMessage.fromJson(json)

                // ── Trust check ──────────────────────────────────────────────
                // Allow only: ourselves (same UUID), session-approved IDs, or
                // permanently trusted IDs stored in DB.
                val isOwnDevice = received.deviceId == deviceId
                val isSessionTrusted = received.deviceId in sessionTrustedIds
                val isDbTrusted = trustedDeviceDao.isTrusted(received.deviceId) > 0

                if (!isOwnDevice && !isSessionTrusted && !isDbTrusted) {
                    Log.i(TAG, "Server: unknown device ${received.deviceId} — requesting approval")
                    val safeName = received.deviceName.take(64).ifEmpty { received.deviceId.take(8) }
                    writer.println(
                        JSONObject()
                            .put("approved", false)
                            .put("reason", "approval_required")
                            .toString()
                    )
                    _pendingTrustRequest.value = TrustRequest(received.deviceId, safeName)
                    return
                }
                // ─────────────────────────────────────────────────────────────

                // Validate limits before processing to prevent bulk-deletion attacks
                if (received.events.size > MAX_EVENTS_PER_SYNC ||
                    received.tombstones.size > MAX_TOMBSTONES_PER_SYNC) {
                    Log.w(TAG, "Server: rejected oversized sync from ${received.deviceId}")
                    return
                }

                Log.d(TAG, "Server: received ${received.events.size} events, ${received.tombstones.size} tombstones from ${received.deviceId}")

                // Merge their data, then send back our current state (post-merge)
                mergeReceivedData(received)
                if (received.babyName.isNotEmpty()) prefs.setBabyName(received.babyName)

                val ourEvents = dao.getAllEventsSync()
                val ourTombstones = tombstoneDao.getAllTombstones()
                writer.println(SyncMessage(deviceId, localDeviceName, ourEvents, ourTombstones, prefs.babyName.value).toJson())

                Log.d(TAG, "Server: sync complete, sent ${ourEvents.size} events back")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming connection: ${e.message}")
        }
    }

    // ── Trust management ─────────────────────────────────────────────────────

    /**
     * Called from UI when user approves an incoming trust request.
     * @param permanent if true, the device is stored in DB and trusted permanently;
     *                  if false, trusted only for this app session.
     */
    suspend fun approveTrust(permanent: Boolean) {
        val request = _pendingTrustRequest.value ?: return
        sessionTrustedIds.add(request.deviceId)
        if (permanent) {
            trustedDeviceDao.insert(
                TrustedDevice(
                    deviceId = request.deviceId,
                    deviceName = request.deviceName,
                    addedAt = System.currentTimeMillis()
                )
            )
            Log.i(TAG, "Trust granted permanently to ${request.deviceName} (${request.deviceId})")
        } else {
            Log.i(TAG, "Trust granted for this session to ${request.deviceName}")
        }
        _pendingTrustRequest.value = null
    }

    /** Called from UI when user denies the trust request. */
    fun denyTrust() {
        val request = _pendingTrustRequest.value
        Log.i(TAG, "Trust denied for ${request?.deviceName} (${request?.deviceId})")
        _pendingTrustRequest.value = null
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

        // socket.soTimeout is the primary hang-prevention mechanism.
        // withTimeoutOrNull is a last-resort safety net for the overall flow.
        try {
            val changes = withTimeoutOrNull(SYNC_TOTAL_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    val socket = Socket()
                    socket.soTimeout = SOCKET_SO_TIMEOUT_MS
                    socket.connect(InetSocketAddress(host, discoveredPort), SOCKET_CONNECT_TIMEOUT_MS)
                    socket.use { s ->
                        val reader = BufferedReader(InputStreamReader(s.inputStream))
                        val writer = PrintWriter(s.outputStream, true)

                        val ourEvents = dao.getAllEventsSync()
                        val ourTombstones = tombstoneDao.getAllTombstones()
                        writer.println(SyncMessage(deviceId, localDeviceName, ourEvents, ourTombstones, prefs.babyName.value).toJson())

                        val json = readLimitedLine(reader) ?: return@use 0

                        // Check whether server rejected us (approval required on their side)
                        try {
                            val check = JSONObject(json)
                            if (!check.optBoolean("approved", true) &&
                                check.optString("reason") == "approval_required") {
                                Log.i(TAG, "Client: server requires approval — ask user on that device")
                                return@use -1  // sentinel: AwaitingApproval
                            }
                        } catch (_: Exception) { /* not a rejection object, proceed normally */ }

                        val received = SyncMessage.fromJson(json)

                        // Validate limits
                        if (received.events.size > MAX_EVENTS_PER_SYNC ||
                            received.tombstones.size > MAX_TOMBSTONES_PER_SYNC) {
                            Log.w(TAG, "Client: rejected oversized sync response")
                            return@use 0
                        }

                        Log.d(TAG, "Client: received ${received.events.size} events, ${received.tombstones.size} tombstones")
                        val count = mergeReceivedData(received)
                        if (received.babyName.isNotEmpty()) prefs.setBabyName(received.babyName)
                        count
                    }
                }
            }

            when {
                changes == null -> {
                    Log.w(TAG, "Sync timed out after ${SYNC_TOTAL_TIMEOUT_MS}ms")
                    _syncState.value = SyncState.Error("Przekroczono limit czasu synchronizacji")
                }
                changes == -1 -> _syncState.value = SyncState.AwaitingApproval
                else -> _syncState.value = SyncState.Success(changes)
            }
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
     *
     * Rules:
     * 1. Tombstones (deletions) are applied first — deletion always wins.
     *    This ensures that a deletion on one device propagates to the other,
     *    even if the other device was offline when the deletion happened.
     *
     * 2. For events: insert if new, update if the received version is newer (updatedAt).
     *    Skip if the event is locally tombstoned (we deleted it).
     *    This "last write wins" strategy means offline edits are preserved as long as
     *    the device clock is roughly accurate relative to the partner device.
     *
     * Offline scenario guarantee: events added offline on either device will be
     * inserted on the other device during the next sync. No offline data is lost
     * unless one device explicitly deleted it (tombstone).
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

        // Step 2: apply events (skip tombstoned, validate before inserting)
        received.events.forEach { event ->
            if (!isValidEvent(event)) {
                Log.w(TAG, "Skipping invalid event: type=${event.eventType} sub=${event.subType}")
                return@forEach
            }
            if (tombstoneDao.countBySyncId(event.syncId) > 0) return@forEach

            // Sanitize note length before persisting
            val safeEvent = if (event.note != null && event.note.length > MAX_NOTE_LENGTH) {
                event.copy(note = event.note.take(MAX_NOTE_LENGTH))
            } else {
                event
            }

            val existing = dao.getEventBySyncId(safeEvent.syncId)
            when {
                existing == null -> {
                    val result = dao.insertEventIgnore(safeEvent.copy(id = 0))
                    if (result != -1L) changes++
                }
                safeEvent.updatedAt > existing.updatedAt -> {
                    dao.updateEvent(safeEvent.copy(id = existing.id))
                    changes++
                }
                // else: our version is equal or newer — keep it
            }
        }

        return changes
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Reads a single line from [reader] but aborts if the payload exceeds [maxBytes].
     * This prevents OOM attacks from a malformed device sending an unbounded stream.
     */
    @Throws(IOException::class)
    private fun readLimitedLine(reader: BufferedReader, maxBytes: Int = MAX_JSON_BYTES): String? {
        val sb = StringBuilder()
        var count = 0
        var ch: Int
        while (reader.read().also { ch = it } != -1) {
            if (ch == '\n'.code) break
            sb.append(ch.toChar())
            count++
            if (count > maxBytes) throw IOException("Sync payload too large (>${maxBytes}B)")
        }
        return if (sb.isEmpty() && ch == -1) null else sb.toString()
    }

    /**
     * Returns true only if the event carries known enum values.
     * Rejects events with unknown types to prevent crashes in the rest of the app
     * (e.g. EventType.valueOf() would throw on an unknown string).
     */
    private fun isValidEvent(event: com.babytracker.data.db.entity.BabyEvent): Boolean {
        if (event.eventType !in VALID_EVENT_TYPES) return false
        if (event.subType !in VALID_SUB_TYPES) return false
        if (event.milliliters != null && (event.milliliters < 0 || event.milliliters > 9999)) return false
        return true
    }
}
