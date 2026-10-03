package dev.martianzoo.pets.api

import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn

/**
 * Shared identity and dependencies of a Kotlin-backed Pets metric or instruction. By default its
 * Pets class name is the implementation's Kotlin simple name.
 */
public abstract class CustomClass(name: String? = null) : HasClassName {
  public constructor(className: ClassName) : this(className.toString())

  final override val className: ClassName = cn(name ?: requireNotNull(this::class.simpleName))

  /**
   * Pets classes this implementation may resolve or produce at runtime. Class loading follows these
   * names when this custom class loads; other references may still load them independently.
   */
  public open val requiredClassNames: Set<ClassName> = emptySet()
}
