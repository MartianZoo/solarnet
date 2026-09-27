package dev.martianzoo.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.token
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.completionAtEnd
import com.github.h0tk3y.betterParse.parser.parseToEnd
import dev.martianzoo.pets.Parsing.Body.BodyElement
import dev.martianzoo.pets.Parsing.Body.BodyElement.ActionElement
import dev.martianzoo.pets.Parsing.Body.BodyElement.DefaultsElement
import dev.martianzoo.pets.Parsing.Body.BodyElement.EffectElement
import dev.martianzoo.pets.Parsing.Body.BodyElement.InvariantElement
import dev.martianzoo.pets.Parsing.Body.BodyElement.NestedDeclaration
import dev.martianzoo.pets.Parsing.Body.BodyElement.PropertyElement
import dev.martianzoo.pets.Transforming.actionSelectors
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.Exceptions.PetSyntaxException
import dev.martianzoo.pets.api.SourceLocation
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.Action.Cost
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Effect.Trigger
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement
import dev.martianzoo.pets.ast.FromExpression
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.Instruction.Quantifier.AMAP
import dev.martianzoo.pets.ast.Instruction.Quantifier.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Quantifier.OPTIONAL
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.PetElement
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.Property
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.ast.ScaledExpression
import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar
import dev.martianzoo.pets.ast.resolveClassLiteralTypeVariableNames
import dev.martianzoo.pets.ast.resolveClassTypeVariableNames
import dev.martianzoo.pets.ast.resolveSelectorTypeVariableNames
import dev.martianzoo.pets.ast.resolveTypeVariableNames
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.DefaultsDeclaration.OneDefault
import kotlin.reflect.KClass

/**
 * Various functions for parsing [PetElement]s or [ClassDeclaration]s from text. The source syntax
 * is
 * [section 11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)
 * of the Pets language specification.
 */
public object Parsing {
  /**
   * Parses a series of Pets class declarations, returning one [ClassDeclaration] per declared
   * class, in source order ([rule
   * L11-1](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)).
   * Nested declarations follow their container, recursively ([rule
   * L11-6](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)),
   * and owner-local classes are lowered to ordinary declarations ([section
   * 12](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-owner-local-classes)).
   * Examples can be reviewed in `global.pets` and `player.pets`.
   *
   * A source containing only whitespace and comments declares nothing. There is no partial success:
   * an incomplete final declaration is rejected.
   *
   * @throws PetSyntaxException if [declarationsSource] is not a complete sequence of declarations
   */
  public fun parseClasses(declarationsSource: String): List<ClassDeclaration> {
    val declarations =
        parse(
            PetsGrammar.rootParser,
            declarationsSource,
            expectedTypeDesc = "Pets class declarations",
        )
    return declarations.flatMap { declaration ->
      DerivedClassLowerer(declaration.className).lowerDeclaration(declaration)
    }
  }

  /**
   * Parses exactly one class declaration, with an optional semicolon-separated body ([rule
   * L11-11](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#11-class-declarations)).
   * This is how a declaration embedded in structured card data is read; syntax examples can be seen
   * in `"components"` fields of `cards.json`.
   *
   * Owner-local class syntax is rejected here, since that syntax is available only where a
   * declaration file is being read ([rule
   * L12-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-owner-local-classes)).
   */
  public fun parseOneLinerClass(declarationSource: String): ClassDeclaration =
      parse(PetsGrammar.oneLineDeclaration, declarationSource).also { declaration ->
        if (
            declaration.allNodes.any { node ->
              node.descendantsOfType<Expression>().any { it.derivedClassBody != null }
            }
        ) {
          throw PetSyntaxException("owner-local classes are not allowed inside class declarations")
        }
      }

  /**
   * Parses the Pets element of type [P] from [elementSource], and returns it *not* surrounded by a
   * `RAW` block. [P] can only be one of the published node kinds like [Effect], [Action],
   * [InstructionTree], [Expression], etc.
   *
   * Owner-local derived Class syntax belongs only to declaration-file grammar. A submitted element
   * has no definition owner ([rule
   * L12-7](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#12-owner-local-classes)).
   * Parsing that far is what keeps the specific error distinct from malformed syntax.
   */
  public inline fun <reified P : PetNode> parse(elementSource: String): P =
      parse(P::class, elementSource)

  /** Non-reified form of [parse]. */
  public fun <P : PetNode> parse(expectedType: KClass<P>, elementSource: String): P {
    val lowerer = DerivedClassLowerer(ClassName.cn("Submitted"))
    val pet = parse(expectedType, elementSource, lowerer)
    if (lowerer.declarations.isNotEmpty()) {
      throw PetSyntaxException("owner-local classes are allowed only in declaration files")
    }
    return pet
  }

  // TODO: Contract this temporary tfm-canon seam.
  public fun <P : PetNode> parse(
      expectedType: KClass<P>,
      elementSource: String,
      derivedClasses: DerivedClassLowerer,
  ): P {
    require(expectedType != PetNode::class) { "missing type info" }

    val parsed = parse(nodeParser(expectedType), elementSource, expectedType.simpleName)
    val lowered = derivedClasses.transformWithoutKindCheck(parsed)
    check(expectedType.isInstance(lowered)) {
      "expected `${expectedType.simpleName}` kind, found `${lowered.kind.simpleName}`"
    }
    @Suppress("UNCHECKED_CAST")
    return lowered as P
  }

