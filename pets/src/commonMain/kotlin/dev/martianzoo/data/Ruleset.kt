package dev.martianzoo.data

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.CustomMetric
import dev.martianzoo.pets.ast.ClassName

public interface Ruleset {
  public val allClassDeclarations: Map<ClassName, ClassDeclaration>

  /**
   * Every class declaration recognized by the authority that produced this ruleset, including
   * declarations inactive in this particular game.
   */
  public val knownClassDeclarations: Map<ClassName, ClassDeclaration>
    get() = allClassDeclarations

  /** Full bundle identities contributing each combined class declaration. */
  public val classDeclarationBundles: Map<ClassName, Set<ClassName>>

  /**
   * Every class declaration this ruleset knows about, including explicit ones and those converted
   * from [Definition]s.
   */
  public val allClassNames: Set<ClassName>

  /**
   * All class declarations that were provided "directly" in source form (i.e., `CLASS Foo...`) as
   * opposed to being converted from [Definition] objects.
   */
  public val explicitClassDeclarations: Set<ClassDeclaration>

  /** Everything implementing [Definition] this ruleset knows about. */
  public val allDefinitions: Set<Definition>

  /** Every Kotlin-provided implementation for this ruleset's `Custom` classes. */
  public val customClasses: Set<CustomClass>

  /** Returns the class declaration having the full name [name]. */
  public fun classDeclaration(name: ClassName): ClassDeclaration

  /** Returns the custom implementation having the name [className]. */
  public fun customClass(className: ClassName): CustomClass

  /** Returns the custom metric implementation having the name [className], if any. */
  public fun customMetric(className: ClassName): CustomMetric?
}
