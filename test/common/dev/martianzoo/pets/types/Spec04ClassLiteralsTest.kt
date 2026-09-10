package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Dependency.Key
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 4 of `docs/type-system-spec.md`: class literals. */
internal class Spec04ClassLiteralsTest {

  /** Production and tags, which are the real uses of class literals. */
  private val table =
      loadTypes(
          """
          CLASS Player1 : Owner
          ABSTRACT CLASS StandardResource : Owned<Owner> {
            CLASS MC
            ABSTRACT CLASS Metal { CLASS Steel, Titanium }
            CLASS Plant
          }
          CLASS Production<Class<StandardResource>> : Owned<Owner>
          ABSTRACT CLASS Tag { CLASS BuildingTag, SpaceTag }
          ABSTRACT CLASS Area { CLASS Tharsis_2_2 }
          CLASS CityTile<Area>
          """
              .trimIndent()
      )

  private fun type(s: String) = table.resolve(te(s))

  // T4-1 What a class literal is

  @Test
  internal fun `T4-1 a class literal names a class instead of depending on a component`() {
    type("Class<Steel>").expressionFull shouldBe te("Class<Steel>")
    type("Production<Class<Steel>, Player1>").expressionFull shouldBe
        te("Production<Player1, Class<Steel>>")
  }

  @Test
  internal fun `T4-1 the Class class is bounded by Component`() {
    table.classClass.baseType.expressionFull shouldBe te("Class<Component>")
    type("Class") shouldBe type("Class<Component>")
  }

  // T4-2 Concreteness

  @Test
  internal fun `T4-2 a class literal is concrete exactly when the class it names is`() {
    type("Class<Steel>").abstract shouldBe false
    type("Class<Metal>").abstract shouldBe true
    type("Class<Component>").abstract shouldBe true
  }

  @Test
  internal fun `T4-2 concreteness ignores the named class's own dependencies`() {
    // `CityTile` the *type* is abstract, because its area is not chosen...
    type("CityTile").abstract shouldBe true
    // ...but `CityTile` the *class* is concrete, so its literal is concrete.
    type("Class<CityTile>").abstract shouldBe false
  }

  // T4-3 Covariance

  @Test
  internal fun `T4-3 class literals are covariant in the class they name`() {
    type("Class<Steel>").isSubtypeOf(type("Class<Metal>")) shouldBe true
    type("Class<Metal>").isSubtypeOf(type("Class<StandardResource>")) shouldBe true
    type("Class<Steel>").isSubtypeOf(type("Class<Component>")) shouldBe true
    type("Class<Metal>").isSubtypeOf(type("Class<Steel>")) shouldBe false
    type("Class<Plant>").isSubtypeOf(type("Class<Metal>")) shouldBe false
  }

  @Test
  internal fun `T4-3 covariance carries into a dependency position`() {
    type("Production<Class<Steel>>").isSubtypeOf(type("Production<Class<Metal>>")) shouldBe true
    type("Production<Class<Metal>>").isSubtypeOf(type("Production<Class<Steel>>")) shouldBe false
  }

  // T4-4 Reading the represented class

  @Test
  internal fun `T4-4 representedClass exposes the named class, and is absent otherwise`() {
    type("Class<Steel>").representedClass shouldBe table.getClass(cn("Steel"))
    type("Class<Metal>").representedClass shouldBe table.getClass(cn("Metal"))
    type("Steel").representedClass shouldBe null
    type("Production<Class<Steel>>").representedClass shouldBe null
  }

  // T4-5 Bounds

  @Test
  internal fun `T4-5 glb and lub of class literals follow the class hierarchy`() {
    (type("Class<Metal>") glb type("Class<Steel>")) shouldBe type("Class<Steel>")
    (type("Class<Steel>") lub type("Class<Titanium>")) shouldBe type("Class<Metal>")
    (type("Class<Steel>") lub type("Class<Plant>")) shouldBe type("Class<StandardResource>")
  }

  @Test
  internal fun `T4-5 glb of literals for disjoint classes is absent`() {
    (type("Class<Steel>") glb type("Class<Plant>")) shouldBe null
  }

  // T4-6 The operand must be one bare class name

