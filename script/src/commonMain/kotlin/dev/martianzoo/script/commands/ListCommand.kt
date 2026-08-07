package dev.martianzoo.script.commands

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.Metric.Count
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.types.Type
import dev.martianzoo.util.HashMultiset

internal class ListCommand(private val repl: ScriptSession) : ScriptCommand("list") {
  override val usage = "list <Expression>"
  override val help =
      """
        This command is super broken right now.
      """
  override val isReadOnly = true

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.petsWords(PetsCompletionRoot.EXPRESSION)

  override fun noArgs() = withArgs("$COMPONENT")

  override fun withArgs(args: String): List<String> {
    val output = mutableListOf<String>()
    val parentType: Type = repl.gameplay.resolve(args)

    // TODO When applicable include an explicit `<Anyone>` for clarity's sake
    val displayType = repl.game.vocabulary.renderPets(parentType.expression)

    val totalCount = repl.game.reader.count(Count(parentType.expressionFull))
    if (totalCount == 0) return listOf("0 $displayType")

    val directSubclassTypes: List<Type> =
        parentType.rootClass
            .directSubclasses()
            .map { (it.baseType glb parentType)!! }
            .ifEmpty { listOf(parentType) }

    val listing = HashMultiset<Type>()
    directSubclassTypes.forEach {
      listing.add(it, repl.game.reader.count(Count(it.expressionFull)))
    }

    // if (listing.elements.size == 1) {
    //   if (parentType.dependencies.keys.any()) {
    //   }
    // }

    output += buildString {
      append("$totalCount $displayType")
      val overlaps = listing.size - totalCount
      if (overlaps > 0) append(" ($overlaps overlaps)")
      append(":")
    }

    val x = listing.entries.sortedByDescending { (_, ct) -> ct }

    output += x.map { (e, ct) -> "  $ct ${repl.game.vocabulary.renderPets(e.expression)}" }
    return output
  }
}