  private fun <T> parse(
      parser: Parser<T>,
      source: String,
      expectedTypeDesc: String? = null,
  ): T {
    val matches = PetsGrammar.tokenizer.tokenize(source)
    try {
      return parser.parseToEnd(matches).also(::rejectUnsupportedSyntax)
    } catch (e: ParseException) {
      val completion = parser.completionAtEnd(matches)
      val found = matches.getNotIgnored(completion.farthestPosition)
      val location =
          found?.let { SourceLocation(source, it.offset, it.length) }
              ?: SourceLocation(source, source.length, 0)
      val expected =
          completion.expectedTokens
              .map { token ->
                when (val name = token.name) {
                  "ALLCAPS",
                  "mixed-case class name" -> "a class name"
                  "lowerCamel" -> "a property name"
                  "scalar" -> "a non-negative integer"
                  "\n" -> "a newline"
                  "\"[^\"]*\"" -> "quoted text"
                  "arrow" -> "`->`"
                  "doubleColon" -> "`::`"
                  else -> "`$name`"
                }
              }
              .distinct()
              .sorted()
              .joinToString(" or ")
              .ifEmpty { "end of input" }
      val actual =
          when (found?.text) {
            null -> "end of input"
            "\n" -> "a newline"
            else -> "`${found.text}`"
          }
      throw PetSyntaxException(
          when {
            found?.type?.name == "no token matched" && found.text.startsWith('"') ->
                "unterminated quoted text; expected a closing double quote"
            found?.type?.name == "no token matched" ->
                "unrecognized character `${found.text.first()}`"
            else -> "expected $expected; found $actual"
          },
          e,
          location,
      )
    } catch (e: IllegalArgumentException) {
      throw PetSyntaxException(
          e.message ?: "invalid $expectedTypeDesc",
          e,
          SourceLocation(source, 0),
      )
    } catch (e: PetException) {
      if (e.sourceLocation == null) e.sourceLocation = SourceLocation(source, 0)
      throw e
    }
  }

  private fun rejectUnsupportedSyntax(parsed: Any?) {
    if (parsed is PetNode) {
      val expressions = parsed.descendantsOfType<Expression>()
      expressions
          .firstOrNull {
            it.typeVariableName is Expression.TypeVariableName.Declaration &&
                !it.typeVariableName.resolved
          }
          ?.let {
            val marker = it.typeVariableName!!
            throw PetSyntaxException(
                "type variable marker `${marker.authoredSpelling}` is not shared in a scope; use it again in that scope or remove the marker",
                sourceLocation = it.sourceLocation ?: it.className.sourceLocation,
            )
          }
      expressions
          .mapNotNull { it.typeVariableName as? Expression.TypeVariableName.Reference }
          .firstOrNull { !it.resolved }
          ?.let {
            throw PetSyntaxException(
                "type variable marker `${it.authoredSpelling}` has no supplying occurrence",
                sourceLocation = it.boundClassName.sourceLocation,
            )
          }
    }
    when (parsed) {
      is ClassDeclaration ->
          (parsed.allNodes - parsed.dependencies.toSet() - parsed.supertypes).forEach(
              ::rejectUnsupportedSyntax
          )
      is PetNode ->
          parsed.visitDescendants {
            (it as? Expression)?.let(ScaledExpression::rejectIfDenominationless)
            if (it is Metric.Rank && it.selector == null) {
              throw PetSyntaxException(
                  "`RANK { ... }` requires an enclosing expression refinement",
                  sourceLocation = it.sourceLocation,
              )
            }
            true
          }
      is Iterable<*> -> parsed.forEach(::rejectUnsupportedSyntax)
    }
  }

  public fun acceptsNextToken(
      expectedType: KClass<out PetNode>,
      source: String,
      candidate: String,
  ): Boolean {
    require(expectedType != PetNode::class) { "missing type info" }
    return nodeParser(expectedType)
        .completionAtEnd(PetsGrammar.tokenizer.tokenize(source))
        .expectedTokens
        .any { it.match(candidate, 0) > 0 }
  }

  @Suppress("UNCHECKED_CAST")
  private fun <P : PetNode> nodeParser(type: KClass<P>): Parser<P> =
      when (type) {
        Action::class -> PetsGrammar.action
        ClassName::class -> PetsGrammar.className
        Cost::class -> PetsGrammar.cost
        Effect::class -> PetsGrammar.effect
        Expression::class -> PetsGrammar.expression
        Refinement::class -> PetsGrammar.refinement
        FromExpression::class -> PetsGrammar.fromExpression
        InstructionTree::class -> PetsGrammar.instructionTree
        Instruction::class -> PetsGrammar.instruction
        Metric::class -> PetsGrammar.metric
        Property::class -> PetsGrammar.property
        PropertyName::class -> PetsGrammar.propertyName
        PropertyValue::class -> PetsGrammar.propertyValue
        Requirement::class -> PetsGrammar.requirement
        Scalar::class -> PetsGrammar.scalar
        ScaledExpression::class -> PetsGrammar.scaledExpression
        Trigger::class -> PetsGrammar.trigger
        else -> error("unrecognized parser type: `$type`")
      }
          as Parser<P>

