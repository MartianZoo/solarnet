package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression

/**
 * Whether the printed card [cardName] satisfies [filter], a `TagFilter`, `NoTagsFilter`,
 * `RequirementFilter`, or `ReferenceFilter` expression such as the criterion recorded by
 * `SearchForCard`. Uses only printed definitions from [catalog], independent of live game state.
 */
public fun cardMatchesFilter(
    catalog: TfmCatalog,
    cardName: ClassName,
    filter: Expression,
): Boolean {
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
