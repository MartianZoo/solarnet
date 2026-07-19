package dev.martianzoo.tfm.api

import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.COMPONENT
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
  fun everyRulesetIncludesThePetsRuntimeDeclarations() {
    TfmRuleset.Empty().classDeclaration(COMPONENT)
  }

  @Test
  fun definitionsAndTheirExtraClassesBecomeDeclarations() {
    val ruleset =
        object : TfmRuleset.Empty() {
          override val cardDefinitions =
              setOf(
                  CardDefinition(
                      CardData(
                          id = "123",
                          deck = "PRELUDE",
                          immediate = "Plant",
                          components = setOf("CLASS Foo<Boo> : Loo { HAS =1 Bar; Abc: Xyz }"),
                      )
                  )
              )
        }

    ruleset.classDeclaration(cn("IndustrialCenter")).abstract shouldBe false
    ruleset.classDeclaration(cn("Foo")).dependencies.shouldHaveSize(1)
  }

  @Test
  fun compositionCoalescesIdenticalClassDeclarations() {
    val declaration = parseOneLinerClass("CLASS Shared")

    val composed = TfmRuleset.compose(ruleset(declaration), ruleset(declaration))

    composed.explicitClassDeclarations.shouldContainExactly(declaration)
    composed.classDeclaration(cn("Shared")) shouldBe declaration
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

    shouldThrow<PetException> {
      TfmRuleset.compose(ruleset(concrete), ruleset(abstract)).allClassDeclarations
    }
  }

  @Test
  fun resolvingCompositionKeepsSelectedBundlesAndNonBundleContributions() {
    val base = bundle("TerraformingMars", "B", declaration = "CLASS BaseContent : AutoLoad")
    val venus = bundle("VenusNextExpansion", "V", declaration = "CLASS VenusContent : AutoLoad")
    val extension = ruleset(parseOneLinerClass("CLASS ExtensionContent : AutoLoad"))
    val source = TfmRuleset.compose(base, venus, extension)

    val resolved = source.resolve(setOf(cn("TerraformingMars")))

    resolved.allClassNames.containsAll(setOf(cn("BaseContent"), cn("ExtensionContent"))) shouldBe
        true
    (cn("VenusContent") in resolved.allClassNames) shouldBe false
  }

  @Test
  fun resolutionAppliesSameKindReplacementBeforeClassIndexing() {
    val original = CardDefinition(CardData(id = "039"))
    val replacement = CardDefinition(CardData(id = "X31", replaces = "039"))
    val source =
        TfmRuleset.compose(
            cardBundle("TerraformingMars", "B", original),
            cardBundle("PromosExpansion", "X", replacement),
        )

    val resolved = source.resolve(setOf(cn("TerraformingMars"), cn("PromosExpansion")))

    resolved.cardDefinitions.map { it.id }.shouldContainExactly("X31")
    resolved.classDeclaration(cn("DeimosDownPromo")) shouldBe replacement.asClassDeclaration
    resolved.classDeclarationBundles.keys shouldBe resolved.allClassNames
    (cn("DeimosDown") in resolved.classDeclarationBundles) shouldBe false
  }

  @Test
  fun everyRequiredBundleMustBeSelected() {
    val card =
        CardDefinition(
            CardData(id = "X40", requiredBundles = "PreludeExpansion, VenusNextExpansion")
        )
    val source =
        TfmRuleset.compose(
            cardBundle("PromosExpansion", "X", card),
            bundle("PreludeExpansion", "P", "CLASS PreludeExpansion"),
            bundle("VenusNextExpansion", "V", "CLASS VenusNextExpansion"),
        )

    val withoutVenus = source.resolve(setOf(cn("PromosExpansion"), cn("PreludeExpansion")))
    withoutVenus.cardDefinitions.shouldHaveSize(0)
    withoutVenus.classDeclarationBundles.keys shouldBe withoutVenus.allClassNames
    (card.className in withoutVenus.classDeclarationBundles) shouldBe false

    val withVenus =
        source.resolve(
            setOf(cn("PromosExpansion"), cn("PreludeExpansion"), cn("VenusNextExpansion"))
        )
    withVenus.cardDefinitions.shouldContainExactly(card)
    withVenus.classDeclarationBundles
        .getValue(card.className)
        .shouldContainExactly(cn("PromosExpansion"))
  }

  private fun ruleset(vararg declarations: ClassDeclaration): TfmRuleset =
      object : TfmRuleset.Empty() {
        override val explicitClassDeclarations = declarations.toSet()
      }

  private fun bundle(
      name: String,
      code: String?,
      declaration: String,
  ): TfmRuleset.Bundle =
      object : TfmRuleset.Bundle(cn(name), code) {
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
