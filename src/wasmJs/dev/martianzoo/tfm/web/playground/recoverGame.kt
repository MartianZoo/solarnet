@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json

@JsFun(
    """() => new Promise((resolve, reject) => {
  const request = indexedDB.open("mars-playground-recovery", 1);
  request.onupgradeneeded = () => request.result.createObjectStore("games");
  request.onerror = () => reject(request.error);
  request.onsuccess = () => {
    const db = request.result;
    const transaction = db.transaction("games");
    const get = transaction.objectStore("games").get("latest");
    get.onsuccess = () => resolve(get.result ?? null);
    get.onerror = () => reject(get.error);
    transaction.oncomplete = () => db.close();
  };
})"""
)
private external fun readRecovery(): Promise<JsString?>

@JsFun(
    """(contents) => new Promise((resolve, reject) => {
  const request = indexedDB.open("mars-playground-recovery", 1);
  request.onupgradeneeded = () => request.result.createObjectStore("games");
  request.onerror = () => reject(request.error);
  request.onsuccess = () => {
    const db = request.result;
    const transaction = db.transaction("games", "readwrite");
    transaction.objectStore("games").put(contents, "latest");
    transaction.oncomplete = () => { db.close(); resolve("saved"); };
    transaction.onerror = () => { db.close(); reject(transaction.error); };
  };
})"""
)
private external fun writeRecovery(contents: String): Promise<JsString>

internal suspend fun savedRecovery(): PlaySession.Recovery? {
  val text: JsString? = readRecovery().await()
  return text?.toString()?.let { Json.decodeFromString<PlaySession.Recovery>(it) }
}

internal suspend fun saveRecovery(recovery: PlaySession.Recovery) {
  val saved: JsString = writeRecovery(Json.encodeToString(recovery)).await()
  check(saved.toString() == "saved")
}