  /**
   * The complete token vocabulary and grammar. Declarations are the root; the public API also
   * selects individual productions. `parser { ... }` ties recursive/forward references, and each
   * production is constructed once. AST construction and scope resolution run only after a
   * production succeeds; completion walks the same combinators without running those maps.
   */
  private object PetsGrammar : Grammar<List<ClassDeclaration>>() {
    // First match wins: comments precede '/', multi-character punctuation precedes its prefixes,
    // and keywords precede names. Newlines separate declaration-body elements and are significant.
    private val continuation by regexToken(Regex("\\\\\r?\n"), "backslash-newline", ignored = true)
    private val whitespace by
        regexToken(Regex("[ \\t\\r]+"), "horizontal-whitespace", ignored = true)
    private val comment by regexToken(Regex("//[^\\r\\n]*"), "line-comment", ignored = true)
    private val quotedToken by regexToken(Regex("\"[^\"]*\""))
    private val arrow by literalToken("arrow", "->")
    private val doubleColon by literalToken("doubleColon", "::")
    private val bang by literalToken("!", "!")
    private val at by literalToken("@", "@")
    private val caret by literalToken("^", "^")
    private val plus by literalToken("+", "+")
    private val comma by literalToken(",", ",")
    private val minus by literalToken("-", "-")
    private val dot by literalToken(".", ".")
    private val slash by literalToken("/", "/")
    private val colon by literalToken(":", ":")
    private val semicolon by literalToken(";", ";")
    private val equals by literalToken("=", "=")
    private val question by literalToken("?", "?")
    private val lparen by literalToken("(", "(")
    private val rparen by literalToken(")", ")")
    private val lbracket by literalToken("[", "[")
    private val rbracket by literalToken("]", "]")
    private val lbrace by literalToken("{", "{")
    private val rbrace by literalToken("}", "}")
    private val langle by literalToken("<", "<")
    private val rangle by literalToken(">", ">")
    private val newline by literalToken("\n", "\n")
    private val byKeyword by keyword("BY")
    private val countKeyword by keyword("COUNT")
    private val eachKeyword by keyword("EACH")
    private val evalKeyword by keyword("EVAL")
    private val fromKeyword by keyword("FROM")
    private val hasKeyword by keyword("HAS")
    private val ifKeyword by keyword("IF")
    private val maxKeyword by keyword("MAX")
    private val notKeyword by keyword("NOT")
    private val orKeyword by keyword("OR")
    private val rankKeyword by keyword("RANK")
    private val thenKeyword by keyword("THEN")
    private val xKeyword by keyword("X")
    private val abstractKeyword by keyword("ABSTRACT")
    private val classKeyword by keyword("CLASS")
    private val defaultKeyword by keyword("DEFAULT")
    private val metricKeyword by keyword("Metric")
    private val numberKeyword by keyword("Number")
    private val requirementKeyword by keyword("Requirement")
    private val mixedCaseName by
        regexToken(
            Regex("""\b[A-Z](?=[A-Za-z0-9_]*[a-z])[A-Za-z0-9_]*\b"""),
            "mixed-case class name",
        )
    private val allCapsName by regexToken(Regex("""\b[A-Z][A-Z0-9_]*\b"""), "ALLCAPS")
    private val lowerCamelName by regexToken(Regex("""\b[a-z][A-Za-z0-9]*\b"""), "lowerCamel")
    private val integerToken by regexToken(Regex("""\b(0|[1-9][0-9]*)"""), "scalar")

    private val sourcePosition: Parser<SourceLocation?> =
        object : Parser<SourceLocation?> {
          override fun tryParse(
              tokens: TokenMatchesSequence,
              fromPosition: Int,
          ): ParseResult<SourceLocation?> =
              object : Parsed<SourceLocation?>() {
                override val value: SourceLocation? =
                    tokens.getNotIgnored(fromPosition)?.let(::location)
                override val nextPosition: Int = fromPosition
              }
        }

    // Names and amounts.
    val className: Parser<ClassName> by
        (mixedCaseName or allCapsName) map
            {
              ClassName.cn(it.text).also { name -> name.sourceLocation = location(it) }
            }
    val propertyName: Parser<PropertyName> by
        lowerCamelName map
            {
              PropertyName(it.text).also { name -> name.sourceLocation = location(it) }
            }
    private val integer by
        integerToken map
            {
              it.text.toIntOrNull()
                  ?: throw PetSyntaxException(
                      "integer `${it.text}` exceeds the maximum supported value ${Int.MAX_VALUE}",
                      sourceLocation = location(it),
                  )
            }
    private val quotedText by quotedToken map { it.text.removeSurrounding("\"") }
    private val numericScalar by locatedNode(integer map ::ActualScalar)
    private val xScalar by optional(integer) and skip(xKeyword) map { XScalar(it ?: 1) }
    val scalar: Parser<Scalar> by xScalar or numericScalar
    private val quantifier by
        optional((bang asJust MANDATORY) or (dot asJust AMAP) or (question asJust OPTIONAL))

