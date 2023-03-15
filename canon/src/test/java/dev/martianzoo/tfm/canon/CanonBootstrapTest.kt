package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.engine.Component
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.pets.ast.Metric.Companion.metric
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toSetStrict
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonBootstrapTest {
  @Test
  fun loadsExpectedClasses() {
    val loader = Engine.loadClasses(GameSetup(Canon, "BRMPX", 4))
    val unusedCards =
        Canon.cardDefinitions.filter { "VC".contains(it.bundle) }.classNames().toSetStrict()

    val milestoneNames = Canon.milestoneDefinitions.classNames().toSetStrict()
    val expected: List<ClassName> =
        (Canon.allClassNames - unusedCards)
            .filterNot { it.matches(regex) }
            .filterNot { it in milestoneNames && "HEV".contains(Canon.milestone(it).bundle) }

    // TODO Border
    assertThat(loader.allClasses.classNames()).containsExactlyElementsIn(expected - cn("Border"))
  }

  val regex = Regex("(Hellas|Elysium|Player5|Camp|Venus|Area2|AirScrap|Card247).*")

  @Test
  fun classCounts() {
    val game = Engine.newGame(GameSetup(Canon, "BRM", 3))

    fun checkCount(count: Int, expr: String) {
      assertThat(game.count(metric(expr))).isEqualTo(count)
    }

    checkCount(1, "Class<Class>")
    checkCount(2, "Class<CityTile>") // Huh? aha, Capital
    checkCount(3, "Class<GlobalParameter>")
    checkCount(4, "Class<CardResource>") // Plants Microbes Animals.. and Fighters
    checkCount(5, "Class<Milestone>")
    checkCount(6, "Class<StandardResource>")
    checkCount(7, "Class<StandardAction>")
    checkCount(9, "Class<SpecialTile>") // oops missing some
    checkCount(10, "Class<Tag>")
    checkCount(11, "Class<OwnedTile>") // so here too
    checkCount(12, "Class<WaterArea>")
    checkCount(63, "Class<Area>")

    assertThat(game.count(metric("Class<CardFront>"))).isGreaterThan(200)
    assertThat(game.count(metric("Class<Component>"))).isGreaterThan(300)
    assertThat(game.count(metric("Class"))).isGreaterThan(300)
  }

  @Test
  fun createsExpectedSingletons() {
    val game = Engine.newGame(GameSetup(Canon, "BRMPX", 3))
    val startingComponents: Multiset<Component> = game.getComponents(game.resolve(COMPONENT.expr))

    // 19 duplicate TR and 4 duplicate PROD[M]
    assertThat(startingComponents).hasSize(startingComponents.elements.size + 69)

    val isArea: (Component) -> Boolean = { it.toString().startsWith("[Tharsis_") }
    // val isBorder: (Component) -> Boolean = { it.toString().startsWith("[Border<") }
    val isClass: (Component) -> Boolean = { it.toString().startsWith("[Class<") }

    assertThat(startingComponents.count(isArea)).isEqualTo(61)
    // assertThat(startingComponents.count(isBorder)).isEqualTo(312)
    assertThat(startingComponents.count(isClass)).isGreaterThan(400)

    val theRest =
        startingComponents.filterNot {
          isArea(it) ||
              // isBorder(it) ||
              isClass(it) ||
              it.hasType(game.resolve(expression("TerraformRating"))) ||
              it.hasType(game.resolve(expression("Production<Class<Megacredit>>"))) // TODO PROD[M]
        }
    assertThat(theRest.toStrings())
        .containsExactly(
            "[Game]",
            "[Tharsis]",
            "[Area021]", "[Area081]", "[FloatingInSpace]",
            "[Player1]", "[Player2]", "[Player3]",
            "[PlayCardFromHand]", "[UseStandardProject]", "[ClaimMilestone]",
            "[UseActionFromCard]", "[ConvertHeat]", "[ConvertPlants]",
            "[SellPatents]",
            "[PowerPlantSP]", "[AsteroidSP]", "[AquiferSP]", "[GreenerySP]", "[CitySP]",
            "[GrossHack<Player1>]", "[GrossHack<Player2>]", "[GrossHack<Player3>]",
        )
  }
}
