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
    assertContains(source, "public sealed interface Component : HasExpression")
    assertContains(source, "public val _authoredEffects: List<Effect>")
    assertContains(source, "public override fun toString(): String")
    assertContains(source, "public class Class<out C : Component>")
    assertContains(source, ") : HasClassName,")
    assertContains(source, "get() = expression.arguments.single().className")
    assertContains(source, "public override val expression: Expression")
    assertContains(source, "public override fun toString(): String = expression.toString()")
    assertFalse(source.contains("val type: Type"))
    assertContains(source, "generatedPetsExpression(typeOf<CityTile<A0, A1>>())")
    assertContains(source, "public class Plant<")
    assertContains(source, "public companion object")
    assertContains(source, ") : StandardResource")
    assertContains(source, "public sealed interface Owned<out A : Anyone> : Component")
    assertContains(source, "public class OceanTile<out MA : MarsArea>")
    assertContains(source, "Tile<MA>")
    assertContains(source, "public sealed interface Cardbound<")
    assertContains(source, "CardFront<P, Class<CardBack<*, *>>>")
    assertContains(source, "public class Production<")
    assertContains(source, "Class<StandardResource<*>>")
    assertContains(source, "public class Callisto ")
    assertContains(source, "ColonyTileSelection<Class<Callisto>>")
    assertContains(source, "public class AerialMappers<out P : Player>")
    assertContains(source, "ResourceCard<P, Class<ProjectCard<*, *>>, Class<Floater<*, *>>>")
    assertFalse(source.contains("class AerialMappers<out P : Player, out C"))
    assertContains(source, "public val className: ClassName = ClassName.cn(\"AerialMappers\")")
    assertContains(source, "public fun fromExpression(expression: Expression): AerialMappers<*>")
    assertContains(
        source,
        "public fun generatedPetsComponent(expression: Expression): HasExpression",
    )
    assertContains(source, "public val c: Class<AerialMappers<*>>")
    assertContains(source, "public override val _authoredEffects: List<Effect>")
    assertContains(source, "Parsing.parse<Effect>(\"End: VictoryPoint / Animal<This>\")")
    assertContains(
        source,
        "public sealed interface Adjacency<out T0 : Tile<Area>, out T1 : Tile<Area>>",
    )
    assertContains(
        source,
        "public sealed interface DelayedColonyTile<out C0 : Class<ColonyTile>, " +
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
  fun canonicalToolEmitsEveryClassAcrossSemanticFiles() {
    val table = Canon.withPlayers(5).classTable
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
