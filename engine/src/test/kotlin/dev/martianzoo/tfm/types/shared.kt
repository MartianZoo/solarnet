package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.PetsNode
import kotlin.reflect.KClass

internal inline fun <reified T : PetsNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetsNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    assertThat(parse(type, start).toString()).isEqualTo(end)

internal inline fun <reified T : PetsNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetsNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    assertThat(parse(type, start.toString())).isEqualTo(end)
