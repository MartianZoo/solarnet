package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.data.TaskResult
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.util.toSetStrict
import org.junit.jupiter.api.BeforeEach

abstract class ColoniesCardTest {
  protected val colonyTiles: Set<ClassName> =
      setOf("Luna", "Io", "Triton", "Europa", /*delayed*/ "Titan").toSetStrict(::cn)
  protected val setup = GameSetup(Canon, "BRMC", 3, colonyTiles)
  protected val game = Engine.newGame(setup)
  protected val eng = game.tfm(ENGINE)
  protected val p1 = game.tfm(PLAYER1)
  protected val p2 = game.tfm(PLAYER2)

  protected fun TaskResult.expect(string: String) =
      TestHelpers.assertNetChanges(this, game, eng, string)

  @BeforeEach
  fun commonSetup() {
    for (p in listOf(p1, p2)) {
      p.godMode().manual("100, 5 ProjectCard")
    }
    eng.phase("Action")
  }
}
