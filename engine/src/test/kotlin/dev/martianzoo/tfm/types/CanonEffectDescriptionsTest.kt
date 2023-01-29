package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class CanonEffectDescriptionsTest {
  @Disabled // TODO
  @Test
  fun gyropolis() {
    val loader = PClassLoader(Canon, true)
    val card = loader.load(cn("Gyropolis"))
    loader.frozen = true

    assertThat(card.describe().split("\n"))
        .containsExactly(
            "Name:     Gyropolis",
            "Id:       C230",
            "Abstract: false",
            "Supers:   Owned, CardFront, AutomatedCard",
            "Subs:     (none)",
            "Deps:     Owned_0=Owner",
            "Effects:  This: CityTile<LandArea(HAS MAX 0 CityTile<Anyone>), Owner>!, " +
                "-2 Production<Owner, Class<Energy>>, " +
                "Production<Owner, Class<Megacredit>>! / VenusTag<Owner>, " +
                "Production<Owner, Class<Megacredit>>! / EarthTag<Owner>",
        )
  }
}
