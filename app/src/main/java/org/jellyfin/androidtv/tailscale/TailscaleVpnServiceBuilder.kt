package org.jellyfin.androidtv.tailscale

import android.net.VpnService
import libtailscale.ParcelFileDescriptor
import libtailscale.VPNServiceBuilder

class TailscaleVpnServiceBuilder(private val builder: VpnService.Builder) : VPNServiceBuilder {
	override fun addAddress(p0: String, p1: Int) {
		builder.addAddress(p0, p1)
	}

	override fun addDNSServer(p0: String) {
		builder.addDnsServer(p0)
	}

	override fun addRoute(p0: String, p1: Int) {
		builder.addRoute(p0, p1)
	}

	override fun excludeRoute(p0: String, p1: Int) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
			val prefix = android.net.IpPrefix(java.net.InetAddress.getByName(p0), p1)
			builder.excludeRoute(prefix)
		}
	}

	override fun addSearchDomain(p0: String) {
		builder.addSearchDomain(p0)
	}

	override fun establish(): ParcelFileDescriptor? = builder.establish()?.let { ParcelFileDescriptor(it) }

	override fun setMTU(p0: Int) {
		builder.setMtu(p0)
	}
}

class ParcelFileDescriptor(private val fd: android.os.ParcelFileDescriptor) : libtailscale.ParcelFileDescriptor {
	override fun detach(): Int = fd.detachFd()
}
