package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.Catalog
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GamePremise
import dev.martianzoo.pets.systemClassDeclarations
import io.kotest.assertions.throwables.shouldThrow

/** Support shared by the type-system tests. */

/** Parses one type expression. */
internal fun te(s: String): Expression = parse(s)

/** Compiles [declarations] into a master class table, alongside the system classes. */
internal fun loadTypes(vararg declarations: String): ClassTable =
    testCatalog(declarations.joinToString("\n")).classTable

/** Builds a catalog from Pets source, plus the system classes. */
internal fun testCatalog(
    petsText: String,
    customImplementations: Set<CustomClass> = emptySet(),
    moduleSelections: Map<ClassName, Set<ClassSelection>> = emptyMap(),
): Catalog {
  val explicitDeclarations = parseClasses(petsText).toSet()
  val declarations = systemClassDeclarations + explicitDeclarations
  return object : Catalog {
    override val explicitClassDeclarations: Set<ClassDeclaration> = explicitDeclarations
    override val allClassDeclarations: Map<ClassName, ClassDeclaration> =
        declarations.associateBy(ClassDeclaration::className).also {
          require(it.size == declarations.size) { "duplicate test Class declaration" }
        }
    override val customClasses: Set<CustomClass> = customImplementations
    override val modules: Map<ClassName, Set<ClassSelection>> = moduleSelections
    override val classTable: ClassTable by lazy { ClassLoader(this).loadEverything() }
  }
}

/** Builds the game view of [catalog] whose premise selects exactly [activeClassNames]. */
internal fun gameView(catalog: Catalog, vararg activeClassNames: String): ClassTable =
    ClassTable.forPremise(
        GamePremise(
            catalog,
            emptySet(),
            activeClassNames.mapTo(linkedSetOf()) { ClassSelection(cn(it)) },
            emptySet(),
        )
    )

/** Asserts that [block] rejects an argument, as cross-universe operations do. */
internal inline fun shouldThrowIae(block: () -> Unit): IllegalArgumentException =
    shouldThrow<IllegalArgumentException>(block)

/** A pretend world in which every requirement holds. */
internal val fullWorld: TypeInfo = FixedWorld { true }

/** A pretend world in which no requirement holds. */
internal val emptyWorld: TypeInfo = FixedWorld { false }

/**
 * A pretend world in which a requirement holds exactly when its rendered text contains one of
 * [truths].
 */
internal fun world(vararg truths: String): TypeInfo = FixedWorld { requirement ->
  truths.any { it in "$requirement" }
}

private class FixedWorld(private val answer: (Requirement) -> Boolean) : TypeInfo {
  override fun isAbstract(e: Expression): Boolean = error("unused by the type system")

  override fun ensureNarrows(wide: Expression, narrow: Expression): Unit =
      error("unused by the type system")

  override fun has(requirement: Requirement): Boolean = answer(requirement)
}

/** A world that records every requirement it is asked about, in order. */
internal class RecordingWorld(private val answer: Boolean = true) : TypeInfo {
  val questions: MutableList<String> = mutableListOf()

  override fun isAbstract(e: Expression): Boolean = error("unused by the type system")

  override fun ensureNarrows(wide: Expression, narrow: Expression): Unit =
      error("unused by the type system")

  override fun has(requirement: Requirement): Boolean {
    questions += "$requirement"
    return answer
  }
}
