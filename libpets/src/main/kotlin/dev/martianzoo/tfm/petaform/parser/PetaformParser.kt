package dev.martianzoo.tfm.petaform.parser

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.utils.Tuple2
import dev.martianzoo.tfm.petaform.api.Action
import dev.martianzoo.tfm.petaform.api.Action.Cost
import dev.martianzoo.tfm.petaform.api.Action.Cost.Spend
import dev.martianzoo.tfm.petaform.api.ComponentDecl
import dev.martianzoo.tfm.petaform.api.ComponentDecls
import dev.martianzoo.tfm.petaform.api.Effect
import dev.martianzoo.tfm.petaform.api.Effect.Trigger
import dev.martianzoo.tfm.petaform.api.Effect.Trigger.OnGain
import dev.martianzoo.tfm.petaform.api.Effect.Trigger.OnRemove
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.Instruction
import dev.martianzoo.tfm.petaform.api.Instruction.Gain
import dev.martianzoo.tfm.petaform.api.Instruction.Gated
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity
import dev.martianzoo.tfm.petaform.api.Instruction.Intensity.Companion.intensity
import dev.martianzoo.tfm.petaform.api.Instruction.Per
import dev.martianzoo.tfm.petaform.api.Instruction.Remove
import dev.martianzoo.tfm.petaform.api.PetaformNode
import dev.martianzoo.tfm.petaform.api.Predicate
import dev.martianzoo.tfm.petaform.api.QuantifiedExpression
import dev.martianzoo.tfm.petaform.api.RootType
import kotlin.reflect.KClass
import kotlin.reflect.cast

object PetaformParser {
  inline fun <reified P : PetaformNode> parse(petaform: String) = parse(P::class, petaform)

  fun <P : PetaformNode> parse(type: KClass<P>, petaform: String): P {
    val parser: Parser<PetaformNode> = parsers[type]!!
    try {
      val pet = parser.parse(petaform)
      require(type == ComponentDecls::class || pet.countProds() <= 1) { petaform }
      return type.cast(pet)
    } catch (e: ParseException) {
      throw IllegalArgumentException("expecting ${type.simpleName}, input was: $petaform", e)
    }
  }

  fun <T> Parser<T>.parse(petaform: String) =
      parseToEnd(DefaultTokenizer(tokens).tokenize(petaform))

  private val tokens = mutableListOf<Token>()

  private val comment = regex("//[^\n]*", ignore = true)
  private val whitespace = regex(" +", ignore = true)

  private val arrow = literal("->")
  private val twoColons = literal("::")

  private val comma = literal(",")
  private val minus = literal("-")
  private val slash = literal("/")
  private val colon = literal(":")
  private val bang = literal("!")
  private val dot = literal(".")
  private val questy = literal("?")
  private val leftParen = literal("(")
  private val rightParen = literal(")")
  private val leftAngle = literal("<")
  private val rightAngle = literal(">")
  private val leftBracket = literal("[")
  private val rightBracket = literal("]")
  private val leftBrace = literal("{")
  private val rightBrace = literal("}")
  private val newline = literal("\n")

  private val prod = regex("\\bPROD\\b")
  private val max = regex("\\bMAX\\b")
  private val orToken = regex("\\bOR\\b")
  private val has = regex("\\bHAS\\b")
  private val component = regex("\\bcomponent\\b")
  private val abstract = regex("\\babstract\\b")
  private val default = regex("\\bdefault\\b")

  private val scalar = regex("\\b(0|[1-9][0-9]*)\\b")
  private val ident = regex("\\b[A-Z][a-z][A-Za-z0-9_]*\\b")

  private val parsers = mutableMapOf<KClass<out PetaformNode>, Parser<PetaformNode>>()

  private val prodStart = prod and leftBracket
  private val prodEnd = rightBracket

  private val rootType: Parser<RootType> = ident map { RootType(it.text) } // Plant

  val expression = publish(object {

    private val specializations: Parser<List<Expression>> = // <Player1, LandArea>
        skip(leftAngle) and
        separatedTerms(parser { expression }, comma) and
        skip(rightAngle)
    private val optionalSpecializations = optionalList(specializations)

    private val hazzer = skip(has) and parser { predicate }
    private val predicates: Parser<List<Predicate>> = group(separatedTerms(hazzer, comma))
    private val optionalPredicates = optionalList(predicates)

    // CityTile<Player1, LandArea>(HAS blahblah)
    val expression = rootType and optionalSpecializations and optionalPredicates map {
      (type, refs, preds) -> Expression(type, refs, preds)
    }
  }.expression)

