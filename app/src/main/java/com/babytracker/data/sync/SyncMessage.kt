package com.babytracker.data.sync

import com.babytracker.data.db.entity.BabyEvent
import org.json.JSONArray
import org.json.JSONObject

data class SyncMessage(
    val deviceId: String,
    val events: List<BabyEvent>
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("deviceId", deviceId)
        val arr = JSONArray()
        events.forEach { event ->
            val e = JSONObject()
            e.put("syncId", event.syncId)
            e.put("eventType", event.eventType)
            e.put("subType", event.subType)
            e.put("timestamp", event.timestamp)
            if (event.milliliters != null) e.put("milliliters", event.milliliters)
            if (event.note != null) e.put("note", event.note)
            arr.put(e)
        }
        obj.put("events", arr)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String): SyncMessage {
            val obj = JSONObject(json)
            val deviceId = obj.getString("deviceId")
            val arr = obj.getJSONArray("events")
            val events = (0 until arr.length()).map { i ->
                val e = arr.getJSONObject(i)
                BabyEvent(
                    id = 0,
                    syncId = e.getString("syncId"),
                    eventType = e.getString("eventType"),
                    subType = e.getString("subType"),
                    timestamp = e.getLong("timestamp"),
                    milliliters = if (e.has("milliliters")) e.getInt("milliliters") else null,
                    note = if (e.has("note")) e.getString("note") else null
                )
            }
            return SyncMessage(deviceId, events)
        }
    }
}
