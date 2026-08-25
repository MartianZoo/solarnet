package dev.martianzoo.types

import dev.martianzoo.api.CustomClass
import dev.martianzoo.api.Exceptions
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.systemClassDeclarations
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

internal fun te(s: String): Expression = parse(s)

internal inline fun <reified T : PetNode> testRoundTrip(start: String, end: String = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: String, end: String = start) =
    parse(type, start).toString() shouldBe end

internal inline fun <reified T : PetNode> testRoundTrip(start: T, end: T = start) =
    testRoundTrip(T::class, start, end)

internal fun <T : PetNode> testRoundTrip(type: KClass<T>, start: T, end: T = start) =
    parse(type, start.toString()) shouldBe end

internal fun assertFails(message: String = "(no message)", shouldFail: () -> Unit) =
    withClue(message) { shouldThrow<Exceptions.ExpressionException>(shouldFail) }

internal fun loadTypes(vararg declarations: String): ClassTable =
    loader(declarations.joinToString("\n"))

internal fun loader(petsText: String): ClassTable {
  val explicitDeclarations = parseClasses(petsText).toSet()
  val declarations = systemClassDeclarations + explicitDeclarations
  val authority =
      object : Authority {
        override val explicitClassDeclarations: Set<ClassDeclaration> = explicitDeclarations
        override val allClassDeclarations: Map<ClassName, ClassDeclaration> =
            declarations.associateBy(ClassDeclaration::className).also {
              require(it.size == declarations.size) { "duplicate test Class declaration" }
            }
        override val allDefinitions: Set<Definition> = emptySet()
        override val customClasses: Set<CustomClass> = emptySet()
        override val classTable: ClassTable by lazy { ClassLoader(this).loadEverything() }
      }
  return authority.classTable
}
