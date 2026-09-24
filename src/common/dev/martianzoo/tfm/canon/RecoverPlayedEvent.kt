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

/** Recovers one selected played event as the project back for that same face. */
internal object RecoverPlayedEvent : CustomClass() {
  override fun translate(game: GameReader, event: Type): Instruction {
    val owner = event.typeDependencies.single { it.key == Key(OWNED, 0) }.boundType
    val face = event.typeDependencies.mapNotNull { it.boundType.representedClass }.single()
    return change(
        gaining =
            cn("ProjectCard")
                .of(
                    owner.expression,
                    face.className.classExpression(),
                    cn("Hand").expression,
                ),
        removing = event.expression,
    )
  }
}
