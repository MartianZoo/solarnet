package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth
import dev.martianzoo.tfm.pets.PetsParser.parse
import dev.martianzoo.tfm.pets.ast.PetsNode
import kotlin.reflect.KClass

inline fun <reified T : PetsNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

fun <T : PetsNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    Truth.assertThat(parse(type, start).toString()).isEqualTo(end)

inline fun <reified T : PetsNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

fun <T : PetsNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    Truth.assertThat(parse(type, start.toString())).isEqualTo(end)

inline fun <reified T : PetsNode> testSampleStrings(inputs: String) =
    testSampleStrings(T::class, inputs)

fun <T : PetsNode> testSampleStrings(type: KClass<T>, inputs: String): Boolean {
  for (sample in inputs.split('\n')) {
    val regen = parse(type, sample).toString()
    if (regen != sample) return false
  }
  return true
}
