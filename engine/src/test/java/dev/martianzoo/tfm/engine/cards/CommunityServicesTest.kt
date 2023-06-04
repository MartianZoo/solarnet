package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.TerraformingMarsApi
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

class CommunityServicesTest {
  @Test
  fun communityServices() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))
    val p1 = TerraformingMarsApi(game, PLAYER1)

    with(game.gameplay(PLAYER1).turnLayer().operationLayer()) {
      initiate("10 ProjectCard, ForcedPrecipitation")
      initiate("AtmoCollectors") { doTask("2 Floater<AtmoCollectors>") }
      initiate("Airliners") { doTask("2 Floater<AtmoCollectors>") }

      assertThat(p1.production(cn("M"))).isEqualTo(2)

      initiate("CommunityServices") // 3 tagless cards: Atmo, Airl, Comm
      assertThat(p1.production(cn("M"))).isEqualTo(5)
    }
  }
}