    // Expressions recurse through arguments and refinements. Selectors and class headers cannot
    // consume an owner-local body: their following braces belong to the enclosing production.
    private val namedMarker by
        className and skip(at) map { AuthoredTypeVariableMarker(it.asString) }
    private val anonymousMarker by at map { AuthoredTypeVariableMarker(null) }
    private val typeVariableMarker by namedMarker or anonymousMarker
    private val hasRefinement by
        skip(hasKeyword) and parser { requirementDisjunction } map Refinement.Companion::has
    private val notRefinement by
        skip(notKeyword) and parser { selectorExpression } map Refinement::Not
    val refinement: Parser<Refinement> by
        group(commaSeparated(hasRefinement or notRefinement) map Refinement.Companion::create)
    val expression: Parser<Expression> by expressionParser(allowDerivedClass = true)
    private val selectorExpression: Parser<Expression> by
        expressionParser(allowDerivedClass = false)

    private fun expressionParser(allowDerivedClass: Boolean): Parser<Expression> {
      val argument = parser { if (allowDerivedClass) expression else selectorExpression }
      val arguments =
          skip(langle) and separatedTerms(argument, comma, acceptZero = true) and skip(rangle)
      val base =
          optional(typeVariableMarker) and
              className and
              optional(arguments) and
              optional(refinement) map
              { (marker, clazz, args, ref) ->
                expression(marker, clazz, args, ref)
              }
      return locatedNode(
          if (allowDerivedClass) {
            base and
                optional(parser { derivedClassBody }) map
                { (parsed, body) ->
                  if (body == null) parsed else parsed.withDerivedClassBody(body)
                }
          } else base
      )
    }

    private val explicitProperty by
        expression and
            skip(dot) and
            propertyName map
            { (receiver, name) ->
              Property(name, receiver)
            }
    val property by locatedNode(explicitProperty or (propertyName map ::Property))

    val scaledExpression: Parser<ScaledExpression> by scaledExpression(scalar)
    // Requirements allow numeric targets only. Keep their denominationless error consistent with
    // instruction amounts, without admitting X into a requirement.
    private val requirementAmount by scaledExpression(numericScalar)

    private fun scaledExpression(amount: Parser<Scalar>): Parser<ScaledExpression> =
        ((amount and optional(expression)) or (optional(amount) and expression)) map
            { (scalar, expression) ->
              val resolved = scalar ?: ActualScalar(1)
              if (expression == null) ScaledExpression.denominationless(resolved)
              else ScaledExpression.scaledEx(expression, resolved)
            }

    private val unchangedFromArgument by expression map FromExpression::Unchanged
    private val fullFromExpression by
        expression and
            skip(fromKeyword) and
            expression map
            { (to, from) ->
              FromExpression.Full(to, from)
            }
    private val fromArguments by
        zeroOrMore(unchangedFromArgument and skip(comma)) and
            parser { fromExpression } and
            zeroOrMore(skip(comma) and unchangedFromArgument) map
            { (before, from, after) ->
              before + from + after
            }
    private val compactFromExpression by
        className and
            skip(langle) and
            fromArguments and
            skip(rangle) and
            optional(refinement) map
            { (name, arguments, refinement) ->
              FromExpression.Compact(name, arguments, refinement)
            }
    val fromExpression: Parser<FromExpression> by fullFromExpression or compactFromExpression

    // Metrics, from tightest to loosest: primary/scaling, MAX, subtraction, OR.
    private val explicitRank by
        skip(rankKeyword) and
            selectorExpression and
            skip(lbrace) and
            commaSeparated(parser { metric }) and
            skip(rbrace) map
            { (selector, metrics) ->
              val resolved = resolveSelectorTypeVariableNames(selector, metrics)
              Metric.Rank(resolved[0] as Expression, resolved.drop(1).map { it as Metric })
            }
    private val implicitRank by
        skip(rankKeyword) and
            skip(lbrace) and
            commaSeparated(parser { metric }) and
            skip(rbrace) map
            {
              Metric.Rank(null, it)
            }
    private val rank by locatedNode(explicitRank or implicitRank)
    private val metricTransform by
        locatedNode(
            transform(parser { metric }) map { (name, node) -> Metric.Transform(node, name.text) }
        )
    private val metricEval by locatedNode(skip(evalKeyword) and property map Metric::Eval)
    private val metricCount by expression map Metric::Count
    private val nonconstantMetric: Parser<Metric> by
        rank or metricEval or metricTransform or property or metricCount or group(parser { metric })
    private val scaledMetric by
        integer and nonconstantMetric map { (unit, metric) -> Metric.scaled(metric, unit) }
    private val metricConstant by integer map Metric::Constant
    private val metricPrimary by scaledMetric or nonconstantMetric or metricConstant
    private val metricAtom by
        metricPrimary and
            optional(skip(maxKeyword) and metricPrimary) map
            { (metric, limit) ->
              if (limit == null) metric else Metric.Max(metric, limit)
            }
    // After an instruction/cost '/', OR belongs to that instruction/cost's container.
    private val metricSubtraction by
        leftAssociative(metricAtom, minus) { left, _, right ->
          Metric.Subtract(left, right)
        }
    val metric: Parser<Metric> by
        separatedTerms(metricSubtraction, orKeyword) map
            { authored ->
              val flattened = authored.flatMap { if (it is Metric.Or) it.metrics else listOf(it) }
              if (flattened.distinct().size != flattened.size) {
                throw PetSyntaxException("duplicate metric `OR` alternatives: `$flattened`")
              }
              if (authored.size == 1) authored.single() else Metric.Or.create(authored)!!
            }

