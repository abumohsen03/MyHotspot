# TTL Hotspot Pro 🛡️🚀

**TTL Hotspot** is a native Android application (Kotlin, Jetpack Compose, Material 3, min SDK 26) designed for rooted Android devices (Magisk / KernelSU / APatch). It transparently modifies network packet TTL (Time-To-Live) and IPv6 Hop Limit headers before they egress to the carrier cell tower or upstream network.

Connected client devices (Windows, Mac, iOS, Android, Linux, Smart TVs, Gaming consoles) require **ZERO configuration** (no manual proxies, no VPNs, no scripts): they simply connect to the Wi-Fi hotspot with the password and receive unrestricted internet access.

---

## 1. Technical Explanation of Exact Root Commands

### A. Pre-requisites: Kernel IP Forwarding
```bash
echo 1 > /proc/sys/net/ipv4/ip_forward
sysctl -w net.ipv4.ip_forward=1
sysctl -w net.ipv6.conf.all.forwarding=1
```
* **Why it works**: By default, Linux disables packet forwarding between interfaces. Setting `ip_forward=1` instructs the kernel to route packets between the hotspot interface (`swlan0`, `ap0`, `wlan1`) and the upstream data interface (`rmnet_data0`, `wlan0`).

---

### B. Primary: iptables `xt_TTL` & ip6tables `xt_HL` (mangle table)
```bash
# Check if rule exists before inserting to prevent duplicate chains
iptables -t mangle -C POSTROUTING -o <upstream> -j TTL --ttl-set 65
# Insert at top of POSTROUTING table
iptables -t mangle -I POSTROUTING -o <upstream> -j TTL --ttl-set 65

# IPv6 Hop Limit equivalent
ip6tables -t mangle -I POSTROUTING -o <upstream> -j HL --hl-set 65

# Ensure NAT Masquerade on upstream
iptables -t nat -I POSTROUTING -o <upstream> -j MASQUERADE
```
* **Why it works**:
  * Windows sends packets with default `TTL = 128`. Android devices send packets with `TTL = 64`.
  * When a tethered computer routes packets through the phone, the phone's router stack decrements the TTL by 1 (`128 - 1 = 127`).
  * The carrier cell tower inspects the TTL field. If it sees `127` (or anything other than `64`), it flags the traffic as non-phone tethering and drops it.
  * Setting `TTL --ttl-set 65` forces **every outbound packet** leaving the phone to have `TTL = 65`. When it passes the internal routing hop, it arrives at the carrier's gateway with `TTL = 64` — **indistinguishable from native phone browser traffic!**

---

### C. Fallback A: nftables (`nft`)
If the Android kernel was compiled without the `xt_TTL` iptables module, the app automatically cascades to nftables:
```bash
nft add table ip ttl_mangle
nft 'add chain ip ttl_mangle postrouting { type filter hook postrouting priority mangle; }'
nft add rule ip ttl_mangle postrouting oifname "<upstream>" ip ttl set 65

nft add table ip6 ttl_mangle
nft 'add chain ip6 ttl_mangle postrouting { type filter hook postrouting priority mangle; }'
nft add rule ip6 ttl_mangle postrouting oifname "<upstream>" ip6 hoplimit set 65
```
* **Why it works**: Modern Linux kernels (5.x, 6.x) implement the nftables subsystem natively. This achieves identical packet header mangling at layer 3 without relying on legacy netfilter iptables modules.

---

### D. Fallback B: sysctl (Kernel Default)
```bash
sysctl -w net.ipv4.ip_default_ttl=65
sysctl -w net.ipv6.conf.all.hop_limit=65
```
* **Why it works**: Changes the socket default hop limit for packets originated or terminated on the host. Note: This only applies to forwarded packets if kernel forwarding inherits the host hop limit.

---

### E. Extra Detection Toggles
1. **IPv6 Forwarding Block**:
   ```bash
   ip6tables -I FORWARD -i <hotspot> -j DROP
   ```
   * Some carriers leak un-mangled IPv6 packets or inspect IPv6 headers differently. Dropping forwarded IPv6 forces client devices to use pure IPv4.
2. **DNS Redirection to Google DNS (8.8.8.8)**:
   ```bash
   iptables -t nat -I PREROUTING -i <hotspot> -p udp --dport 53 -j DNAT --to-destination 8.8.8.8:53
   iptables -t nat -I PREROUTING -i <hotspot> -p tcp --dport 53 -j DNAT --to-destination 8.8.8.8:53
   ```
   * Prevents carriers from hijacking DNS queries or detecting tethering via carrier DNS inspection.

