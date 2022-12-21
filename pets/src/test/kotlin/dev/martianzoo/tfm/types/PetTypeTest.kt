package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import org.junit.jupiter.api.Test

class PetTypeTest {
  val table = PetClassLoader(Canon.allDefinitions).loadAll()

  @Test
  fun wtf1() {
    assertThat(table.isValid("GreeneryTile<Area220>")).isTrue() // TODO FALSE
  }

  // also UseAction2, PlayCard

  @Test
  fun wtf2() {
    assertThat(table.resolve("Animal<Card072>").abstract).isTrue()
  }

  @Test
  fun wtf3() {
    assertThat(table.resolve("Animal<Player1>").abstract).isTrue()
  }

  @Test
  fun wtf4() {
    assertThat(table.resolve("Animal<Card072, Player1>").abstract).isTrue() // TODO FALSE
  }

  @Test
  fun wtf5() {
    assertThat(table.resolve("Animal<Player1, Card072>").abstract).isTrue() // TODO FALSE
  }

  @Test
  fun wtf6() {
    assertThat(table.resolve("Animal<Player1, Card072<Player1>>").abstract).isFalse()
  }

  @Test
  fun wtf7() {
    assertThat(table.resolve("Animal<Card072<Player1>, Player1>").abstract).isFalse()
  }
}