    // Requirements: atom, OR, comma conjunction. An atom is also the gate of an instruction.
    private val countedMetric by integer and metricAtom
    private val minimumRequirement: Parser<Requirement> by
        (property map { Requirement.Min(1, it) }) or
            mapLocated(countedMetric) { (target, metric) -> Requirement.Min(target, metric) } or
            mapLocated(requirementAmount, Requirement::Min)
    private val maximumRequirement by
        skip(maxKeyword) and
            ((countedMetric map { (target, metric) -> Requirement.Max(target, metric) }) or
                (requirementAmount map Requirement::Max))
    private val exactRequirement by
        skip(equals) and
            ((countedMetric map { (target, metric) -> Requirement.Exact(target, metric) }) or
                (requirementAmount map Requirement::Exact))
    private val requirementTransform by
        locatedNode(
            transform(parser { requirement }) map
                { (name, node) ->
                  Requirement.Transform(node, name.text)
                }
        )
    private val requirementEval by locatedNode(skip(evalKeyword) and property map Requirement::Eval)
    private val requirementAtom: Parser<Requirement> by
        requirementEval or
            requirementTransform or
            minimumRequirement or
            maximumRequirement or
            exactRequirement or
            group(parser { requirement })
    private val requirementDisjunction by
        separatedTerms(requirementAtom, orKeyword) map { Requirement.Or.create(it.toSet()) }
    val requirement: Parser<Requirement> by
        commaSeparated(requirementDisjunction) map Requirement.And.Companion::create

    // Instructions: change, per, BY, OR, gate, THEN, comma group (L2-16).
    private val gain by
        scaledExpression and
            quantifier map
            { (amount, quantifier) ->
              Instruction.Gain.gain(amount, quantifier)
            }
    private val remove by
        skip(minus) and
            scaledExpression and
            quantifier map
            { (amount, quantifier) ->
              Instruction.Remove.remove(amount, quantifier)
            }
    private val transmute by
        optional(scalar) and
            fromExpression and
            quantifier map
            { (scalar, from, quantifier) ->
              Instruction.Transmute(from, scalar ?: ActualScalar(1), quantifier)
            }
    private val change by locatedNode(transmute or gain or remove)
    private val perInstruction by
        change and
            optional(skip(slash) and metricSubtraction) map
            { (instruction, metric) ->
              if (metric == null) instruction else Instruction.Per(instruction, metric)
            }
    private val instructionTransform by
        locatedNode(
            transform(parser { instructionSyntax }) map
                { (name, node) ->
                  Instruction.Transform(node, name.text)
                }
        )
    private val each by
        skip(eachKeyword) and
            selectorExpression and
            skip(lbrace) and
            parser { instructionSyntax } and
            skip(rbrace) map
            { (selector, body) ->
              val resolved = resolveSelectorTypeVariableNames(selector, listOf(body))
              Instruction.Each(resolved[0] as Expression, resolved[1] as InstructionTree)
            }
    private val instructionPrimary: Parser<InstructionTree> by
        each or instructionTransform or perInstruction or group(parser { instructionSyntax })
    private val instructionAtom by
        instructionPrimary and
            optional(skip(byKeyword) and expression) map
            { (instruction, actor) ->
              if (actor == null) instruction else Instruction.By.createTree(instruction, actor)
            }
    private val instructionChoice by
        separatedTerms(instructionAtom, orKeyword) map
            {
              val seen = mutableSetOf<InstructionTree>()
              it.firstOrNull { !seen.add(it) }
                  ?.let { duplicate ->
                    throw PetSyntaxException(
                        "duplicate `OR` alternative `$duplicate`; remove the repeated alternative",
                        sourceLocation =
                            duplicate.sourceLocation
                                ?: duplicate
                                    .descendantsOfType<Expression>()
                                    .firstOrNull()
                                    ?.sourceLocation,
                    )
                  }
              Instruction.Or.createTree(it)
            }
    private val gatedInstruction by
        locatedNode(
            optional(requirementAtom and skip(colon)) and
                instructionChoice map
                { (gate, instruction) ->
                  Instruction.Gated.createTree(gate, instruction)
                }
        )
    private val instructionSequence by
        separatedTerms(gatedInstruction, thenKeyword) map Instruction.Then::createTree
    private val instructionSyntax: Parser<InstructionTree> by
        commaSeparated(instructionSequence) map InstructionGroup::createTree
    // Resolve symmetric local scopes only after the complete tree's enclosing selectors have
    // claimed their references. Recursive grammar references must use instructionSyntax.
    val instructionTree: Parser<InstructionTree> by
        instructionSyntax map ::resolveLocalTypeVariableNames
    val instruction: Parser<Instruction> by
        instructionSyntax map
            {
              val instruction =
                  it as? Instruction
                      ?: throw PetSyntaxException("expected one instruction, found group `$it`")
              resolveLocalTypeVariableNames(instruction) as Instruction
            }

