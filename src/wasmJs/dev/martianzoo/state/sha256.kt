package dev.martianzoo.state

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.coroutines.await

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    "(text) => globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))" +
        ".then(buffer => Array.from(new Uint8Array(buffer), byte => " +
        "byte.toString(16).padStart(2, '0')).join(''))"
)
private external fun digestHex(text: String): Promise<JsString>

@OptIn(ExperimentalWasmJsInterop::class)
internal actual suspend fun sha256(value: String): String {
  val digest: JsString = digestHex(value).await()
  return digest.toString()
}
