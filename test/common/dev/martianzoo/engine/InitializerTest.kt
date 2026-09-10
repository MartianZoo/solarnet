package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class InitializerTest {
  @Test
  internal fun generatedPremiseDoesNotFallThroughToDirectCreation() {
    val premise =
        testGamePremise(
                """
                CLASS BootstrapProbe { HAS =1 This }
                CLASS BrokenPremise { HAS =1 This }
                """,
                players = 1,
            )
            .copy(
                initialComponentTypes = setOf(cn("BootstrapProbe").expression),
                premiseClassName = cn("BrokenPremise"),
            )

    val failure = shouldThrow<PetException> { Engine.newGame(premise) }

    failure.message.orEmpty().shouldInclude("Player1 (found 0)")
    failure.message.orEmpty().shouldInclude("BootstrapProbe (found 0)")
  }

  @Test
  internal fun directPremiseCreatesPlayersAndInitialComponentsWithoutAGeneratedRecipe() {
    val premise =
        testGamePremise("CLASS BootstrapProbe { HAS =1 This }", players = 2)
            .copy(initialComponentTypes = setOf(cn("BootstrapProbe").expression))

    val game = Engine.newGame(premise)
    val admin = game.agent(ADMIN)

    premise.premiseClassName shouldBe null
    admin.count("Player") shouldBe 2
    admin.count("BootstrapProbe") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun bootstrapRejectsTaskWithMultipleConcreteNarrowings() {
    val premise =
        testGamePremise(
                """
                ABSTRACT CLASS Choice { CLASS Left, Right }
                CLASS BootstrapProbe { This: Choice }
                """,
                players = 0,
            )
            .copy(initialComponentTypes = setOf(cn("BootstrapProbe").expression))

    shouldThrow<TaskException> { Engine.newGame(premise) }.message.orEmpty().shouldInclude("Choice")
  }
}
