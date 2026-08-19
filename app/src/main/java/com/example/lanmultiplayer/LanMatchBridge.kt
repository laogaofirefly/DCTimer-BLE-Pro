package com.example.lanmultiplayer

/** Java/XML 原版 MainActivity 与 LAN 层之间的最小桥接，不触碰原版计时界面。 */
object LanMatchBridge {
    interface Sender { fun publish(timeMs: Long, penalty: Int, dnf: Boolean, scramble: String) }
    @Volatile private var publish: Sender? = null
    @Volatile private var enabled = false

    @JvmStatic fun attach(sender: Sender) {
        publish = sender
        enabled = true
    }

    @JvmStatic fun detach() {
        enabled = false
        publish = null
    }

    @JvmStatic fun publishFinish(timeMs: Long, penalty: Int, dnf: Boolean, scramble: String) {
        if (enabled) publish?.publish(timeMs, penalty, dnf, scramble)
    }
}