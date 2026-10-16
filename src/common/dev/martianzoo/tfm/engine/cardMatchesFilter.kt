package dev.martianzoo.tfm.engine

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.state.GameReader
import dev.martianzoo.tfm.canon.cardRequirement
import dev.martianzoo.tfm.canon.cardTags
import dev.martianzoo.tfm.state.tfmCatalog

/**
 * Whether the printed card [cardName] satisfies [filter], a `TagFilter`, `NoTagsFilter`,
 * `RequirementFilter`, or `ReferenceFilter` expression such as the criterion recorded by
 * `SearchForCard`.
 */
public fun cardMatchesFilter(reader: GameReader, cardName: ClassName, filter: Expression): Boolean {
  val catalog = reader.tfmCatalog
  val card = catalog.card(cardName)
  return when (filter.className) {
    cn("TagFilter") -> {
      val tag = catalog.classTable.getClass(filter.arguments.single().arguments.single().className)
      cardTags(card).elements.any { catalog.classTable.getClass(it).isSubtypeOf(tag) }
    }
    cn("NoTagsFilter") ->
        cardTags(card).elements.none {
          catalog.classTable.getClass(it).isSubtypeOf(catalog.classTable.getClass(cn("Tag")))
        }
    cn("RequirementFilter") -> cardRequirement(card) != null
    cn("ReferenceFilter") -> {
      val reference =
          catalog.classTable.getClass(filter.arguments.single().arguments.single().className)
      card.declaration.allNodes.any { node ->
        node.descendantsOfType<ClassName>().any {
          catalog.classTable.findClass(it)?.isSubtypeOf(reference) == true
        }
      }
    }
    else -> error("unrecognized card filter: $filter")
  }
}
