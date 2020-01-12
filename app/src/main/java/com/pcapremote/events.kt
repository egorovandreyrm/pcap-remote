package com.pcapremote

class SnifferRunningStateChanged

data class SnifferStatsEvent(val capturedPackets: Int, val sentToSsh: Int) {
    companion object {
        const val SENT_TO_SSH_CLIENT_NOT_CONNECTED = -1
    }
}