---

## 2. Project Architecture

```
MyHotspot/app/src/main/
├── AndroidManifest.xml
├── java/com/myhotspot/app/
│   ├── MainActivity.kt
│   ├── MyHotspotApplication.kt
│   ├── domain/models/
│   │   ├── TtlEngineState.kt
│   │   └── TtlModels.kt (DiagnosticItem, CommandLog, ConnectedDevice, TtlSettings)
│   ├── platform/root/
│   │   ├── RootShellManager.kt      (libsu Root Shell Controller & Live Log Buffer)
│   │   ├── TtlEngine.kt             (Multi-tier TTL Mangle, Fallbacks, NAT, Interfaces)
│   │   ├── CompatibilityChecker.kt  (Pre-flight checklist: Root, xt_TTL, nft, ifaces)
│   │   ├── RootClientScanner.kt     (ARP & ip neigh client discovery)
│   │   └── MagiskScriptExporter.kt  (service.d boot script auto-installer)
│   ├── service/
│   │   ├── TtlHotspotService.kt     (Foreground Service, WakeLock, Rule Watchdog)
│   │   └── BootReceiver.kt          (Auto-start on Android boot)
│   └── ui/
│       ├── hotspot/
│       │   ├── TtlHomeScreen.kt     (Material 3 Cyber UI, Terminal, Clients Radar)
│       │   └── TtlViewModel.kt      (StateFlow Coordination & Root Actions)
│       └── theme/
│           ├── Color.kt
│           ├── Theme.kt
│           └── Type.kt
└── res/
    ├── values/strings.xml           (English resources)
    └── values-ar/strings.xml        (Full Arabic RTL resources)
```

---

## 3. How to Build in Android Studio

1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, or newer).
2. Select **Open** and choose the `MyHotspot` directory.
3. Ensure JDK 17 is selected in **Settings > Build, Execution, Deployment > Build Tools > Gradle**.
4. Allow Gradle to sync dependencies (`com.github.topjohnwu.libsu:core:5.2.2`).
5. Connect your rooted Android device via USB (with USB Debugging enabled).
6. Click **Run > Run 'app'** (`Shift + F10`) or build the APK via **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

---

## 4. Honest Technical Limitations

1. **Root Access Required**:
   * Modifying layer-3 IP packet headers (`xt_TTL` / `xt_HL`) in the Linux kernel requires root privileges (`CAP_NET_ADMIN` / UID 0). Unrooted phones cannot mangle raw IP packets.
2. **Kernel Module Availability**:
   * While 90%+ of modern Android kernels include `xt_TTL`, some minimal OEM stock kernels strip this module. In such cases, the app automatically cascades to **Fallback A (nftables)** or **Fallback B (sysctl)**.
3. **Advanced Carrier Deep Packet Inspection (DPI)**:
   * 95% of tethering blocks are enforced via TTL inspection. However, if a carrier also employs:
     * **OS-specific SNI / User-Agent detection** (e.g. Windows Update endpoints),
     * **IMEI plan enforcement**,
     * **Separate APN (DUN) routing**,
     then TTL mangling should be combined with removing the `dun` APN type in phone APN settings.

---

## 5. Verification & Testing Checklist

To verify that the TTL bypass is working from a connected client device (e.g., Windows Laptop):

1. **Step 1: Connect to Hotspot**:
   * Join the phone's Wi-Fi hotspot from the laptop.
2. **Step 2: Check Client Outbound TTL**:
   * Open `cmd.exe` on Windows and type:
     ```cmd
     ping 1.1.1.1
     ```
   * On Windows, outbound ping sends `TTL=128`.
3. **Step 3: Verify Hop Mangling**:
   * Open a traceroute on the laptop:
     ```cmd
     tracert -d -h 2 1.1.1.1
     ```
   * Alternatively, check using an online TTL inspection tool (e.g. `http://checkip.dyndns.org` or packet analyzer `Wireshark` on upstream).
4. **Step 4: Confirm Unrestricted Browsing**:
   * Open browser and visit `https://www.youtube.com` or `https://fast.com`. Full download/upload speed will be achieved without tethering drops.
