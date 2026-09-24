@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.OWNED
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Change.Companion.change
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.Type

/** Moves the exact selected project back from Selecting to Hand. */
internal object MoveSelectedCard : CustomClass() {
  override fun translate(game: GameReader, back: Type): Instruction {
    val face = back.typeDependencies.mapNotNull { it.boundType.representedClass }.single()
    val owner = back.typeDependencies.single { it.key == Key(OWNED, 0) }.boundType
    val projectCard = cn("ProjectCard")
    val classFace = face.className.classExpression()
    return change(
        gaining = projectCard.of(owner.expression, classFace, cn("Hand").expression),
        removing = back.expression,
    )
  }
}