    // Costs and actions.
    private val spendCost by scaledExpression map Cost::Spend
    private val costTransform by
        locatedNode(
            transform(parser { cost }) map { (name, node) -> Cost.Transform(node, name.text) }
        )
    private val costAtom by costTransform or spendCost or group(parser { cost })
    val cost: Parser<Cost> by
        costAtom and
            optional(skip(slash) and metricSubtraction) map
            { (cost, metric) ->
              if (metric == null) cost else Cost.Per(cost, metric)
            }
    val action: Parser<Action> by
        optional(cost) and
            skip(arrow) and
            instructionTree map
            { (cost, instruction) ->
              resolveTypeVariableNames(Action(cost, instruction), cost, instruction)
            }

    // Triggers and effects.
    private val onGain by expression map Trigger.OnGainOf.Companion::create
    private val onRemove by skip(minus) and expression map Trigger.OnRemoveOf.Companion::create
    private val xGain by skip(xKeyword) and onGain map Trigger::XTrigger
    private val xRemove by
        skip(minus) and
            skip(xKeyword) and
            expression map
            Trigger.OnRemoveOf.Companion::create map
            Trigger::XTrigger
    private val triggerAtom: Parser<Trigger> by xGain or xRemove or onGain or onRemove
    private val triggerTransform by
        locatedNode(
            transform(triggerAtom) map { (name, node) -> Trigger.Transform(node, name.text) }
        )
    private val triggerPrimary by triggerTransform or triggerAtom or group(parser { trigger })
    private val triggerAlternatives by
        separatedTerms(triggerPrimary, orKeyword) map
            {
              if (it.size == 1) it.first() else Trigger.Or(it)
            }
    private val byTrigger by
        triggerAlternatives and
            optional(skip(byKeyword) and expression) map
            { (trigger, actor) ->
              if (actor == null) trigger else Trigger.ByTrigger(trigger, actor)
            }
    val trigger: Parser<Trigger> by
        byTrigger and
            optional(skip(ifKeyword) and requirement) map
            { (trigger, condition) ->
              if (condition == null) trigger else Trigger.IfTrigger(trigger, condition)
            }
    private val effectSeparator by (doubleColon or colon) map { it.text == "::" }
    val effect: Parser<Effect> by
        trigger and
            effectSeparator and
            instructionTree map
            { (trigger, automatic, instruction) ->
              resolveTypeVariableNames(
                  Effect(trigger = trigger, automatic = automatic, instruction = instruction),
                  trigger,
                  instruction,
              )
            }

    // Property values and class declarations.
    val propertyValue: Parser<PropertyValue> by
        (metricKeyword asJust PropertyValue.MetricType) or
            (numberKeyword asJust PropertyValue.NumberType) or
            (requirementKeyword and skip(question) asJust PropertyValue.OptionalRequirementType) or
            (requirementKeyword asJust PropertyValue.RequirementType) or
            (skip(hasKeyword) and
                quotedPet(Requirement::class) map
                PropertyValue::RequirementValue) or
            (skip(countKeyword) and quotedPet(Metric::class) map PropertyValue::MetricValue) or
            (integer map PropertyValue::NumberValue)
    private val newlines by zeroOrMore(newline)
    private val dependencies by skip(langle) and commaSeparated(selectorExpression) and skip(rangle)
    private val supertypes by skip(colon) and commaSeparated(selectorExpression)
    private val classKind by
        (abstractKeyword and classKeyword asJust ABSTRACT) or (classKeyword asJust CONCRETE)
    private val signature by
        mapLocated(classKind and className and optional(dependencies) and optional(supertypes)) {
            (kind, name, deps, supes) ->
          ClassDeclaration(
              className = name,
              kind = kind,
              dependencies = deps.orEmpty(),
              supertypes =
                  buildSet {
                    supes.orEmpty().forEach { supertype ->
                      if (!add(supertype))
                          throw PetSyntaxException(
                              "duplicate supertype `$supertype` on `$name`",
                              sourceLocation = supertype.sourceLocation,
                          )
                    }
                  },
          )
        }
    private val invariant by skip(hasKeyword) and locatedNode(requirement) map ::InvariantElement
    private val gainDefault by
        skip(plus) and
            expression and
            quantifier map
            { (expression, quantifier) ->
              rejectRefinedDefault(expression)
              DefaultsDeclaration(
                  gainOnly = OneDefault(expression.arguments, quantifier),
                  forClass = expression.className,
              )
            }
    private val removeDefault by
        skip(minus) and
            expression and
            quantifier map
            { (expression, quantifier) ->
              rejectRefinedDefault(expression)
              DefaultsDeclaration(
                  removeOnly = OneDefault(expression.arguments, quantifier),
                  forClass = expression.className,
              )
            }
    private val universalDefault by
        expression map
            {
              rejectRefinedDefault(it)
              DefaultsDeclaration(universal = OneDefault(it.arguments), forClass = it.className)
            }
    private val defaults by
        skip(defaultKeyword) and
            (gainDefault or removeDefault or universalDefault) map
            ::DefaultsElement
    private val propertyAssignment by
        propertyName and
            skip(equals) and
            propertyValue map
            { (name, value) ->
              PropertyElement(name to value)
            }
    private val effectElement by locatedNode(effect) map ::EffectElement
    private val actionElement by locatedNode(action) map ::ActionElement
    private val bodyElementExceptNestedClasses: Parser<BodyElement> by
        invariant or defaults or propertyAssignment or effectElement or actionElement
    private val derivedClassBodyElement: Parser<BodyElement> by
        invariant or propertyAssignment or effectElement or actionElement
    private val nestedDeclaration by parser { declaration } map ::NestedDeclaration
    private val bodyElement by bodyElementExceptNestedClasses or nestedDeclaration
    // L11-5: only newline-separated bodies admit nested declarations.
    private val multilineBodyInterior by
        mapLocated(
            separatedTerms(bodyElement, oneOrMore(newline), acceptZero = true),
            ::Body,
        )
    private val multilineBody by
        skip(lbrace) and
            skip(newlines) and
            multilineBodyInterior and
            skip(newlines) and
            skip(rbrace)
    private val oneLineBody by oneLineBody(bodyElementExceptNestedClasses, acceptZero = false)
    private val declaration: Parser<List<ClassDeclaration>> by
        mapLocated(
            skip(newlines) and
                optional(quotedText and skip(newlines)) and
                signature and
                optional(multilineBody or oneLineBody)
        ) { (doc, signature, body) ->
          (body ?: Body()).toDeclarations(signature, doc)
        }
    // Pass this production directly to completionAtEnd: its analyzer understands combinators,
    // while a Grammar wrapper would fall back to executing the parser and its semantic maps.
    override val rootParser: Parser<List<ClassDeclaration>> by
        zeroOrMore(declaration) and skip(newlines) map { it.flatten() }
    // L12-4: owner-local bodies admit neither DEFAULT clauses nor nested declarations.
    private val derivedClassBody by oneLineBody(derivedClassBodyElement, acceptZero = true)
    val oneLineDeclaration: Parser<ClassDeclaration> by
        signature and
            optional(oneLineBody) map
            { (signature, body) ->
              (body ?: Body()).toDeclarations(signature).single()
            }

