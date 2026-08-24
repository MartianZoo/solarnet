package dev.martianzoo.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.data.ClassDeclaration.ClassKind
import dev.martianzoo.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.data.ClassDeclaration.ClassKind.CONCRETE
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.data.ClassDeclaration.DefaultsDeclaration.OneDefault
import dev.martianzoo.pets.ClassParsing.Body.BodyElement
import dev.martianzoo.pets.ClassParsing.Body.BodyElement.ActionElement
import dev.martianzoo.pets.ClassParsing.Body.BodyElement.DefaultsElement
import dev.martianzoo.pets.ClassParsing.Body.BodyElement.EffectElement
import dev.martianzoo.pets.ClassParsing.Body.BodyElement.InvariantElement
import dev.martianzoo.pets.ClassParsing.Body.BodyElement.NestedDeclGroup
import dev.martianzoo.pets.ClassParsing.Body.BodyElement.PropertyElement
import dev.martianzoo.pets.ClassParsing.BodyElements.bodyElementExceptNestedClasses
import dev.martianzoo.pets.ClassParsing.BodyElements.derivedClassBodyElement
import dev.martianzoo.pets.ClassParsing.NestableDecl.IncompleteNestableDecl
import dev.martianzoo.pets.ClassParsing.Signatures.moreSignatures
import dev.martianzoo.pets.ClassParsing.Signatures.signature
import dev.martianzoo.pets.Transforming.actionListToEffects
import dev.martianzoo.pets.Transforming.actionSelectors
import dev.martianzoo.pets.ast.Action
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Parsing.classFullName
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.util.KClassMultimap
import dev.martianzoo.util.associateStrict
import dev.martianzoo.util.plus
import dev.martianzoo.util.toSetStrict

internal object ClassParsing : PetTokenizer() {
  private val nls = zeroOrMore(char('\n'))

  /*
   * These objects like [Signatures] are purely for grouping and to limit visibility of the
   * fine-grained details never needed again.
   */

  private object Signatures {

    private val dependencies: Parser<List<Expression>> =
        optionalList(
            skipChar('<') and
                commaSeparated(Expression.parser(allowDerivedClass = false)) and
                skipChar('>')
        )

    private val supertypeList: Parser<List<Expression>> =
        optionalList(skipChar(':') and commaSeparated(Expression.parser(allowDerivedClass = false)))

    val signature: Parser<Signature> =
        classFullName and
            dependencies and
            supertypeList map
            { (name, deps, supes) ->
              Signature(name, deps, supes)
            }

    // This should only be included in the bodiless case
    val moreSignatures: Parser<MoreSignatures> =
        zeroOrMore(skipChar(',') and signature) map ClassParsing::MoreSignatures
  }

  private object BodyElements {
    private val invariant: Parser<Requirement> = skip(_has) and Requirement.parser()

    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and
            Expression.parser() and
            intensity map
            { (expr, int) ->
              require(expr.refinement == null)
              DefaultsDeclaration(
                  gainOnly = OneDefault(expr.arguments, int),
                  forClass = expr.className,
              )
            }

    private val removeOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('-') and
            Expression.parser() and
            intensity map
            { (expr, int) ->
              require(expr.refinement == null)
              DefaultsDeclaration(
                  removeOnly = OneDefault(expr.arguments, int),
                  forClass = expr.className,
              )
            }

    private val allCasesDefault: Parser<DefaultsDeclaration> by lazy {
      Expression.parser() map
          {
            require(it.refinement == null)
            DefaultsDeclaration(universal = OneDefault(it.arguments), forClass = it.className)
          }
    }

    private val triggerOnlyDefaults: Parser<DefaultsDeclaration> =
        Expression.parser() and
            skipChar(':') map
            { expr ->
              require(expr.refinement == null)
              DefaultsDeclaration(
                  triggerOnly = OneDefault(expr.arguments),
                  forClass = expr.className,
              )
            }

    private val default: Parser<DefaultsDeclaration> =
        skip(_default) and
            (gainOnlyDefaults or removeOnlyDefaults or triggerOnlyDefaults or allCasesDefault)

