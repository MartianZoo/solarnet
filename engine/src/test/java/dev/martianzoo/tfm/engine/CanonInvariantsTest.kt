package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.types.MClassLoader
import dev.martianzoo.types.MType
import dev.martianzoo.util.toStrings
import kotlin.Int.Companion.MAX_VALUE
import org.junit.jupiter.api.Test

internal class CanonInvariantsTest {

  val table = MClassLoader(Canon).loadEverything() as MClassLoader

  @Test
  fun componentCountLimits() {
    fun checkComponentLimit(type: String, intRange: IntRange) {
      val mtype: MType = table.resolve(parse<Expression>(type))
      assertThat(mtype.toComponent().allowedRange).isEqualTo(intRange)
    }

    fun checkTypeLimits(type: String, vararg pairs: Pair<String, IntRange>) {
      val mtype: MType = table.resolve(parse<Expression>(type))
      // TODO do something
    }

    checkComponentLimit("Class<Plant>", 1..1)

    checkComponentLimit("Engine", 0..1)
    checkComponentLimit("TerraformingMars", 1..1)
    checkComponentLimit("Tharsis", 1..1)
    checkComponentLimit("Tharsis_5_5", 1..1)
    checkComponentLimit("PlayCardSA", 1..1)
    checkComponentLimit("PowerPlantSP", 1..1)
    checkComponentLimit("Generation", 1..MAX_VALUE)

    checkComponentLimit("Plant<P1>", 0..MAX_VALUE)

    checkComponentLimit("OxygenStep", 0..14)
    checkComponentLimit("TemperatureStep", 0..19)
    checkComponentLimit("VenusStep", 0..15)

    checkComponentLimit("Ants<P1>", 0..1)
    checkComponentLimit("ActionUsedMarker<P1, Ants<P1>>", 0..1)
    checkComponentLimit("PowerTag<P1, Ants<P1>>", 0..2)
    checkComponentLimit("Accept<P1, Class<Steel>>", 0..1)
    checkComponentLimit("Pass<P1>", 0..1)

    checkTypeLimits("SetupPhase", "Phase" to 1..1)
    checkTypeLimits(
        "OceanTile<Tharsis_5_5>",
        "OceanTile" to 0..9,
        "Tile<Tharsis_5_5>" to 0..1)
  }

  @Test
  fun generalInvariants() {
    assertThat(table.generalInvariants.toStrings())
        .containsExactly(
            "=1 Phase",
            "MAX 9 OceanTile",
        )
  }
}