    private fun oneLineBody(element: Parser<BodyElement>, acceptZero: Boolean): Parser<Body> =
        mapLocated(
            skip(lbrace) and separatedTerms(element, semicolon, acceptZero) and skip(rbrace),
            ::Body,
        )

    private fun rejectRefinedDefault(expression: Expression) {
      if (expression.refinement != null)
          throw PetSyntaxException(
              "`DEFAULT` must name an unrefined class expression; found `$expression`",
              sourceLocation = expression.sourceLocation,
          )
    }

    /**
     * Resolves symmetric instruction-local scopes only after enclosing selectors have claimed their
     * references. The traversal remains inside-out so a transmutation still outranks an enclosing
     * sequence for names that no supplier already owns.
     */
    private fun resolveLocalTypeVariableNames(tree: InstructionTree): InstructionTree {
      if (
          tree.descendantsOfType<Instruction.Transmute>().isEmpty() &&
              tree.descendantsOfType<Instruction.Then>().isEmpty()
      ) {
        return tree
      }
      return object : PetTransformer() {
            override fun transformNode(node: PetNode): PetNode {
              // Symmetric scopes contain instructions; leave unrelated parser-only expression
              // metadata untouched until one of those scopes resolves its own subtree.
              if (node is Expression) return node
              val transformed = transformChildren(node)
              return when (transformed) {
                is Instruction.Transmute ->
                    Instruction.Transmute.resolveTypeVariableNames(transformed)
                is Instruction.Then -> Instruction.Then.resolveTypeVariableNames(transformed)
                else -> transformed
              }
            }
          }
          .transformInstructionTree(tree)
    }

    private data class AuthoredTypeVariableMarker(val name: String?)

    private fun expression(
        marker: AuthoredTypeVariableMarker?,
        clazz: ClassName,
        args: List<Expression>?,
        ref: Refinement?,
    ): Expression {
      val domain =
          Expression(
              clazz,
              args.orEmpty(),
              argumentsSpecified = args != null,
              typeVariableName =
                  marker?.let { Expression.TypeVariableName.Declaration(it.name, clazz) },
          )
      val boundRefinement = ref?.let {
        object : PetTransformer() {
              override fun transformNode(node: PetNode): PetNode =
                  when {
                    node is Metric.Rank && node.selector == null -> {
                      val metrics = node.metrics.map(::transformMetric)
                      node.copy(selector = domain.copy(typeVariableName = null), metrics = metrics)
                    }
                    node is Expression -> node
                    else -> transformChildren(node)
                  }
            }
            .transformRefinement(it)
      }
      return resolveClassLiteralTypeVariableNames(domain.copy(refinement = boundRefinement)).also {
        it.sourceLocation = clazz.sourceLocation
      }
    }

    // Keep regex matching anchored on every runtime. The pinned better-parse JS regex token
    // accesses Kotlin's private regex fields to set a sticky flag; Regex.matchAt needs no such
    // hook.
    private fun regexToken(
        regex: Regex,
        name: String = regex.pattern,
        ignored: Boolean = false,
    ): Token =
        token(name, ignored) { input, from -> regex.matchAt(input, from)?.value?.length ?: 0 }

