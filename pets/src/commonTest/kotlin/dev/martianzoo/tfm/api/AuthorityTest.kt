package dev.martianzoo.tfm.api

import dev.martianzoo.api.Exceptions.PetException
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.BundleContentSelection.Kind.CARDS
import dev.martianzoo.tfm.data.CardDefinition
import dev.martianzoo.tfm.data.CardDefinition.CardData
import dev.martianzoo.types.ClassTable
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AuthorityTest {
  @Test
  fun everyAuthorityIncludesThePetsRuntimeDeclarations() {
    TfmAuthority().classDeclaration(COMPONENT)
  }

  @Test
  fun definitionsAndTheirExtraClassesBecomeDeclarations() {
    val authority =
        object : TfmAuthority() {
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

    authority.classDeclaration(cn("Card123")).abstract shouldBe false
    authority.classDeclaration(cn("Foo")).dependencies.shouldHaveSize(1)
  }

  @Test
  fun compositionCoalescesIdenticalClassDeclarations() {
    val declaration = parseOneLinerClass("CLASS Shared")
    val composed = TfmAuthority.compose(authority(declaration), authority(declaration))

    composed.classDeclaration(cn("Shared")) shouldBe declaration
  }

  @Test
  fun compositionRejectsAmbiguousModuleOwnership() {
    val declarations =
        "ABSTRACT CLASS Module\nCLASS SharedModule : Module"
            .lines()
            .map(::parseOneLinerClass)
            .toSet()
    val first =
        object : Bundle(cn("First")) {
          override val explicitClassDeclarations = declarations
        }
    val second =
        object : Bundle(cn("Second")) {
          override val explicitClassDeclarations = declarations
        }

    shouldThrow<IllegalArgumentException> {
      TfmAuthority.compose(first, second).modules
    }
  }

  @Test
  fun compositionRejectsDifferentDeclarationsWithTheSameName() {
    val concrete = parseOneLinerClass("CLASS Shared")
    val abstract = parseOneLinerClass("ABSTRACT CLASS Shared")

    shouldThrow<PetException> {
      TfmAuthority.compose(authority(concrete), authority(abstract)).allClassDeclarations
    }
  }

  @Test
  fun compositionRejectsConflictingDisplayNames() {
    fun named(displayName: String) =
        object : TfmAuthority() {
          override val displayNamesByLanguage = mapOf("en" to mapOf(cn("Shared") to displayName))
        }

    shouldThrow<IllegalArgumentException> {
      TfmAuthority.compose(named("First"), named("Second")).displayNamesByLanguage
    }
  }

  @Test
  fun moduleCanSelectOneContentKindFromAnotherBundle() {
    val moduleBundle =
        object : Bundle(cn("ModuleProvider")) {
          override val explicitClassDeclarations =
              setOf(
                  parseOneLinerClass("ABSTRACT CLASS Module"),
                  parseOneLinerClass("ABSTRACT CLASS CardFront"),
                  parseOneLinerClass("CLASS ExampleModule : Module"),
              )
          override val moduleContentSelections =
              mapOf(
                  cn("ExampleModule") to
                      setOf(BundleContentSelection(cn("ContentProvider"), setOf(CARDS)))
              )
        }
    val card = CardDefinition(CardData(id = "123"))
    val unrelated = parseOneLinerClass("CLASS Unrelated : AutoLoad")
    val contentBundle =
        object : Bundle(cn("ContentProvider")) {
          override val explicitClassDeclarations = setOf(unrelated)
          override val cardDefinitions = setOf(card)
        }
    val source = TfmAuthority.compose(moduleBundle, contentBundle)

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("ExampleModule")))

    table.isActive(card.className) shouldBe true
    table.isActive(unrelated.className) shouldBe false
  }

  @Test
  fun replacementsRemainKnownWhileTheSelectedModuleActivatesOnlyTheReplacement() {
    val moduleBundle =
        bundle("Base", "ABSTRACT CLASS Module\nABSTRACT CLASS CardFront\nCLASS Base : Module")
    val original = CardDefinition(CardData(id = "039"))
    val replacement = CardDefinition(CardData(id = "X31", replaces = "039"))
    val baseCards = cardBundle("BaseCards", original)
    val replacementCards = cardBundle("ReplacementCards", replacement)
    val configuredModuleBundle =
        object : Bundle(cn("ConfiguredModule")) {
          override val explicitClassDeclarations = moduleBundle.explicitClassDeclarations
          override val moduleContentSelections =
              mapOf(
                  cn("Base") to
                      setOf(
                          BundleContentSelection(cn("BaseCards"), setOf(CARDS)),
                          BundleContentSelection(cn("ReplacementCards"), setOf(CARDS)),
                      )
              )
        }
    val source = TfmAuthority.compose(configuredModuleBundle, baseCards, replacementCards)

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("Base")))

    source.cardDefinitions.map { it.className }.toSet() shouldBe setOf(cn("Card039"), cn("CardX31"))
    table.isActive(cn("Card039")) shouldBe false
    table.isActive(cn("CardX31")) shouldBe true
  }

  @Test
  fun replacementExclusionsFollowTheEntireChain() {
    val moduleBundle =
        object : Bundle(cn("ModuleProvider")) {
          override val explicitClassDeclarations =
              bundle(
                      "Declarations",
                      """
                      ABSTRACT CLASS Module
                      ABSTRACT CLASS CardFront
                      CLASS Base : Module
                      CLASS Latest : Module
                      """
                          .trimIndent(),
                  )
                  .explicitClassDeclarations
          override val moduleContentSelections =
              mapOf(
                  cn("Base") to setOf(BundleContentSelection(cn("OriginalCards"), setOf(CARDS))),
                  cn("Latest") to setOf(BundleContentSelection(cn("LatestCards"), setOf(CARDS))),
              )
        }
    val original = CardDefinition(CardData(id = "001"))
    val intermediate = CardDefinition(CardData(id = "002", replaces = "001"))
    val latest = CardDefinition(CardData(id = "003", replaces = "002"))
    val source =
        TfmAuthority.compose(
            moduleBundle,
            cardBundle("OriginalCards", original),
            cardBundle("IntermediateCards", intermediate),
            cardBundle("LatestCards", latest),
        )

    val table = ClassTable.forPremise(source.gamePremise(GameConfig("Base, Latest")))

    table.isActive(original.className) shouldBe false
    table.isActive(intermediate.className) shouldBe false
    table.isActive(latest.className) shouldBe true
  }

  @Test
  fun multipleSelectedReplacementsForOneDefinitionAreRejected() {
    val moduleBundle =
        object : Bundle(cn("ModuleProvider")) {
          override val explicitClassDeclarations =
              bundle(
                      "Declarations",
                      """
                      ABSTRACT CLASS Module
                      ABSTRACT CLASS CardFront
                      CLASS First : Module
                      CLASS Second : Module
                      """
                          .trimIndent(),
                  )
                  .explicitClassDeclarations
          override val moduleContentSelections =
              mapOf(
                  cn("First") to setOf(BundleContentSelection(cn("FirstCards"), setOf(CARDS))),
                  cn("Second") to setOf(BundleContentSelection(cn("SecondCards"), setOf(CARDS))),
              )
        }
    val original = CardDefinition(CardData(id = "001"))
    val first = CardDefinition(CardData(id = "002", replaces = "001"))
    val second = CardDefinition(CardData(id = "003", replaces = "001"))
    val source =
        TfmAuthority.compose(
            moduleBundle,
            cardBundle("OriginalCards", original),
            cardBundle("FirstCards", first),
            cardBundle("SecondCards", second),
        )

    shouldThrow<IllegalArgumentException> {
      source.gamePremise(GameConfig("First, Second"))
    }
  }

  @Test
  fun individualCardConfigurationIsNotSupported() {
    val source =
        TfmAuthority.compose(
            bundle(
                "Base",
                "ABSTRACT CLASS Module\nABSTRACT CLASS CardFront\nCLASS Base : Module",
            ),
            cardBundle("Cards", CardDefinition(CardData(id = "123"))),
        )

    shouldThrow<IllegalArgumentException> { source.gamePremise(GameConfig("Card123")) }
  }

  private fun authority(vararg declarations: ClassDeclaration): TfmAuthority =
      object : TfmAuthority() {
        override val explicitClassDeclarations = declarations.toSet()
      }

  private fun bundle(name: String, declarations: String): Bundle =
      object : Bundle(cn(name)) {
        override val explicitClassDeclarations =
            declarations.lines().filter(String::isNotBlank).map(::parseOneLinerClass).toSet()
      }

  private fun cardBundle(name: String, vararg cards: CardDefinition): Bundle =
      object : Bundle(cn(name)) {
        override val cardDefinitions = cards.toSet()
      }
}
