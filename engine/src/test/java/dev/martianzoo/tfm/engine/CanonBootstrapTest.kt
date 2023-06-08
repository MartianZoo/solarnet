package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.ComponentGraph.Component
import dev.martianzoo.tfm.engine.ComponentGraph.Component.Companion.toComponent
import dev.martianzoo.tfm.pets.HasClassName.Companion.classNames
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.types.MClassLoader
import dev.martianzoo.types.MType
import dev.martianzoo.util.toSetStrict
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonBootstrapTest {
  @Test
  fun loadsExpectedClasses() {
    val table = MClassLoader(GameSetup(Canon, "BMRPTX", 4))
    val unusedCards =
        Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.classNames().toSetStrict()

    val milestoneNames = Canon.milestoneDefinitions.classNames().toSetStrict()
    val expected: List<ClassName> =
        (Canon.allClassNames - unusedCards)
            .filterNot { it.toString().matches(regex) }
            .filterNot { it in milestoneNames && "HEV".contains(Canon.milestone(it).bundle) }

    assertThat(table.allClasses.classNames()).containsExactlyElementsIn(expected)
  }

  val regex =
      Regex(
          "(Hellas|Elysium|Player5|Camp|Venus|Area2|AllDraw|" +
              "AirScrap|Card247|CardC05|UseAction3|MandateV02F).*")

  @Test
  fun classCounts() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 3)).reader

    fun checkCount(count: Int, expr: String) {
      assertThat(game.count(parse<Metric>(expr))).isEqualTo(count)
    }

    checkCount(1, "Class<Class>")
    checkCount(1, "Class<CityTile>") // Removed Capital for now
    checkCount(3, "Class<GlobalParameter>")
    checkCount(4, "Class<CardResource>") // Plants Microbes Animals.. and Fighters
    checkCount(5, "Class<Milestone>")
    checkCount(6, "Class<StandardResource>")
    checkCount(8, "Class<StandardAction>")
    checkCount(8, "Class<SpecialTile>") // oops missing some
    checkCount(10, "Class<Tag>")
    checkCount(10, "Class<OwnedTile>") // so here too
    checkCount(12, "Class<WaterArea>")
    checkCount(63, "Class<Area>")

    assertThat(game.count(parse<Metric>("Class<CardFront>"))).isGreaterThan(200)
    assertThat(game.count(parse<Metric>("Class<Component>"))).isGreaterThan(300)
    assertThat(game.count(parse<Metric>("Class"))).isGreaterThan(300)
  }

  @Test
  fun createsExpectedSingletons() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 3)).reader
    val starting =
        game.getComponents(game.resolve(COMPONENT.expression)).map { (it as MType).toComponent() }

    // 19 duplicate TR and 4 duplicate PROD[M]
    assertThat(starting).hasSize(starting.elements.size + 69)

    val isArea: (Component) -> Boolean = { it.toString().startsWith("[Tharsis_") }
    // val isBorder: (Component) -> Boolean = { it.toString().startsWith("[Border<") }
    val isClass: (Component) -> Boolean = { it.toString().startsWith("[Class<") }

    assertThat(starting.count(isArea)).isEqualTo(61)
    // assertThat(starting.count(isBorder)).isEqualTo(312)
    assertThat(starting.count(isClass)).isGreaterThan(400)

    val theRest =
        starting.filterNot {
          isArea(it) ||
              // isBorder(it) ||
              isClass(it) ||
              it.hasType(game.resolve(parse("TerraformRating"))) ||
              it.hasType(game.resolve(parse("Production<Class<Megacredit>>")))
        }
    assertThat(theRest.toStrings())
        .containsExactly(
            "[Engine]",
            "[SetupPhase]",
            "[Tharsis]",
            "[Area021]", "[Area081]", "[FloatingInSpace]",
            "[Player1]", "[Player2]", "[Player3]",

            "[PlayCardSA]",
            "[UseStandardProjectSA]",
            "[ClaimMilestoneSA]",
            "[UseCardActionSA]",
            "[ConvertHeatSA]",
            "[ConvertPlantsSA]",
            "[HandleMandates]",
            "[SellPatents]",

            "[PowerPlantSP]", "[AsteroidSP]", "[AquiferSP]", "[GreenerySP]", "[CitySP]",
            "[GrossHack<Player1>]", "[GrossHack<Player2>]", "[GrossHack<Player3>]",
            "[SuperHack<Player1>]", "[SuperHack<Player2>]", "[SuperHack<Player3>]",
            "[TrWatcher<Player1>]", "[TrWatcher<Player2>]", "[TrWatcher<Player3>]",

            "[Generation]",
        )
  }
}
