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
        Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.map { it.name }.toSet()

    val regex = Regex("(Hellas|Elysium|Player5|Camp" +
        "|Venus|Area2|Floater|Dirigible|AirScrappingSP).*")
    val expected = (Canon.allClassDeclarations.keys - unusedExpansionCards).filterNot {
      it.matches(regex)
    }.filterNot {
      "HEV".contains(Canon.milestonesByClassName[it]?.bundle ?: "x")
    }

    assertThat(game.classTable.loadedClassNames()).containsExactlyElementsIn(expected)
  }

  @Test
  fun createdSingletons() {
    val bundles = setOf(Base, CorporateEra, Tharsis, Prelude, Promos).map { it.id }
    val game = Engine.newGame(Canon, 3, bundles)
    val all = game.components.getAll(game.classTable.resolve("Component"))
    assertThat(all.map { it.asTypeExpression.toString() }).containsExactly(
      "Player1",
      "Player2",
      "Player3",
      "Generation",

      "ClaimMilestone",
      "ConvertHeat",
      "ConvertPlants",
      "PlayCardFromHand",
      "UseActionFromCard",
      "UseStandardProject",
      "SellPatents",

      "PowerPlantSP",
      "AsteroidSP",
      "AquiferSP",
      "GreenerySP",
      "CitySP",
    )
  }
}
