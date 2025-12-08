package org.jellyfin.androidtv.tailscale

import libtailscale.InputStream
import libtailscale.OutputStream
import java.io.ByteArrayInputStream

object TailscaleStreams {
	fun inputStream(bytes: ByteArray): InputStream = object : InputStream {
		private val backing = ByteArrayInputStream(bytes)
		override fun close() {
			backing.close()
		}

		override fun read(): ByteArray {
			val buffer = ByteArray(4096)
			val read = backing.read(buffer)
			return if (read <= 0) ByteArray(0) else buffer.copyOf(read)
		}
	}

	fun outputStream(collect: (ByteArray) -> Unit): OutputStream = object : OutputStream {
		override fun close() {}
		override fun write(p0: ByteArray?): Long {
			p0?.let(collect)
			return (p0?.size ?: 0).toLong()
		}
	}
}
