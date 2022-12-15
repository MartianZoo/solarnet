package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth
import dev.martianzoo.tfm.pets.PetsParser.parse

inline fun <reified T : PetsNode> testRoundTrip(start: String, end: String = start) =
    Truth.assertThat(parse<T>(start).toString()).isEqualTo(end)

inline fun <reified T : PetsNode> testRoundTrip(start: T, end: T = start) =
    Truth.assertThat(parse<T>(start.toString())).isEqualTo(end)

inline fun <reified T : PetsNode> testSampleStrings(inputs: String): Boolean {
  var wereGoodBro = true
  inputs.split('\n').forEachIndexed { i, sample ->
    val regen = parse<T>(sample).toString()
    if (regen != sample) {
      wereGoodBro = false
      println("original $i: $sample")
      println("came out as: $regen")
      println()
    }
  }
  return wereGoodBro
}
