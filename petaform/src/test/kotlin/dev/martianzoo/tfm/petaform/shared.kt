package dev.martianzoo.tfm.petaform

import com.google.common.truth.Truth

inline fun <reified T : PetaformNode> testRoundTrip(start: String, end: String = start) =
    Truth.assertThat(PetaformParser.parse<T>(start).toString()).isEqualTo(end)

inline fun <reified T : PetaformNode> testRoundTrip(start: T, end: T = start) =
    Truth.assertThat(PetaformParser.parse<T>(start.toString())).isEqualTo(end)