  private val explicitScalar: Parser<Int> = scalar map { it.text.toInt() } // 3
  private val implicitScalar: Parser<Int> = optional(explicitScalar) map { it ?: 1 } // "", meaning 1
  private val implicitType: Parser<Expression> = optional(expression) map { // "", meaning "Megacredit"
    it ?: Expression.DEFAULT
  }
  val qeWithScalar = explicitScalar and implicitType map {
    (scalar, expr) -> QuantifiedExpression(expr, scalar)
  }
  val qeWithType = implicitScalar and expression map {
    (scalar, expr) -> QuantifiedExpression(expr, scalar)
  }
  val qe = qeWithScalar or qeWithType

  val predicates = PredicateParsers().also { publish(it.predicate) }
  class PredicateParsers {
    private val minPredicate = qe map Predicate::Min
    private val maxPredicate = skip(max) and qeWithScalar map Predicate::Max
    private val prodPredicate = prodBox(parser { predicate }) map Predicate::Prod
    private val onePredicate: Parser<Predicate> = minPredicate or maxPredicate or prodPredicate

    // OR binds more tightly than ,
    private val bareOrPredicate =
        separatedMultiple(onePredicate or parser { groupedAndPredicate }, orToken) map Predicate::Or
    private val bareAndPredicate =
        separatedMultiple(bareOrPredicate or onePredicate, comma) map Predicate::And

    private val groupedAndPredicate: Parser<Predicate> = group(bareAndPredicate)
    private val groupedOrPredicate = group(bareOrPredicate)
    val predicate: Parser<Predicate> = bareAndPredicate or bareOrPredicate or onePredicate
    val safePredicate = groupedAndPredicate or groupedOrPredicate or onePredicate
  }
  val predicate = predicates.predicate

  // slash > gate > or > then > comma -- keep in sync with Instruction.precedence()
  private val instruction = publish(
      object {

        // Atoms
        val intensity = optional(bang or dot or questy) map { intensity(it?.text) }
        val gain = qe and intensity map { gain(it) }
        val remove = skip(minus) and qe and intensity map { remove(it) }
        val atom = gain or remove

        // Reentrancy
        val anyInstr: Parser<Instruction> = parser { instruction }
        val anyGroup = group(anyInstr)

        // Slash binds most tightly
        val maybePer = atom and optional(skip(slash) and qeWithType) map { (instr, qe) ->
          if (qe == null) instr else Per(instr, qe)
        }

        // Prod can wrap anything as it is self-grouping
        val maybeProd = maybePer or (prodBox(anyInstr) map Instruction::Prod)

        // Gating colon binds next
        val gateable = maybeProd or anyGroup
        val gated = predicates.safePredicate and skip(colon) and gateable map { gated(it) }
        val maybeGated = gated or maybeProd

        // Then OR
        val orTerm = maybeGated or anyGroup
        val orInstr = separatedMultiple(orTerm, orToken) map Instruction::or
        val maybeOr = orInstr or maybeGated

        // Lastly the comma binds most loosely of all
        val andTerm = maybeOr or anyGroup
        val multi = separatedMultiple(andTerm, comma) map Instruction::multi

        val instruction = multi or maybeOr
      }.instruction,
  )

  val action = publish(object {
    private val spendCost = qe map ::Spend
    private val prodCost: Parser<Cost.Prod> = prodBox(parser { cost }) map Cost::Prod
    private val perableCost = spendCost or prodCost

    // for now, a per can't contain an Or/Multi; will have to be distributed
    private val perCost = perableCost and skip(slash) and qeWithType map {
      (cost, qe) -> Cost.Per(cost, qe)
    }
    private val oneCost = perCost or perableCost

    // OR binds more tightly than ,
    private val orCost = separatedMultiple(oneCost or group(parser { multiCost }), orToken) map Cost::Or
    private val multiCost: Parser<Cost.Multi> = separatedMultiple(orCost or oneCost, comma) map Cost::Multi

    private val cost = multiCost or orCost or oneCost

    val action = publish(optional(cost) and skip(arrow) and instruction map { (c, i) -> Action(c, i) })
  }.action)

