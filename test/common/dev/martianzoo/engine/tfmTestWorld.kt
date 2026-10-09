package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow

internal fun canonicalPremise(
    vararg included: ClassName,
    players: Int = 2,
    catalog: TfmCatalog? = null,
    setupComponents: Set<Expression> = emptySet(),
): GamePremise {
  val config =
      GameConfig.create(
          included = included.toList(),
          playerNames = (1..players).map { cn("Player$it") },
      )
  val resolvedCatalog = (catalog ?: Canon).withPlayers(players)
  val base = resolvedCatalog.gamePremise(config)
  val selected =
      if (catalog == null) base
      else {
        val extensionClassNames =
            catalog.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className } -
                Canon.explicitClassDeclarations.mapTo(hashSetOf()) { it.className }
        base.copy(
            classSelections = base.classSelections + extensionClassNames.map(::ClassSelection)
        )
      }
  return if (setupComponents.isEmpty()) selected
  else selected.withTestSetup(setupComponents.joinToString())
}

internal fun setUpGame(premise: GamePremise = canonicalPremise()): World =
    TfmEngine.newGame(premise).apply {
      val agents = testAgents()
      TfmWorkflow.Stepwise(agents).setupPhase()
      actors.filterIsInstance<Player>().forEach { player ->
        agents.tfm(player).keepStartingProjects(0)
      }
    }
