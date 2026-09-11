package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Dependency.Key
import dev.martianzoo.pets.types.DependencySet.DependencyPath
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Section 3 of `docs/type-system-spec.md`: dependencies. */
internal class Spec03DependenciesTest {

  /** A fragment of the real tile/area/ownership model. */
  private val mars =
      loadTypes(
          """
          CLASS Player1 : Owner
          CLASS Player2 : Owner
          ABSTRACT CLASS Area {
            ABSTRACT CLASS MarsArea {
              ABSTRACT CLASS LandArea { CLASS Tharsis_2_2 }
              ABSTRACT CLASS WaterArea { CLASS Tharsis_1_1 }
            }
          }
          ABSTRACT CLASS Occupant<Area>
          ABSTRACT CLASS Tile : Occupant
          ABSTRACT CLASS OwnedTile : Tile, Owned<Owner>
          CLASS GreeneryTile : OwnedTile, Tile<MarsArea>
          CLASS OceanTile : Tile<WaterArea>
          """
              .trimIndent()
      )

  private fun klass(name: String) = mars.getClass(cn(name))

  private fun type(s: String) = mars.resolve(te(s))

  // T3-1 Keys

  @Test
  internal fun `T3-1 each declared dependency gets a key naming its declaring class and slot`() {
    klass("Occupant").dependencies.keys shouldContainExactly listOf(Key(cn("Occupant"), 0))
    "${Key(cn("Occupant"), 0)}" shouldBe "Occupant_0"

    val adjacency = loadTypes("ABSTRACT CLASS Area", "ABSTRACT CLASS Adjacency<Area, Area>")
    adjacency.getClass(cn("Adjacency")).dependencies.keys shouldContainExactly
        listOf(Key(cn("Adjacency"), 0), Key(cn("Adjacency"), 1))
  }

  @Test
  internal fun `T3-1 a class with no dependencies has an empty dependency set`() {
    loadTypes("CLASS Plant").getClass(cn("Plant")).dependencies.keys.shouldBeEmpty()
  }

  // T3-2 Inheritance

  @Test
  internal fun `T3-2 a subclass inherits every dependency under the original key`() {
    klass("Tile").dependencies.keys shouldContainExactly listOf(Key(cn("Occupant"), 0))
    klass("GreeneryTile").dependencies.keys shouldContainExactly
        listOf(Key(cn("Occupant"), 0), Key(cn("Owned"), 0))
  }

  @Test
  internal fun `T3-2 a supertype expression narrows the inherited bound`() {
    klass("Occupant").baseType.expressionFull shouldBe te("Occupant<Area>")
    klass("GreeneryTile").baseType.expressionFull shouldBe te("GreeneryTile<MarsArea, Owner>")
    klass("OceanTile").baseType.expressionFull shouldBe te("OceanTile<WaterArea>")
  }

