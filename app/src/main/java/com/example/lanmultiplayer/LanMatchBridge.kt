package com.example.lanmultiplayer

import android.app.Activity
import android.content.Intent
import java.lang.ref.WeakReference
/** Java/XML 原版 MainActivity 与 LAN 层之间的最小桥接，不触碰原版计时界面。 */
object LanMatchBridge {
    interface Sender { fun publish(timeMs: Long, penalty: Int, dnf: Boolean, scramble: String) }
    @Volatile private var publish: Sender? = null
    @Volatile private var enabled = false
    @Volatile private var roomName: String? = null
@Volatile private var roomActivity: WeakReference<Activity>? = null
    @Volatile private var lanActivity: WeakReference<Activity>? = null

    @JvmStatic fun setRoom(name: String?) { roomName = name?.takeIf { it.isNotBlank() } }
    @JvmStatic fun hasRoom(): Boolean = enabled && roomName != null
    @JvmStatic fun getRoomName(): String? = roomName
    @JvmStatic fun bindActivity(activity: Activity) { roomActivity = WeakReference(activity) }
    @JvmStatic fun bindLanActivity(activity: Activity) { lanActivity = WeakReference(activity) }
    @JvmStatic fun openRoom(activity: Activity) {
        if (roomName == null) return
        val existing = lanActivity?.get()
        if (existing != null && !existing.isFinishing) {
            existing.startActivity(Intent(existing, com.example.lanmultiplayer.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        } else {
            activity.startActivity(Intent(activity, com.example.lanmultiplayer.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }

    @JvmStatic fun attach(sender: Sender) {
        publish = sender
        enabled = true
    }

    @JvmStatic fun detach() {
        enabled = false
        publish = null
        roomName = null
        roomActivity = null
    }

    @JvmStatic fun publishFinish(timeMs: Long, penalty: Int, dnf: Boolean, scramble: String) {
        if (enabled) publish?.publish(timeMs, penalty, dnf, scramble)
    }
}