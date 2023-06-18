package dev.martianzoo.tfm.engine.games

import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay
import org.junit.jupiter.api.BeforeEach

abstract class AbstractSoloTest : AbstractFullGameTest() {
  protected lateinit var me: TfmGameplay
  protected lateinit var opponent: TfmGameplay

  override fun setup() = GameSetup(Canon, "BRHVPX", 2)

  protected abstract fun cityAreas(): Pair<String, String>
  protected abstract fun greeneryAreas(): Pair<String, String>

  @BeforeEach
  fun soloSetup() {
    me = p1
    opponent = p2

    me.godMode().manual("-6 TR")
    opponent.godMode().manual(
        "CityTile<${cityAreas().first}>, GreeneryTile<${greeneryAreas().first}>")
    opponent.godMode().manual(
        "CityTile<${cityAreas().second}>, GreeneryTile<${greeneryAreas().second}>")
    opponent.godMode().manual("-2 OxygenStep")

    opponent.godMode().sneak("99, 99 S, 99 T, 99 P, 99 E, 99 H")
    opponent.godMode().sneak("PROD[99, 99 S, 99 T, 99 P, 99 E, 99 H]")

    engine.phase("Corporation")
  }
}
