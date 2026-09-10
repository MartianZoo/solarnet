package dev.martianzoo.engine

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmWorkflow

internal fun canonicalPremise(
    vararg included: ClassName,
    players: Int = 2,
    catalog: TfmCatalog? = null,
    initialComponentTypes: Set<Expression> = emptySet(),
): GamePremise {
  val config =
      GameConfig.create(
          included = included.toList(),
          playerNames = (1..players).map { cn("Player$it") },
      )
  val resolvedCatalog = (catalog ?: Canon).withPlayers(players)
  val base = resolvedCatalog.gamePremise(config, initialComponentTypes)
  if (catalog == null) return base
  val extensionClassNames =
      catalog.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className } -
          Canon.explicitClassDeclarations.mapTo(hashSetOf()) { it.className }
  return base.copy(
      classSelections = base.classSelections + extensionClassNames.map(::ClassSelection),
  )
}

internal fun setUpGame(premise: GamePremise = canonicalPremise()): World =
    Engine.newGame(premise).apply {
      TfmWorkflow.Manual(this).setupPhase()
      actors.filterIsInstance<Player>().forEach { agent(it).doTask("-10 ProjectCard<Hand>") }
    }
