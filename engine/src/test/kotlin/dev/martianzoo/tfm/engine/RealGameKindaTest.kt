package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Bundle.Base
import dev.martianzoo.tfm.canon.Canon.Bundle.CorporateEra
import dev.martianzoo.tfm.canon.Canon.Bundle.Prelude
import dev.martianzoo.tfm.canon.Canon.Bundle.Promos
import dev.martianzoo.tfm.canon.Canon.Bundle.Tharsis
import org.junit.jupiter.api.Test

class RealGameKindaTest {
  @Test
  fun loadsExpectedClasses() {
    println()

    val game = GameStarter.newGame(3, setOf(Base, CorporateEra, Tharsis, Prelude, Promos))

    val otherExps = Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.map { it.className }

    val expected = (Canon.allClassDeclarations.keys - otherExps).filterNot {
      it.matches(Regex(
          "(Hellas|Elysium|Milestone[HEV]|Player[45]|Camp" +
          "|Venus|Area2|Floater|Dirigible|AirScrappingSP).*"
      ))
    }
    assertThat(game.classTable.loadedClassNames()).containsExactlyElementsIn(expected)
  }

  @Test
  fun createdSingletons() {
    val game = GameStarter.newGame(3, setOf(Base, CorporateEra, Tharsis, Prelude, Promos))
    assertThat(game.components.getAll(game.classTable.resolve("Component"))).containsExactly()
    // welp
  }
}
