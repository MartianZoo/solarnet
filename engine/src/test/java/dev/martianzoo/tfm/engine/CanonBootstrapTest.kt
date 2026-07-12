package dev.martianzoo.tfm.engine

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.engine.Component
import dev.martianzoo.engine.Component.Companion.toComponent
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toStrings
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Tests for the Canon data set. */
internal class CanonBootstrapTest {
  @Test
  fun classCounts() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 3)).reader

    fun checkCount(count: Int, expr: String) {
      game.count(parse<Metric>(expr)) shouldBe count
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

    game.count(parse<Metric>("Class<CardFront>")).shouldBeGreaterThan(200)
    game.count(parse<Metric>("Class<Component>")).shouldBeGreaterThan(300)
    game.count(parse<Metric>("Class")).shouldBeGreaterThan(300)
  }

  @Test
  fun createsExpectedSingletons() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 3)).reader
    val starting: Multiset<Component> =
        game.getComponents(game.resolve(COMPONENT.expression)).map { it.toComponent(game) }

    // 19 duplicate TR and 4 duplicate PROD[M]
    starting.shouldHaveSize(starting.elements.size + 69)

    val isArea: (Component) -> Boolean = { it.toString().startsWith("Tharsis_") }
    // val isBorder: (Component) -> Boolean = { it.toString().startsWith("[Border<") }
    val isClass: (Component) -> Boolean = { it.toString().startsWith("Class<") }

    starting.count(isArea) shouldBe 61
    // starting.count(isBorder) shouldBe 312
    starting.count(isClass).shouldBeGreaterThan(400)

    val theRest = starting.filterNot {
      isArea(it) ||
          // isBorder(it) ||
          isClass(it) ||
          it.hasType(game.resolve(parse("TerraformRating"))) ||
          it.hasType(game.resolve(parse("Production<Class<Megacredit>>")))
    }
    theRest
        .toStrings()
        .shouldContainExactlyInAnyOrder(
            "Engine",
            "TerraformingMars",
            "SetupPhase",
            "Tharsis",
            "Area021",
            "Area081",
            "FloatingInSpace",
            "Player1",
            "Player2",
            "Player3",
            "PlayCardSA",
            "UseStandardProjectSA",
            "ClaimMilestoneSA",
            "UseCardActionSA",
            "ConvertHeatSA",
            "ConvertPlantsSA",
            "HandleMandates",
            "SellPatents",
            "PowerPlantSP",
            "AsteroidSP",
            "AquiferSP",
            "GreenerySP",
            "CitySP",
            "GrossHack<Player1>",
            "GrossHack<Player2>",
            "GrossHack<Player3>",
            "Entropy<Player1>",
            "Entropy<Player2>",
            "Entropy<Player3>",
            "TrWatcher<Player1>",
            "TrWatcher<Player2>",
            "TrWatcher<Player3>",
            "Generation",
        )
  }
}