    private fun keyword(word: String): Token = regexToken(Regex("$word\\b"), word)

    private inline fun <reified T> commaSeparated(parser: Parser<T>) = separatedTerms(parser, comma)

    private inline fun <reified T> group(parser: Parser<T>) =
        skip(lparen) and parser and skip(rparen)

    private inline fun <reified T> transform(parser: Parser<T>) =
        allCapsName and skip(lbracket) and parser and skip(rbracket)

    private fun location(token: TokenMatch): SourceLocation =
        SourceLocation(token.input.toString(), token.offset, token.length)

    /** Keep validation at a grammar production's authored start without hiding its grammar. */
    private inline fun <reified T, R> mapLocated(
        parser: Parser<T>,
        crossinline transform: (T) -> R,
    ): Parser<R> =
        sourcePosition and
            parser map
            { (location, value) ->
              try {
                transform(value)
              } catch (e: PetException) {
                if (e.sourceLocation == null) e.sourceLocation = location
                throw e
              } catch (e: IllegalArgumentException) {
                throw PetSyntaxException(e.message ?: "invalid Pets syntax", e, location)
              }
            }

    private inline fun <reified P : PetNode> locatedNode(parser: Parser<P>): Parser<P> =
        sourcePosition and
            parser map
            { (location, node) ->
              node.also { it.sourceLocation = location }
            }

    private fun <P : PetNode> quotedPet(type: KClass<P>): Parser<P> =
        quotedToken map
            { token ->
              val source = token.text.removeSurrounding("\"")
              fun rebase(location: SourceLocation): SourceLocation =
                  location.copy(
                      source = token.input.toString(),
                      offset = token.offset + 1 + location.offset,
                  )
              try {
                Parsing.parse(type, source).also { parsed ->
                  parsed.visitDescendants { node ->
                    node.sourceLocation
                        ?.takeIf { it.source == source }
                        ?.let { node.sourceLocation = rebase(it) }
                    true
                  }
                }
              } catch (e: PetException) {
                e.sourceLocation = e.sourceLocation?.let(::rebase) ?: location(token)
                throw e
              }
            }
  }

  // Declaration assembly: retain nested bodies until their enclosing class is known.
  internal class Body(elements: List<BodyElement> = emptyList()) {
    private val invariants = elements.filterIsInstance<InvariantElement>().map { it.invariant }
    private val defaults = elements.filterIsInstance<DefaultsElement>().map { it.defaults }
    private val effects = elements.filterIsInstance<EffectElement>().map { it.effect }
    private val actions = elements.filterIsInstance<ActionElement>().map { it.action }
    // Rule L11-9: reject a name assigned twice in one body, so declaration order
    // never becomes an accidental override rule.
    private val properties = buildMap {
      elements.filterIsInstance<PropertyElement>().forEach { element ->
        val (name, value) = element.property
        if (name in this)
            throw PetSyntaxException(
                "property `$name` is assigned twice: `${get(name)}` and `$value`",
                sourceLocation = name.sourceLocation,
            )
        put(name, value)
      }
    }
    private val nestedDeclarations = elements.filterIsInstance<NestedDeclaration>()

    fun asDerivedDeclaration(className: ClassName, supertype: Expression): ClassDeclaration =
        toDeclarations(ClassDeclaration(className, CONCRETE, supertypes = setOf(supertype)))
            .single()

    /** Container first, followed by its descendants in source order. */
    fun toDeclarations(
        header: ClassDeclaration,
        docstring: String? = null,
    ): List<ClassDeclaration> {
      val mergedDefaults = DefaultsDeclaration.merge(defaults)
      val declaration =
          resolveClassTypeVariableNames(
              header.copy(
                  invariants =
                      buildSet {
                        invariants.forEach { invariant ->
                          if (!add(invariant))
                              throw PetSyntaxException(
                                  "duplicate invariant `HAS $invariant` on `${header.className}`",
                                  sourceLocation = invariant.sourceLocation,
                              )
                        }
                      },
                  authoredEffects = effects,
                  authoredActions = actions,
                  defaultsDeclaration = mergedDefaults,
                  properties = properties,
                  extraNodes = actionSelectors(actions),
                  docstring = docstring,
              )
          )
      return buildList {
        add(declaration)
        nestedDeclarations.forEach { nested ->
          val child = nested.declarations.first()
          // L11-6: attach only the immediate child to this container. Its descendants already
          // name their own containers; preserve an explicitly supplied parent specialization.
          add(
              if (child.supertypes.any { it.className == header.className }) child
              else child.copy(supertypes = setOf(header.className.expression) + child.supertypes)
          )
          addAll(nested.declarations.drop(1))
        }
      }
    }

    sealed class BodyElement {
      class InvariantElement(val invariant: Requirement) : BodyElement()

      class DefaultsElement(val defaults: DefaultsDeclaration) : BodyElement()

      class PropertyElement(val property: Pair<PropertyName, PropertyValue>) : BodyElement()

      class EffectElement(val effect: Effect) : BodyElement()

      class ActionElement(val action: Action) : BodyElement()

      class NestedDeclaration(val declarations: List<ClassDeclaration>) : BodyElement()
    }
  }
}
