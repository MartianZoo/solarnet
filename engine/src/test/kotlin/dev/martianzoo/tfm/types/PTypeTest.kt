package dev.martianzoo.tfm.types

import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn as cn1
import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import org.junit.jupiter.api.Test

private class PTypeTest {

  @Test
  fun testCycle() {
    val table: PClassLoader =
        loadTypes(
            "ABSTRACT CLASS Anyone",
            "CLASS Player1 : Anyone",
            "CLASS Player2 : Anyone",
            "ABSTRACT CLASS Owned<Anyone>",
            "ABSTRACT CLASS CardFront : Owned",
            "ABSTRACT CLASS Cardbound<CardFront> : Owned",
            "ABSTRACT CLASS ResourcefulCard<Class<CardResource>> : CardFront",
            "ABSTRACT CLASS CardResource : " +
                "Owned<Anyone>, Cardbound<Anyone, ResourcefulCard>", // TODO <Class<This>> ?
            "CLASS Animal : CardResource<ResourcefulCard<Class<Animal>>>",
            "CLASS Microbe : CardResource<ResourcefulCard<Class<Microbe>>>",
            "CLASS Fish : ResourcefulCard<Class<Animal>>",
            "CLASS Ants : ResourcefulCard<Class<Microbe>>",
        )
    assertThat(table.resolve(typeExpr("Animal<Fish>")).abstract).isTrue()

    val fish = table.resolve(typeExpr("Animal<Player1, Fish<Player1>>"))
    assertThat(fish.abstract).isFalse()

    assertThat(table.get(cn1("Fish")).baseType.toString()).isEqualTo("Fish<Anyone, Class<Animal>>")

    // TODO get these working
    // assertFails { table.resolve("Animal<Ants>") }
    // assertThat(table["Animal"].baseType.toString())
    //    .isEqualTo("Animal<Anyone, ResourcefulCard<Anyone, Class<Animal>>>")
    // assertFails {
    //  table.resolve("Microbe<Player1, Ants<Player2>>")
    // }
  }
}
