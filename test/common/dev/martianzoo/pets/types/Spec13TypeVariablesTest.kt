package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Then
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 13 of `docs/type-system-spec.md`: type variables. */
internal class Spec13TypeVariablesTest {

  private val resources =
      loadTypes(
          "CLASS Player1 : Owner",
          "ABSTRACT CLASS StandardResource : Owned<Owner> { CLASS Plant, Steel }",
          "ABSTRACT CLASS Production<Class<StandardResource>> : Owned<Owner>",
          "ABSTRACT CLASS Receipt<Class<StandardResource>>",
      )

  private fun effect(source: String): Effect =
      resources.inferTypeVariables().transformEffect(parse(source))

  private fun names(scope: TypeVariableScope) =
      scope.variables.map { "${it.declaration.expression}" }

  // 13-1 A variable is a kind of type

  @Test
  internal fun `13-1 a variable is a Type whose structural meaning is its bound`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person> { This: Token<Person> }",
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
    "${variable.expression}" shouldBe "Person"
  }

  @Test
  internal fun `13-1 every occurrence is a Type view of the same variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person> { This: Token<Person> }",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()

    variable.occurrences shouldBe listOf(variable.declaration) + variable.usages
    variable.declaration.typeVariable shouldBe variable
    variable.usages.single().typeVariable shouldBe variable
    variable.occurrences.map { "${it.expression}" } shouldContainExactly listOf("Person", "Person")
    variable.occurrences.map { it.ordinal } shouldBe listOf(0, 1)
  }

  // 13-2 Class-header variables

  @Test
  internal fun `13-2 each eligible abstract header expression declares one variable`() {
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
  internal fun `13-2 a concrete or This header expression declares nothing`() {
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
  internal fun `13-2 occurrences that reach one dependency path are one variable`() {
    // `Cardbound<CardFront<Player>> : Owned<Player>`: the card's owner is the component's owner.
    val cards =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner>",
            "ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>",
        )

    cards.getClass(cn("Cardbound")).typeVariables.map { "$it" } shouldContainExactly
        listOf("CardFront<Owner>", "Owner")
    cards
        .getClass(cn("Cardbound"))
        .isEqualityConstrainedDependency(Dependency.Key(cn("Owned"), 0)) shouldBe true
  }

  @Test
  internal fun `13-2 two header roots spelled alike stay independent`() {
    val table =
        loadTypes("ABSTRACT CLASS Person { CLASS Alice }", "ABSTRACT CLASS Duo<Person, Person>")

    table.getClass(cn("Duo")).typeVariables.map { "$it" } shouldContainExactly
        listOf("Person", "Person")
    table.getClass(cn("Duo")).typeVariables.distinct().size shouldBe 2
  }

  // 13-3 Uses in the class body

  @Test
  internal fun `13-3 header text repeated in the class's own effects is a use`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Holder<Box<Person>> { This: Token<Box> }",
        )
    val holder = table.getClass(cn("Holder"))
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())

    names(effect.typeVariables) shouldContainExactly listOf("Box<Person>")
  }

  @Test
  internal fun `13-3 a simple header variable may head an occurrence that adds arguments`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Holder<Person> { This: Box<Person> }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.map { "${it.expression}" } shouldContainExactly listOf("Person", "Person")
  }

  @Test
  internal fun `13-3 an effect use that could name two header variables is rejected`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Ambiguous<Person, Person> { This: Person }",
        )

    shouldThrow<PetException> { table.getClass(cn("Ambiguous")).typeVariables }
  }

  // 13-4 Inheritance

  @Test
  internal fun `13-4 a subclass does not redeclare an inherited variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person> { This: Token<Person> }",
            "ABSTRACT CLASS Middle : Badge",
            "CLASS Leaf : Badge<Alice>",
        )

    table.getClass(cn("Badge")).typeVariables.map { "$it" } shouldContainExactly listOf("Person")
    table.getClass(cn("Middle")).typeVariables.map { "$it" } shouldContainExactly listOf()
    table.getClass(cn("Leaf")).typeVariables.map { "$it" } shouldContainExactly listOf()
  }

  // 13-5 Capturing values

  @Test
  internal fun `13-5 specializing a component type supplies its header variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Holder<Box<Person>> { This: Box<Person> }",
        )
    val holder = table.getClass(cn("Holder"))
    val effect = holder.interpretTypeVariablesIn(holder.declaration.effects.single())
    val bindings =
        table
            .resolve(te("Holder<Box<Alice>>"))
            .variableBindingsFrom(holder.defaultType, effect.typeVariables.variables)

    bindings.map { (variable, value) -> "$variable=$value" } shouldContainExactly
        listOf("Box<Person>=Box<Alice>", "Person=Alice")
    effect.typeVariables.bind(bindings).transformEffect(effect).toString() shouldBe
        "This: Box<Alice>"
  }

  @Test
  internal fun `13-5 an unchanged abstract value supplies nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person> { This: Token<Person> }",
        )
    val badge = table.getClass(cn("Badge"))
    val variable = badge.typeVariables.single()

    badge.defaultType.variableBindingsFrom(badge.defaultType, listOf(variable)) shouldBe emptyMap()
  }

  @Test
  internal fun `13-5 a subclass that fixes the dependency does supply a value`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Token<Person>",
            "ABSTRACT CLASS Badge<Person> { This: Token<Person> }",
            "CLASS Leaf : Badge<Alice>",
        )
    val variable = table.getClass(cn("Badge")).typeVariables.single()
    val leaf = table.getClass(cn("Leaf")).defaultType

    "${leaf.variableBindingsFrom(leaf, listOf(variable))[variable]}" shouldBe "Alice"
  }

  @Test
  internal fun `13-5 both types must share a root class`() {
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

  // 13-6 Inferred variables

  @Test
  internal fun `13-6 one spelling repeated across two choice regions declares one variable`() {
    val trade = effect("StandardResource: StandardResource")
    val variable = trade.typeVariables.variables.single()

    variable.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("StandardResource", "StandardResource")
  }

  @Test
  internal fun `13-6 an expression appearing in only one region declares nothing`() {
    effect("StandardResource: Plant").typeVariables.variables shouldBe listOf()
    effect("StandardResource: Ok").typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `13-6 all occurrences in one region join the same variable`() {
    val many = effect("StandardResource: StandardResource, StandardResource")

    many.typeVariables.variables.single().occurrences.size shouldBe 3
  }

  @Test
  internal fun `13-6 the value chosen for a variable reaches every occurrence`() {
    val trade = effect("Production<Class<StandardResource>>: StandardResource")
    val variable = trade.typeVariables.variables.single()

    trade.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformEffect(trade)
        .toString() shouldBe "Production<Class<Plant>>: Plant"
  }

  // 13-7 Regions

  @Test
  internal fun `13-7 an effect's regions are its trigger and its instruction`() {
    names(effect("StandardResource: StandardResource").typeVariables) shouldContainExactly
        listOf("StandardResource")
  }

  @Test
  internal fun `13-7 an action's regions are its cost and its result`() {
    val action: Action =
        resources
            .inferTypeVariables()
            .transformAction(parse("StandardResource -> StandardResource"))
    val lowered = action.toInstruction() as Then
    val variable = lowered.typeVariables.variables.single()

    lowered.typeVariables
        .bind(mapOf(variable to resources.resolve(te("Plant"))))
        .transformInstruction(lowered)
        .toString() shouldBe "-Plant! THEN Plant"
  }

  @Test
  internal fun `13-7 a THEN sequence's regions are its stages`() {
    val instruction: Instruction =
        resources
            .inferTypeVariables()
            .transformInstruction(parse("StandardResource THEN StandardResource"))

    names((instruction as Then).typeVariables) shouldContainExactly listOf("StandardResource")
  }

  @Test
  internal fun `13-7 a transmutation's regions are its two roles, minus the roots themselves`() {
    val instruction =
        resources
            .inferTypeVariables()
            .transformInstruction(
                parse(
                    "Production<Class<StandardResource>> FROM Production<Class<StandardResource>>"
                )
            )
    val transmute = instruction as Instruction.Transmute

    // The whole gained and removed roots may deliberately differ, so only what is inside counts.
    names(transmute.typeVariables) shouldContainExactly listOf("Class<StandardResource>")
  }

  // 13-8 What does not declare a variable

  @Test
  internal fun `13-8 occurrences confined to requirements do not declare`() {
    effect("StandardResource IF StandardResource: Ok").typeVariables.variables shouldBe listOf()
  }

  @Test
  internal fun `13-8 the expression a metric counts directly does not declare`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "CLASS Marker",
        )

    table
        .inferTypeVariables()
        .transformEffect(parse<Effect>("StandardResource: Marker / StandardResource"))
        .typeVariables
        .variables shouldBe listOf()
  }

  @Test
  internal fun `13-8 recognition prefers the largest repeated expression`() {
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS CardFront : Owned<Owner>",
            "ABSTRACT CLASS Notice<CardFront>",
        )

    // `CardFront<Owner>` repeats, so the nested `Owner` text does not declare its own variable.
    names(
        table
            .inferTypeVariables()
            .transformEffect(parse<Effect>("CardFront<Owner>: Notice<CardFront<Owner>>"))
            .typeVariables
    ) shouldContainExactly listOf("CardFront<Owner>")
  }

  @Test
  internal fun `13-8 the authored spelling is the variable's surface name`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "ABSTRACT CLASS Tile<Area>",
            "ABSTRACT CLASS Notice<Tile>",
        )

    // `Tile` and `Tile<Area>` resolve alike but are different authored names.
    table
        .inferTypeVariables()
        .transformEffect(parse<Effect>("Tile: Notice<Tile<Area>>"))
        .typeVariables
        .variables shouldBe listOf()
  }

  @Test
  internal fun `13-8 an EACH selector declares its own variable, never a header one`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Holder<StandardResource> { This: EACH StandardResource { StandardResource } }",
        )
    val variable = table.getClass(cn("Holder")).typeVariables.single()

    variable.occurrences.size shouldBe 1
  }

  @Test
  internal fun `13-8 a first-stage dependency choice outranks a matching class variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
            "CLASS Offer<Person> { This: Coin<Person> THEN Receipt<Person> }",
        )
    val offer = table.getClass(cn("Offer"))
    val classScoped = offer.interpretTypeVariablesIn(offer.declaration.effects.single())
    val inferred = table.inferTypeVariables().transformEffect(classScoped)
    val choice = (inferred.instruction as Then).typeVariables.variables.single()

    classScoped.typeVariables.isEmpty shouldBe true
    "${choice.declaration.expression}" shouldBe "Person"
    (inferred.instruction as Then)
        .typeVariables
        .bind(mapOf(choice to table.resolve(te("Alice"))))
        .transformEffect(inferred)
        .toString() shouldBe "This: Coin<Alice> THEN Receipt<Alice>"
  }

  @Test
  internal fun `13-8 an earlier gate occurrence belongs to that same first-stage choice`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Eligible<Person>",
            "ABSTRACT CLASS Coin<Person>",
            "ABSTRACT CLASS Receipt<Person>",
            "CLASS Offer<Person> { This: (Eligible<Person>: Coin<Person>) THEN Receipt<Person> }",
        )
    val offer = table.getClass(cn("Offer"))
    val inferred =
        table
            .inferTypeVariables()
            .transformEffect(offer.interpretTypeVariablesIn(offer.declaration.effects.single()))
    val then = inferred.instruction as Then
    val choice = then.typeVariables.variables.single()

    choice.occurrences.map { "${it.expression}" } shouldContainExactly
        listOf("Person", "Person", "Person")
    then.typeVariables
        .bind(mapOf(choice to table.resolve(te("Alice"))))
        .transformEffect(inferred)
        .toString() shouldBe "This: Eligible<Alice>: Coin<Alice> THEN Receipt<Alice>"
  }

  @Test
  internal fun `13-8 authored argument order does not make one enclosing variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Duo<Area, Person>",
            "ABSTRACT CLASS Notice<Component>",
            "ABSTRACT CLASS Token<Component>",
        )

    // `Duo<Area, Person>` and `Duo<Person, Area>` are different authored names, so the shared
    // variables are the two inner ones.
    names(
            table
                .inferTypeVariables()
                .transformEffect(
                    parse<Effect>("Notice<Duo<Area, Person>>: Token<Duo<Person, Area>>")
                )
                .typeVariables
        )
        .toSet() shouldBe setOf("Area", "Person")
  }

  @Test
  internal fun `13-8 identical nested bounds in sibling header branches stay independent`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice, Bob }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Pair<Box<Person>, Box<Person>>",
            "ABSTRACT CLASS Holder<Pair<Box<Person>, Box<Person>>>",
        )

    table.resolve(te("Holder<Pair<Box<Alice>, Box<Bob>>>")).expressionFull shouldBe
        te("Holder<Pair<Box<Alice>, Box<Bob>>>")
  }

  // 13-9 Actor selectors

  private val actors =
      loadTypes(
          "ABSTRACT CLASS Player : Owner, Actor { CLASS Player1, Player2 }",
          "ABSTRACT CLASS Heat : Owned<Owner>",
          "ABSTRACT CLASS Notice<Owner>",
      )

  private fun actorEffect(source: String) =
      actors.inferTypeVariables().transformEffect(parse<Effect>(source))

  @Test
  internal fun `13-9 a simple abstract actor selector binds even with no repetition`() {
    val bound = actorEffect("Heat BY Player: Ok")

    names(bound.typeVariables) shouldContainExactly listOf("Player")
    bound.typeVariables.variables.single().occurrences.size shouldBe 1
  }

  @Test
  internal fun `13-9 Anyone and a refined selector are filters, not binders`() {
    names(actorEffect("Heat BY Anyone: Ok").typeVariables) shouldContainExactly listOf()
    names(actorEffect("Heat BY Player(NOT Owner): Ok").typeVariables) shouldContainExactly listOf()
  }

  @Test
  internal fun `13-9 an exclusion may use the actor variable, and is tested after binding`() {
    val bound = actorEffect("Notice<Owner(NOT Player)> BY Player: Heat<Owner(NOT Player)>")
    val actor = bound.typeVariables.variables.single { "${it.declaration.expression}" == "Player" }
    val event =
        bound.typeVariables.variables.single {
          "${it.declaration.expression}" == "Owner(NOT Player)"
        }

    actor.occurrences.size shouldBe 3
    event.occurrences.size shouldBe 2
    bound.typeVariables
        .bind(mapOf(actor to actors.resolve(te("Player1"))))
        .transformEffect(bound)
        .toString() shouldBe "Notice<Owner(NOT Player1)> BY Player1: Heat<Owner(NOT Player1)>"
  }

  @Test
  internal fun `13-9 a difference occurrence captures its candidate from its own domain`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor { CLASS Player1 }",
            "CLASS Passive : Owner",
            "ABSTRACT CLASS Resource<Owner>",
            "ABSTRACT CLASS Notice<Owner>",
        )
    val bound =
        table
            .inferTypeVariables()
            .transformEffect(
                parse<Effect>("Resource<Owner(NOT Player)> BY Player: Notice<Owner(NOT Player)>")
            )
    val actor = bound.typeVariables.variables.single { "${it.declaration.expression}" == "Player" }
    val afterActor =
        bound.typeVariables
            .bind(mapOf(actor to table.resolve(te("Player1"))))
            .transformEffect(bound)
    val event = afterActor.typeVariables.variables.single()

    event.bound shouldBe table.resolve(te("Owner"))
    "${event.declaration.expression}" shouldBe "Owner(NOT Player)"

    val captured =
        afterActor.typeVariables.bindingsFrom(
            parse("Resource<Owner(NOT Player1)>"),
            table.resolve(parse("Resource<Owner(NOT Player1)>")),
            table.resolve(parse("Resource<Passive>")),
        )

    "${captured[event]}" shouldBe "Passive"
    afterActor.typeVariables.bind(captured).transformEffect(afterActor).toString() shouldBe
        "Resource<Passive> BY Player1: Notice<Passive>"
  }

  // 13-10 Binding

  @Test
  internal fun `13-10 binding replaces only the recorded occurrences`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant, Steel }",
            "ABSTRACT CLASS Notice<StandardResource>",
        )
    val bound =
        table
            .inferTypeVariables()
            .transformEffect(parse<Effect>("StandardResource: Notice<StandardResource>, Steel"))
    val variable = bound.typeVariables.variables.single()

    bound.typeVariables
        .bind(mapOf(variable to table.resolve(te("Plant"))))
        .transformEffect(bound)
        .toString() shouldBe "Plant: Notice<Plant>, Steel"
  }

  @Test
  internal fun `13-10 a refined declaration is evaluated once, while its value is captured`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS StandardResource { CLASS Plant }",
            "ABSTRACT CLASS Marker<StandardResource>",
            "ABSTRACT CLASS Token<StandardResource>",
        )
    val bound =
        table
            .inferTypeVariables()
            .transformEffect(
                parse<Effect>("StandardResource(HAS Marker): Token<StandardResource(HAS Marker)>")
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

  // 13-11 Scope queries

  @Test
  internal fun `13-11 a scope reports the variables and spellings visible in it`() {
    val trade = effect("StandardResource: StandardResource")
    val scope = trade.typeVariables
    val variable = scope.variables.single()

    scope.isEmpty shouldBe false
    TypeVariableScope.EMPTY.isEmpty shouldBe true
    scope.expressionsOf(variable).map { "$it" } shouldContainExactly listOf("StandardResource")
    "${scope.expressionOf(variable.declaration)}" shouldBe "StandardResource"
    scope.variableAt(parse<Expression>("StandardResource")) shouldBe variable
    scope.variableDeclaredAt(parse<Expression>("StandardResource")) shouldBe variable
    scope.variableAt(parse<Expression>("Plant")) shouldBe null
  }

  @Test
  internal fun `13-11 a scope can capture values from a specialized expression`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Container<Component>",
        )
    val authored = parse<Expression>("Container<Box<Person>>")
    val person = authored.arguments.single().arguments.single()
    val scope =
        TypeVariableScope.infer(listOf(authored), table, explicitDeclarations = listOf(person))

    scope
        .bindingsFrom(
            authored,
            table.resolve(authored),
            table.resolve(parse("Container<Box<Alice>>")),
        )
        .map { (variable, value) -> "$variable=$value" } shouldContainExactly listOf("Person=Alice")
  }

  @Test
  internal fun `13-11 capture follows dependency paths, so a mismatched candidate captures nothing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person { CLASS Alice }",
            "ABSTRACT CLASS Box<Person>",
            "CLASS Hand",
            "ABSTRACT CLASS Container<Component>",
        )
    val authored = parse<Expression>("Container<Box<Person>>")
    val person = authored.arguments.single().arguments.single()
    val scope =
        TypeVariableScope.infer(listOf(authored), table, explicitDeclarations = listOf(person))

    scope.bindingsFrom(
        authored,
        table.resolve(authored),
        table.resolve(parse("Container<Hand>")),
    ) shouldBe emptyMap()
  }
}
