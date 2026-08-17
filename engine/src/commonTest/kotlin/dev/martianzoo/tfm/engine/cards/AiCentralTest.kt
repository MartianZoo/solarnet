package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class AiCentralTest : CardTest() {
  @Test
  fun `with its prerequisites, plays AI Central`() {
    newGame()
    engine.phase("Action")
    p1.manual(
        "19, Steel, ProjectCard, $SearchForLife, $InventorsGuild, $DesignedMicroorganisms, PROD[Energy]"
    )
    p1.playProject(AiCentral, 19, steel = 1).expect("PROD[-Energy]")
  }

  @Test
  fun `with AI Central, uses its action`() {
    newGame()
    engine.phase("Action")
    p1.manual("PROD[Energy], $AiCentral")
    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

  @Test
  fun `after a generation, uses AI Central again`() {
    newGame()
    engine.phase("Action")
    p1.manual("PROD[Energy], $AiCentral")
    p1.cardAction1(AiCentral)
    engine.manual("Generation")
    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

  @Test
  fun `with two science tags, tries to play AI Central`() {
    newGame()
    engine.phase("Action")
    p1.manual("21, ProjectCard, $SearchForLife, $InventorsGuild, PROD[Energy]")
    shouldThrow<RequirementException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  fun `without energy production, tries to play AI Central`() {
    newGame()
    engine.phase("Action")
    p1.manual("21, ProjectCard, $SearchForLife, $InventorsGuild, $DesignedMicroorganisms")
    shouldThrow<LimitsException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  fun `after using AI Central, tries to use it again`() {
    newGame()
    engine.phase("Action")
    p1.manual("PROD[Energy], $AiCentral")
    p1.cardAction1(AiCentral)
    shouldThrow<LimitsException> { p1.cardAction1(AiCentral) }
  }
}
