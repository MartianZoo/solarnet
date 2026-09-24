package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.api.SystemClasses.CLASS
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Gain
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.tfm.canon.TfmClasses.PRELUDE_CARD
import dev.martianzoo.tfm.canon.TfmClasses.PROJECT_CARD

/** Routes authored generic card gains through the dealer while leaving exact gains untouched. */
internal object RealCardDrawLowerer {
  fun lower(source: ClassDeclaration): ClassDeclaration {
    val effects = source.effects.map(transformer::transformEffect)
    return source.copy(
        executableEffects = effects.takeUnless { it == source.authoredEffectsWithActions },
    )
  }

  private val transformer =
      object : PetTransformer() {
        override fun transformNode(node: PetNode): PetNode {
          if (node !is Gain) return transformChildren(node)
          val card = node.gaining
          if (card.className !in deckBacks || card.refinement != null) return node
          if (card.arguments.any { it.className == CLASS }) return node
          val drawArguments =
              when (card.arguments.size) {
                0 -> listOf(card.className.classExpression(), HAND.expression)
                1 -> listOf(card.className.classExpression(), card.arguments.single())
                2 ->
                    listOf(
                        card.arguments.first(),
                        card.className.classExpression(),
                        card.arguments.last(),
                    )
                else -> return node
              }
          return node.copy(scaledEx = scaledEx(DRAW_CARD.of(drawArguments), node.count))
        }
      }

  private val DRAW_CARD = cn("DrawCard")
  private val HAND = cn("Hand")
  private val deckBacks: Set<ClassName> =
      setOf(
          PROJECT_CARD,
          PRELUDE_CARD,
          cn("BeginnerCorporationCard"),
          cn("StandardCorporationCard"),
      )
}
