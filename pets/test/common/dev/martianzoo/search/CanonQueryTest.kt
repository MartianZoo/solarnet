package dev.martianzoo.search

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class CanonQueryTest {
  private companion object {
    val authored by lazy { CanonQuery(CanonQuery.Form.AUTHORED) }
    val elaborated by lazy { CanonQuery(CanonQuery.Form.ELABORATED) }
  }

  @Test
  fun findsProductionInsideAnAlternativeAndAGate() {
    val matches =
        elaborated
            .evaluate(
                """
                /Canon/Card[effects/Effect[trigger/WhenGain]/instruction//Gain[
                  @count >= 2 and @gainingClass = 'Production' and .//Expression[@className = 'Plant']
                ]]/@name
                """
                    .trimIndent()
            )
            .map { it.value }
    assertTrue("NitrogenRichAsteroid" in matches)
    assertTrue("NitrophilicMoss" in matches)
    assertTrue("AdaptedLichen" !in matches)
    assertEquals(matches.distinct(), matches)
    assertEquals(0, elaborated.evaluate("//Transform[@transformKind = 'PROD']").size)
    assertTrue(authored.evaluate("//Transform[@transformKind = 'PROD']").size > 0)
  }

  @Test
  fun actionsRemainAuthoredWhileElaborationUsesTheExistingEffectCompiler() {
    assertEquals(1, authored.evaluate("/Canon/Card[@name = 'Ants']/actions/Action").size)
    assertEquals(0, elaborated.evaluate("/Canon/Card[@name = 'Ants']/actions/Action").size)
    assertTrue(
        elaborated
            .evaluate("/Canon/Card[@name = 'Ants']/effects//Gain[@gainingClass = 'Microbe']")
            .size > 0
    )
    assertTrue(
        elaborated
            .evaluate("/Canon/Card[@name = 'Ants']/effects//Remove[@removingClass = 'Microbe']")
            .size > 0
    )
  }

  @Test
  fun returnsCardsEvidenceAndScalarResults() {
    assertEquals(
        "NitrogenRichAsteroid",
        authored.evaluate("/Canon/Card[@name = 'NitrogenRichAsteroid']").first().describe(),
    )
    val gain =
        authored
            .evaluate("/Canon/Card[@name = 'NitrogenRichAsteroid']/effects//Gain[@count = 4]")
            .first()
    assertTrue(gain.describe().startsWith("NitrogenRichAsteroid\t4 Plant"))
    assertNull(gain.xml)
    val withXml =
        authored
            .evaluate(
                "/Canon/Card[@name = 'NitrogenRichAsteroid']/effects//Gain[@count = 4]",
                includeXml = true,
            )
            .first()
    assertTrue(withXml.xml!!.startsWith("<Gain"))
    assertEquals(gain, withXml.copy(xml = null))
    assertNull(authored.evaluate("count(/Canon/Card)", includeXml = true).single().xml)
    assertEquals(
        "31",
        authored.evaluate("/Canon/Card[@name = 'NitrogenRichAsteroid']/@cost").first().value,
    )
    assertTrue(authored.evaluate("count(/Canon/Card)").first().value.toInt() > 0)
    assertEquals(0, authored.evaluate("/Canon/Card[@name = 'NoSuchCard']").size)
    assertFails { authored.evaluate("//Card[") }
  }

  @Test
  fun actionCardRoleMatchesAuthoredActions() {
    // An ActionCard with an immediate benefit but no action.
    assertInvariant(
        "authored",
        "actionCardRoleMatchesAuthoredActions",
        """
        CLASS Decoy : ActionCard<Class<CorporationCard>> {
          cost = 0
          HAS MAX 1 Decoy

          This: MC
        }
        """,
    )
  }

  @Test
  fun adjustableParameterRequirementsUseASingleCount() {
    // A single threshold, but its count combines two adjustable parameters.
    assertInvariant(
        "authored",
        "adjustableParameterRequirementsUseASingleCount",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          requirement = HAS "1 (OxygenStep OR TemperatureStep)"
          HAS MAX 1 Decoy

          This: Plant
        }
        """,
    )
  }

  @Test
  fun animalHoldersHaveAnAnimalTag() {
    // Animal storage and scoring, missing only the animal tag.
    assertInvariant(
        "authored",
        "animalHoldersHaveAnAnimalTag",
        """
        CLASS Decoy : ActiveCard, ResourceCard<Class<Animal>> {
          cost = 0
          HAS MAX 1 Decoy

          End: VictoryPoint / Animal<This>
        }
        """,
    )
  }

  @Test
  fun authoredChangesDoNotTransferExistingTilesOrColonies() {
    // A colony transfer instead of a fresh colony gain.
    assertInvariant(
        "authored",
        "authoredChangesDoNotTransferExistingTilesOrColonies",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy

          This: Colony<ColonyTile> FROM Colony<ColonyTile, Anyone>
        }
        """,
    )
  }

  @Test
  fun buildingCardsHaveAtMostASingleDirectEffectProductionBox() {
    // Splitting PROD[Plant, Energy] creates a second production box.
    assertInvariant(
        "authored",
        "buildingCardsHaveAtMostASingleDirectEffectProductionBox",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy, =1 BuildingTag<This>

          This: PROD[Plant], PROD[Energy]
        }
        """,
    )
  }

  @Test
  fun cardCategoriesMatchTheirPersistentBehavior() {
    // An otherwise green card with a persistent subscription.
    assertInvariant(
        "authored",
        "cardCategoriesMatchTheirPersistentBehavior",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy

          Plant: MC
        }
        """,
    )
  }

  @Test
  fun eventTagsBelongToEventCards() {
    // A green card with an extra event tag.
    assertInvariant(
        "authored",
        "eventTagsBelongToEventCards",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy, =1 EventTag<This>

          This: Plant
        }
        """,
    )
  }

  @Test
  fun cardSubscriptionsDoNotObserveHeatGains() {
    // A normal resource subscription, except that the resource is heat.
    assertInvariant(
        "authored",
        "cardSubscriptionsDoNotObserveHeatGains",
        """
        CLASS Decoy : ActiveCard {
          cost = 0
          HAS MAX 1 Decoy

          Heat: MC
        }
        """,
    )
  }

  @Test
  fun copiedProductionBoxesDoNotLoseConditionsChoicesOrBindings() {
    // The gate sits just outside PROD instead of inside it.
    assertInvariant(
        "authored",
        "copiedProductionBoxesDoNotLoseConditionsChoicesOrBindings",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy, =1 BuildingTag<This>

          This: Plant: PROD[Energy]
        }
        """,
    )
  }

  @Test
  fun copiedProductionBoxesDoNotReferToTheOriginalCardAsThis() {
    // The production amount refers to this card instead of all building tags.
    assertInvariant(
        "authored",
        "copiedProductionBoxesDoNotReferToTheOriginalCardAsThis",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy, =1 BuildingTag<This>

          This: PROD[Plant / BuildingTag<This>]
        }
        """,
    )
  }

  @Test
  fun delegateChangesSpecifyTheirState() {
    // Removing a delegate omits its specific state, such as PartyFollower.
    assertInvariant(
        "authored",
        "delegateChangesSpecifyTheirState",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy

          This: -Delegate
        }
        """,
    )
  }

  @Test
  fun delegateGainsAreAuthoredAsIndividualPlacements() {
    // Two delegates in a single gain instead of successive placements.
    assertInvariant(
        "authored",
        "delegateGainsAreAuthoredAsIndividualPlacements",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy

          This: 2 PartyDelegate
        }
        """,
    )
  }

  @Test
  fun eventCardsDoNotNeedSubscriptionsAfterLeavingPlay() {
    // An event with a subscription other than end-game scoring.
    assertInvariant(
        "authored",
        "eventCardsDoNotNeedSubscriptionsAfterLeavingPlay",
        """
        CLASS Decoy : EventCard {
          cost = 0
          HAS MAX 1 Decoy, =1 EventTag<This>

          Plant: MC
        }
        """,
    )
  }

  @Test
  fun eventCardsHaveNoActionsOrResourceStorage() {
    // An event with otherwise ordinary resource storage and end-game scoring.
    assertInvariant(
        "authored",
        "eventCardsHaveNoActionsOrResourceStorage",
        """
        CLASS Decoy : EventCard, ResourceCard<Class<Science>> {
          cost = 0
          HAS MAX 1 Decoy, =1 EventTag<This>

          End: VictoryPoint / Science<This>
        }
        """,
    )
  }

  @Test
  fun eventScoringUsesBareEndTriggers() {
    // The condition wraps End instead of gating the scoring instruction.
    assertInvariant(
        "authored",
        "eventScoringUsesBareEndTriggers",
        """
        CLASS Decoy : EventCard {
          cost = 0
          HAS MAX 1 Decoy, =1 EventTag<This>

          End IF Plant: VictoryPoint
        }
        """,
    )
  }

  @Test
  fun everyConcreteCardAndMilestoneDeclaresItsClassWideLimit() {
    // The class-wide limit is present but permits two copies.
    assertInvariant(
        "authored",
        "everyConcreteCardAndMilestoneDeclaresItsClassWideLimit",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 2 Decoy

          This: Plant
        }
        """,
    )
  }

  @Test
  fun microbeHoldersHaveAMicrobeTag() {
    // Microbe storage and scoring, missing only the microbe tag.
    assertInvariant(
        "authored",
        "microbeHoldersHaveAMicrobeTag",
        """
        CLASS Decoy : ActiveCard, ResourceCard<Class<Microbe>> {
          cost = 0
          HAS MAX 1 Decoy

          End: VictoryPoint / Microbe<This>
        }
        """,
    )
  }

  @Test
  fun preludeSelfGainBenefitsAreVisibleToCopyPrelude() {
    // The condition wraps the self-gain trigger instead of gating its instruction.
    assertInvariant(
        "authored",
        "preludeSelfGainBenefitsAreVisibleToCopyPrelude",
        """
        CLASS Decoy : CardFront<Class<PreludeCard>> {
          cost = 0
          HAS MAX 1 Decoy

          This IF Plant: MC
        }
        """,
    )
  }

  @Test
  fun resourceCardsIdentifyAConcreteCardResource() {
    // The resource selector names the abstract parent instead of a concrete resource.
    assertInvariant(
        "authored",
        "resourceCardsIdentifyAConcreteCardResource",
        """
        CLASS Decoy : ActiveCard, ResourceCard<Class<CardResource>> {
          cost = 0
          HAS MAX 1 Decoy

          End: VictoryPoint / CardResource<This>
        }
        """,
    )
  }

  @Test
  fun resourceHoldersWithAnimalTagsHoldAnimals() {
    // A valid microbe holder with an extra animal tag.
    assertInvariant(
        "authored",
        "resourceHoldersWithAnimalTagsHoldAnimals",
        """
        CLASS Decoy : ActiveCard, ResourceCard<Class<Microbe>> {
          cost = 0
          HAS MAX 1 Decoy, =1 AnimalTag<This>, =1 MicrobeTag<This>

          End: VictoryPoint / Microbe<This>
        }
        """,
    )
  }

  @Test
  fun resourceHoldingProjectsAreActiveCards() {
    // An otherwise ordinary science-resource project marked green instead of blue.
    assertInvariant(
        "authored",
        "resourceHoldingProjectsAreActiveCards",
        """
        CLASS Decoy : AutomatedCard, ResourceCard<Class<Science>> {
          cost = 0
          HAS MAX 1 Decoy

          End: VictoryPoint / Science<This>
        }
        """,
    )
  }

  @Test
  fun signalsAreNotReal() {
    // A signal with the extra Real supertype.
    assertInvariant(
        "authored",
        "signalsAreNotReal",
        """
        CLASS Decoy : Signal, Real {
          HAS MAX 1 Decoy
        }
        """,
    )
  }

  @Test
  fun tileGainsSpecifyArguments() {
    // A city placement missing only the explicit <> argument list.
    assertInvariant(
        "authored",
        "tileGainsSpecifyArguments",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy

          This: CityTile
        }
        """,
    )
  }

  @Test
  fun perMoneyProductionAccountsForItsOffset() {
    // Money production is counted without subtracting ProdOffset<Class<MC>>.
    assertInvariant(
        "elaborated",
        "perMoneyProductionAccountsForItsOffset",
        """
        CLASS Decoy : AutomatedCard {
          cost = 0
          HAS MAX 1 Decoy

          This: Plant / PROD[MC]
        }
        """,
    )
  }

  private fun queryWith(form: CanonQuery.Form, source: String): CanonQuery =
      CanonQuery(
          form,
          object : TfmCatalog(Canon) {
            override val explicitClassDeclarations =
                super.explicitClassDeclarations + parseClasses(source.trimIndent())
          },
      )

  private fun assertInvariant(form: String, name: String, decoy: String) {
    val query =
        queryWith(
            if (form == "authored") CanonQuery.Form.AUTHORED else CanonQuery.Form.ELABORATED,
            decoy,
        )
    val offenders = query.evaluate(invariant(form, name))
    assertEquals(
        listOf("Decoy"),
        offenders.map { it.name ?: it.owner },
        "$name\n" + offenders.joinToString("\n") { it.describe() },
    )
  }

  @Test
  fun moneyProductionArithmetic() {
    listOf(
            "Plant / PROD[MC]" to true,
            "PROD[Plant / MC]" to true,
            "Plant / 2 (PROD[MC] - ProdOffset<Class<MC>>)" to false,
            "Plant / Production<Class<MC>>" to true,
            "Plant / (PROD[MC] - ProdOffset<Class<MC>>)" to false,
            "Plant / PROD[MC - ProdOffset<Class<MC>>]" to false,
            "Plant / (Production<Class<MC>> OR ProdOffset<Class<MC>>)" to true,
            "Plant / (PROD[MC] - ProdOffset<Class<Steel>>)" to true,
            "Plant / (Production<Class<MC>, Player> - ProdOffset<Class<MC>, SoloOpponent>)" to true,
            "Plant / PROD[Energy]" to false,
            "Plant / Production<Class<StandardResource>>" to true,
            "PROD[EACH Class<@StandardResource> { @StandardResource / (Class<@StandardResource> OR QuickStartVariant OR ProdOffset<Class<@StandardResource>>) - Production<Class<@StandardResource>> }]" to
                false,
        )
        .forEach { (source, fails) ->
          val root = XmlElement("Canon")
          val xml = PetsXml()
          for ((name, supers) in
              mapOf(
                  "MC" to listOf("MC", "StandardResource"),
                  "Production" to listOf("Production"),
              )) {
            val entry = xml.element(root, "Class", mapOf("id" to name, "name" to name))
            val isA = xml.element(entry, "isA")
            supers.forEach { xml.element(isA, "Type", mapOf("name" to it)) }
          }
          val example = xml.element(root, "Class", mapOf("name" to "Example"))
          val tree =
              Canon.classTable
                  .transformDispatcher()
                  .transformInstructionTree(parse<InstructionTree>(source))
          xml.node(example, tree)
          val result =
              XPathDocument(root.toString())
                  .evaluate(invariant("elaborated", "perMoneyProductionAccountsForItsOffset"))
          assertEquals(fails, result.isNotEmpty(), source)
        }
  }

  @Test
  fun rejectsXPathBeyondVersion10() {
    assertFails { authored.evaluate("let $" + "x := 1 return $" + "x") }
    assertFails { authored.evaluate("exists(/Canon/Card)") }
  }

  @Test
  fun preservesXmlEscapingAndScalarResults() {
    val xml = XmlElement("Canon")
    val child = XmlElement("Class")
    child.setAttribute("name", "Test")
    child.setAttribute("pets", "A<B> & \"quoted\"\nnext\tcolumn")
    xml.appendChild(child)
    val query = XPathDocument(xml.toString())
    assertEquals(
        "A<B> & \"quoted\"\nnext\tcolumn",
        query.evaluate("/Canon/Class/@pets").single().value,
    )
    assertEquals(
        query.evaluate("string(9223372036854775808)").single().value,
        query.evaluate("9223372036854775808").single().value,
    )
    assertEquals("true", query.evaluate("boolean(/Canon/Class)").single().value)
    assertEquals("0", query.evaluate("count(/Canon/Card)").single().value)
    assertEquals("Test", query.evaluate("string(/Canon/Class/@name)").single().value)
  }

  @Test
  fun adjustableRequirementsDistinguishDirectCountsFromAlternatives() {
    val count =
        "<Count countedClass='Oxygen'><expression><Expression className='Oxygen'/></expression></Count>"
    val minimum = "<Min><countedMetric>$count</countedMetric></Min>"
    val xml =
        """
      <Class id='Oxygen' name='Oxygen'><isA><Type name='GlobalParameter'/></isA></Class>
      <Card id='Direct' name='Direct'><requirement>$minimum</requirement></Card>
      <Card id='Alternative' name='Alternative'><requirement><Or><requirements>$minimum</requirements></Or></requirement></Card>
      <Card id='Multiple' name='Multiple'><requirement><Min><countedMetric><Subtract>
        <minuend>$count</minuend><subtrahend>$count</subtrahend>
      </Subtract></countedMetric></Min></requirement></Card>
    """
    assertOffenders(
        "adjustableParameterRequirementsUseASingleCount",
        xml,
        listOf("Alternative", "Multiple"),
    )
  }

  @Test
  fun classLimitsMustNameTheirOwnClassAndCannotHideInAnAlternative() {
    fun card(name: String, limit: String): String =
        """
      <Card id='$name' name='$name' abstract='false'><isA><Type name='CardFront'/></isA>
        <invariants>$limit</invariants>
      </Card>
    """
    val xml =
        card("Direct", "<Max pets='MAX 1 Direct'/>") +
            card(
                "Conjunction",
                "<And><requirements><Max pets='MAX 1 Conjunction'/></requirements></And>",
            ) +
            card(
                "Alternative",
                "<Or><requirements><Max pets='MAX 1 Alternative'/></requirements></Or>",
            ) +
            card("WrongClass", "<Max pets='MAX 1 Direct'/>")
    assertOffenders(
        "everyConcreteCardAndMilestoneDeclaresItsClassWideLimit",
        xml,
        listOf("Alternative", "WrongClass"),
    )
  }

  @Test
  fun categoryChecksFollowInheritedPersistentBehavior() {
    val source =
        """
        ABSTRACT CLASS SearchRule : Owned {
          Plant: MC
        }
        CLASS InheritedSearchRule : SearchRule
        CLASS AttachedSearchRule<CardFront> : SearchRule
        CLASS DecoyActive : ActiveCard {
          cost = 0
          HAS MAX 1 DecoyActive
          This:: InheritedSearchRule
        }
        CLASS DecoyWrong : AutomatedCard {
          cost = 0
          HAS MAX 1 DecoyWrong
          This:: InheritedSearchRule
        }
        CLASS DecoyAttached : ActiveCard {
          cost = 0
          HAS MAX 1 DecoyAttached, =1 AttachedSearchRule<This>
        }
        CLASS DecoyAction : ActiveCard, ActionCard {
          cost = 0
          HAS MAX 1 DecoyAction
          -> MC
        }
        CLASS DecoyOrdinary : AutomatedCard {
          cost = 0
          HAS MAX 1 DecoyOrdinary
          This:: Plant
        }
        CLASS DecoyEmptyActive : ActiveCard {
          cost = 0
          HAS MAX 1 DecoyEmptyActive
        }
        CLASS DecoyScoring : AutomatedCard {
          cost = 0
          HAS MAX 1 DecoyScoring
          End IF Plant: VictoryPoint
        }
        CLASS DecoySubscription : ActiveCard {
          cost = 0
          HAS MAX 1 DecoySubscription
          Plant OR End: MC
        }
        CLASS DecoyDirect : AutomatedCard {
          cost = 0
          HAS MAX 1 DecoyDirect
          This: Plant
        }
        """
    for (form in CanonQuery.Form.entries) {
      val query = queryWith(form, source)
      assertEquals(
          listOf("DecoyAction", "DecoyActive", "DecoyAttached", "DecoySubscription", "DecoyWrong"),
          query
              .evaluate(
                  "/Canon/Card[starts-with(@name, 'Decoy')][@hasPersistentBehavior = 'true']/@name"
              )
              .map { it.value },
          form.toString(),
      )
      assertEquals(
          listOf("DecoyEmptyActive", "DecoyWrong"),
          query.evaluate(invariant("authored", "cardCategoriesMatchTheirPersistentBehavior")).map {
            it.name
          },
          form.toString(),
      )
    }
  }

  @Test
  fun concreteResourceLookupCannotBorrowAnotherCardsResource() {
    val xml =
        """
      <Class id='Animal' name='Animal' abstract='false'><isA><Type name='CardResource'/></isA></Class>
      <Class id='AbstractResource' name='AbstractResource' abstract='true'><isA><Type name='CardResource'/></isA></Class>
      <Card id='Valid' name='Valid' resource='Animal'><isA><Type name='ResourceCard'/></isA></Card>
      <Card id='Abstract' name='Abstract' resource='AbstractResource'><isA><Type name='ResourceCard'/></isA></Card>
      <Card id='Missing' name='Missing' resource='Unknown'><isA><Type name='ResourceCard'/></isA></Card>
    """
    assertOffenders(
        "resourceCardsIdentifyAConcreteCardResource",
        xml,
        listOf("Abstract", "Missing"),
    )
  }

  private fun assertOffenders(name: String, xml: String, expected: List<String>) {
    val actual = XPathDocument("<Canon>$xml</Canon>").evaluate(invariant("authored", name))
    assertEquals(expected, actual.map { it.name }, name)
  }

  private fun invariant(form: String, name: String): String =
      GeneratedQueryResources.read("canon-invariants/$form/$name.xpath")
}
