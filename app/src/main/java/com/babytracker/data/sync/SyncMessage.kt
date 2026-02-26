package com.babytracker.data.sync

import com.babytracker.data.db.entity.BabyEvent
import com.babytracker.data.db.entity.SyncTombstone
import org.json.JSONArray
import org.json.JSONObject

data class SyncMessage(
    val deviceId: String,
    val deviceName: String = "",
    val events: List<BabyEvent>,
    val tombstones: List<SyncTombstone> = emptyList(),
    val babyName: String = ""
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("deviceId", deviceId)
        if (deviceName.isNotEmpty()) obj.put("deviceName", deviceName)
        if (babyName.isNotEmpty()) obj.put("babyName", babyName)

        val eventsArr = JSONArray()
        events.forEach { event ->
            val e = JSONObject()
            e.put("syncId", event.syncId)
            e.put("eventType", event.eventType)
            e.put("subType", event.subType)
            e.put("timestamp", event.timestamp)
            e.put("updatedAt", event.updatedAt)
            if (event.milliliters != null) e.put("milliliters", event.milliliters)
            if (event.note != null) e.put("note", event.note)
            eventsArr.put(e)
        }
        obj.put("events", eventsArr)

        val tombstonesArr = JSONArray()
        tombstones.forEach { t ->
            val entry = JSONObject()
            entry.put("syncId", t.syncId)
            entry.put("deletedAt", t.deletedAt)
            tombstonesArr.put(entry)
        }
        obj.put("tombstones", tombstonesArr)

        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): SyncMessage {
            val obj = JSONObject(json)
            val deviceId = obj.getString("deviceId")

            val eventsArr = obj.getJSONArray("events")
            val events = (0 until eventsArr.length()).map { i ->
                val e = eventsArr.getJSONObject(i)
                BabyEvent(
                    id = 0,
                    syncId = e.getString("syncId"),
                    eventType = e.getString("eventType"),
                    subType = e.getString("subType"),
                    timestamp = e.getLong("timestamp"),
                    // Backward-compatible: fall back to timestamp if updatedAt missing
                    updatedAt = if (e.has("updatedAt")) e.getLong("updatedAt") else e.getLong("timestamp"),
                    milliliters = if (e.has("milliliters")) e.getInt("milliliters") else null,
                    note = if (e.has("note")) e.getString("note") else null
                )
            }

            val tombstones = if (obj.has("tombstones")) {
                val arr = obj.getJSONArray("tombstones")
                (0 until arr.length()).map { i ->
                    val t = arr.getJSONObject(i)
                    SyncTombstone(
                        syncId = t.getString("syncId"),
                        deletedAt = t.getLong("deletedAt")
                    )
                }
            } else emptyList()

            val deviceName = if (obj.has("deviceName")) obj.getString("deviceName") else ""
            val babyName = if (obj.has("babyName")) obj.getString("babyName") else ""
            return SyncMessage(deviceId, deviceName, events, tombstones, babyName)
        }
    }
}
