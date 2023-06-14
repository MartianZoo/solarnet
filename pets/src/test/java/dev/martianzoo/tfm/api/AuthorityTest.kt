package dev.martianzoo.tfm.api

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import org.junit.jupiter.api.Test

// Doesn't test much, but the class doesn't do that much
private class AuthorityTest {
  @Test
  fun test() {
    val authority =
        object : TfmAuthority.Empty() {
          override val cardDefinitions =
              setOf(
                  CardDefinition(
                      CardData(
                          id = "123",
                          deck = "PRELUDE",
                          immediate = "Plant",
                          bundle = "Z",
                          components = setOf("CLASS Foo<Boo> : Loo { HAS =1 Bar; Abc: Xyz }"))))
        }
    assertThat(authority.allClassNames).hasSize(2)
    assertThat(authority.classDeclaration(cn("IndustrialCenter")).abstract).isFalse()
    assertThat(authority.classDeclaration(cn("Foo")).dependencies).hasSize(1)
  }
}
