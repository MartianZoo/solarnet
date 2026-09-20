package dev.martianzoo.tfm.web.classviewer

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.text.EnglishCardTextRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

internal class EnglishCardTextBrowserTest {
  @Test
  internal fun rendersTopAndBottomTextInfoCards() {
    val column = document.createElement("article") as HTMLElement
    column.innerHTML =
        """
        <section data-field="top-text-panel" hidden><div data-field="top-text"></div></section>
        <section data-field="bottom-text-panel" hidden><div data-field="bottom-text"></div></section>
        """
            .trimIndent()
    val card = Canon.classTable.getClass(cn("CrediCor"))

    renderCardText(column, card, EnglishCardTextRenderer(Canon.classTable))

    assertEquals(
        "Effect: When you play a card with a printed cost of 20 M€ or more, or use a " +
            "standard project with a printed cost of 20 M€ or more, gain 4 M€.",
        column.querySelector("[data-field='top-text']")?.textContent,
    )
    assertEquals(
        "Gain 57 M€.",
        column.querySelector("[data-field='bottom-text']")?.textContent,
    )
    assertFalse(column.querySelector("[data-field='top-text-panel']")!!.hasAttribute("hidden"))
    assertFalse(column.querySelector("[data-field='bottom-text-panel']")!!.hasAttribute("hidden"))
  }
}
