package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth
import dev.martianzoo.tfm.petaform.PetaformParser.parse

inline fun <reified T : PetaformNode> testRoundTrip(start: String, end: String = start) =
    Truth.assertThat(parse<T>(start).toString()).isEqualTo(end)

inline fun <reified T : PetaformNode> testRoundTrip(start: T, end: T = start) =
    Truth.assertThat(parse<T>(start.toString())).isEqualTo(end)

inline fun <reified T : PetaformNode> testSampleStrings(inputs: String): Boolean {
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
