package dev.martianzoo.tfm.engine

import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.engine.ComponentGraph
import dev.martianzoo.engine.Limiter
import dev.martianzoo.engine.Limiter.RangeRestriction.SimpleRangeRestriction
import dev.martianzoo.engine.Limiter.RangeRestriction.UnboundRangeRestriction
import dev.martianzoo.engine.toComponent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import kotlin.Int.Companion.MAX_VALUE
import kotlin.test.Test

internal class CanonInvariantsTest {

  private val table = Canon.classTable

  @Test
  internal fun introspect() {
    val limiter = Limiter(table, ComponentGraph.empty(table))

    fun checkTypeLimits(s: String, vararg pairs: Pair<String, IntRange>) {
      val c = table.resolve(parse<Expression>(s))
      val actual =
          limiter
              .applicableRangeRestrictions(c.toComponent())
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
    checkComponentLimit("Accept<Player1, Class<Steel>>", 0..1)
    checkComponentLimit("Pass<Player1>", 0..1)

    checkTypeLimits("SetupPhase", "Phase" to 1..1)
    checkTypeLimits(
        "OceanTile<Tharsis_5_5>",
        "OceanTile" to 0..9,
        "Tile<Tharsis_5_5>" to 0..1,
    )
  }

  @Test
  internal fun testLookup() {
    val limiter = Limiter(table, ComponentGraph.empty(table))

    fun restrictions(a: String) = limiter.rangeRestrictionsByClass[table.getClass(cn(a))]

    fun checkSimple(a: String, b: String = a, range: IntRange) {
      restrictions(a)!!.shouldContain(
          SimpleRangeRestriction(table.resolve(parse<Expression>(b)), range)
      )
    }
    fun checkUnbound(
        constrainedType: String,
        expr: Expression,
        range: IntRange,
        declaringType: String = constrainedType,
    ) {
      val clazz = table.getClass(cn(declaringType))
      restrictions(constrainedType)!!.shouldContain(
          UnboundRangeRestriction(expr, clazz, table, range)
      )
    }

    checkSimple("$Ants", range = 0..1)
    checkSimple("OceanTile", range = 0..9)
    checkSimple("ActionPhase", "Phase", range = 1..1)
    checkSimple("Tharsis_5_5", range = 1..1)
    checkSimple("GreeneryTile", "Tile<Tharsis_5_5>", range = 0..1)

    checkSimple("FlownTradeFleet", "FlownTradeFleet<Callisto>", range = 0..1)
    checkSimple("Colony", "Colony<Callisto>", range = 0..3)

    checkUnbound("Pass", THIS.expression, 0..1)
    checkUnbound("VenusTag", THIS.expression, 0..2)
    checkUnbound("ColonyProduction", THIS.expression, range = 0..6)
  }
}
