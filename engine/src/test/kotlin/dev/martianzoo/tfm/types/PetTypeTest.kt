package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PetTypeTest {

  @Test
  fun testCycle() {
    val table: PetClassLoader = loadTypes(
        "ABSTRACT CLASS Player",
        "CLASS Player1 : Player",
        "CLASS Player2 : Player",
        "ABSTRACT CLASS Owned<Player>",
        "ABSTRACT CLASS CardFront : Owned",
        "ABSTRACT CLASS Cardbound<CardFront> : Owned",
        "ABSTRACT CLASS ResourcefulCard<CardResource.CLASS> : CardFront",
        "ABSTRACT CLASS CardResource : Owned<Player>, Cardbound<Player, ResourcefulCard<This.CLASS>>",

        "CLASS Animal : CardResource",
        "CLASS Microbe : CardResource<ResourcefulCard<Microbe.CLASS>>",

        "CLASS Fish : ResourcefulCard<Animal.CLASS>",
        "CLASS Ants : ResourcefulCard<Microbe.CLASS>",
    ) as PetClassLoader
    assertThat(table.resolve("Animal<Fish>").abstract).isTrue()

    val fish = table.resolve("Animal<Player1, Fish<Player1>>")
    assertThat(fish.abstract).isFalse()

    assertThat(table["Fish"].baseType.toString()).isEqualTo("Fish<Player, Animal.CLASS>")

    // TODO get these working
    //assertThrows<RuntimeException> { table.resolve("Animal<Ants>") }
    //assertThat(table["Animal"].baseType.toString()).isEqualTo("Animal<Player, ResourcefulCard<Player, Animal.CLASS>>")
    //assertThrows<RuntimeException> { table.resolve("Microbe<Player1, Ants<Player2>>") }
  }
}
