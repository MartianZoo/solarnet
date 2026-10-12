package dev.martianzoo.state

import kotlin.js.Promise
import kotlinx.coroutines.await

@Suppress("UNUSED_VARIABLE") // js() references encoded and buffer by name.
internal actual suspend fun sha256(value: String): String {
  val encoded = js("new TextEncoder().encode(value)")
  val buffer =
      (js("globalThis.crypto.subtle.digest('SHA-256', encoded)") as Promise<dynamic>).await()
  val digest = js("new Uint8Array(buffer)")
  return ByteArray(32) { index -> (digest[index] as Int).toByte() }.toHexString()
}
