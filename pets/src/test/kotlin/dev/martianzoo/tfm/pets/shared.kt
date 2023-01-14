package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth
import dev.martianzoo.tfm.pets.ElementParsers.parsePets
import dev.martianzoo.tfm.pets.ast.PetNode
import kotlin.reflect.KClass

internal inline fun <reified T : PetNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    Truth.assertThat(parsePets(type, start).toString()).isEqualTo(end)

internal inline fun <reified T : PetNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    Truth.assertThat(parsePets(type, start.toString())).isEqualTo(end)

internal inline fun <reified T : PetNode> testSampleStrings(inputs: String) =
    testSampleStrings(T::class, inputs)

internal fun <T : PetNode> testSampleStrings(type: KClass<T>, inputs: String): Boolean {
  for (sample in inputs.split('\n')) {
    val regen = parsePets(type, sample).toString()
    if (regen != sample) return false
  }
  return true
}
