/*
    This file is part of PCAP Remote.

    PCAP Remote is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    PCAP Remote is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with PCAP Remote. If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019 by Andrey Egorov
*/

package com.pcapremote

import timber.log.Timber
import java.net.NetworkInterface
import java.net.SocketException

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
