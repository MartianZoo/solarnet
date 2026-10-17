package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.ClassTable
import kotlin.test.Test
import kotlin.test.assertTrue

internal class ActionScopeRemovalInvariantTest {
  @Test
  internal fun directRemovalChoiceIsRejected() {
    assertViolation("Watcher", table("CLASS Watcher { -ActionScope: MC OR Steel }"))
  }

  @Test
  internal fun helperGainCannotHideALaterChoice() {
    assertViolation(
        "Watcher",
        table(
            """
            CLASS Watcher { -ActionScope: Reward }
            CLASS Reward : Signal { This: MC OR Steel }
            """
        ),
    )
  }

  @Test
  internal fun consumingAnEntitlementCannotProvokeARewardChoice() {
    assertViolation("Watcher", table("CLASS Watcher { -Baton: MC OR Steel }"))
  }

  @Test
  internal fun anOptionalRemovalDefaultCannotHideACleanupChoice() {
    assertViolation(
        "Player",
        table(
            playerEffects = "-ActionScope<This>: FINALLY { -Baton<This> }",
            batonBody = "DEFAULT -Baton?",
        ),
    )
  }

  @Test
  internal fun aDependentOfADependentCannotAskAChoiceOnExpiry() {
    assertViolation(
        "ScopedReward",
        table("CLASS ScopedReward<WildTagElection> { -This: MC OR Steel }"),
    )
  }

  @Test
  internal fun tagRemovalIncludesTagsAssignedToExpiringElections() {
    assertViolation("Watcher", table("CLASS Watcher { -ScienceTag: MC OR Steel }"))
  }

  @Test
  internal fun inheritedSelfRemovalEffectsAreChecked() {
    assertViolation(
        "RemovalReward",
        table(
            """
            ABSTRACT CLASS RemovalReward { -This: MC OR Steel }
            CLASS ScopedReward<ActionScope> : RemovalReward
            """
        ),
    )
  }

  @Test
  internal fun aBroadRemovalSubscriptionCannotHideAScopeListener() {
    assertViolation(
        "Watcher",
        table("CLASS Watcher { -Scope IF MC: MC OR Steel }"),
    )
  }

  @Test
  internal fun aFlowOwnerCannotAppendARewardChoice() {
    assertViolation(
        "Player",
        table(playerEffects = "-ActionScope<This>: FINALLY { -Baton<This> THEN (MC OR Steel) }"),
    )
  }

  @Test
  internal fun choosingWhetherToConsumeAnEntitlementIsRejected() {
    assertViolation(
        "Player",
        table(playerEffects = "-ActionScope<This>: FINALLY { -1? Baton<This> }"),
    )
  }

  @Test
  internal fun anUnfixedCleanupAmountIsRejected() {
    assertViolation(
        "Player",
        table(playerEffects = "-ActionScope<This>: FINALLY { -X Baton<This> }"),
    )
  }

  @Test
  internal fun choosingWhoseEntitlementToConsumeIsRejected() {
    assertViolation(
        "Player",
        table(playerEffects = "-ActionScope<This>: FINALLY { -Baton<Player> }"),
    )
  }

  @Test
  internal fun choosingTheCleanupActorIsRejected() {
    assertViolation(
        "Player",
        table(playerEffects = "-ActionScope<This>: FINALLY Player { -Baton<This> }"),
    )
  }

  @Test
  internal fun aClassLiteralDependencyDoesNotExpireWithItsRepresentedScope() {
    assertValid(table("CLASS ScopeMetadata<Class<ActionScope>> { -This: MC OR Steel }"))
  }

  @Test
  internal fun choicesTriggeredWhileTheScopeExistsRemainAllowed() {
    assertValid(table("CLASS Watcher { ActionScope: MC OR Steel }"))
  }

  @Test
  internal fun fixedBookkeepingAndOffersForSubsequentPlayRemainAllowed() {
    assertValid(
        table(
            playerEffects =
                """
                -ActionScope<This>: FINALLY { -Baton<This> }
                -Baton<This>: StandardAction<This> OR PassBaton<This>
                -ActionScope<This>: FINALLY { \
                  TASK This { \
                    StandardAction<This> OR DoRequiredActions<This> \
                  } \
                }
                """,
            passEffects = "-ActionScope<@Player>: FINALLY { OfferAction<@Player> }",
            phaseEffects = "-ActionScope: FINALLY { ProductionPhase FROM This }",
        )
    )
  }

  @Test
  internal fun aBatonHandoffCannotHideARewardChoice() {
    assertViolation("Watcher", table("CLASS Watcher { PassBaton: MC OR Steel }"))
  }

  @Test
  internal fun aBatonHandoffMayTransferTheBatonDirectly() {
    assertValid(
        table(
            """
            CLASS NextPlayer<Previous@Player, Next@Player> {
              PassBaton<Previous@>: FINALLY { Baton<Next@ FROM Previous@> }
            }
            """
        )
    )
  }

  @Test
  internal fun aSecondActionDependentCannotAddAnExpiryChoice() {
    assertViolation("Watcher", table("CLASS Watcher { -SecondAction: MC OR Steel }"))
  }

  @Test
  internal fun fixedBookkeepingDoesNotNeedAWhitelistedOwnerName() {
    assertValid(table("CLASS Watcher { -ActionScope<@Player>: -Baton<@Player> }"))
  }

  private fun assertViolation(owner: String, table: ClassTable) {
    val violations = actionScopeRemovalViolations(table)
    assertTrue(violations.any { it.startsWith("$owner:") }, "$owner was not reported: $violations")
  }

  private fun assertValid(table: ClassTable) {
    val violations = actionScopeRemovalViolations(table)
    assertTrue(violations.isEmpty(), violations.joinToString("\n"))
  }

  private fun table(
      additions: String = "",
      playerEffects: String = "",
      passEffects: String = "",
      phaseEffects: String = "",
      batonBody: String = "",
  ): ClassTable {
    val declarations =
        parseClasses(
            """
        ABSTRACT CLASS Player : Owner, Actor { $playerEffects }
        CLASS TestPlayer : Player
        CLASS ActionScope<Player> : Scope
        ABSTRACT CLASS TagHolder<Player>
        CLASS WildTagElection<ActionScope> : TagHolder<Player>
        ABSTRACT CLASS Tag<TagHolder>
        CLASS ScienceTag : Tag
        CLASS Baton<Player> { $batonBody }
        CLASS SecondAction<Baton>
        CLASS OfferAction<Player> : Signal
        CLASS PassBaton<Player> : Signal
        CLASS StandardAction<Player> : Signal
        CLASS DoRequiredActions<Player> : Signal
        CLASS Pass<Player> { $passEffects }
        ABSTRACT CLASS Phase
        CLASS ActionPhase : Phase { $phaseEffects }
        CLASS ProductionPhase : Phase
        CLASS MC
        CLASS Steel
        $additions
        """
        )
    return ClassLoader(ClassDeclaration.indexByName(systemClassDeclarations + declarations))
        .loadEverything()
  }
}
