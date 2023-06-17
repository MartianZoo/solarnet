package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.Type
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonBootstrapTest {
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
    val starting: Multiset<out Type> = game.getComponents(game.resolve(COMPONENT.expression))

    // 19 duplicate TR and 4 duplicate PROD[M]
    assertThat(starting).hasSize(starting.elements.size + 69)

    val isArea: (Type) -> Boolean = { it.toString().startsWith("[Tharsis_") }
    // val isBorder: (Component) -> Boolean = { it.toString().startsWith("[Border<") }
    val isClass: (Type) -> Boolean = { it.toString().startsWith("[Class<") }

    assertThat(starting.count(isArea)).isEqualTo(61)
    // assertThat(starting.count(isBorder)).isEqualTo(312)
    assertThat(starting.count(isClass)).isGreaterThan(400)

    val theRest =
        starting.filterNot {
          isArea(it) ||
              // isBorder(it) ||
              isClass(it) ||
              it.narrows(game.resolve(parse("TerraformRating"))) ||
              it.narrows(game.resolve(parse("Production<Class<Megacredit>>")))
        }
    assertThat(theRest.toStrings())
        .containsExactly(
            "[Engine]",
            "[TerraformingMars]",
            "[SetupPhase]",
            "[Tharsis]",
            "[Area021]",
            "[Area081]",
            "[FloatingInSpace]",
            "[Player1]",
            "[Player2]",
            "[Player3]",
            "[PlayCardSA]",
            "[UseStandardProjectSA]",
            "[ClaimMilestoneSA]",
            "[UseCardActionSA]",
            "[ConvertHeatSA]",
            "[ConvertPlantsSA]",
            "[HandleMandates]",
            "[SellPatents]",
            "[PowerPlantSP]",
            "[AsteroidSP]",
            "[AquiferSP]",
            "[GreenerySP]",
            "[CitySP]",
            "[GrossHack<Player1>]",
            "[GrossHack<Player2>]",
            "[GrossHack<Player3>]",
            "[SuperHack<Player1>]",
            "[SuperHack<Player2>]",
            "[SuperHack<Player3>]",
            "[TrWatcher<Player1>]",
            "[TrWatcher<Player2>]",
            "[TrWatcher<Player3>]",
            "[Generation]",
        )
  }
}
