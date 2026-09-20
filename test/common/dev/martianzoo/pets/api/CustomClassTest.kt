package dev.martianzoo.pets.api

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.types.ClassTable
import dev.martianzoo.pets.types.Type
import dev.martianzoo.pets.types.loadTypes
import dev.martianzoo.pets.util.Multiset
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test
import kotlin.test.assertEquals

internal class CustomClassTest {
  @Test
  internal fun classNameDefaultsToKotlinSimpleName() {
    assertEquals(cn("AutomaticallyNamed"), AutomaticallyNamed.className)
    shouldThrow<IllegalArgumentException> { object : CustomClass() {} }
  }

  @Test
  internal fun unimplementedTranslationArityFailsExplicitly() {
    val customClass = object : CustomClass("Unimplemented") {}
    val type = loadTypes("CLASS Argument").resolve(parse("Argument"))

    shouldThrow<NotImplementedError> { customClass.translate(UnusedGameReader) }
    shouldThrow<NotImplementedError> { customClass.translate(UnusedGameReader, type) }
    shouldThrow<NotImplementedError> { customClass.translate(UnusedGameReader, type, type) }
    shouldThrow<NotImplementedError> {
      customClass.translate(UnusedGameReader, type, type, type)
    }
    shouldThrow<NotImplementedError> {
      customClass.translate(UnusedGameReader, type, type, type, type)
    }
  }

  private object AutomaticallyNamed : CustomClass()

  private object UnusedGameReader : GameReader {
    override val actors: List<Actor>
      get() = error("unused")

    override val catalog: Catalog
      get() = error("unused")

    override val classTable: ClassTable
      get() = error("unused")

    override fun resolve(expression: Expression): Type = error("unused")

    override fun isAbstract(e: Expression): Boolean = error("unused")

    override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = error("unused")

    override fun has(requirement: Requirement): Boolean = error("unused")

    override fun count(metric: Metric): Int = error("unused")

    override fun count(type: Type): Int = error("unused")

    override fun countComponent(concreteType: Type): Int = error("unused")

    override fun getComponents(type: Type): Multiset<Type> = error("unused")

    override fun getDependents(component: Type): Set<Type> = error("unused")
  }
}