  val effect = publish(object {
    private val onGainTrigger = expression map { OnGain(it) }
    private val onRemoveTrigger = skip(minus) and expression map { OnRemove(it) }
    private val nonProdTrigger = onGainTrigger or onRemoveTrigger
    private val prodTrigger = prodBox(nonProdTrigger) map Trigger::Prod
    private val trigger = publish(nonProdTrigger or prodTrigger)

    private val colons = (twoColons map { true }) or (colon map { false })
    val effect = publish(trigger and colons and maybeGroup(instruction) map {
      (trig, immed, instr) -> Effect(trig, instr, immed)
    })
  }.effect)

  object ComponentStuff {
    val isAbstract: Parser<Boolean> = (component map {false}) or (abstract map {true})
    //  val multiComponentLine: Parser<ComponentDecls> =
    //  isAbstract and separatedMultiple(expression, comma) and skip(newline) map {
    //  (abst, exprs) -> ComponentDecls(exprs.map { ComponentDecl(it, abst) }.toSet())
    //}

    val defaultSpec = skip(default) and instruction
    val componentContent: Parser<PetaformNode> =
        defaultSpec or action or effect or parser { componentDeclaration }

    val contents = separatedTerms(optional(componentContent), newline) map { it.filterNotNull() }
    val body: Parser<List<PetaformNode>> =
        optionalList(skip(leftBrace and newline) and contents and skip(rightBrace))

    val supertypes = optionalList(skip(colon) and separatedTerms(expression, comma))
    val singleComponent: Parser<ComponentDecls> =
        isAbstract and expression and supertypes and body map {
          (abst, expr, sups, contents) ->
            val acts = contents.filterIsInstance<Action>().toSet()
            val effs = contents.filterIsInstance<Effect>().toSet()
            val defs = contents.filterIsInstance<Instruction>().toSet()
            val subs = contents.filterIsInstance<ComponentDecls>().toSet()

            val cd = ComponentDecl(expr, abst, sups.toSet(), acts, effs, defs, false)
            ComponentDecls(setOf(cd) + subs.flatMap { it.decls }.map { insertSupertype(it, expr) }.toSet())
        }

    private fun insertSupertype(comp: ComponentDecl, supertype: Expression) =
        if (comp.complete) {
          comp
        } else if (comp.supertypes.any { it.rootType == supertype.rootType }) { // TODO
          comp.copy(complete = true)
        } else {
          comp.copy(supertypes = comp.supertypes + Expression(supertype.rootType), complete = true)
        }

    val componentDeclaration: Parser<ComponentDecls> =
        optional(singleComponent) map { it ?: ComponentDecls() }

    val components: Parser<ComponentDecls> =
        separatedTerms(componentDeclaration, newline) map {
          ComponentDecls(it.flatMap(ComponentDecls::decls).map { it.copy(complete=true) }.toSet())
        }
  }

  val components = publish(ComponentStuff.components)

  private fun regex(r: String, ignore: Boolean = false) = regexToken(r, ignore).also { tokens += it }
  private fun literal(l: String, ignore: Boolean = false) = literalToken(l, ignore).also { tokens += it }

  private fun gated(pair: Tuple2<Predicate, Instruction>) = Gated(pair.t1, pair.t2)
  private fun gain(pair: Tuple2<QuantifiedExpression, Intensity?>) = Gain(pair.t1, pair.t2)
  private fun remove(pair: Tuple2<QuantifiedExpression, Intensity?>) = Remove(pair.t1, pair.t2)

  private inline fun <reified T> optionalList(parser: Parser<List<T>>) =
      optional(parser) map { it ?: listOf() }

  private inline fun <reified T> prodBox(parser: Parser<T>) =
      skip(prodStart) and parser and skip(prodEnd)

  private inline fun <reified T> separatedMultiple(term: Parser<T>, sep: Token): Parser<List<T>> {
    return term and skip(sep) and separatedTerms(term, sep) map { (first, rest) ->
      listOf(first) + rest
    }
  }

  private inline fun <reified T> group(contents: Parser<T>) =
      skip(leftParen) and contents and skip(rightParen)

  private inline fun <reified T> maybeGroup(contents: Parser<T>) =
      group(contents) or contents

  private inline fun <reified P : PetaformNode> publish(parser: Parser<P>): Parser<P> {
    parsers[P::class] = parser
    return parser
  }
}
