package dev.martianzoo.tfm.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.Int.Companion.MAX_VALUE
import kotlin.test.Test

internal class CanonInvariantsTest {

  private val table = Canon.classTable

  @Test
  internal fun introspect() {
    fun checkTypeLimits(s: String, vararg pairs: Pair<String, IntRange>) {
      val c = table.resolve(parse<Expression>(s))
      val actual =
          table.componentLimits
              .limitsFor(c)
              .filter { it.range != 0..MAX_VALUE }
              .map { it.type.expression.toString() to it.range }
      actual.shouldContainExactlyInAnyOrder(*pairs)
    }
    fun checkComponentLimit(s: String, range: IntRange) = checkTypeLimits(s, s to range)

    checkComponentLimit("Class<Plant>", 1..1)
    checkComponentLimit("Engine", 0..1)
    checkComponentLimit("TerraformingMars", 1..1)
    checkTypeLimits("TharsisMap", "MarsMap" to 1..1, "TharsisMap" to 1..1)
    checkComponentLimit("Tharsis_5_5", 1..1)
    checkComponentLimit("PlayCardSA", 1..1)
    checkComponentLimit("PowerPlantSP", 0..1)

    checkComponentLimit("OxygenStep", 0..14)
    checkComponentLimit("TemperatureStep", 0..19)
    checkComponentLimit("VenusStep", 0..15)

    checkComponentLimit("ActionUsedMarker<Player1, $Ants<Player1>>", 0..1)
    checkComponentLimit("TharsisRepublic_Mandate<Player1>", 0..1)
    checkComponentLimit("PowerTag<Player1, $Ants<Player1>>", 0..2)
    checkComponentLimit("VenusTag<Player1, $Ants<Player1>>", 0..2)
    checkComponentLimit("Accept<Player1, Class<Steel>>", 0..1)
    checkComponentLimit("Pass<Player1>", 0..1)
    checkComponentLimit("ColonyProduction<Callisto>", 0..6)
    checkComponentLimit("FlownTradeFleet<Callisto>", 0..1)
    checkComponentLimit("Colony<Callisto>", 0..3)

    checkTypeLimits("SetupPhase", "Phase" to 1..1)
    checkTypeLimits(
        "OceanTile<Tharsis_5_5>",
        "OceanTile" to 0..9,
        "Tile<Tharsis_5_5>" to 0..1,
    )
  }
}
