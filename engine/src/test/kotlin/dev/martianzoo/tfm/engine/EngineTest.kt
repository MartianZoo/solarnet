package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Bundle.Base
import dev.martianzoo.tfm.canon.Canon.Bundle.CorporateEra
import dev.martianzoo.tfm.canon.Canon.Bundle.Prelude
import dev.martianzoo.tfm.canon.Canon.Bundle.Promos
import dev.martianzoo.tfm.canon.Canon.Bundle.Tharsis
import org.junit.jupiter.api.Test

class EngineTest {
  @Test
  fun loadsExpectedClasses() {

    val bundles = setOf(Base, CorporateEra, Tharsis, Prelude, Promos).map { it.id }
    val game = Engine.newGame(Canon, 4, bundles)

    val unusedExpansionCards =
        Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.map { it.className }

    val expected = (Canon.allClassDeclarations.keys - unusedExpansionCards).filterNot {
      it.matches(Regex(
          "(Hellas|Elysium|Milestone[HEV]|Player5|Camp" +
          "|Venus|Area2|Floater|Dirigible|AirScrappingSP).*"
      ))
    }

    assertThat(game.classTable.loadedClassNames()).containsExactlyElementsIn(expected)
  }

  @Test
  fun createdSingletons() {
    val bundles = setOf(Base, CorporateEra, Tharsis, Prelude, Promos).map { it.id }
    val game = Engine.newGame(Canon, 3, bundles)
    assertThat(game.components.getAll(game.classTable.resolve("Component"))).containsExactly()
    // welp
  }
}
