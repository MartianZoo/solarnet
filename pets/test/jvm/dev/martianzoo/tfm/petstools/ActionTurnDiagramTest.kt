package dev.martianzoo.tfm.petstools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class ActionTurnDiagramTest {
  @Test
  fun productionExitIsShownOnlyWhenTheSourceContainsThatHandoff() {
    val before = mapOf("phase.pets" to "CLASS ActionPhase { -Baton: ProductionPhase FROM This }")
    val after = mapOf("phase.pets" to "CLASS ActionPhase { -Baton: Baton }")
    assertTrue(ActionTurnDiagram.overview(before).contains("flow_8 -> production;"))
    assertFalse(ActionTurnDiagram.overview(after).contains("production ["))
  }

  @Test
  fun phaseEntryFindsSupertypeTriggersAndInheritedListeners() {
    val sources =
        mapOf(
            "phases.pets" to
                """
                ABSTRACT CLASS Phase
                ABSTRACT CLASS TurnBasedPhase : Phase { This: Baton }
                ABSTRACT CLASS IntermediatePhase : TurnBasedPhase
                CLASS ActionPhase : IntermediatePhase
                CLASS StartToken { TurnBasedPhase: Baton }
                CLASS OtherPhase : Phase { This: WrongOffer }
                """
                    .trimIndent()
        )
    val graph = ActionTurnDiagram.render(sources, 0)
    assertTrue(graph.contains("StartToken&#160;reacts"))
    assertTrue(graph.contains("TurnBasedPhase&#160;reacts"))
    assertTrue(graph.contains("+ActionPhase&#160;matches&#160;this&#160;supertype&#160;trigger"))
    assertTrue(graph.contains("HREF=\"#panel-2\""))
    assertFalse(graph.contains("WrongOffer"))
  }

  @Test
  fun movingAListenerToANewHelperKeepsItInTheDiagram() {
    val graph =
        ActionTurnDiagram.render(
            mapOf("new.pets" to "CLASS NewTurnHelper { -ActionScope: -Baton }")
        )
    assertTrue(graph.contains("NewTurnHelper&#160;reacts"))
    assertTrue(graph.contains("HREF=\"#panel-9\""))
  }

  @Test
  fun inactivePhaseListenersAreExcludedEvenThroughInheritance() {
    val graph =
        ActionTurnDiagram.render(
            mapOf(
                "phases.pets" to
                    """
                    ABSTRACT CLASS Phase
                    ABSTRACT CLASS OtherPhase : Phase { Baton: WrongOffer }
                    CLASS CorporationPhase : OtherPhase { Baton: AlsoWrong }
                    CLASS ActionPhase : Phase { This: Baton }
                    CLASS InRound { Baton: OfferAction }
                    """
                        .trimIndent()
            )
        )
    assertFalse(graph.contains("WrongOffer"))
    assertFalse(graph.contains("AlsoWrong"))
    assertTrue(graph.contains("OfferAction"))
  }

  @Test
  fun missingPanelEventsRequireReviewRatherThanProducingAnEmptyDiagram() {
    assertFailsWith<IllegalStateException> {
      ActionTurnDiagram.render(mapOf("probe.pets" to "CLASS InRound { Baton: OfferAction }"), 6)
    }
  }

  @Test
  fun aChangedRuleChangesItsGuardAndDestinationWithoutEditingTheGenerator() {
    val original = "CLASS InRound { -ActionScope IF Baton: FINALLY { -Baton } }"
    val changed = "CLASS InRound { -ActionScope IF SecondAction: FINALLY { OfferAction } }"
    val before = ActionTurnDiagram.render(mapOf("probe.pets" to original))
    val after = ActionTurnDiagram.render(mapOf("probe.pets" to changed))
    assertTrue(before.contains("IF&#160;Baton"))
    assertTrue(before.contains("HREF=\"#panel-9\""))
    assertTrue(after.contains("IF&#160;SecondAction"))
    assertTrue(after.contains("HREF=\"#panel-3\""))
    assertFalse(after.contains("HREF=\"#panel-9\""))
    assertNotEquals(before, after)
    val originalOverview = ActionTurnDiagram.overview(mapOf("probe.pets" to original))
    val changedOverview = ActionTurnDiagram.overview(mapOf("probe.pets" to changed))
    assertTrue(originalOverview.contains("flow_5 -> flow_8;"))
    assertFalse(changedOverview.contains("flow_5 -> flow_8;"))
    assertTrue(changedOverview.contains("flow_5 -> flow_2;"))
  }

  @Test
  fun sourceMapInsertionOrderDoesNotChangeAnyOutput() {
    val a = "CLASS InRound { Baton: OfferAction }"
    val b = "CLASS Pass { Baton: -Baton }"
    val forward = linkedMapOf("a.pets" to a, "b.pets" to b)
    val reverse = linkedMapOf("b.pets" to b, "a.pets" to a)
    assertEquals(ActionTurnDiagram.render(forward), ActionTurnDiagram.render(reverse))
    assertEquals(ActionTurnDiagram.evidence(forward), ActionTurnDiagram.evidence(reverse))
    assertEquals(ActionTurnDiagram.overview(forward), ActionTurnDiagram.overview(reverse))
  }

  @Test
  fun choicesSequenceAndDeferralRemainVisibleWithSeparateGainAndRemovalDestinations() {
    val source =
        """
        CLASS InRound {
          -ActionScope: FINALLY { -SecondAction THEN (StandardAction OR -Baton) }
        }
        """
            .trimIndent()
    val graph = ActionTurnDiagram.render(mapOf("probe.pets" to source))
    assertTrue(graph.contains("FINALLY"))
    assertTrue(graph.contains("IN&#160;ORDER&#160;/&#160;THEN"))
    assertTrue(graph.contains("CHOOSE&#160;/&#160;OR"))
    assertTrue(graph.contains("HREF=\"#panel-4\""))
    assertTrue(graph.contains("HREF=\"#panel-9\""))
    assertTrue(ActionTurnDiagram.evidence(mapOf("probe.pets" to source)).contains("probe.pets:2"))
  }

  @Test
  fun selfRemovalPointsToTheOwnersRemovalPanel() {
    val graph =
        ActionTurnDiagram.render(
            mapOf("probe.pets" to "CLASS ActionScope { This: FINALLY { -This } }")
        )
    assertTrue(graph.contains("HREF=\"#panel-6\""))
    assertFalse(graph.contains("+This happens"))
  }

  @Test
  fun unsupportedTriggerFormsFailInsteadOfSilentlyOmittingARule() {
    assertFailsWith<IllegalStateException> {
      ActionTurnDiagram.render(
          mapOf("probe.pets" to "CLASS InRound { Baton BY Admin: OfferAction }")
      )
    }
  }
}