  @Test
  internal fun `T3-2 newly declared dependencies follow the inherited ones`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS CardBack",
            "ABSTRACT CLASS CardLocation",
            "ABSTRACT CLASS Card<CardBack>",
            "CLASS HeldCard<CardLocation> : Card",
        )

    table.getClass(cn("HeldCard")).dependencies.keys shouldContainExactly
        listOf(Key(cn("Card"), 0), Key(cn("HeldCard"), 0))
    table.getClass(cn("HeldCard")).baseType.expressionFull shouldBe
        te("HeldCard<CardBack, CardLocation>")
  }

  // T3-3 Several supertypes constraining one key

  @Test
  internal fun `T3-3 bounds inherited for one key are intersected`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "ABSTRACT CLASS MarsArea : Area",
            "ABSTRACT CLASS LandArea : MarsArea",
            "ABSTRACT CLASS Tile<Area>",
            "ABSTRACT CLASS MarsTile : Tile<MarsArea>",
            "ABSTRACT CLASS LandTile : Tile<LandArea>",
            "CLASS GreeneryTile : MarsTile, LandTile",
        )

    table.getClass(cn("GreeneryTile")).baseType.expressionFull shouldBe te("GreeneryTile<LandArea>")
  }

  @Test
  internal fun `T3-3 bounds for one key with no common narrowing are an error`() {
    shouldThrow<PetException> {
          loadTypes(
              """
              ABSTRACT CLASS Area
              CLASS Land : Area
              CLASS Water : Area
              ABSTRACT CLASS Tile<Area>
              ABSTRACT CLASS LandTile : Tile<Land>
              ABSTRACT CLASS WaterTile : Tile<Water>
              CLASS Amphibious : LandTile, WaterTile
              """
                  .trimIndent()
          )
        }
        .message
        .shouldContain("Amphibious inherits incompatible bounds for Tile_0")
  }

  // T3-4 Arguments intersect the bound

  @Test
  internal fun `T3-4 an argument intersects the declared bound rather than replacing it`() {
    type("GreeneryTile<Area>") shouldBe type("GreeneryTile")
    type("GreeneryTile<Area>").expressionFull shouldBe te("GreeneryTile<MarsArea, Owner>")
    type("GreeneryTile<LandArea>").expressionFull shouldBe te("GreeneryTile<LandArea, Owner>")
  }

  @Test
  internal fun `T3-4 Anyone names the widest ownership without widening a narrowed bound`() {
    val table =
        loadTypes(
            "CLASS SoloOpponent : Owner",
            "ABSTRACT CLASS Player : Owner { CLASS Player1 }",
            "ABSTRACT CLASS Card : Owned<Player> { CLASS ProjectCard }",
            "ABSTRACT CLASS Resource : Owned<Owner> { CLASS Plant }",
        )

    table.resolve(te("ProjectCard<Anyone>")).expressionFull shouldBe te("ProjectCard<Player>")
    table.resolve(te("Plant<Anyone>")).expressionFull shouldBe te("Plant<Owner>")
    table.resolve(te("ProjectCard<Player1>")).abstract shouldBe false
    table.resolve(te("Plant<SoloOpponent>")).abstract shouldBe false
    shouldThrow<ExpressionException> { table.resolve(te("ProjectCard<SoloOpponent>")) }
  }

  @Test
  internal fun `T3-4 an argument outside the bound is an error`() {
    shouldThrow<ExpressionException> { type("OceanTile<Tharsis_2_2>") }
    shouldThrow<ExpressionException> { type("Occupant<Player1>") }
  }

  // T3-5 Argument matching

  @Test
  internal fun `T3-5 arguments match remaining dependencies greedily from left to right`() {
    type("GreeneryTile<Tharsis_2_2, Player1>") shouldBe type("GreeneryTile<Player1, Tharsis_2_2>")
    type("GreeneryTile<Tharsis_2_2, Player1>").expressionFull shouldBe
        te("GreeneryTile<Tharsis_2_2, Player1>")
  }

  @Test
  internal fun `T3-5 order decides when two dependencies accept the same argument`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2, Tharsis_2_3 }",
            "ABSTRACT CLASS Adjacency<Area, Area>",
        )

    table.resolve(te("Adjacency<Tharsis_2_2>")).expressionFull shouldBe
        te("Adjacency<Tharsis_2_2, Area>")
    table.resolve(te("Adjacency<Tharsis_2_2, Tharsis_2_3>")).expressionFull shouldBe
        te("Adjacency<Tharsis_2_2, Tharsis_2_3>")
    table.resolve(te("Adjacency<Tharsis_2_3, Tharsis_2_2>")).expressionFull shouldBe
        te("Adjacency<Tharsis_2_3, Tharsis_2_2>")
  }

  @Test
  internal fun `T3-5 an argument that matches no remaining dependency is an error`() {
    shouldThrow<ExpressionException> { type("GreeneryTile<Tharsis_2_2, Tharsis_2_2>") }
    shouldThrow<ExpressionException> { type("Player1<Tharsis_2_2>") }
  }

  // T3-6 Reporting matched keys

  @Test
  internal fun `T3-6 matchDependencyKeys reports the key each authored argument filled`() {
    klass("GreeneryTile").matchDependencyKeys(listOf(te("Tharsis_2_2"), te("Player1"))) shouldBe
        listOf(Key(cn("Occupant"), 0), Key(cn("Owned"), 0))
    klass("GreeneryTile").matchDependencyKeys(listOf(te("Player1"), te("Tharsis_2_2"))) shouldBe
        listOf(Key(cn("Owned"), 0), Key(cn("Occupant"), 0))
  }

  // T3-7 `This` in a supertype argument

  @Test
  internal fun `T3-7 This in a supertype argument binds to the inheriting class`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Link<Class<Component>>",
            "ABSTRACT CLASS SelfBound : Link<Class<This>>",
            "ABSTRACT CLASS SelfMiddle : SelfBound",
            "CLASS SelfLeaf : SelfMiddle",
        )

    table.getClass(cn("SelfBound")).baseType.expressionFull shouldBe
        te("SelfBound<Class<SelfBound>>")
    table.getClass(cn("SelfMiddle")).baseType.expressionFull shouldBe
        te("SelfMiddle<Class<SelfMiddle>>")
    table.getClass(cn("SelfLeaf")).baseType.expressionFull shouldBe te("SelfLeaf<Class<SelfLeaf>>")
  }

  @Test
  internal fun `T3-7 a literal class name in a supertype argument is not rebound`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Link<Class<Component>>",
            "ABSTRACT CLASS LiteralBound : Link<Class<LiteralBound>>",
            "ABSTRACT CLASS LiteralMiddle : LiteralBound",
            "CLASS LiteralLeaf : LiteralMiddle",
        )

    table.getClass(cn("LiteralLeaf")).baseType.expressionFull shouldBe
        te("LiteralLeaf<Class<LiteralBound>>")
  }

  @Test
  internal fun `T3-7 only the This positions are rebound, in place`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS LeftComponent",
            "ABSTRACT CLASS RightComponent",
            "ABSTRACT CLASS Pair<Class<LeftComponent>, Class<RightComponent>>",
            "ABSTRACT CLASS Wrapper<Pair<Class<LeftComponent>, Class<RightComponent>>>",
            "ABSTRACT CLASS Mixed : LeftComponent, RightComponent, " +
                "Wrapper<Pair<Class<This>, Class<Mixed>>>",
            "CLASS MixedLeaf : Mixed",
        )

    table.getClass(cn("MixedLeaf")).baseType.expressionFull shouldBe
        te("MixedLeaf<Pair<Class<MixedLeaf>, Class<Mixed>>>")
  }

  // T3-8 Dependency equalities

  private fun equalityCards() =
      loadTypes(
          "CLASS Player1 : Owner",
          "CLASS Player2 : Owner",
          "CLASS Card : Owned<Owner>",
          "ABSTRACT CLASS Linked<Card<Owner>> : Owned<Owner>",
          "CLASS InheritedLink : Linked",
      )

  @Test
  internal fun `T3-8 one header variable used twice forces its two positions to agree`() {
    val cards =
        loadTypes(
            "CLASS Player1 : Owner",
            "CLASS Player2 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner> { CLASS Pets }",
            "ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner> { CLASS Animal }",
        )

    cards.getClass(cn("Cardbound")).baseType.expressionFull shouldBe
        te("Cardbound<Owner, CardFront<Owner>>")
    cards.getClass(cn("Cardbound")).isEqualityConstrainedDependency(Key(cn("Owned"), 0)) shouldBe
        true

    // Choosing the owner also chooses the card's owner, and vice versa.
    cards.resolve(te("Animal<Player1>")).expressionFull shouldBe
        te("Animal<Player1, CardFront<Player1>>")
    cards.resolve(te("Animal<Pets<Player1>>")).expressionFull shouldBe
        te("Animal<Player1, Pets<Player1>>")
    cards.resolve(te("Animal<Player1, Pets>")) shouldBe cards.resolve(te("Animal<Pets<Player1>>"))
    shouldThrow<ExpressionException> { cards.resolve(te("Animal<Player1, Pets<Player2>>")) }
  }

  @Test
  internal fun `T3-8 independent dependency roots stay independent even when spelled alike`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2, Tharsis_2_3 }",
            "ABSTRACT CLASS Adjacency<Area, Area>",
        )

    table
        .getClass(cn("Adjacency"))
        .isEqualityConstrainedDependency(Key(cn("Adjacency"), 0)) shouldBe false
    table.resolve(te("Adjacency<Tharsis_2_2, Tharsis_2_3>")).expressionFull shouldBe
        te("Adjacency<Tharsis_2_2, Tharsis_2_3>")
  }

  @Test
  internal fun `T3-8 shared variables are narrowed before a difference is tested`() {
    val cards = equalityCards()

    (cards.resolve(te("Card<Player1>")) glb cards.resolve(te("Card<Player2>"))) shouldBe null
    (cards.resolve(te("Card<Player1>")) glb cards.resolve(te("Card(NOT Card<Player2>)"))) shouldBe
        cards.resolve(te("Card<Player1>"))
    cards.resolve(te("Linked<Player1, Card(NOT Card<Player2>)>")) shouldBe
        cards.resolve(te("Linked<Player1>"))
    cards.resolve(te("Linked<Player1, Owned(NOT Card)>")).abstract shouldBe true
  }

  @Test
  internal fun `T3-8 equality-constrained concrete types are enumerated once`() {
    val cards = equalityCards()

    cards
        .getClass(cn("InheritedLink"))
        .concreteTypes()
        .map { it.expressionFull.toString() }
        .toList()
        .shouldContainExactlyInAnyOrder(
            "InheritedLink<Player1, Card<Player1>>",
            "InheritedLink<Player2, Card<Player2>>",
        )
  }

  // T3-9 Dependency targets must be unique

  @Test
  internal fun `T3-9 a dependency may only target a type limited to one copy`() {
    val unlimited = loadTypes("CLASS Plant", "CLASS Holder<Plant>")
    shouldThrow<PetException> { unlimited.componentLimits }.message shouldContain "Holder -> Plant"

    val limited = loadTypes("CLASS Plant { HAS MAX 1 This }", "CLASS Holder<Plant>")
    limited.componentLimits.limitsFor(limited.resolve(te("Plant"))).map { it.range } shouldBe
        listOf(0..1)
  }

  @Test
  internal fun `T3-9 exact per-type and stronger aggregate limits make valid dependency targets`() {
    val table =
        loadTypes(
            "CLASS ExactTarget { HAS =1 This }",
            "CLASS MaxTarget { HAS MAX 1 This }",
            "ABSTRACT CLASS AggregateTarget { HAS MAX 1 AggregateTarget }",
            "CLASS AggregateTargetA : AggregateTarget",
            "CLASS AggregateTargetB : AggregateTarget",
            "CLASS Dependent<ExactTarget, MaxTarget, AggregateTarget>",
        )

    table.componentLimits
  }

  @Test
  internal fun `T3-9 dependency multiplicity validation waits for a concrete dependent class`() {
    val valid =
        loadTypes(
            "ABSTRACT CLASS Target",
            "CLASS UniqueTarget : Target { HAS MAX 1 This }",
            "CLASS RepeatableTarget : Target",
            "ABSTRACT CLASS AbstractDependent<Target>",
            "CLASS ConcreteDependent : AbstractDependent<UniqueTarget>",
        )
    valid.componentLimits

    val invalid =
        loadTypes(
            "ABSTRACT CLASS Target",
            "CLASS UniqueTarget : Target { HAS MAX 1 This }",
            "CLASS RepeatableTarget : Target",
            "ABSTRACT CLASS AbstractDependent<Target>",
            "CLASS ConcreteDependent<Target> : AbstractDependent<Target>",
        )
    shouldThrow<PetException> { invalid.componentLimits }.message shouldContain
        "ConcreteDependent -> RepeatableTarget"
  }

  @Test
  internal fun `T3-9 class invariants used as limits must count one component expression`() {
    val table =
        loadTypes(
            "CLASS Foo",
            "CLASS Bar",
            "CLASS InvalidInvariant { HAS Foo OR Bar }",
            "CLASS Dependent<InvalidInvariant>",
        )

    shouldThrow<PetException> { table.componentLimits }
  }

  // T3-10 Dependency sets

  @Test
  internal fun `T3-10 a dependency set is keyed, and equality ignores order`() {
    val tile = type("GreeneryTile<Tharsis_2_2, Player1>")

    tile.dependencies.get(Key(cn("Occupant"), 0)).expressionFull shouldBe te("Tharsis_2_2")
    tile.dependencies.get(Key(cn("Owned"), 0)).expressionFull shouldBe te("Player1")
    tile.dependencies.getIfPresent(Key(cn("Tile"), 0)) shouldBe null
    tile.dependencies shouldBe type("GreeneryTile<Player1, Tharsis_2_2>").dependencies
  }

  @Test
  internal fun `T3-10 flatten walks nested dependency paths`() {
    val cards =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner> { CLASS Pets }",
            "ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner> { CLASS Animal }",
        )
    val animal = cards.resolve(te("Animal<Pets<Player1>>"))

    animal.dependencies
        .flatten()
        .mapKeys { (path, _) -> path.keyList.joinToString(".") }
        .mapValues { (_, klass) -> "$klass" } shouldBe
        mapOf(
            "Owned_0" to "Player1",
            "Cardbound_0" to "Pets",
            "Cardbound_0.Owned_0" to "Player1",
        )
    animal.dependencies
        .at(DependencyPath(listOf(Key(cn("Cardbound"), 0), Key(cn("Owned"), 0))))
        .expressionFull shouldBe te("Player1")
  }

  @Test
  internal fun `T3-10 narrowedDependencies reports only what a type narrowed below its class`() {
    type("GreeneryTile").narrowedDependencies.keys.shouldBeEmpty()
    type("GreeneryTile<Tharsis_2_2>").narrowedDependencies.keys shouldContainExactly
        listOf(Key(cn("Occupant"), 0))
  }

  // T3-11 Cycles

  @Test
  internal fun `T3-11 a dependency cycle between class headers is rejected`() {
    shouldThrow<PetException> { loadTypes("CLASS Foo<Bar>", "CLASS Bar<Foo>") }
    shouldThrow<PetException> { loadTypes("ABSTRACT CLASS Foo<Foo>") }
  }

  @Test
  internal fun `T3-11 a one-way dependency between two classes is fine`() {
    val table = loadTypes("ABSTRACT CLASS Area", "CLASS Tile<Area>", "CLASS Marker<Tile>")

    table.getClass(cn("Marker")).baseType.expressionFull shouldBe te("Marker<Tile<Area>>")
  }
}
