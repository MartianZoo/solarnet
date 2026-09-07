package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class InitializerTest {
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
