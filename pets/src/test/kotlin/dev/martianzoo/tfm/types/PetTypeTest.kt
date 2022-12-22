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
    val birds = table.resolve("Card072").petClass
    println(birds.allSuperclasses)
    println(birds.baseType)
    println(birds.directDependencyKeys)
    println(birds.allDependencyKeys)
    assertThat(table.resolve("Player1").abstract).isFalse()
    assertThat(table.resolve("Card072<Player1>").abstract).isTrue() // TODO FALSE
    assertThat(table.resolve("Animal<Player1, Card072<Player1>>").abstract).isTrue() // TODO FALSE
  }

  @Test
  fun wtf7() {
    assertThat(table.resolve("Animal<Card072<Player1>, Player1>").abstract).isTrue() // TODO FALSE
  }
}
