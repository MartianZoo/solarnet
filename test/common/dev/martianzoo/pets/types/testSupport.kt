package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.TransformHandler
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.systemClassDeclarations
import io.kotest.assertions.throwables.shouldThrow

/** Support shared by the type-system tests. */

/** Parses one type expression. */
internal fun te(s: String): Expression = parse(s)

/** Compiles Pets declarations directly, without game assembly or runtime implementations. */
internal fun loadTypes(
    vararg declarations: String,
    transformHandlerFactories: Map<String, (ClassTable) -> TransformHandler> = emptyMap(),
): ClassTable =
    ClassLoader(
            ClassDeclaration.indexByName(
                systemClassDeclarations + parseClasses(declarations.joinToString("\n"))
            ),
            transformHandlerFactories,
        )
        .loadEverything()

/** Constructs a selected Pets universe without game configuration policy. */
internal fun gameView(
    master: ClassTable,
    vararg selectedClassNames: String,
): ClassTable =
    ClassLoader.forPremise(
        PremiseClassTable(master, emptySet()),
        selectedClassNames.map(::cn),
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
  override val classTable: ClassTable
    get() = error("unused by resolved type judgments")

  override fun isAbstract(e: Expression): Boolean = error("unused by the type system")

  override fun ensureNarrows(wide: Expression, narrow: Expression): Unit =
      error("unused by the type system")

  override fun ensureSelectionNarrows(wide: Expression, narrow: Expression): Unit = error("unused")

  override fun has(requirement: Requirement): Boolean = answer(requirement)
}
