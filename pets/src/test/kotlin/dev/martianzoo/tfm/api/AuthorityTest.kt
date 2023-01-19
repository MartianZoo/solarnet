package dev.martianzoo.tfm.api

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.Deck
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

// Doesn't test much, but the class doesn't do that much
class AuthorityTest {
  @Test
  fun test() {
    val authority = object : Authority.Empty() {
      override val cardDefinitions = listOf(
          CardDefinition(
              idRaw = "123",
              deck = Deck.PRELUDE,
              effectsText = setOf("This: Plant"),
              bundle =  "Z",
              extraClassesText = setOf("CLASS Foo<Boo> : Loo { HAS =1 Bar; Abc: Xyz }"))
      )
    }
    assertThat(authority.allClassDeclarations).hasSize(2)
    assertThat(authority.classDeclaration(cn("IndustrialCenter")).abstract).isFalse()
    assertThat(authority.classDeclaration(cn("Foo")).dependencies).hasSize(1)
  }
}
