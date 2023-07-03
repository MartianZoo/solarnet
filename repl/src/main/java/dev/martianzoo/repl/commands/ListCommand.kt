package dev.martianzoo.repl.commands

import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.api.Type
import dev.martianzoo.api.TypeInfo.StubTypeInfo
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.types.MType
import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset

internal class ListCommand(val repl: ReplSession) : ReplCommand("list") {
  override val usage = "list <Expression>"
  override val help = """
        This command is super broken right now.
      """
  override val isReadOnly = true
  override fun noArgs() = withArgs("$COMPONENT")

  override fun withArgs(args: String): List<String> {
    val output = mutableListOf<String>()

    // have to use tfm to personalize it to the current player TODO
    val parentType: MType = repl.tfm.resolve(args) as MType

    // When applicable we want to include an explicit `<Anyone>` for clarity's sake
//    val displayType = try {
//      val expr: Expression = parentType.expression
//      val newExpr = expr.replaceArguments(listOf(ANYONE.of()) + expr.arguments)
//      repl.game.reader.resolve(newExpr)
//      newExpr
//    } catch (ignore: Exception) {
//      parentType.expression
//    }
    val displayType = parentType.expression

    val allComponents: Multiset<out Type> = repl.tfm.reader.getComponents(parentType)
    if (allComponents.none()) return listOf("0 $displayType")

    val directSubclassTypes: List<MType> = parentType.root.directSubclasses().map {
      (it.baseType glb parentType)!!
    }.ifEmpty { listOf(parentType) }

    var listing = HashMultiset<MType>()
    directSubclassTypes.forEach {
      listing.add(it, repl.game.components.count(it, StubTypeInfo))
    }

//    if (listing.elements.size == 1) {
//      if (parentType.dependencies.keys.any()) {
//      }
//    }

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