  @Test
  internal fun `T4-6 a class literal takes exactly one bare class name`() {
    shouldThrow<ExpressionException> { type("Class<Steel, Plant>") }
    shouldThrow<ExpressionException> { type("Class<Steel<Player1>>") }
    shouldThrow<ExpressionException> { type("Class<CityTile<Tharsis_2_2>>") }
    shouldThrow<ExpressionException> { type("Class<Class<Steel>>") }
    shouldThrow<ExpressionException> { type("Class<Class<Component>>") }
    shouldThrow<ExpressionException> { type("Production<Class<Steel, Plant>>") }
  }

  @Test
  internal fun `T4-6 the Class class may itself be named by a literal`() {
    type("Class<Class>").abstract shouldBe false
    type("Class<Class>").representedClass shouldBe table.classClass
  }

  @Test
  internal fun `T4-6 the named class must exist`() {
    shouldThrow<ExpressionException> { type("Class<Jackalope>") }
    // Including where a declaration merely counts one.
    shouldThrow<PetException> { loadTypes("CLASS Querying { HAS MAX 0 Class<Jackalope> }") }
    loadTypes("CLASS Querying { HAS MAX 0 Class<Jackalope> }", "CLASS Jackalope")
        .resolve(te("Class<Jackalope>"))
        .abstract shouldBe false
  }

  @Test
  internal fun `T4-6 an effect may not gain a class representative`() {
    // The one component per concrete class is fixed before any effect can run.
    shouldThrow<PetException> {
      loadTypes("CLASS Source { This:: Class<Target> }", "CLASS Target")
    }
  }

  // T4-7 The slot inside a class literal is not a component dependency

  @Test
  internal fun `T4-7 the slot inside a class literal holds a class, not a type`() {
    val literal = type("Class<Steel>")

    literal.dependencies.keys shouldContainExactly listOf(Key(cn("Class"), 0))
    literal.typeDependencies.shouldBeEmpty()
    literal.narrowedDependencies.keys shouldContainExactly listOf(Key(cn("Class"), 0))
  }

  @Test
  internal fun `T4-7 a dependency bounded by a class literal is an ordinary dependency`() {
    // Exactly one `Class<Steel>` component exists, so a production really can depend on it.
    type("Production<Class<Steel>, Player1>").typeDependencies.map { "${it.key}" } shouldBe
        listOf("Owned_0", "Production_0")
    type("Production<Class<Steel>, Player1>")
        .dependencies
        .get(Key(cn("Production"), 0))
        .expressionFull shouldBe te("Class<Steel>")
  }

  // T4-8 Enumeration

  @Test
  internal fun `T4-8 every concrete class has exactly one literal`() {
    type("Class<Metal>").allConcreteSubtypes().map { "$it" }.toList() shouldContainExactly
        listOf("Class<Steel>", "Class<Titanium>")
    type("Class<Steel>").allConcreteSubtypes().map { "$it" }.toList() shouldContainExactly
        listOf("Class<Steel>")
    type("Class<Tag>").allConcreteSubtypes().map { "$it" }.toList() shouldContainExactly
        listOf("Class<BuildingTag>", "Class<SpaceTag>")
  }

  @Test
  internal fun `T4-8 a literal for a class with no concrete subclass enumerates nothing`() {
    val empty = loadTypes("ABSTRACT CLASS Award")

    empty.resolve(te("Class<Award>")).allConcreteSubtypes().toList().shouldBeEmpty()
  }

  // T4-9 `Class<This>`

  @Test
  internal fun `T4-9 a Class-of-This literal in a header names the inheriting class`() {
    val cards =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner>",
            "ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>",
            "ABSTRACT CLASS ResourceCard<Class<CardResource>> : CardFront",
            "ABSTRACT CLASS CardResource : Cardbound<ResourceCard<Class<This>>> " +
                "{ CLASS Animal, Microbe }",
            "CLASS Fish : ResourceCard<Class<Animal>>",
            "CLASS Ants : ResourceCard<Class<Microbe>>",
        )

    cards.getClass(cn("Animal")).baseType.expressionFull shouldBe
        te("Animal<Owner, ResourceCard<Owner, Class<Animal>>>")
    cards.resolve(te("Animal<Player1, Fish>")).expressionFull shouldBe
        te("Animal<Player1, Fish<Player1, Class<Animal>>>")
    shouldThrow<ExpressionException> { cards.resolve(te("Animal<Ants>")) }
  }
}
