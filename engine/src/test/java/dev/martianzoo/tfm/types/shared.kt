package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.pets.Parsing.parseElement
import dev.martianzoo.tfm.pets.ast.PetNode
import kotlin.reflect.KClass
import org.junit.jupiter.api.assertThrows

internal inline fun <reified T : PetNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    assertThat(parseElement(type, start).toString()).isEqualTo(end)

internal inline fun <reified T : PetNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    assertThat(parseElement(type, start.toString())).isEqualTo(end)

internal fun assertFails(message: String = "(no message)", shouldFail: () -> Unit) =
    assertThrows<RuntimeException>(message, shouldFail)

internal fun loadTypes(vararg decl: String) =
    loader(
        """
        ABSTRACT CLASS ${SpecialClassNames.COMPONENT}
        CLASS ${SpecialClassNames.CLASS}<${SpecialClassNames.COMPONENT}>
        ${decl.joinToString("") { "$it\n" }}
        """
            .trimIndent())
