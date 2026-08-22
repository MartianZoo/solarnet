package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.engine.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.engine.TestOption.ElysiumMapOption
import dev.martianzoo.tfm.engine.TestOption.HellasMapOption
import dev.martianzoo.tfm.engine.TestOption.MilestonesAwardsExpansion
import dev.martianzoo.tfm.engine.TestOption.Prelude2Expansion
import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.TestOption.TerraCimmeriaMapOption
import dev.martianzoo.tfm.engine.TestOption.TharsisMapOption
import dev.martianzoo.tfm.engine.TestOption.TurmoilCardPack
import dev.martianzoo.tfm.engine.TestOption.UtopiaPlanitiaMapOption
import dev.martianzoo.tfm.engine.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.engine.TestOption.WorldGovernmentOption
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonAdmissibilityTest {
  @Test
  fun everySupportedMapBuildsAnIdleWorldWithTheRequestedMap() {
    val maps =
        listOf(
            TharsisMapOption to "Tharsis",
            HellasMapOption to "Hellas",
            ElysiumMapOption to "Elysium",
            UtopiaPlanitiaMapOption to "Utopia",
            TerraCimmeriaMapOption to "Cimmeria",
        )

    maps.forEach { (option, mapClass) ->
      val world = Engine.newGame(canonicalPremise(option))

      world.classTable.isActive(cn(mapClass)) shouldBe true
      world.actors.shouldContainExactly(PLAYER1, PLAYER2, ENGINE)
      world.isIdle() shouldBe true
    }
  }

  @Test
  fun representativeCompleteConfigurationBuildsOneCoherentProjection() {
    val colonies = testColonyTiles(players = 2)
    val selected =
        arrayOf(
            CorporateEraExpansion,
            TerraCimmeriaMapOption,
            VenusNextExpansion,
            MilestonesAwardsExpansion,
            PreludeExpansion,
            Prelude2Expansion,
            ColoniesExpansion,
            TurmoilCardPack,
            PromoCardPack,
            WorldGovernmentOption,
        )

    val world = Engine.newGame(canonicalPremise(*selected, colonyTiles = colonies))

    selected.forEach { world.classTable.isActive(it.className) shouldBe true }
    colonies.forEach { world.classTable.isActive(it) shouldBe true }
    world.classTable.isActive(cn("Cimmeria")) shouldBe true
    world.isIdle() shouldBe true
  }
}
