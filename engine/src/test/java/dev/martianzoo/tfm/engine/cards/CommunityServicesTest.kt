package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class CommunityServicesTest {
  @Test
  fun communityServices() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))
    val p1 = game.tfm(PLAYER1)

    with(game.gameplay(PLAYER1).godMode()) {
      manual("10 ProjectCard, ForcedPrecipitation")
      manual("AtmoCollectors") { doTask("2 Floater<AtmoCollectors>") }
      manual("Airliners") { doTask("2 Floater<AtmoCollectors>") }

      assertThat(p1.production(cn("M"))).isEqualTo(2)

      manual("CommunityServices") // 3 tagless cards: Atmo, Airl, Comm
      assertThat(p1.production(cn("M"))).isEqualTo(5)
    }
  }
}
