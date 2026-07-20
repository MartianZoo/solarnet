package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.cards.CardTest
import dev.martianzoo.util.toSetStrict
import kotlin.test.BeforeTest

abstract class ColoniesCardTest : CardTest() {
  protected val colonyTiles: Set<ClassName> =
      setOf("Luna", "Io", "Triton", "Europa", /*delayed*/ "Titan").toSetStrict(::cn)
  protected val p1: TfmGameplay
    get() = player1

  protected val p2: TfmGameplay
    get() = player2

  @BeforeTest
  fun commonSetup() {
    newGame("BRMC", 3, colonyTiles)
    engine.sneak("100 Megacredit<P1>, 5 ProjectCard<P1>, 100 Megacredit<P2>, 5 ProjectCard<P2>")
    engine.phase("Action")
  }
}