    private val property: Parser<Pair<PropertyName, PropertyValue>> =
        PropertyName.parser() and
            skipChar('=') and
            PropertyValue.parser() map
            { (name, value) ->
              name to value
            }

    private val invariantElement = invariant map ::InvariantElement
    private val defaultsElement = default map ::DefaultsElement
    private val propertyElement = property map ::PropertyElement
    private val effectElement = Effect.parser() map { EffectElement(it) }
    private val actionElement = Action.parser() map { ActionElement(it) }

    val bodyElementExceptNestedClasses: Parser<BodyElement> =
        invariantElement or defaultsElement or propertyElement or effectElement or actionElement

    val derivedClassBodyElement: Parser<BodyElement> =
        invariantElement or propertyElement or effectElement or actionElement
  }

  internal object Declarations {
    private val kind: Parser<ClassKind> =
        (_abstract and _class asJust ABSTRACT) or (_class asJust CONCRETE)

    private val bodyElement = parser { bodyElementExceptNestedClasses or nestedGroup }

    private val multilineBodyInterior: Parser<Body> =
        separatedTerms(bodyElement, oneOrMore(char('\n')), acceptZero = true) map ClassParsing::Body

    private val multilineBody: Parser<Body> =
        skipChar('{') and skip(nls) and multilineBodyInterior and skip(nls) and skipChar('}')

    private val oneLineBody: Parser<Body> by lazy {
      oneLineBodyParser(bodyElementExceptNestedClasses, acceptZero = false)
    }

    private val docstring: Parser<String> = quotedText

    private val nestableGroup: Parser<NestableDeclGroup> =
        skip(nls) and
            optional(docstring and skip(nls)) and
            kind and
            signature and
            (multilineBody or oneLineBody or moreSignatures) map
            { (doc, kind, sig, bodyOrSigs) ->
              bodyOrSigs.convert(kind, sig, doc)
            }

    // a declaration group that can be nested, that in this case *IS* nested
    private val nestedGroup: Parser<NestedDeclGroup> = nestableGroup map ::NestedDeclGroup

    // a declaration group that could've been nested but is *NOT*
    val topLevelGroup: Parser<List<ClassDeclaration>> = nestableGroup map { it.finishAll() }

    val declarationFile: Parser<List<ClassDeclaration>> =
        zeroOrMore(topLevelGroup) and skip(nls) map { it.flatten() }

    // Single-line and owner-local derived Class bodies

    private fun oneLineBodyParser(
        bodyElement: Parser<BodyElement>,
        acceptZero: Boolean,
    ): Parser<Body> =
        skipChar('{') and
            separatedTerms(bodyElement, char(';'), acceptZero = acceptZero) and
            skipChar('}') map
            ClassParsing::Body

    val derivedClassBody: Parser<Body> by lazy {
      oneLineBodyParser(derivedClassBodyElement, acceptZero = true)
    }

    val oneLineDecl: Parser<ClassDeclaration> =
        kind and
            signature and
            optional(oneLineBody) map
            { (kind, sig, body) ->
              NestableDeclGroup(kind, sig, body ?: Body()).finishOnlyDecl()
            }
  }

  // The rest of the file is temporary types used only during parsing.

  internal data class Signature(val asDeclaration: ClassDeclaration) :
      HasClassName by asDeclaration {
    constructor(
        className: ClassName,
        dependencies: List<Expression>,
        supertypes: List<Expression>,
    ) : this(
        ClassDeclaration(
            className = className,
            kind = ABSTRACT, // needs to be overwritten!
            dependencies = dependencies,
            supertypes = supertypes.toSetStrict(),
        ),
    )
  }

  internal sealed class MoreSignaturesOrBody {
    abstract fun convert(
        kind: ClassKind,
        firstSignature: Signature,
        docstring: String?,
    ): NestableDeclGroup
  }

