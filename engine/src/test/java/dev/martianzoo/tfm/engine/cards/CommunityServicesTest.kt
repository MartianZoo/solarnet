package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.engine.PlayerSession.Companion.session
import dev.martianzoo.tfm.engine.TerraformingMars.production
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

class CommunityServicesTest {
  @Test
  fun communityServices() {
    val game = Engine.newGame(GameSetup(Canon, "CVERB", 2))
    with(game.session(PLAYER1)) {
      operation("10 ProjectCard, ForcedPrecipitation")
      operation("AtmoCollectors", "2 Floater<AtmoCollectors>")
      operation("Airliners", "2 Floater<AtmoCollectors>")

      assertThat(production(cn("M"))).isEqualTo(2)

      operation("CommunityServices") // 3 tagless cards: Atmo, Airl, Comm
      assertThat(production(cn("M"))).isEqualTo(5)
    }
  }
}
