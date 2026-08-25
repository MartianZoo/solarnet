package dev.martianzoo.engine

import dev.martianzoo.api.CustomClass
import dev.martianzoo.data.Authority
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.Definition
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.types.ClassLoader
import dev.martianzoo.types.ClassTable
import dev.martianzoo.types.Type
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TransformersSubstitutionTest {
  private val table =
      testTable(
          """
          CLASS Alice : Owner
          ABSTRACT CLASS Resource
          CLASS Plant : Resource
          ABSTRACT CLASS Production<Class<Resource>> : Owned
          ABSTRACT CLASS Document
          CLASS Report : Document
          ABSTRACT CLASS Play<Class<Document>> : Owned
          """
              .trimIndent()
      )
  private val transformers = Transformers(table)

  @Test
  internal fun `finds owner and resource substitutions`() {
    val type = table.resolve(parse<Expression>("Production<Alice, Class<Plant>>"))

    findSubstitutions(type) shouldBe
        mapOf(
            cn("Resource") to cn("Plant").expression,
            cn("Owner") to cn("Alice").expression,
        )
  }

  @Test
  internal fun `finds owner and represented-class substitutions`() {
    val type = table.resolve(parse<Expression>("Play<Alice, Class<Report>>"))

    findSubstitutions(type) shouldBe
        mapOf(
            cn("Document") to cn("Report").expression,
            cn("Owner") to cn("Alice").expression,
        )
  }

  private fun findSubstitutions(type: Type): Map<ClassName, Expression> =
      transformers.findSubstitutions(type.rootClass.defaultType.dependencies, type.dependencies)
}

private fun testTable(source: String): ClassTable {
  val explicitDeclarations = parseClasses(source).toSet()
  val declarations = systemClassDeclarations + explicitDeclarations
  val authority =
      object : Authority {
        override val explicitClassDeclarations: Set<ClassDeclaration> = explicitDeclarations
        override val allClassDeclarations: Map<ClassName, ClassDeclaration> =
            declarations.associateBy(ClassDeclaration::className)
        override val allDefinitions: Set<Definition> = emptySet()
        override val customClasses: Set<CustomClass> = emptySet()
        override val classTable: ClassTable by lazy { ClassLoader(this).loadEverything() }
      }
  return authority.classTable
}
