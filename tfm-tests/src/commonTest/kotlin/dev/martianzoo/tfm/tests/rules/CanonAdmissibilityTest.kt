package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.Cimmeria
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Elysium
import dev.martianzoo.tfm.tests.TestOption.Hellas
import dev.martianzoo.tfm.tests.TestOption.Prelude2Expansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.Tharsis
import dev.martianzoo.tfm.tests.TestOption.TurmoilCardPack
import dev.martianzoo.tfm.tests.TestOption.Utopia
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.TestOption.WorldGovernmentOption
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonAdmissibilityTest {
  @Test
  internal fun everySupportedMapBuildsAnIdleWorldWithTheRequestedMap() {
    val maps =
        listOf(
            Tharsis to "TharsisMap",
            Hellas to "HellasMap",
            Elysium to "ElysiumMap",
            Utopia to "UtopiaMap",
            Cimmeria to "CimmeriaMap",
        )

    maps.forEach { (option, mapClass) ->
      val world = Engine.newGame(canonicalPremise(option))

      world.classTable.isActive(cn(mapClass)) shouldBe true
      world.actors.shouldContainExactly(PLAYER1, PLAYER2, ENGINE)
      world.isIdle() shouldBe true
    }
  }

  @Test
  internal fun representativeCompleteConfigurationBuildsOneCoherentProjection() {
    val colonies = testColonyTiles(players = 2)
    val selected =
        arrayOf(
            CorporateEraExpansion,
            Cimmeria,
            VenusNextExpansion,
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
    world.classTable.isActive(cn("CimmeriaMap")) shouldBe true
    world.isIdle() shouldBe true
  }
}
