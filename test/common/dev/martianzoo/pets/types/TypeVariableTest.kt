package dev.martianzoo.pets.types

import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.Then
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TypeVariableTest {
  @Test
  internal fun `action lowering preserves variables shared by the cost and result`() {
    val table = loadTypes("ABSTRACT CLASS Resource", "CLASS Plant : Resource")
    val action = table.inferTypeVariables().transformAction(parse<Action>("Resource -> Resource"))
    val lowered = action.toInstruction() as Then
    val variable = lowered.typeVariables.variables.single()

    lowered.typeVariables
        .bind(mapOf(variable to table.resolve(parse("Plant"))))
        .transformInstruction(lowered)
        .toString() shouldBe "-Plant! THEN Plant"
  }

  @Test
  internal fun `a bare effect use captures an inherited compound header variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Token<Box<Person>>",
            "ABSTRACT CLASS Parent<Box<Person>>",
            "CLASS Child : Parent { This: Token<Box> }",
        )
    val klass = table.getClass(parse<Expression>("Child").className)
    val effect = klass.interpretTypeVariablesIn(klass.declaration.effects.single())
    val variable = effect.typeVariables.variables.single()
    val specific = table.resolve(parse("Child<Box<Alice>>"))
    val bindings = specific.variableBindingsFrom(klass.defaultType, listOf(variable))

    effect.typeVariables.bind(bindings).transformEffect(effect).toString() shouldBe
        "This: Token<Box<Alice>>"
  }

  @Test
  internal fun `binding ignores a candidate missing a nested dependency path`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS CardBack",
            "ABSTRACT CLASS Envelope<CardBack>",
            "CLASS Hand",
            "ABSTRACT CLASS Container<Component>",
        )
    val authored = parse<Expression>("Container<Envelope<CardBack>>")
    val cardBack = authored.arguments.single().arguments.single()
    val scope =
        TypeVariableScope.infer(
            listOf(authored),
            table,
            explicitDeclarations = listOf(cardBack),
        )

    scope.bindingsFrom(
        authored,
        table.resolve(authored),
        table.resolve(parse("Container<Hand>")),
    ) shouldBe emptyMap()
  }

  @Test
  internal fun `a complemented occurrence captures its candidate from the dependency domain`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor",
            "CLASS Player1 : Player",
            "CLASS Passive : Owner",
            "ABSTRACT CLASS Resource<Owner>",
            "ABSTRACT CLASS Notice<Owner>",
        )
    val effect =
        table
            .inferTypeVariables()
            .transformEffect(parse("Resource<!Player> BY Player: Notice<!Player>"))
    val actor =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "Player"
        }
    val actorBound =
        effect.typeVariables
            .bind(mapOf(actor to table.resolve(parse("Player1"))))
            .transformEffect(effect)
    val complemented = actorBound.typeVariables.variables.single()

    val bindings =
        actorBound.typeVariables.bindingsFrom(
            parse("Resource<!Player1>"),
            table.resolve(parse("Resource<!Player1>")),
            table.resolve(parse("Resource<Passive>")),
        )

    bindings[complemented].toString() shouldBe "Passive"
    complemented.bound.toString() shouldBe "Owner"
    actorBound.typeVariables.bind(bindings).transformEffect(actorBound).toString() shouldBe
        "Resource<Passive> BY Player1: Notice<Passive>"
    complemented.declaration.expression.toString() shouldBe "!Player"
  }

  @Test
  internal fun `actor declaration and its derived uses remain ordinary Type views`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor",
            "CLASS Player1 : Player",
            "ABSTRACT CLASS Notice<Actor>",
            "ABSTRACT CLASS Pair<Actor, Actor>",
        )
    val effect =
        table
            .inferTypeVariables()
            .transformEffect(parse("Notice<!Player> BY Player: Pair<Player, !Player>"))
    val variable =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "Player"
        }
    val eventVariable =
        effect.typeVariables.variables.single {
          it.declaration.expression.toString() == "!Player"
        }

    variable.declaration.expression.toString() shouldBe "Player"
    variable.usages.map { it.expression.toString() } shouldContainExactly
        listOf("!Player", "Player", "!Player")
    listOf<Type>(
            variable.bound,
            variable,
            variable.declaration,
            variable.usages.first(),
        )
        .map(Type::groundType)
        .distinct() shouldContainExactly listOf(table.resolve(parse("Player")))
    variable.bound.typeVariable shouldBe null
    variable.declaration.typeVariable shouldBe variable
    eventVariable.usages.map { it.expression.toString() } shouldContainExactly listOf("!Player")
    effect.typeVariables
        .bind(mapOf(variable to table.resolve(parse("Player1"))))
        .transformEffect(effect)
        .toString() shouldBe "Notice<!Player1> BY Player1: Pair<Player1, !Player1>"
  }

  @Test
  internal fun `authored argument order does not identify one enclosing variable`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Place",
            "CLASS Mars : Place",
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "ABSTRACT CLASS Duo<Place, Person>",
            "ABSTRACT CLASS Notice<Component>",
            "ABSTRACT CLASS Token<Component>",
        )
    val effect =
        table
            .inferTypeVariables()
            .transformEffect(parse("Notice<Duo<Place, Person>>: Token<Duo<Person, Place>>"))
    effect.typeVariables.variables.map { it.declaration.expression.toString() }.toSet() shouldBe
        setOf("Place", "Person")
  }

  @Test
  internal fun `class specialization binds only inspected header variables`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Holder<Box<Person>> { This: Box<Person> }",
        )
    val klass = table.getClass(parse<Expression>("Holder").className)
    val effect = klass.interpretTypeVariablesIn(klass.declaration.effects.single())
    val box = klass.typeVariables.single { it.declaration.expression.toString() == "Box<Person>" }
    val person = klass.typeVariables.single { it.declaration.expression.toString() == "Person" }

    box.usages.any { it.expression.toString() == "Box<Person>" } shouldBe true
    person.usages.any { it.expression.toString() == "Person" } shouldBe true

    val specific = table.resolve(parse("Holder<Box<Alice>>"))
    val bindings = specific.variableBindingsFrom(klass.defaultType, effect.typeVariables.variables)
    bindings[box].toString() shouldBe "Box<Alice>"
    bindings[person].toString() shouldBe "Alice"
    effect.typeVariables.bind(bindings).transformEffect(effect).toString() shouldBe
        "This: Box<Alice>"
  }

  @Test
  internal fun `identical nested bounds in sibling header branches remain independent`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "CLASS Alice : Person",
            "CLASS Bob : Person",
            "ABSTRACT CLASS Box<Person>",
            "ABSTRACT CLASS Pair<Box<Person>, Box<Person>>",
            "ABSTRACT CLASS Holder<Pair<Box<Person>, Box<Person>>>",
        )

    table.resolve(parse("Holder<Pair<Box<Alice>, Box<Bob>>>")).expressionFull.toString() shouldBe
        "Holder<Pair<Box<Alice>, Box<Bob>>>"
  }

  @Test
  internal fun `an effect use cannot choose between independent header declarations`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Person",
            "ABSTRACT CLASS Ambiguous<Person, Person> { This: Person }",
        )
    val klass = table.getClass(parse<Expression>("Ambiguous").className)

    shouldThrow<PetException> { klass.typeVariables }
  }
}
