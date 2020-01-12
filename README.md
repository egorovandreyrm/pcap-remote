PCAP Remote is a non-root network sniffer app that allows you to debug and analyze
Android traffic on your desktop PC using the app's built-in SSH server, which is useful and often a must when developing mobile applications that use complex/custom network protocols. The traffic is captured using an Android OS feature called VpnService.

The app is primarily designed to be used in conjunction with Wireshark, which is the most famous tool for network troubleshooting, analysis, software and communications protocol development, and education.

Although Wireshark is the tool that is recommended, other similar tools can also be used as captured packets are saved in the commonly used pcapng format.
Features:
#1: Remote capturing using the built-in SSH server;
#2: Supporting Wireshark sshdump tool (https://www.wireshark.org/docs/man-pages/sshdump.html);
#3: Capturing traffic as .pcap file;
#4: MITM (Man-in-the-middle) functionality, which allows you to decrypt traffic in Wireshark;
#5: No root required.

Limitations:
#1: TLS 1.3 is not supported when using the MITM functionality;
#2: The SSH server only supports IP v4 clients;
#3: Hotspot/Tethering traffic can't be captured.

Info/How-to: https://egorovandreyrm.com/pcap-remote-tutorial/
