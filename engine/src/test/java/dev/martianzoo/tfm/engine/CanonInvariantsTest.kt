package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SystemClasses.THIS
import dev.martianzoo.engine.Effector
import dev.martianzoo.engine.Limiter
import dev.martianzoo.engine.Limiter.RangeRestriction.SimpleRangeRestriction
import dev.martianzoo.engine.Limiter.RangeRestriction.UnboundRangeRestriction
import dev.martianzoo.engine.WritableComponentGraph
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.MClassLoader
import kotlin.Int.Companion.MAX_VALUE
import org.junit.jupiter.api.Test

internal class CanonInvariantsTest {

  private val table = MClassLoader(Canon).loadEverything() as MClassLoader

  @Test
  fun introspect() {
    val effector = Effector({ null!! }, table)
    val limiter = Limiter(table, WritableComponentGraph(effector))

    fun checkTypeLimits(s: String, vararg pairs: Pair<String, IntRange>) {
      val c = table.resolve(parse<Expression>(s))
      val actual =
          limiter
              .applicableRangeRestrictions(c.toComponent())
              .filter { it.range != 0..MAX_VALUE }
              .map { it.mtype.expression.toString() to it.range }
      assertThat(actual).containsExactly(*pairs)
    }
    fun checkComponentLimit(s: String, range: IntRange) = checkTypeLimits(s, s to range)

    checkComponentLimit("Class<Plant>", 1..1)
    checkComponentLimit("Engine", 0..1)
    checkComponentLimit("TerraformingMars", 1..1)
    checkComponentLimit("Tharsis", 1..1)
    checkComponentLimit("Tharsis_5_5", 1..1)
    checkComponentLimit("PlayCardSA", 1..1)
    checkComponentLimit("PowerPlantSP", 1..1)

    checkComponentLimit("OxygenStep", 0..14)
    checkComponentLimit("TemperatureStep", 0..19)
    checkComponentLimit("VenusStep", 0..15)

    checkComponentLimit("ActionUsedMarker<Player1, Ants<Player1>>", 0..1)
    checkComponentLimit("PowerTag<Player1, Ants<Player1>>", 0..2)
    checkComponentLimit("Accept<Player1, Class<Steel>>", 0..1)
    checkComponentLimit("Pass<Player1>", 0..1)

    checkTypeLimits("SetupPhase", "Phase" to 1..1)
    checkTypeLimits("OceanTile<Tharsis_5_5>", "OceanTile" to 0..9, "Tile<Tharsis_5_5>" to 0..1)
  }

  @Test
  fun testLookup() {
    val limiter = Limiter(table, WritableComponentGraph(Effector({ null!! }, table)))

    fun restrictions(a: String) = limiter.rangeRestrictionsByClass[table.getClass(cn(a))]

    fun checkSimple(a: String, b: String = a, range: IntRange) {
      assertThat(restrictions(a))
          .contains(SimpleRangeRestriction(table.resolve(parse<Expression>(b)), range))
    }
    fun checkUnbound(type: String, expr: Expression, range: IntRange) {
      val clazz = table.getClass(cn(type))
      assertThat(restrictions(type)).contains(UnboundRangeRestriction(expr, clazz, range))
    }

    checkSimple("Ants", range = 0..1)
    checkSimple("OceanTile", range = 0..9)
    checkSimple("ActionPhase", "Phase", range = 1..1)
    checkSimple("Tharsis_5_5", range = 1..1)
    checkSimple("GreeneryTile", "Tile<Tharsis_5_5>", range = 0..1)

    checkSimple("Trade", "Trade<Luna>", range = 0..1)
    checkSimple("Colony", "Colony<Luna>", range = 0..3)
    checkSimple("TradeFleetA", range = 0..1)

    checkUnbound("Pass", THIS.expression, 0..1)
    checkUnbound("VenusTag", THIS.expression, 0..2)
    checkUnbound("ColonyProduction", THIS.expression, range = 0..6)
  }
}
