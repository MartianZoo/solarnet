package dev.martianzoo.codegen

import com.squareup.kotlinpoet.TypeSpec
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.systemClassDeclarations
import dev.martianzoo.pets.types.ClassLoader
import dev.martianzoo.pets.types.ClassTable
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class PetsTypeGeneratorTest {
  @Test
  fun generatesEveryResolvedClassWithHierarchyAndCovariantDependencies() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Keeper
            CLASS Person : Keeper
            ABSTRACT CLASS HeldBy<K@Keeper>
            ABSTRACT CLASS Widget
            CLASS Token : Widget, HeldBy<Person>
            ABSTRACT CLASS Crate<W@Widget>
            CLASS TokenCrate : Crate<Token>
            """
        )
    val generatedFiles =
        PetsTypeGenerator(
                table,
                "example.types",
                "ExamplePets",
                generatedAt = Instant.parse("2026-08-06T12:34:56Z"),
            )
            .generate()
    val source = generatedFiles.joinToString("\n")
    val generatedTypes = generatedFiles.flatMap { it.members.filterIsInstance<TypeSpec>() }

    assertEquals(listOf("ExamplePetsTypes"), generatedFiles.map { it.name })
    assertEquals(table.allClasses().size, generatedTypes.size)
    assertContains(source, "Generated at 2026-08-06T12:34:56Z")
    assertContains(source, "public interface Component : HasExpression")
    assertContains(source, "public class Class<out C : Component>")
    assertContains(source, "public interface HeldBy<out K : Keeper>")
    assertContains(source, ") : Widget,\n    HeldBy<Person>")
    assertContains(source, "public interface Crate<out W : Widget>")
    assertContains(source, ") : Crate<Token>")
    assertContains(
        source,
        "public fun generatedPetsComponent(expression: Expression): HasExpression",
    )
    assertFalse(source.contains("Card"))
    assertFalse(source.contains("MapArea"))
  }

  @Test
  fun numericNarrowingOfMetricGeneratesAConstantMetric() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Scored { score = Metric }
            ABSTRACT CLASS FixedScore : Scored { score = Number }
            CLASS EightPoints : FixedScore { score = 8 }
            """
        )
    val source =
        PetsTypeGenerator(table, "example.properties", "ExampleProperties")
            .generate()
            .joinToString()

    assertContains(source, "public val score: Metric")
    assertContains(source, "public override val score: Metric = parsedScore")
    assertContains(source, "private val parsedScore: Metric = Parsing.parse<Metric>(\"8\")")
    assertFalse(source.contains("public val score: Int"))
  }

  @Test
  fun kotlinParametersFollowPetsTypeVariableIdentityRatherThanSpelling() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Person
            ABSTRACT CLASS Separate<Person, Person>
            ABSTRACT CLASS Shared<P@Person, P@Person>
            ABSTRACT CLASS Box<Person>
            ABSTRACT CLASS Pair<Person, Person>
            ABSTRACT CLASS Across<Box<P@Person>, Box<P@Person>>
            CLASS AcrossToken : Across
            ABSTRACT CLASS Within<Pair<P@Person, P@Person>>
            CLASS WithinToken : Within
            """
        )
    val source =
        PetsTypeGenerator(table, "example.variables", "ExampleVariables").generate().joinToString()

    assertContains(source, "public interface Separate<out P0 : Person, out P1 : Person>")
    assertContains(source, "public interface Shared<out P : Person>")
    assertContains(
        source,
        "public interface Across<out P : Person, out B0 : Box<P>, out B1 : Box<P>>",
    )
    assertContains(source, "AcrossToken::class -> listOf(1, 2)")
    assertContains(source, "public interface Within<out P0 : Person, out P1 : Pair<P0, P0>>")
    assertContains(source, "WithinToken::class -> listOf(1)")
  }

  private fun loadTypes(source: String): ClassTable =
      ClassLoader(
              ClassDeclaration.indexByName(
                  systemClassDeclarations + parseClasses(source.trimIndent())
              )
          )
          .loadEverything()
}
