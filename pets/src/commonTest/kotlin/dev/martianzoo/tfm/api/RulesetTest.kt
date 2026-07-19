package dev.martianzoo.tfm.api

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Doesn't test much, but the class doesn't do that much
internal class RulesetTest {
  @Test
  fun test() {
    val ruleset =
        object : TfmRuleset.Empty() {
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
    ruleset.allClassNames.shouldHaveSize(2)
    ruleset.classDeclaration(cn("IndustrialCenter")).abstract shouldBe false
    ruleset.classDeclaration(cn("Foo")).dependencies.shouldHaveSize(1)
  }
}
