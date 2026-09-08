package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.ClassDeclaration

internal fun modulePremiseDeclaration(classNames: Set<ClassName>): ClassDeclaration {
  val hasOrderedBootstrap = cn("BaseGameModule") in classNames && cn("ModulesReady") in classNames
  val effects =
      if (hasOrderedBootstrap) {
        """
        This:: EACH Class<BaseGameModule> { BaseGameModule }, EACH Class<Module>(NOT Class<BaseGameModule>) { Module }
        This: ModulesReady
        """
            .trimIndent()
      } else {
        "This:: EACH Class<Module> { Module }"
      }
  return parseClasses(
          """
          "The resolved Module selection that initializes this game"
          CLASS Premise : System {
            HAS =1 This
            $effects
          }
          """
              .trimIndent()
      )
      .single()
}
