package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import kotlin.reflect.KClass
import io.kotest.assertions.withClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe

internal fun te(s: String): Expression = parse(s)

internal inline fun <reified T : PetNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    parse(type, start).toString() shouldBe end

internal inline fun <reified T : PetNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    parse(type, start.toString()) shouldBe end

internal fun assertFails(message: String = "(no message)", shouldFail: () -> Unit) =
    withClue(message) { shouldThrow<Exceptions.ExpressionException>(shouldFail) }

internal fun loadTypes(vararg decl: String) =
    loader(
        """
          ABSTRACT CLASS $COMPONENT
          CLASS $CLASS<$COMPONENT> { HAS =1 This }
          CLASS Ok
          ${decl.joinToString("") { "$it\n" }}
        """
            .trimIndent())
