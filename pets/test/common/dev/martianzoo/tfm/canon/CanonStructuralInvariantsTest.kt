package dev.martianzoo.tfm.canon

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.testsupport.validatedComponentLimits
import kotlin.test.Test
import kotlin.test.assertTrue

/** Canon checks that require a resolved game or analysis beyond the XPath catalog projection. */
internal class CanonStructuralInvariantsTest {
  @Test
  internal fun dependencyTargetsHaveUniqueComponentLimits() {
    val options =
        "VenusNextExpansion, PreludeExpansion, ColoniesExpansion, TurmoilExpansion, prelude2:cards, promos:cards"
    Canon.bundles
        .filter { it.marsMapDefinitions.isNotEmpty() }
        .forEach { bundle ->
          val premise =
              Canon.gamePremise(
                  GameConfig(
                      "${bundle.primaryModuleName}, $options, ${Canon.colonyTileClassNames.joinToString()}",
                      "Player1",
                      "Player2",
                  )
              )
          validatedComponentLimits(premise.classTable)
        }
  }

  @Test
  internal fun actionScopeExpiryOnlyPerformsReviewedHandoffs() {
    val offenders = actionScopeRemovalViolations(Canon.classTable)
    assertTrue(
        offenders.isEmpty(),
        "ActionScope expiry must not start choices for the completed action; review these removal effects:\n" +
            offenders.joinToString("\n"),
    )
  }

  @Test
  internal fun everyConcreteHasActionsClassHandlesAllActionSlots() {
    val table = Canon.classTable
    val hasActions = table.getClass(cn("HasActions"))
    val slots = (1..3).associateWith { parse<Effect>("UseAction<This, Action$it>: Die").trigger }
    val concrete = table.allClasses().filter { !it.abstract && it.isSubtypeOf(hasActions) }
    assertTrue(concrete.isNotEmpty(), "No concrete HasActions classes examined")
    val missing = concrete.flatMap { klass ->
      val triggers = klass.allSuperclasses().flatMap { it.declaration.effects }.map { it.trigger }
      slots.filterValues { it !in triggers }.keys.map { "${klass.className}: Action$it" }
    }
    assertTrue(missing.isEmpty(), "Missing UseAction reactions:\n${missing.joinToString("\n")}")
  }
}
