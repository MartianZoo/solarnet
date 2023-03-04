package dev.martianzoo.tfm.canon

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonLinkagesTest {
  @Test
  fun onlyCardboundHasDepToDepLinkages() {
    val declarations = Canon.allClassDeclarations.values
    val haveLinkages = declarations.filter { it.depToDepLinkages.any() }
    assertThat(haveLinkages.classNames())
        .containsExactly(
            cn("Cardbound"),
            cn("Adjacency"), // TODO fix this!!
            cn("Border"), // and this!!
        )
    assertThat(Canon.classDeclaration(cn("Cardbound")).depToDepLinkages).containsExactly(ANYONE)
  }

  @Test
  fun depToEffectLinkages() {
    val declarations = Canon.allClassDeclarations.values
    val haveLinkages = declarations.filter { it.depToEffectLinkages.any() }
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
