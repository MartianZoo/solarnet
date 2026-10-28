@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.tfm.canon.Canon
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest

@JsFun("""() => navigator.storage.getDirectory()""")
private external fun testFolder(): Promise<JsAny>

@JsFun("""async (handle) => (await handle.getFile()).text()""")
private external fun testFileText(handle: JsAny): Promise<JsString>

@JsFun("""(folder) => Promise.resolve(folder)""")
private external fun selectedFolder(folder: JsAny): Promise<JsAny>

@JsFun("""() => new Promise(resolve => setTimeout(() => resolve(true), 50))""")
private external fun browserTick(): Promise<JsBoolean>

@JsFun(
    """(folder) => {
      const previous = globalThis.showDirectoryPicker;
      globalThis.showDirectoryPicker = options => {
        if (options.id !== "solarnet-live-log" || options.mode !== "readwrite")
          throw new Error("Wrong directory picker options");
        return Promise.resolve(folder);
      };
      return previous;
    }"""
)
private external fun useTestDirectoryPicker(folder: JsAny): JsAny

@JsFun("""(previous) => { globalThis.showDirectoryPicker = previous; }""")
private external fun restoreDirectoryPicker(previous: JsAny)

@JsFun("""(a, b) => a === b""") private external fun sameHandle(a: JsAny, b: JsAny): JsBoolean

internal class LiveLogFileTest {
  @Test
  fun connectingLogOpensTheNativeDirectoryPicker() = runTest {
    val folder: JsAny = testFolder().await()
    val previous = useTestDirectoryPicker(folder)
    try {
      val selected: JsAny = authorizeLiveLogFolder().await()
      assertTrue(sameHandle(selected, folder).toBoolean())
    } finally {
      restoreDirectoryPicker(previous)
    }
  }

  @Test
  fun rememberedFolderReopensAndReplacesTheSavedLog() = runTest {
    val folder: JsAny = testFolder().await()
    keepLiveLogFolder(folder)
    val restored = assertNotNull(savedLiveLogFolder())
    assertTrue(hasLiveLogPermission(restored))
    overwriteLiveLogFile(openLiveLogFile(restored), "before undo")
    val reopened = openLiveLogFile(restored)
    overwriteLiveLogFile(reopened, "after undo")
    val contents: JsString = testFileText(reopened).await()
    assertEquals("after undo", contents.toString())
  }

  @Test
  fun startingAGameWritesTheInitialReplayToTheConnectedFolder() = runTest {
    val folder: JsAny = testFolder().await()
    overwriteLiveLogFile(openLiveLogFile(folder), "")
    val recording = RecordingStore()
    assertTrue(recording.attachLiveLog(selectedFolder(folder)))
    val config = GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")
    assertTrue(recording.play(config, Canon.gamePremise(config), seed = 42))

    val handle = openLiveLogFile(folder)
    var contents = ""
    for (attempt in 1..40) {
      val read: JsString = testFileText(handle).await()
      contents = read.toString()
      if (contents.isNotBlank()) break
      val tick: JsBoolean = browserTick().await()
      assertTrue(tick.toBoolean())
    }
    assertTrue(contents.isNotBlank(), recording.error ?: "The initial log was not written")
    assertTrue(GameFileJson.parse(contents).hasEvents)
  }

  @Test
  fun connectingAfterGameStartWritesTheCurrentReplay() = runTest {
    val folder: JsAny = testFolder().await()
    val handle = openLiveLogFile(folder)
    overwriteLiveLogFile(handle, "")
    val recording = RecordingStore()
    val config = GameConfig("TharsisMap, SoloMode, CorporateEraExpansion", "Green")
    assertTrue(recording.play(config, Canon.gamePremise(config), seed = 43))
    assertTrue(recording.attachLiveLog(selectedFolder(folder)))

    var contents = ""
    for (attempt in 1..40) {
      val read: JsString = testFileText(handle).await()
      contents = read.toString()
      if (contents.isNotBlank()) break
      val tick: JsBoolean = browserTick().await()
      assertTrue(tick.toBoolean())
    }
    assertTrue(contents.isNotBlank(), recording.error ?: "The current log was not written")
    assertTrue(GameFileJson.parse(contents).hasEvents)
  }
}
