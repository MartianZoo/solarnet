package dev.martianzoo.data

import dev.martianzoo.api.CustomClass
import dev.martianzoo.pets.ast.ClassName

interface Authority {
  /** Returns every bundle code (e.g. `"B"`) this authority has any information on. */
  val allBundles: Set<String>

  val allClassDeclarations: Map<ClassName, ClassDeclaration>

  /**
   * Every class declaration this authority knows about, including explicit ones and those converted
   * from [Definition]s.
   */
  val allClassNames: Set<ClassName>

  /**
   * All class declarations that were provided "directly" in source form (i.e., `CLASS Foo...`) as
   * opposed to being converted from [Definition] objects.
   */
  val explicitClassDeclarations: Set<ClassDeclaration>

  /** Everything implementing [Definition] this authority knows about. */
  val allDefinitions: Set<Definition>

  /** Every custom instruction this authority knows about. */
  val customClasses: Set<CustomClass>

  /** Returns the class declaration having the full name [name]. */
  fun classDeclaration(name: ClassName): ClassDeclaration

  /** Returns the custom instruction implementation having the name [className]. */
  fun customClass(className: ClassName): CustomClass
}
