package dev.martianzoo.tfm.canon

import com.google.common.truth.MapSubject
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonLinkagesTest {

  fun <K, V> MapSubject.containsExactlyPairs(vararg pairs: Pair<K, V>) =
      containsExactlyEntriesIn(pairs.toMap())

  @Test
  fun depToEffectLinkages() {
    val declarations = Canon.allClassDeclarations.values
    val linkages: Map<String, List<String>> =
        declarations
            .associate { decl ->
              decl.className.toString() to decl.effects.flatMap { it.linkages }.toSet().toStrings()
            }
            .filterValues { it.any() }

    assertThat(linkages)
        .containsExactlyPairs(
            // "Border" to listOf(""),
            // "Neighbor" to listOf(""),
            "Production" to listOf("StandardResource"),
            "PlayCard" to listOf("CardFront"),
            "Accept" to listOf("Resource"),
            "Owed" to listOf("Resource"),
            "Pay" to listOf("Resource"),

            // TODO these should be harmless, but they're wrong; how to get them out?
            // OPEN CLASS might help
            "Astrodrill" to listOf("Asteroid"),
            "PharmacyUnion" to listOf("Disease"),
            "AerialMappers" to listOf("Floater"),
            "Extremophiles" to listOf("Microbe"),
            "FloatingHabs" to listOf("Floater"),
            "AtmoCollectors" to listOf("Floater"),
            "JovianLanterns" to listOf("Floater"),
            "CometAiming" to listOf("Asteroid"),
            "DirectedImpactors" to listOf("Asteroid"),
            "AsteroidRights" to listOf("Asteroid"),
        )
  }
}
