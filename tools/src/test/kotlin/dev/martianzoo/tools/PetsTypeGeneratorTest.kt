package dev.martianzoo.tools

import com.squareup.kotlinpoet.TypeSpec
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.types.ClassLoader
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PetsTypeGeneratorTest {
  @Test
  fun generatesEveryResolvedClassWithHierarchyAndCovariantDependencies() {
    val table = ClassLoader(Canon).loadEverything()
    val generated =
        PetsTypeGenerator(
                table,
                "example.types",
                "Types",
                generatedAt = Instant.parse("2026-08-06T12:34:56Z"),
            )
            .generate()
    val source = generated.toString()
    val generatedTypes = generated.members.filterIsInstance<TypeSpec>()

    assertEquals(table.allClasses().size + 1, generatedTypes.size)
    assertContains(source, "Generated at 2026-08-06T12:34:56Z")
    table.allClasses().forEach { klass ->
      val generatedClass = generatedTypes.single { it.name == klass.className.toString() }
      assertEquals(
          klass.dependencies.keys.size,
          generatedClass.typeVariables.size,
          "${klass.className} did not expose every inherited dependency",
      )
    }
    assertContains(source, "public object PetsClasses")
    assertContains(source, "public sealed interface Component")
    assertContains(source, "public val type: Type")
    assertContains(source, "public class Plant<")
    assertContains(source, "public override val type: Type")
    assertContains(source, ") : StandardResource")
    assertContains(source, "public sealed interface Owned<out ANY : Anyone> : Component")
    assertContains(source, "public class OceanTile<out MA : MarsArea>")
    assertContains(source, "Tile<MA>")
    assertContains(source, "public sealed interface Cardbound<")
    assertContains(source, "CardFront<P>")
    assertContains(source, "public class Production<")
    assertContains(source, "Class<PetsClasses.StandardResource>")
    assertContains(
        source,
        "public class AerialMappers<out P : Player, out C : Class<PetsClasses.Floater>>",
    )
    assertContains(source, "ResourceCard<P, C>")
    assertContains(
        source,
        "public sealed interface Adjacency<out T0 : Tile<Area>, out T1 : Tile<Area>>",
    )
  }

  @Test
  fun canonicalToolEmitsOnlyPetsSourceClassesInHierarchyOrder() {
    val source = generateCanonicalPetsTypes(PetsTypeGeneratorOptions()).toString()

    assertContains(source, "public class TemperatureStep(")
    assertContains(source, "public sealed interface CardFront<")
    assertContains(source, "public class MarsRow<out MA : MarsArea>(")
    assertFalse(source.contains("public class Tharsis_1_1"))
    assertFalse(source.contains("public class AerialMappers"))
    assertFalse(source.contains("public class Terraformer"))

    val component = source.indexOf("\npublic sealed interface Component")
    val hidden = source.indexOf("\npublic sealed interface Hidden")
    val system = source.indexOf("\npublic sealed interface System")
    val engine = source.indexOf("\npublic class Engine")
    assertTrue(component < hidden && hidden < system && system < engine)
    assertTrue(component < source.indexOf("\npublic object PetsClasses"))
  }
}
