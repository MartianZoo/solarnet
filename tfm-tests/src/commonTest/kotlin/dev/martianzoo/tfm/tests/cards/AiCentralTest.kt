package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AiCentralTest : CardTest() {
  @Test
  internal fun `Can be played with three science tags`() {
    newGame()
    engine.phase("Action")
    p1.manual(
        "19, Steel, ProjectCard, $SearchForLife, $InventorsGuild, $DesignedMicroorganisms, PROD[Energy]"
    )
    p1.playProject(AiCentral, 19, steel = 1).expect("PROD[-Energy]")
  }

  @Test
  internal fun `Can use its action`() {
    newGame()
    engine.phase("Action")
    p1.manual("PROD[Energy], $AiCentral")
    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

  @Test
  internal fun `Can use its action again next generation`() {
    newGame()
    engine.phase("Action")
    p1.manual("PROD[Energy], $AiCentral")
    p1.cardAction1(AiCentral)
    engine.manual("Generation")
    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

  @Test
  internal fun `Cannot be played with only two science tags`() {
    newGame()
    engine.phase("Action")
    p1.manual("21, ProjectCard, $SearchForLife, $InventorsGuild, PROD[Energy]")
    shouldThrow<RequirementException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  internal fun `Cannot be played without energy production`() {
    newGame()
    engine.phase("Action")
    p1.manual("21, ProjectCard, $SearchForLife, $InventorsGuild, $DesignedMicroorganisms")
    shouldThrow<LimitsException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  internal fun `Cannot use its action twice in one generation`() {
    newGame()
    engine.phase("Action")
    p1.manual("PROD[Energy], $AiCentral")
    p1.cardAction1(AiCentral)
    shouldThrow<LimitsException> { p1.cardAction1(AiCentral) }
  }
}
