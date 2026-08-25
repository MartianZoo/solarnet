package dev.martianzoo.engine

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.types.Type
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TransformersSubstitutionTest {
  private val table =
      testClassTable(
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
