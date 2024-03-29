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
    if ("C" in setup().bundles) me.godMode().manual("PROD[-2]")

    val opp = opponent.godMode()
    opp.manual("CityTile<${cityAreas().first}>")
    opp.manual("GreeneryTile<${greeneryAreas().first}>")
    opp.manual("CityTile<${cityAreas().second}>")
    opp.manual("GreeneryTile<${greeneryAreas().second}>")
    opp.manual("-2 OxygenStep!")

    opp.manual("99, 99 S, 99 T, 99 P, 99 E, 99 H")
    opp.manual("PROD[99, 99 S, 99 T, 99 P, 99 E, 99 H]")

    engine.phase("Corporation")
  }

  protected fun nextRound(wgt: String, cardsBought: Int) {
    p1.pass()
    opponent.godMode().manual(wgt)
    engine.nextGeneration(cardsBought, 0)
  }
}
