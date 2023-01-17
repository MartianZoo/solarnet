package dev.martianzoo.tfm.engine

import com.google.common.collect.Multiset
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

class EngineTest {
  @Test
  fun loadsExpectedClasses() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 4))
    val unusedExpansionCards =
        Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.map { it.name }.toSet()

    val milestoneNames = Canon.milestoneDefinitions.map { it.name }
    val regex = Regex("(Hellas|Elysium|Player5|Camp|Row" +
        "|Venus|Area2|Floater|Dirigible|AirScrappingSP).*")
    val expected = (Canon.allClassNames - unusedExpansionCards).filterNot {
      it.matches(regex)
    }.filterNot {
      it in milestoneNames && "HEV".contains(Canon.milestone(it).bundle)
    }

    assertThat(game.classTable.loadedClassNames()).containsExactlyElementsIn(expected)
  }

  @Test
  fun createdSingletons() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 3))
    val all: Multiset<Component> = game.components.getAll(game.classTable.resolve("Component"))

    val isArea: (Component) -> Boolean =
        { it.asTypeExpression.toString().startsWith("Tharsis_") }

    assertThat(all.elementSet().count(isArea)).isEqualTo(61)

    val isBorder: (Component) -> Boolean =
        { it.asTypeExpression.asGeneric().root == cn("Border") }
    assertThat(all.elementSet().count(isBorder)).isEqualTo(312)

    assertThat(all.filterNot(isArea)
        .filterNot(isBorder)
        .map { it.asTypeExpression.toString() }).containsExactly(
      "Player1",
      "Player2",
      "Player3",
      "Generation",
      "Tharsis",

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
