@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.martianzoo.tfm.web.playground

import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest

/** The actual Play menu must handle the production alternatives revealed by task selection. */
internal class NitrogenRichAsteroidPlayTest {
  @Test
  fun everyTitaniumAmountReachesBothProductionChoices() = runTest {
    for (amount in 1..11) {
      // An asynchronous read lets the browser service Karma between independent replays.
      val contents: JsString = readNraFixture().await()
      val session = recoverPlaySession(recovery(contents.toString()))
      session.setSafeAutoexec(true)
      session.choose(session.prompt().options.first { it.label != "Ok" })
      session.choose(session.prompt().options.single { it.label == amount.toString() })

      val prompt = session.prompt()
      assertNull(session.problem, "Titanium $amount")
      assertTrue(prompt.enabled, "Titanium $amount: $prompt")
      assertNull(prompt.warning, "Titanium $amount")
      val production = prompt.options.single { it.arms.size == 2 }
      assertTrue(production.enabled)
      assertFalse(production.unproven)
      assertTrue(production.unprovenArms.isEmpty())
      val player = session.agents[session.player]
      assertEquals(1, player.count("NitrogenRichAsteroid"))
      val before = player.count("Production<Class<Plant>>")
      // Exercise both displayed alternatives across the payment amounts.
      val larger = amount % 2 == 0
      session.chooseTaskArm(production, production.arms[if (larger) 1 else 0])
      assertNull(session.problem)
      assertEquals(before + if (larger) 4 else 1, player.count("Production<Class<Plant>>"))
      assertTrue(session.prompt().enabled)
    }
  }

  @Test
  fun reenablingAutoexecAfterManualPaymentShowsProductionChoices() = runTest {
    val contents: JsString = readNraFixture().await()
    val session = recoverPlaySession(recovery(contents.toString()))
    session.choose(session.prompt().options.first { it.label != "Ok" })
    session.choose(session.prompt().options.single { it.label == "1" })
    session.choose(session.prompt().options.first { it.enabled })
    session.setSafeAutoexec(true)

    assertNull(session.problem)
    assertTrue(session.prompt().enabled)
    assertEquals(2, session.prompt().options.single { it.arms.size == 2 }.arms.size)
  }

  @Test
  fun failedMenuQueryIsShownOnceAndManualPlayRemainsAvailable() = runTest {
    val contents: JsString = readNraFixture().await()
    val session = recoverPlaySession(recovery(contents.toString()))
    session.choose(session.prompt().options.first { it.label != "Ok" })
    session.choose(session.prompt().options.single { it.label == "1" })
    var calls = 0
    // Simulate a programming error in a search callback, rather than a gameplay rejection.
    session.searchTrace = {
      calls++
      error("injected choice-check failure")
    }
    session.setSafeAutoexec(true)
    val failed = session.prompt()
    assertFalse(failed.enabled)
    assertFalse(session.hasDraftInput, "A failed menu query is not draft player input")
    assertTrue(assertNotNull(session.problem).contains("injected choice-check failure"))
    val failedCalls = calls
    repeat(5) { assertSame(failed, session.prompt()) }
    assertEquals(failedCalls, calls, "Rendering must not repeat the failed search")

    session.setSafeAutoexec(false)
    assertNull(session.problem)
    assertTrue(session.prompt().enabled)
    assertTrue(session.prompt().options.any { it.enabled })
  }

  private fun recovery(file: String) =
      PlaySession.Recovery(
          seed = 0,
          startOrdinal = 0,
          // This fixture resumes an already dealt card and does not draw any subsequent cards.
          dealt = listOf(PlaySession.Recovery.DeckPosition(0, emptyMap())),
          gameFile = file,
      )
}

@JsFun(
    """async () => {
  const response = await fetch('/base/kotlin/replays/PlaygroundNraFreeze20261027.json.gz');
  if (!response.ok) throw new Error('Fixture HTTP ' + response.status);
  return await new Response(response.body.pipeThrough(new DecompressionStream('gzip'))).text();
}"""
)
private external fun readNraFixture(): Promise<JsString>
