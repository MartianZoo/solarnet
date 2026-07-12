package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Doesn't test much, but the class doesn't do that much
internal class AuthorityTest {
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
                          components = setOf("CLASS Foo<Boo> : Loo { HAS =1 Bar; Abc: Xyz }"),
                      )
                  )
              )
        }
    authority.allClassNames.shouldHaveSize(2)
    authority.classDeclaration(cn("IndustrialCenter")).abstract shouldBe false
    authority.classDeclaration(cn("Foo")).dependencies.shouldHaveSize(1)
  }
}
