package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parseAsIs
import dev.martianzoo.tfm.pets.ast.PetNode
import kotlin.reflect.KClass

internal inline fun <reified T : PetNode> checkBothWays(asText: String, node: T) =
    checkBothWays(T::class, asText, node)

internal fun <T : PetNode> checkBothWays(type: KClass<T>, asText: String, node: T) {
  assertThat(node.toString()).isEqualTo(asText)
  assertThat(parseAsIs(type, asText)).isEqualTo(node)
}

internal inline fun <reified T : PetNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    assertThat(parseAsIs(type, start).toString()).isEqualTo(end)

internal inline fun <reified T : PetNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    assertThat(parseAsIs(type, start.toString())).isEqualTo(end)

internal inline fun <reified T : PetNode> testSampleStrings(inputs: String) =
    testSampleStrings(T::class, inputs)

internal fun <T : PetNode> testSampleStrings(type: KClass<T>, inputs: String) {
  var errors = ""
  for (sample in inputs.split('\n')) {
    val regen = parseAsIs(type, sample).toString()
    if (regen != sample) errors += "$sample\n"
  }
  if (errors.isNotEmpty()) {
    throw RuntimeException(errors)
  }
}