  private class MoreSignatures(private val moreSignatures: List<Signature>) :
      MoreSignaturesOrBody() {
    override fun convert(kind: ClassKind, firstSignature: Signature, docstring: String?) =
        NestableDeclGroup(
            (firstSignature plus moreSignatures).map {
              IncompleteNestableDecl(kind, it, docstring)
            },
        )
  }

  internal class Body(private val elements: KClassMultimap<BodyElement>) : MoreSignaturesOrBody() {
    constructor(list: List<BodyElement> = emptyList()) : this(KClassMultimap(list))

    override fun convert(kind: ClassKind, firstSignature: Signature, docstring: String?) =
        NestableDeclGroup(kind, firstSignature, this, docstring)

    private inline fun <reified E : BodyElement> getAll() = elements.get<E>()

    val invariants = getAll<InvariantElement>().map { it.invariant }
    val defaultses = getAll<DefaultsElement>().map { it.defaults }
    val effects = getAll<EffectElement>().map { it.effect }
    val actions = getAll<ActionElement>().map { it.action }
    val properties = getAll<PropertyElement>().associateStrict { it.property }
    val nestedGroups = getAll<NestedDeclGroup>().map { it.declGroup }

    fun asDerivedDeclaration(className: ClassName, supertype: Expression): ClassDeclaration =
        NestableDeclGroup(
                CONCRETE,
                Signature(className, emptyList(), listOf(supertype)),
                this,
            )
            .finishOnlyDecl()

    sealed class BodyElement {
      class InvariantElement(val invariant: Requirement) : BodyElement()

      class DefaultsElement(val defaults: DefaultsDeclaration) : BodyElement()

      class PropertyElement(val property: Pair<PropertyName, PropertyValue>) : BodyElement()

      class EffectElement(val effect: Effect) : BodyElement()

      class ActionElement(val action: Action) : BodyElement()

      class NestedDeclGroup(val declGroup: NestableDeclGroup) : BodyElement()
    }
  }

  internal class NestableDeclGroup(private val declList: List<NestableDecl>) {
    constructor(
        kind: ClassKind,
        signature: Signature,
        body: Body,
        docstring: String? = null,
    ) : this(create(kind, signature, body, docstring))

    private fun unnestAllFrom(container: ClassName): List<NestableDecl> = declList.map {
      it.unnestOneFrom(container)
    }

    fun finishOnlyDecl() = declList.single().decl

    fun finishAll() = declList.map { it.decl }

    private companion object {
      fun create(
          kind: ClassKind,
          signature: Signature,
          body: Body,
          docstring: String?,
      ): List<NestableDecl> {
        val mergedDefaults = DefaultsDeclaration.merge(body.defaultses)
        require(mergedDefaults.forClass in setOf(null, signature.className))
        val newDecl =
            signature.asDeclaration.copy(
                kind = kind,
                invariants = body.invariants.toSetStrict(),
                effects = body.effects + actionListToEffects(body.actions),
                defaultsDeclaration = mergedDefaults,
                properties = body.properties,
                extraNodes = actionSelectors(body.actions),
                docstring = docstring,
            )
        val unnested = body.nestedGroups.flatMap { it.unnestAllFrom(signature.className) }
        return IncompleteNestableDecl(newDecl) plus unnested
      }
    }
  }

  internal sealed class NestableDecl {
    abstract val decl: ClassDeclaration

    abstract fun unnestOneFrom(container: ClassName): NestableDecl

    private data class CompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
      override fun unnestOneFrom(container: ClassName) = this
    }

    data class IncompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
      constructor(
          kind: ClassKind,
          signature: Signature,
          docstring: String?,
      ) : this(signature.asDeclaration.copy(kind = kind, docstring = docstring))

      // This returns a new NestableDecl that looks like it could be a sibling to containingClass
      // instead of nested inside it
      override fun unnestOneFrom(container: ClassName): NestableDecl {
        return if (decl.supertypes.any { it.className == container }) {
          CompleteNestableDecl(decl)
        } else {
          val supertypes = (container.expression plus decl.supertypes).toSetStrict()
          CompleteNestableDecl(decl.copy(supertypes = supertypes))
        }
      }
    }
  }
}
