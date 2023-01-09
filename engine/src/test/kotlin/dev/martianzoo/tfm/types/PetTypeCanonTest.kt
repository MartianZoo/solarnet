package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.canon.Canon
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class PetTypeCanonTest {
  val table = PetClassLoader(Canon).loadEverything()

  @Test
  fun wtf1() {
    assertThrows<RuntimeException> { table.resolve("GreeneryTile<Area220>") }
  }

  // also UseAction2, PlayCard

  @Test
  fun wtf2() {
    assertThat(table.resolve("Animal<Birds>").abstract).isTrue()
  }

  @Test
  fun wtf3() {
    assertThat(table.resolve("Animal<Player1>").abstract).isTrue()
  }

  @Test
  fun wtf5() {
    assertThat(table.resolve("Animal<Player1, Birds>").abstract).isTrue() // TODO FALSE
  }

  @Test
  fun wtf6() {
    assertThat(table.resolve("Player1").abstract).isFalse()
    assertThat(table.resolve("Birds<Player1>").abstract).isFalse()
    assertThat(table.resolve("Animal<Player1, Birds<Player1>>").abstract).isFalse()
  }

  @Test
  fun wtf7() {
    assertThat(table.resolve("Animal<Player1, Birds<Player1>>").abstract).isFalse()
  }
}
