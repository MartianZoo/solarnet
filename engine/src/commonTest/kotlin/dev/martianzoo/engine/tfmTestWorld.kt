package dev.martianzoo.engine

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.TfmWorkflow

internal fun canonicalPremise(
    vararg included: ClassName,
    players: Int = 2,
    catalog: TfmCatalog? = null,
): GamePremise {
  val config =
      GameConfig.create(
          included = included.toList(),
          playerNames = (1..players).map { cn("Player$it") },
      )
  val base = Canon.gamePremise(config)
  if (catalog == null) return base
  val extensionClassNames =
      catalog.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className } -
          Canon.explicitClassDeclarations.mapTo(hashSetOf()) { it.className }
  return base.copy(
      catalog = catalog,
      classSelections = base.classSelections + extensionClassNames.map(::ClassSelection),
  )
}

internal fun setUpGame(premise: GamePremise = canonicalPremise()): World =
    Engine.newGame(premise, inputOnlySynonyms = TEST_CLASS_SYNONYMS).apply {
      TfmWorkflow.Manual(this).setupPhase()
    }

internal val TEST_CLASS_SYNONYMS: List<Pair<String, String>> =
    listOf(
        "M" to "MC",
        "S" to "Steel",
        "T" to "Titanium",
        "P" to "Plant",
        "E" to "Energy",
        "H" to "Heat",
        "TR" to "TerraformRating",
        "VP" to "VictoryPoint",
    )
