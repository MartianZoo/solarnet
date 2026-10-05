package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.PetTransformer
import dev.martianzoo.pets.TableWorld
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.TypeInfo
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.TypeVariableName.Declaration
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Then
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.typeVariablesFor
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.systemClassDeclarations
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 13 of `docs/type-system-spec.md`: type variables. */
internal class Spec13TypeVariablesTest {

  private val resources =
      loadTypes(
          "CLASS Player1 : Anyone",
          "ABSTRACT CLASS StandardResource : Owned<Anyone> {\nCLASS Plant\nCLASS Steel\n}",
          "ABSTRACT CLASS Production<Class<StandardResource>> : Owned<Anyone>",
          "ABSTRACT CLASS Receipt<Class<StandardResource>>",
      )

  private fun effect(source: String): Effect =
      resources.recordTypeVariableScopes().transformEffect(parse(source))

  private fun names(scope: TypeVariableScope) =
      scope.variables.map { it.name ?: "${it.declaration.expression}" }

  // T13-1 A variable is a kind of type

  @Test
  internal fun `T13-1 a variable is a Type whose structural meaning is its bound`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<P@Person> { This: Token<P@Person> }",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()

    variable.typeVariable shouldBe variable
    variable.bound shouldBe table.resolve(te("Person"))
    variable.groundType shouldBe table.resolve(te("Person"))
    variable.bound.typeVariable shouldBe null
    variable.rootClass shouldBe table.getClass(cn("Person"))
    variable.abstract shouldBe true
    variable.isSubtypeOf(table.resolve(te("Person"))) shouldBe true
    table.resolve(te("Alice")).isSubtypeOf(variable) shouldBe true
    "${variable.expression}" shouldBe "P@Person"
  }

  @Test
  internal fun `T13-1 every occurrence is a Type view of the same variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<P@Person> { This: Token<P@Person> }",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()

    variable.occurrences shouldBe listOf(variable.declaration) + variable.usages
    variable.declaration.typeVariable shouldBe variable
    variable.usages.single().typeVariable shouldBe variable
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("P@Person", "P@Person")
    variable.occurrences.map { it.ordinal } shouldBe listOf(0, 1)
  }

  // T13-2 Class-header variables

  @Test
  internal fun `T13-2 each eligible abstract header expression declares one variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Holder<Box<Person>>",
        )

    table.getClass(cn("Holder")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Box<Person>", "Person")
  }

  @Test
  internal fun `T13-2 a class literal declares either its represented class or itself`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Carries<Class<Person>>",
            "ABSTRACT CLASS Represented<Class<Person>>",
            "ABSTRACT CLASS Literal<@Class<Person>> : Carries<@Class>",
        )

    table.getClass(cn("Represented")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Person")
    table.getClass(cn("Literal")).typeVariables.map { "$it" } shouldContainExactly
        listOf("@Class<Person>")
  }

  @Test
  internal fun `T13-2 a concrete or This header expression declares nothing`() {
    val table =
        loadTypes(
            "CLASS Alice",
            "ABSTRACT CLASS Box<Alice>",
            "ABSTRACT CLASS Link<Class<Component>>",
            "ABSTRACT CLASS SelfBound : Link<Class<This>>",
        )

    table.getClass(cn("Box")).typeVariables.shouldContainExactly(listOf())
    table.getClass(cn("SelfBound")).typeVariables.map { "$it" } shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-2 an explicit name links nested and inherited dependency positions`() {
    val cards =
        loadTypes(
            "CLASS Player1 : Anyone",
            "ABSTRACT CLASS CardFront : Owned<Anyone>",
            "ABSTRACT CLASS Cardbound<CardFront<CardHolder@Anyone>> : Owned<CardHolder@Anyone>",
        )

    cards.getClass(cn("Cardbound")).typeVariables.map { "$it" } shouldContainExactly
        listOf("CardFront<CardHolder@Anyone>", "CardHolder", "Me")
    cards
        .getClass(cn("Cardbound"))
        .isEqualityConstrainedDependency(Dependency.Key(cn("Owned"), 0)) shouldBe true
  }

  @Test
  internal fun `T13-2 nested and inherited positions declare distinct variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person : Anyone {\nCLASS Alice\nCLASS Bob\n}",
            "ABSTRACT CLASS City : Owned<Person>",
            "ABSTRACT CLASS Cathedral<City<Person>, CathedralHolder@Person> : " +
                "Owned<CathedralHolder@Person>",
        )

    val cathedral = table.getClass(cn("Cathedral"))
    cathedral.typeVariables.map { "$it" } shouldContainExactly
        listOf("City<Person>", "Person", "CathedralHolder", "Me")
    cathedral.typeVariables.distinct().size shouldBe 4
  }

  @Test
  internal fun `T13-2 each equal header root declares its own variable`() {
    val table =
        loadTypes("ABSTRACT CLASS Person { CLASS Alice }", "ABSTRACT CLASS Duo<Person, Person>")

    table.getClass(cn("Duo")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Person", "Person")
    table.getClass(cn("Duo")).typeVariables.distinct().size shouldBe 2
  }

  @Test
  internal fun `T13-2 an explicit name can link two header roots`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Duo<P@Person, P@Person>",
        )

    table.getClass(cn("Duo")).typeVariables.map { "$it" } shouldContainExactly listOf("P")
    table.getClass(cn("Duo")).isEqualityConstrainedDependency(Dependency.Key(cn("Duo"), 0)) shouldBe
        true
  }

  @Test
  internal fun `T13-2 each sibling branch declares its own nested variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person {\nCLASS Alice\nCLASS Bob\n}",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Pair<Box<Person>, Box<Person>>",
            "ABSTRACT CLASS Holder<Pair<Box<Person>, Box<Person>>>",
        )

    val pair = table.getClass(cn("Pair"))
    pair.typeVariables.map { "$it" } shouldContainExactly
        listOf("Box<Person>", "Person", "Box<Person>", "Person")
    pair.typeVariables.distinct().size shouldBe 4

    // The two people are free to differ, so no equality propagation (T3-8) forces them together,
    // at whatever depth the sibling branches sit.
    table.resolve(te("Pair<Box<Alice>, Box<Bob>>")).expressionFull shouldBe
        te("Pair<Box<Alice>, Box<Bob>>")
    table.resolve(te("Holder<Pair<Box<Alice>, Box<Bob>>>")).expressionFull shouldBe
        te("Holder<Pair<Box<Alice>, Box<Bob>>>")
  }

  // T13-3 Uses in the class body

  @Test
  internal fun `T13-3 interpreting shared source in another universe preserves earlier scope`() {
    val declarations =
        ClassDeclaration.indexByName(
            systemClassDeclarations +
                parseClasses(
                    """
                    ABSTRACT CLASS Person { CLASS Alice }
                    ABSTRACT CLASS Token<Person>
                    ABSTRACT CLASS Holder<P@Person> { This: Token<P@Person> }
                    """
                )
        )
    val firstTable = ClassLoader(declarations).loadEverything()
    val secondTable = ClassLoader(declarations).loadEverything()
    val firstHolder = firstTable.getClass(cn("Holder"))
    val secondHolder = secondTable.getClass(cn("Holder"))
    val source = firstHolder.declaration.effects.single()

    val first = firstHolder.interpretTypeVariablesIn(source)
    val second = secondHolder.interpretTypeVariablesIn(source)

    first.typeVariables.variables.single().bound shouldBe firstTable.resolve(te("Person"))
    second.typeVariables.variables.single().bound shouldBe secondTable.resolve(te("Person"))
    source.typeVariables.isEmpty shouldBe true
  }

  @Test
  internal fun `T13-3 scope recording uses the game universe for premise Classes`() {
    val sourceUniverse = loadTypes("ABSTRACT CLASS Master")
    val premise =
        ClassLoader.forPremise(
            premiseTable =
                PremiseClassTable(sourceUniverse, parseClasses("ABSTRACT CLASS Local").toSet()),
            roots = emptySet(),
        )
    val info = object : TypeInfo by TableWorld(premise) {}
    val instruction = parse<Instruction>("L@Local THEN L@Local")

    names(instruction.typeVariablesFor(info)) shouldContainExactly listOf("L")
  }

  @Test
  internal fun `T13-3 narrowing shared master variables uses the delegated premise universe`() {
    val universe = loadTypes("ABSTRACT CLASS Piece\nCLASS Box<Piece>")
    val premise =
        ClassLoader.forPremise(
            premiseTable =
                PremiseClassTable(
                    universe,
                    parseClasses("CLASS LocalPiece : Piece\nCLASS OtherPiece : Piece").toSet(),
                ),
            roots = setOf(cn("LocalPiece"), cn("OtherPiece"), cn("Box")),
        )
    val instruction =
        universe
            .recordTypeVariableScopes()
            .transformInstruction(parse<Instruction>("Chosen@Piece! THEN Box<Chosen@Piece>!"))
    val info = object : TypeInfo by TableWorld(premise) {}

    parse<Instruction>("LocalPiece! THEN Box<LocalPiece>!").narrows(instruction, info) shouldBe true
    parse<Instruction>("LocalPiece! THEN Box<OtherPiece>!").narrows(instruction, info) shouldBe
        false
  }

  @Test
  internal fun `T13-3 a named header variable is visible in the class's own effects`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Holder<HeldBox@Box<Person>> { This: Token<HeldBox@Box> }",
        )
    val holder = table.getClass(cn("Holder"))
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())

    names(effect.typeVariables) shouldContainExactly listOf("HeldBox")
  }

  @Test
  internal fun `T13-3 a header variable reference may appear inside another expression`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Holder<P@Person> { This: Box<P@Person> }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("P@Person", "P@Person")
  }

  @Test
  internal fun `T13-3 a class body uses a header variable only through its name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Holder<Person> { This: Person }",
        )

    table.getClass(cn("Holder")).typeVariables.single().usages shouldBe emptyList()
  }

  @Test
  internal fun `T13-3 an ordinary header variable reference cannot receive arguments`() {
    shouldThrow<PetSyntaxException> {
      parseClasses(
          "CLASS Player1 : Anyone\n" +
              "ABSTRACT CLASS Person : Owned<Anyone>\n" +
              "ABSTRACT CLASS Holder<P@Person> { This: P@Person<Player1> }"
      )
    }
  }

  @Test
  internal fun `T13-3 a represented-Class header variable can receive dependency arguments`() {
    val table =
        loadTypes(
            "CLASS Player1 : Anyone",
            "ABSTRACT CLASS Person : Owned<Anyone> { CLASS Alice }",
            "ABSTRACT CLASS Holder<Class<P@Person>> { This: P@Person<Player1> }",
        )
    val holder = table.getClass(cn("Holder"))
    val variable = holder.typeVariables.single { it.name == "P" }
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())
    val specialized = table.resolve(te("Holder<Class<Alice>>"))
    table.resolve(te("Alice")).abstract shouldBe true

    variable.usages.map { "${it.expression}" } shouldContainExactly listOf("P@Person<Player1>")
    val bound =
        effect.typeVariables
            .bind(specialized.variableBindingsFrom(holder.defaultType, listOf(variable)))
            .transformEffect(effect)
    bound.toString() shouldBe "This: Alice<Player1>"
    bound.typeVariables.isEmpty shouldBe true
  }

  @Test
  internal fun `T13-3 a Class-header variable name may also be a Type name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Holder<Holder@Person> { This: Holder@Person }",
        )

    table.getClass(cn("Holder")).typeVariables.single().name shouldBe "Holder"
  }

  @Test
  internal fun `T13-3 a header name can be used by a subclass`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Holder<P@Person>",
            "CLASS Gift : Holder { This: Token<P@Person> }",
        )
    val gift = table.getClass(cn("Gift"))
    val effect = gift.interpretTypeVariablesIn(gift.declaration.effects.single())
    val specialized = table.resolve(te("Gift<Alice>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(gift.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice>"
  }

  @Test
  internal fun `T13-3 only an eligible abstract header occurrence can declare a variable`() {
    shouldThrow<PetException> {
      loadTypes(
          "CLASS Alice",
          "ABSTRACT CLASS Holder<A@Alice>",
      )
    }
  }

  // T13-4 Inheritance

  @Test
  internal fun `T13-4 a short name refers to an inherited header variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Owner@Person>",
            "CLASS NamedBadge : Badge { This: Token<Owner@> }",
        )
    val named = table.getClass(cn("NamedBadge"))
    named.declaration.effects.single().toString() shouldBe "This: Token<Owner@Person>"
    named.typeVariables.single().name shouldBe "Owner"
  }

  @Test
  internal fun `T13-4 a short owner name shares broad and narrow inherited aliases`() {
    checkShortInheritedOwner("Owned, Narrow")
  }

  @Test
  internal fun `T13-4 a short owner name is independent of inherited alias order`() {
    checkShortInheritedOwner("Narrow, Owned")
  }

  private fun checkShortInheritedOwner(supertypes: String) {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Anyone { CLASS Vin }",
            "ABSTRACT CLASS Token : Owned",
            "ABSTRACT CLASS Narrow : Owned<Me@Player>",
            "CLASS Leaf : $supertypes { This: Token<Me@> }",
        )
    val leaf = table.getClass(cn("Leaf"))
    val effect = leaf.interpretTypeVariablesIn(leaf.declaration.effects.single())
    val specialized = table.resolve(te("Leaf<Vin>"))

    leaf.dependencies.keys.size shouldBe 1
    effect.typeVariables
        .bind(specialized.variableBindingsFrom(leaf.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Vin>"
  }

  @Test
  internal fun `T13-4 a subclass retains an inherited variable without redeclaring it`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<P@Person> { This: Token<P@Person> }",
            "ABSTRACT CLASS Middle : Badge",
            "CLASS Leaf : Badge<Alice>",
        )

    table.getClass(cn("Badge")).typeVariables.map { "$it" } shouldContainExactly listOf("P")
    table.getClass(cn("Middle")).typeVariables.map { "$it" } shouldContainExactly listOf("P")
    table.getClass(cn("Leaf")).typeVariables.map { "$it" } shouldContainExactly listOf("P")
  }

  @Test
  internal fun `T13-4 a diamond shares a named dependency`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Root<P@Person>",
            "ABSTRACT CLASS Left : Root",
            "ABSTRACT CLASS Right : Root",
            "CLASS Diamond : Left, Right { This: Token<P@Person> }",
        )
    val diamond = table.getClass(cn("Diamond"))
    val effect = diamond.interpretTypeVariablesIn(diamond.declaration.effects.single())
    val specialized = table.resolve(te("Diamond<Alice>"))

    diamond.dependencies.keys.size shouldBe 1
    effect.typeVariables
        .bind(specialized.variableBindingsFrom(diamond.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice>"
  }

  @Test
  internal fun `T13-4 names introduced on two paths to one dependency agree`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Root<Person>",
            "ABSTRACT CLASS Left : Root<P@Person>",
            "ABSTRACT CLASS Right : Root<P@Person>",
            "CLASS Diamond : Left, Right { This: Token<P@Person> }",
        )
    val diamond = table.getClass(cn("Diamond"))
    val effect = diamond.interpretTypeVariablesIn(diamond.declaration.effects.single())
    val specialized = table.resolve(te("Diamond<Alice>"))

    diamond.dependencies.keys.size shouldBe 1
    effect.typeVariables
        .bind(specialized.variableBindingsFrom(diamond.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice>"
  }

  @Test
  internal fun `T13-4 different inherited names can denote one dependency`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Root<P@Person>",
            "ABSTRACT CLASS Alias : Root<Q@Person>",
            "CLASS Leaf : Alias { This: Token<P@Person> THEN Token<Q@Person> }",
        )
    val leaf = table.getClass(cn("Leaf"))
    val effect = leaf.interpretTypeVariablesIn(leaf.declaration.effects.single())
    val specialized = table.resolve(te("Leaf<Alice>"))

    leaf.dependencies.keys.size shouldBe 1
    effect.typeVariables.variables.size shouldBe 2
    effect.typeVariables
        .bind(specialized.variableBindingsFrom(leaf.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice> THEN Token<Alice>"
  }

  @Test
  internal fun `T13-4 an inherited name scopes both sides of an effect`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Holder<P@Person>",
            "CLASS Gift : Holder { Token<P@Person>: Token<P@Person> }",
        )
    val gift = table.getClass(cn("Gift"))
    val effect = gift.interpretTypeVariablesIn(gift.declaration.effects.single())
    val specialized = table.resolve(te("Gift<Alice>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(gift.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "Token<Alice>: Token<Alice>"
  }

  @Test
  internal fun `T13-4 an inherited name scopes both sides of an action`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS HasActions",
            "ABSTRACT CLASS ActionSlot",
            "CLASS Action1 : ActionSlot",
            "CLASS UseAction<HasActions, ActionSlot>",
            "ABSTRACT CLASS Holder<P@Person>",
            "CLASS Gift : Holder, HasActions { Token<P@Person> -> Token<P@Person> }",
        )
    val gift = table.getClass(cn("Gift"))
    val effect = gift.interpretTypeVariablesIn(gift.declaration.effects.single())
    val specialized = table.resolve(te("Gift<Alice>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(gift.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "UseAction<This, Action1>: -Token<Alice>! THEN Token<Alice>"
  }

  @Test
  internal fun `T13-4 a card inherits Me without redeclaring it`() {
    val table =
        loadTypes(
            "CLASS Joe : Anyone",
            "ABSTRACT CLASS OwnedLike<Me@Anyone>",
            "ABSTRACT CLASS Plant : OwnedLike",
            "ABSTRACT CLASS CardFront : OwnedLike",
            "CLASS FooCard : CardFront { This: Plant<Me@Anyone> }",
        )
    val card = table.getClass(cn("FooCard"))
    val effect = card.interpretTypeVariablesIn(card.declaration.effects.single())
    val specialized = table.resolve(te("FooCard<Joe>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(card.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Plant<Joe>"
  }

  @Test
  internal fun `T13-4 a named owner dependency can narrow to Player`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Anyone { CLASS Joe }",
            "ABSTRACT CLASS Token<Player>",
            "ABSTRACT CLASS OwnedLike<Me@Anyone>",
            "ABSTRACT CLASS PlayerOwned : OwnedLike<Me@Player>",
            "CLASS Foo : PlayerOwned { This: Token<Me@Player> }",
        )
    val foo = table.getClass(cn("Foo"))
    val effect = foo.interpretTypeVariablesIn(foo.declaration.effects.single())
    val specialized = table.resolve(te("Foo<Joe>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(foo.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Joe>"
  }

  @Test
  internal fun `T13-4 executable effects can use an inherited name`() {
    val declarations =
        ClassDeclaration.indexByName(
            systemClassDeclarations +
                parseClasses(
                    """
                    ABSTRACT CLASS Person { CLASS Alice }
                    ABSTRACT CLASS Token<Person>
                    ABSTRACT CLASS Holder<P@Person>
                    CLASS Gift : Holder { This: Token<P@Person> }
                    """
                        .trimIndent()
                )
        )
    val giftSource = declarations.getValue(cn("Gift"))
    val giftDeclaration = giftSource.copy(executableEffects = giftSource.authoredEffects)
    val table = ClassLoader(declarations + (cn("Gift") to giftDeclaration)).loadEverything()
    val gift = table.getClass(cn("Gift"))
    val effect = gift.interpretTypeVariablesIn(gift.declaration.effects.single())
    val specialized = table.resolve(te("Gift<Alice>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(gift.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice>"
  }

  @Test
  internal fun `T13-4 a name survives a Class-of-This header`() {
    val table =
        loadTypes(
            "CLASS Player1 : Anyone",
            "ABSTRACT CLASS Token<Anyone>",
            "ABSTRACT CLASS CardFront : Owned<Anyone>",
            "ABSTRACT CLASS Cardbound<CardFront<CardHolder@Anyone>> : Owned<CardHolder@Anyone>",
            "ABSTRACT CLASS ResourceCard<Class<CardResource>> : CardFront",
            "ABSTRACT CLASS CardResource : Cardbound<ResourceCard<Class<This>>>",
            "CLASS Observer : CardResource { This: Token<CardHolder@Anyone> }",
        )
    val observer = table.getClass(cn("Observer"))
    val effect = observer.interpretTypeVariablesIn(observer.declaration.effects.single())
    val specialized = table.resolve(te("Observer<Player1>"))

    effect.typeVariables
        .bind(
            specialized.variableBindingsFrom(observer.defaultType, effect.typeVariables.variables)
        )
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Player1>"
  }

  @Test
  internal fun `T13-4 an explicit EACH marker shadows an inherited name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Holder<P@Person>",
            "CLASS Gift : Holder { This: EACH P@Person { Token<P@Person> } }",
        )
    val gift = table.getClass(cn("Gift"))
    val effect = gift.interpretTypeVariablesIn(gift.declaration.effects.single())

    effect.typeVariables.isEmpty shouldBe true
    val each = effect.instruction.descendantsOfType<Instruction.Each>().single()
    each.bodyFor(cn("Person").expression).toString() shouldBe "Token<Person>"
  }

  @Test
  internal fun `T13-4 independent inherited names are ambiguous`() {
    val declarations =
        listOf(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Left<P@Person> { This: Token<P@Person> }",
            "ABSTRACT CLASS Right<P@Person> { This: Token<P@Person> }",
        )
    val ambiguous = declarations + "CLASS Ambiguous : Left, Right { This: Token<P@Person> }"
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes(*(declarations + "CLASS Combined : Left, Right").toTypedArray())
        }
        .detail shouldBe "`Combined` inherits ambiguous type variable `P@Person`"
    shouldThrow<InvalidPetDefinitionException> { loadTypes(*ambiguous.toTypedArray()) }
        .detail shouldBe "`Ambiguous` inherits ambiguous type variable `P@Person`"
  }

  @Test
  internal fun `T13-4 narrowing a shared name does not merge independent bindings`() {
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes(
              "ABSTRACT CLASS Player : Anyone",
              "ABSTRACT CLASS Token<Player>",
              "ABSTRACT CLASS Left<Me@Anyone>",
              "ABSTRACT CLASS Right<Me@Anyone>",
              "CLASS Both : Left, Right { This: Token<Me@Player> }",
          )
        }
        .detail shouldBe "`Both` inherits ambiguous type variable `Me@Anyone`"
  }

  @Test
  internal fun `T13-4 effects inherited under distinct names keep their own bindings`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person {\nCLASS Alice\nCLASS Bob\n}",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Left<P@Person> { This: Token<P@Person> }",
            "ABSTRACT CLASS Right<Q@Person> { This: Token<Q@Person> }",
            "CLASS Combined : Left, Right",
        )
    val combined = table.getClass(cn("Combined"))
    val specialized = table.resolve(te("Combined<Alice, Bob>"))

    fun inheritedFrom(parent: String): String {
      val klass = table.getClass(cn(parent))
      val effect = klass.interpretTypeVariablesIn(klass.declaration.effects.single())
      val variables = effect.typeVariables.variables
      return effect.typeVariables
          .bind(specialized.variableBindingsFrom(combined.defaultType, variables))
          .transformEffect(effect)
          .toString()
    }

    inheritedFrom("Left") shouldBe "This: Token<Alice>"
    inheritedFrom("Right") shouldBe "This: Token<Bob>"
  }

  @Test
  internal fun `T13-4 a header cannot reuse an inherited name for another dependency`() {
    shouldThrow<InvalidPetDefinitionException> {
          loadTypes(
              "ABSTRACT CLASS Person",
              "ABSTRACT CLASS Root<P@Person>",
              "ABSTRACT CLASS Mid<P@Person> : Root",
          )
        }
        .detail shouldBe "`Mid` reuses inherited type variable `P@Person` for another dependency"
  }

  @Test
  internal fun `T13-4 a header can mark an inherited binding again`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Root<P@Person>",
            "ABSTRACT CLASS Mid : Root<P@Person>",
            "CLASS Leaf : Mid { This: Token<P@Person> }",
        )
    val leaf = table.getClass(cn("Leaf"))
    val effect = leaf.interpretTypeVariablesIn(leaf.declaration.effects.single())
    val specialized = table.resolve(te("Leaf<Alice>"))

    leaf.dependencies.keys.size shouldBe 1
    effect.typeVariables
        .bind(specialized.variableBindingsFrom(leaf.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice>"
  }

  @Test
  internal fun `T13-4 an inherited name can appear only in a trigger`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Holder<P@Person>",
            "CLASS Gift : Holder { Token<P@Person>: Ok }",
        )
    val gift = table.getClass(cn("Gift"))
    val effect = gift.interpretTypeVariablesIn(gift.declaration.effects.single())
    val specialized = table.resolve(te("Gift<Alice>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(gift.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "Token<Alice>: Ok"
  }

  @Test
  internal fun `T13-4 a subclass invariant marker must still be shared`() {
    shouldThrow<PetSyntaxException> {
          parseClasses(
              """
              ABSTRACT CLASS Person
              ABSTRACT CLASS Base
              CLASS Foo : Base { HAS P@Person }
              """
                  .trimIndent()
          )
        }
        .detail shouldBe
        "type variable marker `P@Person` is not shared in a scope; use it again in that scope or remove the marker"
  }

  @Test
  internal fun `T13-4 a supplied supertype argument needs a name in the subclass body`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person>",
            "ABSTRACT CLASS PersonBadge : Badge<P@Person> { This: Token<P@Person> }",
        )
    val personBadge = table.getClass(cn("PersonBadge"))
    val effect = personBadge.interpretTypeVariablesIn(personBadge.declaration.effects.single())
    val variable = effect.typeVariables.variables.single()
    val specialized = table.resolve(te("PersonBadge<Alice>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(personBadge.defaultType, listOf(variable)))
        .transformEffect(effect)
        .toString() shouldBe "This: Token<Alice>"
  }

  // T13-5 Capturing values

  @Test
  internal fun `T13-5 specializing a component type supplies its header variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Holder<HeldBox@Box<Person>> { This: HeldBox@Box }",
        )
    val holder = table.getClass(cn("Holder"))
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())
    val bindings =
        table
            .resolve(te("Holder<Box<Alice>>"))
            .variableBindingsFrom(holder.defaultType, effect.typeVariables.variables)

    bindings.map { (variable, value) -> "$variable=$value" } shouldContainExactly
        listOf("HeldBox=Box<Alice>")
    effect.typeVariables.bind(bindings).transformEffect(effect).toString() shouldBe
        "This: Box<Alice>"
  }

  @Test
  internal fun `T13-5 nested named variables both specialize a class body`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource<Holder>",
            "ABSTRACT CLASS Holder<Class<Resource>>",
            "CLASS Rock<Holder> : Resource<Holder>",
            "CLASS RockHolder : Holder<Class<Rock>>",
            "CLASS Offer<H@Holder<Class<R@Resource>>> { " + "This: R@Resource<H@Holder> }",
        )
    val offer = table.getClass(cn("Offer"))
    val effect = offer.interpretTypeVariablesIn(offer.declaration.effects.single())
    val specialized = table.resolve(te("Offer<RockHolder>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(offer.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe "This: Rock<RockHolder>"
  }

  @Test
  internal fun `T13-5 represented-Class arguments must agree with the selected Class`() {
    val table =
        loadTypes(
            "CLASS Player1 : Anyone",
            "CLASS Player2 : Anyone",
            "ABSTRACT CLASS Token : Owned<Anyone>",
            "CLASS Player1Token : Token, Owned<Player1>",
            "CLASS Compatible<Class<T@Token>> { This: T@Token<Player1> }",
            "CLASS Conflicting<Class<T@Token>> { This: T@Token<Player2> }",
        )

    fun boundEffect(className: String): Effect {
      val klass = table.getClass(cn(className))
      val effect = klass.interpretTypeVariablesIn(klass.declaration.effects.single())
      val specialized = table.resolve(te("$className<Class<Player1Token>>"))
      val variable = effect.typeVariables.variables.single()
      return effect.typeVariables
          .bind(specialized.variableBindingsFrom(klass.defaultType, listOf(variable)))
          .transformEffect(effect)
    }

    boundEffect("Compatible").toString() shouldBe "This: Player1Token"
    shouldThrow<NarrowingException> { boundEffect("Conflicting") }
  }

  @Test
  internal fun `T13-5 specializing an outer variable preserves a nested local declaration`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Anyone",
            "ABSTRACT CLASS Resource<Anyone>",
            "CLASS Plant<Anyone> : Resource<Anyone>",
            "ABSTRACT CLASS Watcher<Class<ThatResource@Resource>> { " +
                "-X ThatResource@Resource<Victim@Anyone> BY Attacker@Player: " +
                "Resource<Victim@Anyone>, Resource<Attacker@Player> }",
        )
    val watcher = table.getClass(cn("Watcher"))
    val effect =
        table
            .recordTypeVariableScopes()
            .transformEffect(watcher.interpretTypeVariablesIn(watcher.declaration.effects.single()))
    val specialized = table.resolve(te("Watcher<Class<Plant>>"))

    effect.typeVariables
        .bind(specialized.variableBindingsFrom(watcher.defaultType, effect.typeVariables.variables))
        .transformEffect(effect)
        .toString() shouldBe
        "-X Plant<Victim@Anyone> BY Attacker@Player: " +
            "Resource<Victim@Anyone>, Resource<Attacker@Player>"
  }

  @Test
  internal fun `T13-5 an unchanged abstract value supplies nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<P@Person> { This: Token<P@Person> }",
        )
    val badge = table.getClass(cn("Badge"))
    val variable = badge.typeVariables.single()

    badge.defaultType.variableBindingsFrom(badge.defaultType, listOf(variable)) shouldBe emptyMap()
  }

  @Test
  internal fun `T13-5 a subclass that fixes the dependency does supply a value`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<P@Person> { This: Token<P@Person> }",
            "CLASS Leaf : Badge<Alice>",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()
    val leaf = table.getClass(cn("Leaf")).defaultType

    "${leaf.variableBindingsFrom(leaf, listOf(variable))[variable]}" shouldBe "Alice"
  }

  @Test
  internal fun `T13-5 both types must share a root class`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Badge<Person>",
            "ABSTRACT CLASS Middle : Badge",
        )
    val badge = table.getClass(cn("Badge"))
    val variable = badge.typeVariables.single()

    shouldThrowIae {
      table
          .getClass(cn("Middle"))
          .defaultType
          .variableBindingsFrom(badge.defaultType, listOf(variable))
    }
  }

  // T13-6 Explicit Effect variables

  @Test
  internal fun `T13-6 matching markers identify one Effect variable`() {
    val trade = effect("R@StandardResource: R@StandardResource")
    val variable = trade.typeVariables.variables.single()

    variable.name shouldBe "R"
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("R@StandardResource", "R@StandardResource")
  }

  @Test
  internal fun `T13-6 an anonymous marker identifies a variable`() {
    val trade = effect("@StandardResource: @StandardResource")

    trade.typeVariables.variables.single().name shouldBe null
    trade.toString() shouldBe "@StandardResource: @StandardResource"
  }

  @Test
  internal fun `T13-6 anonymous markers may identify variables with different bound Classes`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Anyone, Actor",
            "ABSTRACT CLASS Notice<Anyone>",
            "ABSTRACT CLASS Pair<Anyone, Player>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(parse("Notice<@Anyone> BY @Player: Pair<@Anyone, @Player>"))

    names(scoped.typeVariables) shouldContainExactly listOf("@Anyone", "@Player")
    scoped.typeVariables.variables.map { it.rootClass.className } shouldContainExactly
        listOf(cn("Anyone"), cn("Player"))
  }

  @Test
  internal fun `T13-6 a reference's bound Class must match its declaration`() {
    shouldThrow<PetSyntaxException> {
      parse<Effect>("@StandardResource: @Anyone")
    }
  }

  @Test
  internal fun `T13-6 unnamed Effect expressions do not declare a variable`() {
    effect("StandardResource: StandardResource").typeVariables.variables shouldBe listOf()
    effect("StandardResource: Plant").typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-6 a variable name may also be a Type name`() {
    names(effect("Plant@StandardResource: Plant@StandardResource").typeVariables) shouldBe
        listOf("Plant")
  }

  @Test
  internal fun `T13-6 anonymous and named variables of one bound Class cannot mix`() {
    val message =
        "anonymous type variable marker `@StandardResource` cannot share a scope with a named " +
            "variable of the same bound class"
    shouldThrow<PetSyntaxException> {
          effect(
              "(@StandardResource OR Other@StandardResource): " +
                  "@StandardResource, Other@StandardResource"
          )
        }
        .detail shouldBe message
    shouldThrow<PetSyntaxException> {
          parseClasses("ABSTRACT CLASS Holder<@StandardResource, Other@StandardResource>")
        }
        .detail shouldBe message
  }

  @Test
  internal fun `T13-6 all occurrences in one region join the same variable`() {
    val many = effect("R@StandardResource: R@StandardResource, R@StandardResource")

    many.typeVariables.variables.single().occurrences.size shouldBe 3
  }

  @Test
  internal fun `T13-6 the value chosen for a variable reaches every occurrence`() {
    val trade = effect("Production<Class<R@StandardResource>>: R@StandardResource")
    val variable = trade.typeVariables.variables.single()

    trade.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformEffect(trade)
        .toString() shouldBe "Production<Class<Plant>>: Plant"
  }

  @Test
  internal fun `T13-6 a represented-Class Effect variable accepts dependency arguments`() {
    val trade = effect("Production<Class<R@StandardResource>>: R@StandardResource<Player1>")
    val variable = trade.typeVariables.variables.single()

    trade.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformEffect(trade)
        .toString() shouldBe "Production<Class<Plant>>: Plant<Player1>"

    val acceptingDefaults = effect("Production<Class<R@StandardResource>>: R@StandardResource<>")
    val defaultedVariable = acceptingDefaults.typeVariables.variables.single()
    acceptingDefaults.typeVariables
        .bind(mapOf(defaultedVariable to resources.resolve(te("Plant"))))
        .transformEffect(acceptingDefaults)
        .toString() shouldBe "Production<Class<Plant>>: Plant<>"
  }

  // T13-7 Regions

  @Test
  internal fun `T13-7 an effect's regions are its trigger and its instruction`() {
    names(effect("R@StandardResource: R@StandardResource").typeVariables) shouldContainExactly
        listOf("R")
    shouldThrow<PetSyntaxException> {
      effect("R@StandardResource OR R@StandardResource: Ok")
    }
  }

  @Test
  internal fun `T13-7 an action names a choice shared by its cost and result`() {
    val action: Action =
        resources
            .recordTypeVariableScopes()
            .transformAction(parse("R@StandardResource -> R@StandardResource"))
    val lowered = action.toInstruction() as Then
    val variable = lowered.typeVariables.variables.single()

    names(action.typeVariables) shouldContainExactly listOf("R")
    lowered.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(lowered)
        .toString() shouldBe "-Plant! THEN Plant"

    val ordinary =
        resources
            .recordTypeVariableScopes()
            .transformAction(parse("StandardResource -> StandardResource"))
    ordinary.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-7 recording twice does not duplicate a named Action variable`() {
    val recorder = resources.recordTypeVariableScopes()
    val once = recorder.transformAction(parse("R@StandardResource -> R@StandardResource"))
    val twice = recorder.transformAction(once)

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  @Test
  internal fun `T13-7 an Action variable name may be a Type name`() {
    val action =
        resources
            .recordTypeVariableScopes()
            .transformAction(parse("Plant@StandardResource -> Plant@StandardResource"))

    names(action.typeVariables) shouldContainExactly listOf("Plant")
  }

  @Test
  internal fun `T13-7 a THEN sequence names a choice shared by its stages`() {
    val instruction: Instruction =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("R@StandardResource THEN R@StandardResource"))
    val then = instruction as Then
    val variable = then.typeVariables.variables.single()

    names(then.typeVariables) shouldContainExactly listOf("R")
    then.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(then)
        .toString() shouldBe "Plant THEN Plant"

    val ordinary =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("StandardResource THEN StandardResource")) as Then
    ordinary.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-7 recording twice does not duplicate a named THEN variable`() {
    val recorder = resources.recordTypeVariableScopes()
    val once = recorder.transformInstruction(parse("R@StandardResource THEN R@StandardResource"))
    val twice = recorder.transformInstruction(once) as Then

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  @Test
  internal fun `T13-7 a THEN marker has no authored declaration order and must cross stages`() {
    val shared =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("R@StandardResource THEN R@StandardResource")) as Then
    names(shared.typeVariables) shouldContainExactly listOf("R")

    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Duo<StandardResource, StandardResource>",
            "CLASS Coin",
        )
    shouldThrow<PetSyntaxException> {
      table
          .recordTypeVariableScopes()
          .transformInstruction(parse("Duo<R@StandardResource, R@StandardResource> THEN Coin"))
    }
  }

  @Test
  internal fun `T13-7 a THEN variable name may be a Type name`() {
    val then =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(parse("Plant@StandardResource THEN Plant@StandardResource"))
            as Then

    names(then.typeVariables) shouldContainExactly listOf("Plant")
  }

  @Test
  internal fun `T13-7 a transmutation names a destination choice used by its source`() {
    val transmute =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(
                parse(
                    "Receipt<Class<R@StandardResource>> FROM " +
                        "Production<Class<R@StandardResource>>"
                )
            ) as Instruction.Transmute
    val variable = transmute.typeVariables.variables.single()

    variable.name shouldBe "R"
    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("R@StandardResource", "R@StandardResource")
    transmute.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(transmute)
        .toString() shouldBe "Receipt<Class<Plant>> FROM Production<Class<Plant>>"
  }

  @Test
  internal fun `T13-7 a transmutation names a source choice used by its destination`() {
    val transmute =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(
                parse("StandardResource(NOT R@StandardResource) FROM R@StandardResource")
            ) as Instruction.Transmute
    val variable = transmute.typeVariables.variables.single()

    variable.name shouldBe "R"
    variable.declaration.region shouldBe 1
    transmute.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(transmute)
        .toString() shouldBe "StandardResource(NOT Plant) FROM Plant"
  }

  @Test
  internal fun `T13-7 full transmutation sides share only named variables`() {
    val transmute =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(
                parse(
                    "Production<Class<StandardResource>> FROM Production<Class<StandardResource>>"
                )
            ) as Instruction.Transmute

    transmute.typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `T13-3 a transmutation uses a class variable rather than hiding it`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource { CLASS A }",
            "ABSTRACT CLASS Source<Resource>",
            "ABSTRACT CLASS Destination<Resource>",
            "CLASS Converter<R@Resource> { " +
                "This: Destination<R@Resource> FROM Source<R@Resource> }",
        )
    val converter = table.getClass(cn("Converter"))
    val effect = converter.interpretTypeVariablesIn(converter.declaration.effects.single())
    val transmute = effect.instruction as Instruction.Transmute
    val variable = converter.typeVariables.single()

    effect.typeVariables.variables shouldBe converter.typeVariables
    transmute.typeVariables.variables shouldBe listOf()
    effect.typeVariables
        .bind(mapOf(variable to table.resolve(te("A"))))
        .transformEffect(effect)
        .toString() shouldBe "This: Destination<A> FROM Source<A>"
  }

  @Test
  internal fun `T13-7 a transmutation variable name may be a Type name`() {
    val transmute =
        resources
            .recordTypeVariableScopes()
            .transformInstruction(
                parse(
                    "Receipt<Class<Plant@StandardResource>> FROM " +
                        "Production<Class<Plant@StandardResource>>"
                )
            ) as Instruction.Transmute

    names(transmute.typeVariables) shouldContainExactly listOf("Plant")
  }

  @Test
  internal fun `T13-7 recording twice does not duplicate a named transmutation variable`() {
    val recorder = resources.recordTypeVariableScopes()
    val once =
        recorder.transformInstruction(
            parse(
                "Receipt<Class<R@StandardResource>> FROM " + "Production<Class<R@StandardResource>>"
            )
        )
    val twice = recorder.transformInstruction(once) as Instruction.Transmute

    names(twice.typeVariables) shouldContainExactly listOf("R")
  }

  // T13-8 Variable declaration sites

  @Test
  internal fun `T13-8 an unnamed EACH selector does not use a header variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Holder<StandardResource> { This: EACH StandardResource { StandardResource } }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.size shouldBe 1
  }

  @Test
  internal fun `T13-8 a selector variable name may be a Type name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Holder { This: EACH Holder@Person { Holder@Person } }",
        )

    table.getClass(cn("Holder")).declaration.effects.single().toString() shouldBe
        "This: EACH Holder@Person { Holder@Person }"
  }

  @Test
  internal fun `T13-8 a selector variable is distinct from an equal enclosing marker`() {
    val scoped =
        effect(
            "@StandardResource: " +
                "EACH @StandardResource { @StandardResource }, @StandardResource"
        )
    val outer = scoped.typeVariables.variables.single()
    val each = scoped.instruction.descendantsOfType<Instruction.Each>().single()
    val nestedReference = each.body.descendantsOfType<Expression>().single()

    outer.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("@StandardResource", "@StandardResource")
    scoped.typeVariables.variableAt(nestedReference) shouldBe null
    scoped.typeVariables
        .bind(mapOf(outer to resources.resolve(te("Steel"))))
        .transformEffect(scoped)
        .toString() shouldBe "Steel: EACH @StandardResource { @StandardResource }, Steel"
    scoped.typeVariables.expandNames().transformEffect(scoped).toString() shouldBe
        "StandardResource: EACH @StandardResource { @StandardResource }, StandardResource"
  }

  @Test
  internal fun `T13-8 a selector variable shadows an equal Class-header marker`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Resource {\nCLASS Plant\nCLASS Steel\n}",
            "ABSTRACT CLASS Holder<@Resource> { " +
                "This: EACH @Resource { @Resource }, @Resource }",
        )
    val holder = table.getClass(cn("Holder"))
    val variable = holder.typeVariables.single()
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())

    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("@Resource", "@Resource")
    effect.typeVariables
        .bind(mapOf(variable to table.resolve(te("Steel"))))
        .transformEffect(effect)
        .toString() shouldBe "This: EACH @Resource { @Resource }, Steel"
  }

  @Test
  internal fun `T13-3 a named class variable remains visible inside a THEN`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
            "CLASS Offer<P@Person> { This: Coin<P@Person> THEN Receipt<P@Person> }",
        )
    val offer = table.getClass(cn("Offer"))
    val classScoped = offer.interpretTypeVariablesIn(offer.declaration.effects.single())
    val classVariable = classScoped.typeVariables.variables.single()

    "${classVariable.declaration.expression}" shouldBe "P@Person"
    classVariable.occurrences.size shouldBe 3
    classScoped.typeVariables
        .bind(mapOf(classVariable to table.resolve(te("Alice"))))
        .transformEffect(classScoped)
        .toString() shouldBe "This: Coin<Alice> THEN Receipt<Alice>"
  }

  @Test
  internal fun `T13-8 an earlier gate occurrence belongs to that same first-stage choice`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Eligible<Person>",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse(
                    "This: (Eligible<Choice@Person>: Coin<Choice@Person>) THEN " +
                        "Receipt<Choice@Person>"
                )
            )
    val then = scoped.instruction as Then
    val choice = then.typeVariables.variables.single()

    choice.name.toString() shouldBe "Choice"
    choice.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("Choice@Person", "Choice@Person", "Choice@Person")
    (choice.usages.first().ordinal < choice.declaration.ordinal) shouldBe true
    then.typeVariables
        .bind(mapOf(choice to table.resolve(te("Alice"))))
        .transformEffect(scoped)
        .toString() shouldBe "This: Eligible<Alice>: Coin<Alice> THEN Receipt<Alice>"
  }

  // T13-9 Actor selectors

  private val actors =
      loadTypes(
          "ABSTRACT CLASS Player : Anyone, Actor {\nCLASS Player1\nCLASS Player2\n}",
          "ABSTRACT CLASS Heat : Owned<Anyone>",
          "ABSTRACT CLASS Notice<Anyone>",
      )

  private fun actorEffect(source: String) =
      actors.recordTypeVariableScopes().transformEffect(parse<Effect>(source))

  @Test
  internal fun `T13-9 an explicit trigger binds Me from ownership or Actor`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Anyone, Actor { CLASS Joe }",
            "ABSTRACT CLASS Plant : Owned<Anyone>",
            "ABSTRACT CLASS Notice",
            "ABSTRACT CLASS Token<Anyone>",
        )
    val joe = table.resolve(te("Joe"))

    fun boundEffect(source: String, bound: String): String {
      val effect = table.recordTypeVariableScopes().transformEffect(parse<Effect>(source))
      val variable = effect.typeVariables.variables.single()
      variable.rootClass.className shouldBe cn(bound)
      return effect.typeVariables.bind(mapOf(variable to joe)).transformEffect(effect).toString()
    }

    boundEffect("Plant<Me@Anyone>: Token<Me@Anyone>", "Anyone") shouldBe "Plant<Joe>: Token<Joe>"
    boundEffect("Notice BY Me@Anyone: Token<Me@Anyone>", "Anyone") shouldBe
        "Notice BY Joe: Token<Joe>"
    boundEffect("Plant<Me@Player>: Token<Me@Player>", "Player") shouldBe "Plant<Joe>: Token<Joe>"
    boundEffect("Notice BY Me@Player: Token<Me@Player>", "Player") shouldBe
        "Notice BY Joe: Token<Joe>"
  }

  @Test
  internal fun `T13-9 an unnamed actor selector is only a filter`() {
    names(actorEffect("Heat BY Player: Ok").typeVariables) shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-9 an unnamed actor filter does not bind the instruction`() {
    val bound = actorEffect("Heat BY Player: Notice<Player>")

    names(bound.typeVariables) shouldContainExactly listOf()
    bound.toString() shouldBe "Heat BY Player: Notice<Player>"
  }

  @Test
  internal fun `T13-9 Anyone and a refined selector are filters, not binders`() {
    names(actorEffect("Heat BY Actor: Ok").typeVariables) shouldContainExactly listOf()
    names(actorEffect("Heat BY Player(NOT Anyone): Ok").typeVariables) shouldContainExactly listOf()
  }

  @Test
  internal fun `T13-9 an exclusion may use the actor variable, and is tested after binding`() {
    val bound =
        actorEffect(
            "Notice<Other@Anyone(NOT ActingPlayer@Player)> BY ActingPlayer@Player: " +
                "Heat<Other@Anyone>"
        )
    val actor = bound.typeVariables.variables.single { it.name == "ActingPlayer" }
    val event = bound.typeVariables.variables.single { it.name == "Other" }

    actor.occurrences.size shouldBe 3
    event.occurrences.size shouldBe 2
    bound.typeVariables
        .bind(mapOf(actor to actors.resolve(te("Player1"))))
        .transformEffect(bound)
        .toString() shouldBe "Notice<Other@Anyone(NOT Player1)> BY Player1: Heat<Other@Anyone>"
  }

  @Test
  internal fun `T13-9 a difference occurrence captures its candidate from its own domain`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Anyone, Actor { CLASS Player1 }",
            "CLASS Passive : Anyone",
            "ABSTRACT CLASS Resource<Anyone>",
            "ABSTRACT CLASS Notice<Anyone>",
        )
    val bound =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>(
                    "Resource<Other@Anyone(NOT ActingPlayer@Player)> " +
                        "BY ActingPlayer@Player: Notice<Other@Anyone>"
                )
            )
    val actor = bound.typeVariables.variables.single { it.name == "ActingPlayer" }
    val afterActor =
        bound.typeVariables
            .bind(mapOf(actor to table.resolve(te("Player1"))))
            .transformEffect(bound)
    val event = afterActor.typeVariables.variables.single()

    event.bound shouldBe table.resolve(te("Anyone"))
    "${afterActor.typeVariables.expressionOf(event.declaration)}" shouldBe
        "Other@Anyone(NOT Player1)"

    val captured =
        afterActor.typeVariables.bindingsFrom(
            afterActor.trigger.descendantsOfType<Expression>().first(),
            table.resolve(parse("Resource<Anyone(NOT Player1)>")),
            table.resolve(parse("Resource<Passive>")),
        )

    "${captured[event]}" shouldBe "Passive"
    afterActor.typeVariables.bind(captured).transformEffect(afterActor).toString() shouldBe
        "Resource<Passive> BY Player1: Notice<Passive>"
  }

  // T13-10 Binding

  @Test
  internal fun `T13-10 binding replaces only the recorded occurrences`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource {\nCLASS Plant\nCLASS Steel\n}",
            "ABSTRACT CLASS Notice<StandardResource>",
        )
    val bound =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>(
                    "R@StandardResource: Notice<R@StandardResource>, StandardResource, Steel"
                )
            )
    val variable = bound.typeVariables.variables.single()

    bound.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(bound)
        .toString() shouldBe "Plant: Notice<Plant>, StandardResource, Steel"
  }

  @Test
  internal fun `T13-10 selecting a represented class settles its choice with component dependencies open`() {
    val scoped = effect("Production<Class<R@StandardResource>>: R@StandardResource<Player1>")
    val variable = scoped.typeVariables.variables.single()
    val plant = resources.resolve(te("Plant"))
    plant.abstract shouldBe true

    val bound = scoped.typeVariables.bind(mapOf(variable to plant)).transformEffect(scoped)

    bound.toString() shouldBe "Production<Class<Plant>>: Plant<Player1>"
    bound.typeVariables.isEmpty shouldBe true
  }

  @Test
  internal fun `T13-10 a class literal projects the selected component type to its root class`() {
    val scoped = effect("R@StandardResource: Receipt<Class<R@StandardResource>>")
    val variable = scoped.typeVariables.variables.single()

    val bound =
        scoped.typeVariables
            .bind(mapOf(variable to resources.resolve(te("Plant<Player1>"))))
            .transformEffect(scoped)

    bound.toString() shouldBe "Plant<Player1>: Receipt<Class<Plant>>"
    bound.typeVariables.isEmpty shouldBe true
  }

  @Test
  internal fun `T13-11 represented-class captures agree while occurrence arguments differ`() {
    val table =
        loadTypes(
            "CLASS Player1 : Anyone",
            "CLASS Player2 : Anyone",
            "ABSTRACT CLASS Resource : Owned<Anyone> { CLASS Plant }",
            "CLASS Pair<Class<Resource>, Resource>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>("Pair<Class<R@Resource>, R@Resource<Player1>>: R@Resource<Player2>")
            )
    val captured =
        scoped.typeVariables.bindingsFrom(
            (scoped.trigger as Effect.Trigger.OnGainOf).expression,
            table.resolve(te("Pair<Class<Resource>, Resource<Player1>>")),
            table.resolve(te("Pair<Class<Plant>, Plant<Player1>>")),
        )

    scoped.typeVariables.bind(captured).transformEffect(scoped).toString() shouldBe
        "Pair<Class<Plant>, Plant<Player1>>: Plant<Player2>"
  }

  @Test
  internal fun `T13-10 binding follows marked occurrences through copied syntax`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource {\nCLASS Plant\nCLASS Steel\n}",
            "ABSTRACT CLASS Notice<StandardResource>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>("R@StandardResource: Notice<R@StandardResource>, StandardResource")
            )
    val copier =
        object : PetTransformer() {
          override fun transformNode(node: PetNode): PetNode {
            val transformed = transformChildren(node)
            return if (transformed is Expression) transformed.copy() else transformed
          }
        }
    val copied = copier.transformEffect(scoped)
    val variable = copied.typeVariables.variables.single()

    copied.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(copied)
        .toString() shouldBe "Plant: Notice<Plant>, StandardResource"
  }

  @Test
  internal fun `T13-10 binding omits arguments fixed by the chosen subclass`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Kind {\nCLASS Fixed\nCLASS Other\n}",
            "ABSTRACT CLASS Box<Kind>",
            "CLASS FixedBox : Box<Fixed>",
            "ABSTRACT CLASS Notice<Box<Kind>>",
            "ABSTRACT CLASS Holder<Class<B@Box>, K@Kind> { " + "This: Notice<B@Box<K@Kind>> }",
        )
    val holder = table.getClass(cn("Holder"))
    val bound = holder.interpretTypeVariablesIn(holder.declaration.effects.single())
    val box = bound.typeVariables.variables.first { it.bound.rootClass.className == cn("Box") }
    val kind = bound.typeVariables.variables.first { it.bound.rootClass.className == cn("Kind") }

    bound.typeVariables
        .bind(
            mapOf(
                box to table.resolve(te("FixedBox")),
                kind to table.resolve(te("Fixed")),
            )
        )
        .transformEffect(bound)
        .toString() shouldBe "This: Notice<FixedBox>"

    shouldThrow<NarrowingException> {
      bound.typeVariables.bind(
          mapOf(box to table.resolve(te("FixedBox")), kind to table.resolve(te("Other")))
      )
    }
  }

  @Test
  internal fun `T13-10 a binding must satisfy every recorded occurrence`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "CLASS Hand",
            "ABSTRACT CLASS Notice<StandardResource>",
        )
    val bound =
        table
            .recordTypeVariableScopes()
            .transformEffect(parse<Effect>("R@StandardResource: Notice<R@StandardResource>"))
    val variable = bound.typeVariables.variables.single()

    shouldThrow<NarrowingException> {
      bound.typeVariables.bind(mapOf(variable to table.resolve(te("Hand"))))
    }
  }

  @Test
  internal fun `T13-10 a refined declaration is evaluated once, while its value is captured`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Marker<StandardResource>",
            "ABSTRACT CLASS Token<StandardResource>",
        )
    val bound =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>("R@StandardResource(HAS Marker): Token<R@StandardResource>")
            )
    val variable = bound.typeVariables.variables.single()
    val world = RecordingWorld(answer = true)

    table.resolve(te("Plant")).narrows(variable.bound, world) shouldBe true
    world.questions.size shouldBe 1

    bound.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(bound)
        .toString() shouldBe "Plant: Token<Plant>"
    world.questions.size shouldBe 1
  }

  @Test
  internal fun `T13-10 nested selection predicates are consumed along their dependency paths`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Place { CLASS Spot }",
            "CLASS Marker<Place>",
            "CLASS Box<Place>",
            "CLASS Outer<Box<Place>>",
            "CLASS Notice<Outer<Box<Place>>>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(parse<Effect>("@Outer<Box<Place(HAS Marker)>>: Notice<@Outer>"))
    val variable = scoped.typeVariables.variables.single()
    val chosen = table.resolve(te("Outer<Box<Spot>>"))
    val before = RecordingWorld(answer = true)

    chosen.narrows(variable.bound, before) shouldBe true
    before.questions shouldContainExactly listOf("Marker<Spot>")

    val bound = scoped.typeVariables.bind(mapOf(variable to chosen)).transformEffect(scoped)
    bound.toString() shouldBe "Outer<Box<Spot>>: Notice<Outer<Box<Spot>>>"

    val after = RecordingWorld(answer = false)
    chosen.narrows(
        table.resolve(bound.trigger.descendantsOfType<Expression>().first()),
        after,
    ) shouldBe true
    after.questions.size shouldBe 0
  }

  @Test
  internal fun `T13-10 consuming predicates preserves other paths and structural exclusions`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Place {\nCLASS First\nCLASS Second\nCLASS Forbidden\n}",
            "CLASS Marker<Place>",
            "CLASS OtherMarker<Place>",
            "CLASS Box<Place, Place>",
            "CLASS Notice<Box<Place, Place>>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>("@Box<Place(HAS Marker), Place(NOT Forbidden)>: Notice<@Box>")
            )
    val variable = scoped.typeVariables.variables.single()

    scoped.typeVariables
        .bind(
            mapOf(variable to table.resolve(te("Box<First(HAS OtherMarker), Second(HAS Marker)>")))
        )
        .transformEffect(scoped)
        .toString() shouldBe
        "Box<First(HAS OtherMarker), Second(HAS Marker)>: Notice<Box<First(HAS OtherMarker), Second(HAS Marker)>>"

    val forbidden = table.resolve(te("Box<First, Forbidden>"))
    val excluded = scoped.typeVariables.bind(mapOf(variable to forbidden)).transformEffect(scoped)
    forbidden.narrows(
        table.resolve(excluded.trigger.descendantsOfType<Expression>().first()),
        RecordingWorld(),
    ) shouldBe false
  }

  @Test
  internal fun `T13-10 a partial THEN binding retains unchecked nested selection predicates`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Place {\nCLASS First\nCLASS Second\n}",
            "CLASS Marker<Place>",
            "CLASS Box<Place, Place>",
            "CLASS Notice<Box<Place, Place>>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformInstruction(
                parse<Instruction>("@Box<Place(HAS Marker), Place>! THEN Notice<@Box>!")
            ) as Then
    val world = RecordingWorld(answer = false)
    val info =
        object : TypeInfo by world {
          override val classTable: ClassTable = table

          override fun isAbstract(e: Expression): Boolean = table.resolve(e).abstract

          override fun ensureNarrows(wide: Expression, narrow: Expression) {
            table.resolve(narrow).ensureNarrows(table.resolve(wide), this)
          }

          override fun ensureSelectionNarrows(wide: Expression, narrow: Expression) {
            table.resolve(narrow).ensureSelectionNarrows(table.resolve(wide), this)
          }
        }

    val bound =
        scoped.typeVariables
            .bind(
                mapOf(
                    scoped.typeVariables.variables.single() to
                        table.resolve(te("Box<Place(HAS Marker), First>"))
                )
            )
            .transformInstruction(scoped) as Then
    bound.toString() shouldBe
        "Box<Place(HAS Marker), First>! THEN Notice<Box<Place(HAS Marker), First>>!"
    world.questions.size shouldBe 0

    parse<Instruction>("Notice<Box<Second, First>>!").narrows(bound.continuation, info) shouldBe
        false
    world.questions shouldContainExactly listOf("Marker<Second>")
  }

  @Test
  internal fun `T13-10 an abstract binding retains unchecked root selection predicates`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Place { CLASS Spot }",
            "CLASS Marker<Place>",
            "CLASS Notice<Place>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(parse<Effect>("@Place(HAS Marker): Notice<@Place>"))
    val variable = scoped.typeVariables.variables.single()
    val chosen = table.resolve(te("Place(HAS Marker)"))
    val world = RecordingWorld(answer = false)

    chosen.narrows(variable.bound, world) shouldBe true
    world.questions.size shouldBe 0

    scoped.typeVariables.bind(mapOf(variable to chosen)).transformEffect(scoped).toString() shouldBe
        "Place(HAS Marker): Notice<Place(HAS Marker)>"
  }

  @Test
  internal fun `T13-10 partial binding consumes only checked dependency predicates`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Place { CLASS First }",
            "CLASS Marker<Place>",
            "CLASS Box<Place, Place>",
            "CLASS Notice<Box<Place, Place>>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(
                parse<Effect>("@Box<Place(HAS Marker), Place(HAS Marker)>: Notice<@Box>")
            )
    val variable = scoped.typeVariables.variables.single()
    val chosen = table.resolve(te("Box<First, Place(HAS Marker)>"))
    val world = RecordingWorld(answer = true)

    chosen.narrows(variable.bound, world) shouldBe true
    world.questions shouldContainExactly listOf("Marker<First>")

    scoped.typeVariables.bind(mapOf(variable to chosen)).transformEffect(scoped).toString() shouldBe
        "Box<First, Place(HAS Marker)>: Notice<Box<First, Place(HAS Marker)>>"
  }

  @Test
  internal fun `T13-10 an abstract choice without a predicate retains its selection constraint`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Place {
              CLASS First
              ABSTRACT CLASS Sub { CLASS Spot }
            }
            """,
            "CLASS Marker<Place>",
            "CLASS Box<Place, Place>",
            "CLASS Notice<Box<Place, Place>>",
        )
    val scoped =
        table
            .recordTypeVariableScopes()
            .transformEffect(parse<Effect>("@Box<Place(HAS Marker), Place>: Notice<@Box>"))
    val variable = scoped.typeVariables.variables.single()
    val chosen = table.resolve(te("Box<Sub, First>"))
    val before = RecordingWorld(answer = true)

    chosen.narrows(variable.bound, before) shouldBe true
    before.questions shouldContainExactly listOf("Marker<Sub>")

    val bound = scoped.typeVariables.bind(mapOf(variable to chosen)).transformEffect(scoped)
    bound.toString() shouldBe "Box<Sub(HAS Marker), First>: Notice<Box<Sub(HAS Marker), First>>"

    val after = RecordingWorld(answer = false)
    table
        .resolve(te("Notice<Box<Spot, First>>"))
        .narrows(
            table.resolve(bound.instruction.descendantsOfType<Expression>().first()),
            after,
        ) shouldBe false
    after.questions shouldContainExactly listOf("Marker<Spot>")
  }

  @Test
  internal fun `T13-10 a partial header binding preserves a later event's specific type`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Resource {
              ABSTRACT CLASS StandardResource { CLASS Steel }
              CLASS Microbe
            }
            CLASS ResourceReward<Resource>
            CLASS ResourceWatcher<Watched@Resource> {
              Watched@Resource: ResourceReward<Watched@Resource>
            }
            """
        )
    val watcher = table.getClass(cn("ResourceWatcher"))
    val effect = watcher.interpretTypeVariablesIn(watcher.declaration.effects.single())
    val partial = table.resolve(te("ResourceWatcher<StandardResource>"))
    val specialized =
        effect.typeVariables
            .bind(partial.variableBindingsFrom(watcher.defaultType, effect.typeVariables.variables))
            .transformEffect(effect)
    val trigger = (specialized.trigger as Effect.Trigger.OnGainOf).expression
    val triggerType = table.resolve(trigger)

    triggerType shouldBe table.resolve(te("StandardResource"))
    table.resolve(te("Microbe")).narrows(triggerType, TableWorld(table)) shouldBe false

    val captures =
        specialized.typeVariables.bindingsFrom(trigger, triggerType, table.resolve(te("Steel")))
    specialized.typeVariables.bind(captures).transformEffect(specialized) shouldBe
        parse<Effect>("Steel: ResourceReward<Steel>")
  }

  // T13-11 Capture follows dependency paths

  @Test
  internal fun `T13-11 a scope can capture values from a specialized expression`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Container<Component>",
        )
    val container = parse<Expression>("Container<Box<Person>>")
    val person =
        container.arguments
            .single()
            .arguments
            .single()
            .copy(typeVariableName = Declaration("P", cn("Person")))
    val authored =
        container.copy(
            arguments = listOf(container.arguments.single().copy(arguments = listOf(person)))
        )
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            markedDeclarations = listOf(person),
        )

    scope
        .bindingsFrom(
            authored,
            table.resolve(authored),
            table.resolve(parse("Container<Box<Alice>>")),
        )
        .map { (variable, value) -> "$variable=$value" } shouldContainExactly listOf("P=Alice")
  }

  @Test
  internal fun `T13-11 capture follows the represented Class type`() {
    val table = loadTypes("ABSTRACT CLASS Person { CLASS Alice }")
    val classExpression = parse<Expression>("Class<Person>")
    val person =
        classExpression.arguments.single().copy(typeVariableName = Declaration("P", cn("Person")))
    val authored = classExpression.copy(arguments = listOf(person))
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            markedDeclarations = listOf(person),
        )

    scope
        .bindingsFrom(
            authored,
            table.resolve(authored),
            table.resolve(parse("Class<Alice>")),
        )
        .map { (variable, value) -> "$variable=$value" } shouldContainExactly listOf("P=Alice")
  }

  @Test
  internal fun `T13-11 capture follows dependency paths, so a mismatched candidate captures nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "CLASS Hand",
            "ABSTRACT CLASS Container<Component>",
        )
    val container = parse<Expression>("Container<Box<Person>>")
    val person =
        container.arguments
            .single()
            .arguments
            .single()
            .copy(typeVariableName = Declaration("P", cn("Person")))
    val authored =
        container.copy(
            arguments = listOf(container.arguments.single().copy(arguments = listOf(person)))
        )
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            markedDeclarations = listOf(person),
        )

    scope.bindingsFrom(
        authored,
        table.resolve(authored),
        table.resolve(parse("Container<Hand>")),
    ) shouldBe emptyMap()
  }

  @Test
  internal fun `T13-11 capture follows only the selected dependency occurrence`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person {\nCLASS Alice\nCLASS Bob\n}",
            "ABSTRACT CLASS Pair<Person, Person>",
        )
    val pair = parse<Expression>("Pair<Person, Person>")
    val person = pair.arguments.first().copy(typeVariableName = Declaration("P", cn("Person")))
    val authored = pair.copy(arguments = listOf(person, pair.arguments.last()))
    val scope =
        TypeVariableScope.fromDeclarations(
            listOf(authored),
            table,
            markedDeclarations = listOf(person),
        )

    scope
        .bindingsFrom(
            authored,
            table.resolve(authored),
            table.resolve(parse("Pair<Alice, Bob>")),
        )
        .map { (variable, value) -> "$variable=$value" } shouldContainExactly listOf("P=Alice")
  }

  @Test
  internal fun nestedSelectorReferencesSurviveOrdinarySettlementExpansion() {
    val each =
        parse<InstructionTree>("EACH P@Person { A@Wrapper<Base<P@Person>> FROM A@Wrapper }")
            as Instruction.Each
    val bound = each.bodyFor(cn("Alice").expression) as Instruction.Transmute
    bound.gaining.expression.arguments.single().arguments.single().className shouldBe cn("Alice")
    bound.removing.expression.arguments.single().arguments.single().className shouldBe cn("Alice")
    parse<InstructionTree>(each.toString()) shouldBe each
  }
}
