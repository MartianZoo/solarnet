package dev.martianzoo.types

import dev.martianzoo.api.Exceptions
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

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

internal fun loadTypes(vararg decl: String) = loader(decl.joinToString("\n"))
