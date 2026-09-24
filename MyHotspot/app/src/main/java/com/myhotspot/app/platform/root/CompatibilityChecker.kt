package com.myhotspot.app.platform.root

import com.myhotspot.app.domain.models.DiagnosticItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatibilityChecker @Inject constructor(
    private val shell: RootShellManager,
    private val ttlEngine: TtlEngine
) {

    suspend fun runDiagnostics(): List<DiagnosticItem> = withContext(Dispatchers.IO) {
        val items = mutableListOf<DiagnosticItem>()

        // 1. Root Check
        val hasRoot = shell.isRootAvailable()
        items.add(
            DiagnosticItem(
                id = "root",
                title = "صلاحيات الروت (Magisk / Root Access)",
                isPassed = hasRoot,
                detail = if (hasRoot) "تم منح صلاحيات الروت بنجاح (UID=0)" else "لم يتم العثور على روت! التطبيق يتطلب روت لتعديل الحزم.",
                isCritical = true
            )
        )

        if (!hasRoot) {
            return@withContext items
        }

        // 2. iptables binary
        val iptablesCheck = shell.execute("which iptables")
        val hasIptables = iptablesCheck.isSuccess && iptablesCheck.out.isNotEmpty()
        items.add(
            DiagnosticItem(
                id = "iptables",
                title = "أداة جدار الحماية (iptables)",
                isPassed = hasIptables,
                detail = if (hasIptables) "متوفر: ${iptablesCheck.out.firstOrNull()}" else "غير متوفر في النظام",
                isCritical = true
            )
        )

        // 3. iptables TTL target kernel module support (xt_TTL)
        // Test insert and delete a dummy rule on loopback interface
        val ttlTestInsert = shell.execute("iptables -t mangle -I POSTROUTING -o lo -j TTL --ttl-set 64")
        val hasTtlTarget = ttlTestInsert.isSuccess
        if (hasTtlTarget) {
            shell.execute("iptables -t mangle -D POSTROUTING -o lo -j TTL --ttl-set 64 2>/dev/null")
        }
        items.add(
            DiagnosticItem(
                id = "xt_ttl",
                title = "دعم النواة لهدف الـ TTL (Kernel xt_TTL Target)",
                isPassed = hasTtlTarget,
                detail = if (hasTtlTarget) "النواة تدعم تعديل الـ TTL المباشر (Primary Mode مدعوم ✓)" else "النواة لا تدعم xt_TTL، سيتم التراجع تلقائياً إلى nftables أو sysctl.",
                isCritical = false
            )
        )

        // 4. ip6tables HL target (xt_HL)
        val hlTestInsert = shell.execute("ip6tables -t mangle -I POSTROUTING -o lo -j HL --hl-set 64")
        val hasHlTarget = hlTestInsert.isSuccess
        if (hasHlTarget) {
            shell.execute("ip6tables -t mangle -D POSTROUTING -o lo -j HL --hl-set 64 2>/dev/null")
        }
        items.add(
            DiagnosticItem(
                id = "xt_hl",
                title = "دعم الـ IPv6 Hop Limit (Kernel xt_HL)",
                isPassed = hasHlTarget,
                detail = if (hasHlTarget) "مدعوم لحزم IPv6 بنجاح ✓" else "غير متوفر لحزم IPv6 (يمكن استخدام خيار حظر IPv6 لمنع التسريب)",
                isCritical = false
            )
        )

        // 5. nftables binary check
        val nftCheck = shell.execute("which nft")
        val hasNft = nftCheck.isSuccess && nftCheck.out.isNotEmpty()
        items.add(
            DiagnosticItem(
                id = "nftables",
                title = "أداة nftables البديلة (Fallback A)",
                isPassed = hasNft,
                detail = if (hasNft) "متوفر كبديل احتياطي ممتاز" else "غير متوفر",
                isCritical = false
            )
        )

        // 6. Upstream Interface
        val upstream = ttlEngine.detectUpstreamInterface()
        val hasUpstream = upstream != "None"
        items.add(
            DiagnosticItem(
                id = "upstream",
                title = "واجهة الإنترنت النشطة (Upstream Interface)",
                isPassed = hasUpstream,
                detail = if (hasUpstream) "متصلة: $upstream" else "لا يوجد اتصال إنترنت نشط حالياً",
                isCritical = true
            )
        )

        // 7. Hotspot Interface
        val hotspot = ttlEngine.detectHotspotInterface()
        val hasHotspot = hotspot != "None"
        items.add(
            DiagnosticItem(
                id = "hotspot_iface",
                title = "واجهة بث الهوتسبوت (Hotspot Interface)",
                isPassed = hasHotspot,
                detail = if (hasHotspot) "الواجهة المكتشفة: $hotspot" else "يرجى تشغيل الهوتسبوت لتحديد الواجهة",
                isCritical = false
            )
        )

        items
    }
}
