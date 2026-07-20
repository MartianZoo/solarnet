package dev.martianzoo.web

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.browser.window

internal class BrowserHistoryTest {
  @AfterTest
  fun clearHistory() {
    window.localStorage.removeItem("rego-plastics-command-history")
  }

  @Test
  fun recordsAndReloadsHistory() {
    val history = BrowserHistory()
    history.record("help")
    history.record("count Plant")

    assertEquals(listOf("1: help", "2: count Plant"), BrowserHistory().lines())
    assertEquals(listOf("2: count Plant"), BrowserHistory().lines(1))
  }
}
