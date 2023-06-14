package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.Exceptions
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.PetNode
import kotlin.reflect.KClass
import org.junit.jupiter.api.assertThrows

internal fun te(s: String): Expression = parse(s)

internal inline fun <reified T : PetNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    assertThat(parse(type, start).toString()).isEqualTo(end)

internal inline fun <reified T : PetNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    assertThat(parse(type, start.toString())).isEqualTo(end)

internal fun assertFails(message: String = "(no message)", shouldFail: () -> Unit) =
    assertThrows<Exceptions.ExpressionException>(message, shouldFail)

internal fun loadTypes(vararg decl: String) =
    loader(
        """
          ABSTRACT CLASS $COMPONENT
          CLASS $CLASS<$COMPONENT> { HAS =1 This }
          CLASS Ok
          ${decl.joinToString("") { "$it\n" }}
        """
            .trimIndent())
