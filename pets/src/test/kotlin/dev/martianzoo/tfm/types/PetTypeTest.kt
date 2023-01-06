package dev.martianzoo.tfm.types

import com.google.common.truth.Truth
import org.junit.jupiter.api.Test

class PetTypeTest {

  @Test
  fun testCycle() {
    val table: PetClassTable = loadTypes(
        "ABSTRACT CLASS Player",
        "CLASS Player1 : Player",
        "ABSTRACT CLASS Owned<Player>",
        "ABSTRACT CLASS CardFront : Owned",
        "ABSTRACT CLASS Cardbound<CardFront> : Owned",
        "ABSTRACT CLASS CardResource : Cardbound<ResourcefulCard>",
        "CLASS Animal : CardResource<ResourcefulCard<Animal.CLASS>>",
        "ABSTRACT CLASS ResourcefulCard<CardResource.CLASS> : CardFront",
        "CLASS Fish : ResourcefulCard<Animal.CLASS>",
    )
    Truth.assertThat(table.resolve("Animal<Fish>").abstract).isTrue()

    val fish = table.resolve("Animal<Player1, Fish<Player1>>")
    Truth.assertThat(fish.abstract).isFalse()
  }
}
