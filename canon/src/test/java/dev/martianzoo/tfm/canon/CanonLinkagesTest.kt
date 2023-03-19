package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonLinkagesTest {
  @Test
  fun depToEffectLinkages() {
    val declarations = Canon.allClassDeclarations.values
    val linkages: Map<String, List<String>> =
        declarations.associate { decl ->
          decl.className.toString() to decl.effects.flatMap { it.linkages }
              .toSet()
              .toStrings()
        }.filterValues { it.any() }

    assertThat(linkages)
        .containsExactly(
            // "Border",
            // "Neighbor",
            "Production", listOf("StandardResource"),
            "PlayCard", listOf("CardFront"),
            "Accept", listOf("Resource"),
            "Owed", listOf("Resource"),
            "Pay", listOf("Resource"),

            // TODO these should be harmless, but they're wrong; how to get them out?
            // OPEN CLASS might help
            "Astrodrill", listOf("Asteroid"),
            "PharmacyUnion", listOf("Disease"),
            "AerialMappers", listOf("Floater"),
            "Extremophiles", listOf("Microbe"),
            "FloatingHabs", listOf("Floater"),
            "AtmoCollectors", listOf("Floater"),
            "JovianLanterns", listOf("Floater"),
            "CometAiming", listOf("Asteroid"),
            "DirectedImpactors", listOf("Asteroid"),
            "AsteroidRights", listOf("Asteroid"),
        )
  }
}
