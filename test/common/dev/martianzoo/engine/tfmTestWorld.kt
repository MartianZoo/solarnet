package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.withTestSoloSetup
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
  val withSolo = withTestSoloSetup(selected)
  return if (setupComponents.isEmpty()) withSolo
  else withSolo.withTestSetup(setupComponents.joinToString())
}

internal fun setUpGame(premise: GamePremise = canonicalPremise()): Agents {
  val world = TfmEngine.newGame(premise)
  val agents = Agents(world) { NONE }
  val players = world.actors.filterIsInstance<Player>()
  // Keep the test in setup rather than following FINALLY into the corporation phase.
  agents.runSystem("SetupPhase FROM BootstrapPhase")
  world.tasks
      .extract { it }
      .filter { it.deferred }
      .forEach { world.actorEngine(it.assignee).dropTask(it.id) }
  players.forEach { agents[it].autoExecPolicy = EAGER }
  agents.transact()
  players.forEach { player ->
    val agent = agents[player]
    val offered = agent.count("ProjectCard<Selecting>")
    agent.doTask(if (offered == 0) "Ok" else "-$offered ProjectCard<Selecting>")
  }
  return agents
}
