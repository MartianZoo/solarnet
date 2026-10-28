@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.coroutines.await

/** Restores the folder selected on an earlier visit, without asking again. */
@JsFun(
    """async () => {
  const request = indexedDB.open("solarnet-live-log", 1);
  const db = await new Promise((resolve, reject) => {
    request.onupgradeneeded = () => request.result.createObjectStore("settings");
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  try {
    return await new Promise((resolve, reject) => {
      const get = db.transaction("settings").objectStore("settings").get("directory");
      get.onsuccess = () => resolve(get.result ?? null);
      get.onerror = () => reject(get.error);
    });
  } finally {
    db.close();
  }
}"""
)
private external fun restoreLiveLogFolder(): Promise<JsAny?>

internal suspend fun savedLiveLogFolder(): JsAny? = restoreLiveLogFolder().await()

@JsFun(
    """async (folder) => {
  return (await folder.queryPermission({ mode: "readwrite" })) === "granted";
}"""
)
private external fun canWriteLiveLog(folder: JsAny): Promise<JsBoolean>

internal suspend fun hasLiveLogPermission(folder: JsAny): Boolean {
  val granted: JsBoolean = canWriteLiveLog(folder).await()
  return granted.toBoolean()
}

/** Requests Chrome's directory picker directly from the click when write access is disconnected. */
@JsFun(
    """() => globalThis.showDirectoryPicker({
      id: "solarnet-live-log",
      mode: "readwrite"
    })"""
)
internal external fun authorizeLiveLogFolder(): Promise<JsAny>

@JsFun(
    """async (folder) => {
  const request = indexedDB.open("solarnet-live-log", 1);
  const db = await new Promise((resolve, reject) => {
    request.onupgradeneeded = () => request.result.createObjectStore("settings");
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
  try {
    await new Promise((resolve, reject) => {
      const put = db.transaction("settings", "readwrite")
        .objectStore("settings").put(folder, "directory");
      put.onsuccess = resolve;
      put.onerror = () => reject(put.error);
    });
  } finally {
    db.close();
  }
  return folder;
}"""
)
private external fun rememberLiveLogFolder(folder: JsAny): Promise<JsAny>

internal suspend fun keepLiveLogFolder(folder: JsAny): JsAny = rememberLiveLogFolder(folder).await()

@JsFun("""(folder) => folder.getFileHandle("live-game.json", { create: true })""")
private external fun createLiveLogFile(folder: JsAny): Promise<JsAny>

internal suspend fun openLiveLogFile(folder: JsAny): JsAny = createLiveLogFile(folder).await()

@JsFun(
    """async (handle, contents) => {
      const writable = await handle.createWritable();
      await writable.truncate(0);
      await writable.write(contents);
      await writable.close();
      return "saved";
    }"""
)
private external fun writeLiveLogFile(handle: JsAny, contents: String): Promise<JsString>

internal suspend fun overwriteLiveLogFile(handle: JsAny, contents: String) {
  val saved: JsString = writeLiveLogFile(handle, contents).await()
  check(saved.toString() == "saved")
}
