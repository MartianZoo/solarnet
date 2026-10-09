package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.CustomInstruction
import dev.martianzoo.state.CustomMetric
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.Cimmeria
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Elysium
import dev.martianzoo.tfm.tests.TestOption.Hellas
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.Tharsis
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.Utopia
import dev.martianzoo.tfm.tests.TestOption.Vastitas
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.TestOption.WorldGovernmentRule
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CanonAdmissibilityTest {
  @Test
  internal fun engineImplementationsMatchCanonicalCustomDeclarations() {
    val declarations = Canon.customClassDeclarations.associateBy { it.className }
    val implementationsByName = TfmEngine.customClasses.groupBy { it.className }
    implementationsByName.values.all { it.size == 1 } shouldBe true
    val implementations = implementationsByName.mapValues { (_, matches) -> matches.single() }

    implementations.keys shouldBe declarations.keys
    implementations.forEach { (name, implementation) ->
      val declaration = declarations.getValue(name)
      (implementation is CustomMetric) shouldBe declaration.customMetric
      (implementation is CustomInstruction) shouldBe declaration.customInstruction
    }
    Canon.customClassDependencies.keys.all(declarations::containsKey) shouldBe true
  }

  @Test
  internal fun everySupportedMapBuildsAnIdleWorldWithTheRequestedMap() {
    val maps =
        listOf(
            Tharsis to "TharsisMap",
            Hellas to "HellasMap",
            Elysium to "ElysiumMap",
            Amazonis to "AmazonisMap",
            Vastitas to "VastitasMap",
            Utopia to "UtopiaMap",
            Cimmeria to "CimmeriaMap",
        )

    maps.forEach { (option, mapClass) ->
      val world = TfmEngine.newGame(canonicalPremise(option))

      world.classTable.isInhabited(cn(mapClass)) shouldBe true
      world.actors.shouldContainExactly(PLAYER1, PLAYER2, ADMIN)
      world.isIdle() shouldBe true
    }
  }

  @Test
  internal fun representativeCompleteConfigurationBuildsOneCoherentGameView() {
    val colonies = testColonyTiles(players = 2)
    val selected =
        arrayOf(
            CorporateEraExpansion,
            Cimmeria,
            VenusNextExpansion,
            PreludeExpansion,
            Prelude2CardPack,
            ColoniesExpansion,
            TurmoilExpansion,
            PromoCardPack,
            WorldGovernmentRule,
        )

    val world = TfmEngine.newGame(canonicalPremise(*selected, colonyTiles = colonies))

    selected.forEach { world.classTable.isInhabited(it.className) shouldBe true }
    colonies.forEach { world.classTable.isInhabited(it) shouldBe true }
    world.classTable.isInhabited(cn("CimmeriaMap")) shouldBe true
    world.isIdle() shouldBe true
  }
}
