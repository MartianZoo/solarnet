package dev.martianzoo.tfm.api

import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

  @Test
  fun compositionCoalescesIdenticalClassDeclarations() {
    val declaration = parseOneLinerClass("CLASS Shared")

    val composed = TfmRuleset.compose(ruleset(declaration), ruleset(declaration))

    composed.explicitClassDeclarations.shouldContainExactly(declaration)
    composed.allClassDeclarations.values.shouldContainExactly(declaration)
  }

  @Test
  fun compositionRejectsDifferentDeclarationsWithTheSameName() {
    val concrete = parseOneLinerClass("CLASS Shared")
    val abstract = parseOneLinerClass("ABSTRACT CLASS Shared")
    val composed = TfmRuleset.compose(ruleset(concrete), ruleset(abstract))

    shouldThrow<PetException> { composed.allClassDeclarations }
  }

  @Test
  fun compositionIncludesExplicitlySupportedBundles() {
    val source =
        object : TfmRuleset.Empty() {
          override val allBundles = setOf("BundleWithoutDefinitions")
        }

    TfmRuleset.compose(source).allBundles.shouldContainExactly("BundleWithoutDefinitions")
  }

  private fun ruleset(vararg declarations: ClassDeclaration): TfmRuleset =
      object : TfmRuleset.Empty() {
        override val explicitClassDeclarations = declarations.toSet()
      }
}
