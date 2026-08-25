package dev.martianzoo.engine

import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.GamePremise
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmAuthority
import dev.martianzoo.tfm.engine.TfmWorkflow

internal fun canonicalPremise(
    vararg included: ClassName,
    players: Int = 2,
    authority: TfmAuthority? = null,
): GamePremise {
  val config =
      GameConfig.create(
          included = included.toList(),
          playerNames = (1..players).map { cn("Player$it") },
      )
  val base = Canon.gamePremise(config)
  if (authority == null) return base
  val extensionClassNames =
      authority.explicitClassDeclarations.mapTo(linkedSetOf()) { it.className } -
          Canon.explicitClassDeclarations.mapTo(hashSetOf()) { it.className }
  return base.copy(
      authority = authority,
      classSelections = base.classSelections + extensionClassNames.map(::ClassSelection),
  )
}

internal fun setUpGame(premise: GamePremise = canonicalPremise()): World =
    Engine.newGame(premise, inputOnlySynonyms = TEST_CLASS_SYNONYMS).apply {
      TfmWorkflow.Manual(this).setupPhase()
    }

internal val TEST_CLASS_SYNONYMS: List<Pair<String, String>> =
    listOf(
        "M" to "Megacredit",
        "S" to "Steel",
        "T" to "Titanium",
        "P" to "Plant",
        "E" to "Energy",
        "H" to "Heat",
        "TR" to "TerraformRating",
        "VP" to "VictoryPoint",
    )
