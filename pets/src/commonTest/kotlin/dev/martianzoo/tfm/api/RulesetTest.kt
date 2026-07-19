package dev.martianzoo.tfm.api

import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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
                          loadRequirement = "HAS PreludeExpansion",
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
  fun coalescedDeclarationRetainsAllSelectedBundleProvenance() {
    val source =
        TfmRuleset.compose(
            bundle("BundleOne", "1", declaration = "CLASS Shared"),
            bundle("BundleTwo", "2", declaration = "CLASS Shared"),
        )

    source
        .resolve(setOf(cn("BundleOne"), cn("BundleTwo")))
        .classDeclarationBundles
        .getValue(cn("Shared"))
        .shouldContainExactlyInAnyOrder(cn("BundleOne"), cn("BundleTwo"))

    source
        .resolve(setOf(cn("BundleOne")))
        .classDeclarationBundles
        .getValue(cn("Shared"))
        .shouldContainExactly(cn("BundleOne"))
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

  @Test
  fun resolvingCompositionKeepsRequiredAndSelectedBundleContributions() {
    val system =
        bundle(
            "System",
            null,
            alwaysIncluded = true,
            declaration = "CLASS SystemContent : AutoLoad",
        )
    val base = bundle("TerraformingMars", "B", declaration = "CLASS BaseContent : AutoLoad")
    val venus = bundle("VenusNextExpansion", "V", declaration = "CLASS VenusContent : AutoLoad")
    val extension = ruleset(parseOneLinerClass("CLASS ExtensionContent : AutoLoad"))
    val source = TfmRuleset.compose(system, base, venus, extension)

    val resolved = source.resolve(setOf(cn("TerraformingMars")))

    resolved.allClassNames.shouldContainExactlyInAnyOrder(
        cn("SystemContent"),
        cn("BaseContent"),
        cn("ExtensionContent"),
    )
  }

  @Test
  fun resolutionAppliesSameKindReplacementBeforeClassIndexing() {
    val original = CardDefinition(CardData(id = "039"), "TerraformingMars")
    val replacement = CardDefinition(CardData(id = "X31", replaces = "039"), "PromosExpansion")
    val source =
        TfmRuleset.compose(
            cardBundle("TerraformingMars", "B", original),
            cardBundle("PromosExpansion", "X", replacement),
        )

    val resolved = source.resolve(setOf(cn("TerraformingMars"), cn("PromosExpansion")))

    resolved.cardDefinitions.map { it.id }.shouldContainExactly("X31")
    resolved.classDeclaration(cn("DeimosDownPromo")) shouldBe replacement.asClassDeclaration
  }

  @Test
  fun loadRequirementTestsSelectedBundlePresence() {
    val doubleDown =
        CardDefinition(
            CardData(id = "X40", loadRequirement = "HAS PreludeExpansion"),
            "PromosExpansion",
        )
    val source =
        TfmRuleset.compose(
            cardBundle("PromosExpansion", "X", doubleDown),
            cardBundle("PreludeExpansion", "P"),
        )

    source.resolve(setOf(cn("PromosExpansion"))).cardDefinitions.shouldHaveSize(0)
    source
        .resolve(setOf(cn("PromosExpansion"), cn("PreludeExpansion")))
        .cardDefinitions
        .shouldContainExactly(doubleDown)
  }

  @Test
  fun loadRequirementCannotNameDefinitionsOwnBundle() {
    val selfRequiringCard =
        CardDefinition(
            CardData(id = "X40", loadRequirement = "HAS PromosExpansion"),
            "PromosExpansion",
        )
    val source = TfmRuleset.compose(cardBundle("PromosExpansion", "X", selfRequiringCard))

    val exception =
        shouldThrow<IllegalArgumentException> {
          source.resolve(setOf(cn("PromosExpansion"))).cardDefinitions
        }

    exception.message shouldBe
        "CardDefinition X40 has load requirement PromosExpansion naming its own bundle " +
            "PromosExpansion"
  }

  private fun ruleset(vararg declarations: ClassDeclaration): TfmRuleset =
      object : TfmRuleset.Empty() {
        override val explicitClassDeclarations = declarations.toSet()
      }

  private fun bundle(
      name: String,
      code: String?,
      alwaysIncluded: Boolean = false,
      declaration: String,
  ): TfmRuleset.Bundle =
      object : TfmRuleset.Bundle(cn(name), code, alwaysIncluded) {
        override val explicitClassDeclarations = setOf(parseOneLinerClass(declaration))
      }

  private fun cardBundle(
      name: String,
      code: String,
      vararg cards: CardDefinition,
  ): TfmRuleset.Bundle =
      object : TfmRuleset.Bundle(cn(name), code) {
        override val cardDefinitions = cards.toSet()
      }
}
