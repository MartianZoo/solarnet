package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonLinkagesTest {
  @Test
  fun depToEffectLinkages() {
    val declarations = Canon.allClassDeclarations.values
    val haveLinkages: List<ClassDeclaration> =
        declarations.filter { decl -> decl.effects.any { it.linkages.any() } }

    assertThat(haveLinkages.classNames().toStrings())
        .containsExactly(
            "Border",
            "Neighbor",
            "Production",
            "Owed",
            "Pay",

            // TODO these should be harmless, but they're wrong; how to get them out?
            // OPEN CLASS might help
            "Astrodrill",
            "PharmacyUnion",
            "AerialMappers",
            "Extremophiles",
            "FloatingHabs",
            "AtmoCollectors",
            "JovianLanterns",
            "CometAiming",
            "DirectedImpactors",
            "AsteroidRights",
        )
  }
}
