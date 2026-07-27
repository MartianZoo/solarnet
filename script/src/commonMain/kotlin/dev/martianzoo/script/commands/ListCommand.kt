package dev.martianzoo.script.commands

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.TypeInfo.StubTypeInfo
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.types.Type
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

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
    val displayType = parentType.expression

    val allComponents: Multiset<Type> = repl.game.reader.getComponents(parentType)
    if (allComponents.none()) return listOf("0 $displayType")

    val directSubclassTypes: List<Type> =
        parentType.rootClass
            .directSubclasses()
            .map { (it.baseType glb parentType)!! }
            .ifEmpty { listOf(parentType) }

    val listing = HashMultiset<Type>()
    directSubclassTypes.forEach { listing.add(it, repl.game.components.count(it, StubTypeInfo)) }

    // if (listing.elements.size == 1) {
    //   if (parentType.dependencies.keys.any()) {
    //   }
    // }

    output += buildString {
      append("${allComponents.size} $displayType")
      val overlaps = listing.size - allComponents.size
      if (overlaps > 0) append(" ($overlaps overlaps)")
      append(":")
    }

    val x = listing.entries.sortedByDescending { (_, ct) -> ct }

    output += x.map { (e, ct) -> "  $ct $e" }
    return output
  }
}
