package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

private class PetTypeTest {

  @Test
  fun testCycle() {
    val table: PetClassLoader =
        loadTypes(
                "ABSTRACT CLASS Anyone",
                "CLASS Player1 : Anyone",
                "CLASS Player2 : Anyone",
                "ABSTRACT CLASS Owned<Anyone>",
                "ABSTRACT CLASS CardFront : Owned",
                "ABSTRACT CLASS Cardbound<CardFront> : Owned",
                "ABSTRACT CLASS ResourcefulCard<CardResource.CLASS> : CardFront",
                "ABSTRACT CLASS CardResource : " +
                        "Owned<Anyone>, Cardbound<Anyone, ResourcefulCard>", // TODO <This.CLASS> ?
                "CLASS Animal : CardResource<ResourcefulCard<Animal.CLASS>>",
                "CLASS Microbe : CardResource<ResourcefulCard<Microbe.CLASS>>",
                "CLASS Fish : ResourcefulCard<Animal.CLASS>",
                "CLASS Ants : ResourcefulCard<Microbe.CLASS>",
        )
            as PetClassLoader
    assertThat(table.resolve("Animal<Fish>").abstract).isTrue()

    val fish = table.resolve("Animal<Player1, Fish<Player1>>")
    assertThat(fish.abstract).isFalse()

    assertThat(table["Fish"].baseType.toString()).isEqualTo("Fish<Anyone, Animal.CLASS>")

    // TODO get these working
    // assertThrows<RuntimeException> { table.resolve("Animal<Ants>") }
    // assertThat(table["Animal"].baseType.toString())
    //    .isEqualTo("Animal<Anyone, ResourcefulCard<Anyone, Animal.CLASS>>")
    // assertThrows<RuntimeException> {
    //  table.resolve("Microbe<Player1, Ants<Player2>>")
    // }
  }
}
