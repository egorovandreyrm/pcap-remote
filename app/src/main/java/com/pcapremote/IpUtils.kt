package com.pcapremote

import timber.log.Timber
import java.net.NetworkInterface
import java.net.SocketException

/**
 * Created by andrey on 17.09.17.
 */

object IpUtils {

    val wifiIpV4Addr: String?
        get() {
            return getWlan0IpAddr(ipv4 = true, ipv6 = false)
        }

    val wifiIpV6Addr: String?
        get() {
            return getWlan0IpAddr(ipv4 = false, ipv6 = true)
        }

    private fun getWlan0IpAddr(ipv4: Boolean, ipv6: Boolean): String? {
        try {
            val ifr = NetworkInterface.getByName("wlan0")
            if (null != ifr) {
                if (!ifr.isLoopback && ifr.isUp) {
                    for (addr in ifr.inetAddresses) {
                        var strAddr = addr.hostAddress

                        if (-1 == strAddr.indexOf(':')) {
                            // v4 addr
                            if (ipv4) {
                                return strAddr
                            }
                        } else {
                            // v6 addr
                            if (ipv6) {
                                val percentSymIndex = strAddr.indexOf('%')
                                if (-1 != percentSymIndex) {
                                    strAddr = strAddr.substring(0, percentSymIndex)
                                }

                                return strAddr
                            }
                        }
                    }
                }
            } else {
                Timber.w("ifr is null")
            }
        } catch (ex: Exception) {
            Timber.e(ex)
        }

        return null
    }

    fun getNetworkInterfacesString(): String {
        val stringBuilder = StringBuilder()

        try {
            for (networkIfr in NetworkInterface.getNetworkInterfaces()) {
                if (!networkIfr.isLoopback && networkIfr.isUp) {
                    for (initAddr in networkIfr.inetAddresses) {
                        var strAddr = initAddr.hostAddress
                        if (-1 != strAddr.indexOf(':')) {
                            // ipv6 addr
                            val percentSymIndex = strAddr.indexOf('%')
                            if (-1 != percentSymIndex) {
                                strAddr = strAddr.substring(0, percentSymIndex)
                            }
                        }

                        if (stringBuilder.isNotEmpty()) {
                            stringBuilder.append("\n")
                        }

                        stringBuilder.append("${networkIfr.name}: $strAddr")
                    }
                }
            }
        } catch (ex: SocketException) {
            Timber.e(ex)
        }

        return stringBuilder.toString()
    }
}
