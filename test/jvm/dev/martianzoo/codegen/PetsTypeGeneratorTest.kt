package dev.martianzoo.codegen

import com.squareup.kotlinpoet.TypeSpec
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PetsTypeGeneratorTest {
  @Test
  fun generatesEveryResolvedClassWithHierarchyAndCovariantDependencies() {
    val table = Canon.classTable
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

    assertEquals(table.allClasses().size, generatedTypes.size)
    assertContains(source, "Generated at 2026-08-06T12:34:56Z")
    assertFalse(source.contains("PetsClasses"))
    assertContains(source, "public interface Component : HasExpression")
    assertContains(source, "public val _authoredEffects: List<Effect>")
    assertContains(source, "public override fun toString(): String")
    assertContains(source, "public class Class<out C : Component>")
    assertContains(source, ") : HasClassName,")
    assertContains(source, "get() = expression.arguments.single().className")
    assertContains(source, "public override val expression: Expression")
    assertContains(source, "public override fun toString(): String = expression.toString()")
    assertFalse(source.contains("val type: Type"))
    assertContains(source, "generatedPetsExpression(typeOf<NormalCityTile<A0, A1>>())")
    assertContains(source, "public class Plant<")
    assertContains(source, "public companion object")
    assertContains(source, ") : StandardResource")
    assertContains(source, "public interface Owned<out A : Anyone> : Component")
    assertContains(source, "public class OceanTile<out MA : MarsArea>")
    assertContains(source, "Tile<MA>")
    assertContains(source, "public interface Cardbound<")
    assertContains(source, "CardFront<P, Class<CardBack<*, *>>>")
    assertContains(source, "public class Production<")
    assertContains(source, "Class<StandardResource<*>>")
    assertContains(source, "public class Callisto ")
    assertContains(source, "ColonyTileSelection<Class<Callisto>>")
    assertContains(source, "public class AerialMappers<out P : Player>")
    assertContains(source, "ResourceCard<P, Class<ProjectCard<*, *>>, Class<Floater<*, *>>>")
    assertFalse(source.contains("class AerialMappers<out P : Player, out C"))
    assertContains(source, "public val name: ClassName")
    assertContains(source, "public override val className: ClassName")
    assertContains(source, "public interface Root<out C : Component>")
    assertContains(source, "public fun <C : Component> of(root: Root<C>): Class<C>")
    assertContains(source, "A Pets class literal cannot specialize")
    assertContains(source, "public fun fromExpression(expression: Expression): AerialMappers<*>")
    assertContains(
        source,
        "public fun generatedPetsComponent(expression: Expression): HasExpression",
    )
    assertContains(source, "public companion object : Class.Root<AerialMappers<*>>")
    assertContains(source, "public override val name: ClassName = ClassName.cn(\"AerialMappers\")")
    assertFalse(source.contains("public inline operator fun <C : Component> invoke(): Class<C>"))
    assertFalse(source.contains("public val c:"))
    assertContains(source, "public override val _authoredEffects: List<Effect>")
    assertContains(source, "Parsing.parse<Effect>(\"End: VictoryPoint / Animal<This>\")")
    assertContains(
        source,
        "public interface Adjacency<out T0 : Tile<Area>, out T1 : Tile<Area>>",
    )
    assertContains(
        source,
        "public interface DelayedColonyTile<out CT : ColonyTile, out C0 : Class<CT>, " +
            "out C1 : Class<CardResource<*, *>>> : ColonyTileSelection<C0>",
    )
    assertContains(source, "public val cost: Int")
    assertContains(source, "public val requirement: Requirement?")
    assertContains(source, "public override val cost: Int = 10")
    assertContains(source, "public override val requirement: Requirement? = parsedRequirement")
    assertContains(
        source,
        "private val parsedRequirement: Requirement = Parsing.parse<Requirement>(\"13 OxygenStep\")",
    )
  }

  @Test
  fun numericNarrowingOfMetricGeneratesAConstantMetric() {
    val declarations =
        parseClasses(
                """
                ABSTRACT CLASS Card
                ABSTRACT CLASS Area
                ABSTRACT CLASS Milestone
                ABSTRACT CLASS Award
                ABSTRACT CLASS Scored { score = Metric }
                ABSTRACT CLASS FixedScore : Scored { score = Number }
                CLASS EightPoints : FixedScore { score = 8 }
                """
                    .trimIndent()
            )
            .toSetStrict()
    val authority =
        object : TfmCatalog() {
          override val explicitClassDeclarations = declarations
        }
    val table = authority.classTable
    val source =
        PetsTypeGenerator(table, "example.properties", "ExampleProperties")
            .generate()
            .joinToString()

    assertContains(source, "public val score: Metric")
    assertContains(source, "public override val score: Metric = parsedScore")
    assertContains(
        source,
        "private val parsedScore: Metric = Parsing.parse<Metric>(\"8\")",
    )
    assertFalse(source.contains("public val score: Int"))
  }

  @Test
  fun kotlinParametersFollowPetsTypeVariableIdentityRatherThanSpelling() {
    val declarations =
        parseClasses(
                """
                ABSTRACT CLASS Card
                ABSTRACT CLASS Area
                ABSTRACT CLASS Milestone
                ABSTRACT CLASS Award
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
                    .trimIndent()
            )
            .toSetStrict()
    val authority =
        object : TfmCatalog() {
          override val explicitClassDeclarations = declarations
        }
    val source =
        PetsTypeGenerator(authority.classTable, "example.variables", "ExampleVariables")
            .generate()
            .joinToString()

    assertContains(source, "public interface Separate<out P0 : Person, out P1 : Person>")
    assertContains(source, "public interface Shared<out P : Person>")
    assertContains(
        source,
        "public interface Across<out P : Person, out B0 : Box<P>, out B1 : Box<P>>",
    )
    assertContains(source, "AcrossToken::class -> listOf(1, 2)")
    assertContains(
        source,
        "public interface Within<out P0 : Person, out P1 : Pair<P0, P0>>",
    )
    assertContains(source, "WithinToken::class -> listOf(1)")
  }

  @Test
  fun canonicalToolEmitsEveryClassAcrossSemanticFiles() {
    val table = Canon.classTable
    val generatedFiles = generateCanonicalPetsTypes(PetsTypeGenerator.Options())
    val typesByFile = generatedFiles.associate { file ->
      file.name to file.members.filterIsInstance<TypeSpec>().mapNotNullTo(linkedSetOf()) { it.name }
    }

    assertEquals(
        setOf(
            "CanonicalPetsTypes",
            "CanonicalPetsCards",
            "CanonicalPetsGoals",
            "CanonicalPetsMapAreas",
        ),
        typesByFile.keys,
    )
    assertTrue("Component" in typesByFile.getValue("CanonicalPetsTypes"))
    assertTrue("Card" in typesByFile.getValue("CanonicalPetsTypes"))
    assertTrue("Area" in typesByFile.getValue("CanonicalPetsTypes"))
    assertTrue("Milestone" in typesByFile.getValue("CanonicalPetsTypes"))
    assertTrue("AerialMappers" in typesByFile.getValue("CanonicalPetsCards"))
    assertTrue("Terraformer35" in typesByFile.getValue("CanonicalPetsGoals"))
    assertTrue("Tharsis_1_1" in typesByFile.getValue("CanonicalPetsMapAreas"))
    assertFalse("Premise" in typesByFile.values.flatten())
    assertFalse(typesByFile.values.flatten().any { it.matches(Regex("Player[1-5]")) })
    val cards = table.getClass(cn("Card"))
    val areas = table.getClass(cn("Area"))
    val milestone = table.getClass(cn("Milestone"))
    val award = table.getClass(cn("Award"))
    assertEquals(
        table
            .allClasses()
            .filter { !it.abstract && it.isSubtypeOf(cards) }
            .mapTo(linkedSetOf()) {
              it.className.toString()
            },
        typesByFile.getValue("CanonicalPetsCards"),
    )
    assertEquals(
        table
            .allClasses()
            .filter { !it.abstract && it.isSubtypeOf(areas) }
            .mapTo(linkedSetOf()) {
              it.className.toString()
            },
        typesByFile.getValue("CanonicalPetsMapAreas"),
    )
    assertEquals(
        table
            .allClasses()
            .filter { !it.abstract && (it.isSubtypeOf(milestone) || it.isSubtypeOf(award)) }
            .mapTo(linkedSetOf()) { it.className.toString() },
        typesByFile.getValue("CanonicalPetsGoals"),
    )
    assertEquals(
        table
            .allClasses()
            .filterNot { klass ->
              !klass.abstract &&
                  (klass.isSubtypeOf(cards) ||
                      klass.isSubtypeOf(areas) ||
                      klass.isSubtypeOf(milestone) ||
                      klass.isSubtypeOf(award))
            }
            .mapTo(linkedSetOf()) { it.className.toString() },
        typesByFile.getValue("CanonicalPetsTypes"),
    )
    assertEquals(
        table.allClasses().mapTo(linkedSetOf()) { it.className.toString() },
        typesByFile.values.flatten().toSet(),
    )
  }
}
