package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DependencyDeclaration
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.ActionElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.DefaultsElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.EffectElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.InvariantElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.NestedDeclGroup
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Declarations.nestedGroup
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.NestableDecl.IncompleteNestableDecl
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.classFullName
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.genericType
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.refinement
import dev.martianzoo.tfm.pets.ast.TypeExpression.TypeParsers.typeExpression
import dev.martianzoo.util.KClassMultimap
import dev.martianzoo.util.onlyElement
import dev.martianzoo.util.plus
import dev.martianzoo.util.toSetStrict

/** Parses the Petaform language. */
internal object ClassDeclarationParsers : PetParser() {
  internal val nls = zeroOrMore(char('\n'))

  internal object Signatures {

    private val dependencies: Parser<List<DependencyDeclaration>> = optionalList(
        skipChar('<') and
        commaSeparated(typeExpression map ::DependencyDeclaration) and
        skipChar('>')
    )

    private val supertypes: Parser<List<GenericTypeExpression>> =
        optionalList(skipChar(':') and commaSeparated(genericType))

    val signature: Parser<Signature> =
        parser { classFullName } and
        dependencies and
        optional(parser { refinement }) and
        // optional(skipChar('[') and parser {Types.classShortName} and skipChar(']')) and
        supertypes map { (name, deps, refin, supes) ->
          Signature(name, deps, refin, supes)
        }

    val moreSignatures: Parser<MoreSignatures> =
        zeroOrMore(skipChar(',') and signature) map ::MoreSignatures
  }

  internal object BodyElements {
    private val invariant: Parser<Requirement> = skip(_has) and Requirement.parser()

    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and genericType and intensity map { (type, int) ->
          require(type.root == THIS)
          require(type.refinement == null)
          DefaultsDeclaration(gainOnlySpecs = type.args, gainIntensity = int)
        }

    private val allCasesDefault: Parser<DefaultsDeclaration> =
        parser { genericType } map {
          require(it.root == THIS)
          require(it.refinement == null)
          DefaultsDeclaration(universalSpecs = it.args)
        }

    private val default: Parser<DefaultsDeclaration> =
        skip(_default) and (gainOnlyDefaults or allCasesDefault)


    val bodyElementNoClass: Parser<BodyElement> =
        (invariant map ::InvariantElement) or
        (default map ::DefaultsElement) or
        (Effect.parser() map ::EffectElement) or
        (Action.parser() map ::ActionElement)

    val bodyElement: Parser<BodyElement> = bodyElementNoClass or parser { nestedGroup }
  }

  internal object Declarations {
    private val isAbstract: Parser<Boolean> = optional(_abstract) and skip(_class) map { it != null }

    private val multilineBodyInterior: Parser<Body> =
        separatedTerms(BodyElements.bodyElement, oneOrMore(char('\n')), acceptZero = true) map ::Body

    private val multilineBody: Parser<Body> =
        skipChar('{') and skip(nls) and
            multilineBodyInterior and
            skip(nls) and skipChar('}')

    private val nestableGroup: Parser<NestableDeclGroup> =
        skip(nls) and
            isAbstract and
            Signatures.signature and
            (multilineBody or Signatures.moreSignatures) map {
          (abs, sig, bodyOrSigs) -> bodyOrSigs.convert(abs, sig)
        }

    // they not just *can* be nested, they *are* nested
    val nestedGroup: Parser<NestedDeclGroup> = nestableGroup map ::NestedDeclGroup

    // they *can* be nested, but they are *not*
    val topLevelGroup: Parser<List<ClassDeclaration>> = nestableGroup map { it.finishAll() }

    // For CardDefinition

    private val oneLineBody: Parser<Body> =
        skipChar('{') and
            separatedTerms(BodyElements.bodyElementNoClass, char(';')) and
            skipChar('}') map ::Body

    val oneLineDecl: Parser<ClassDeclaration> =
        isAbstract and Signatures.signature and optional(oneLineBody) map { (abs, sig, body) ->
          NestableDeclGroup(abs, sig, body ?: Body()).finishOnlyDecl()
        }
  }

  val topLevelGroup = Declarations.topLevelGroup
  val oneLineDecl = Declarations.oneLineDecl

  internal data class Signature(val asClassDecl: ClassDeclaration) {
    constructor(
        className: ClassName,
        dependencies: List<DependencyDeclaration>,
        topInvariant: Requirement?,
        supertypes: List<GenericTypeExpression>,
    ) : this(ClassDeclaration(
        abstract = true,
        name = className,
        id = className,
        dependencies = dependencies,
        topInvariant = topInvariant,
        supertypes = supertypes.toSetStrict()
    ))
  }

  internal sealed class MoreSignaturesOrBody {
    abstract fun convert(abstract: Boolean, firstSignature: Signature): NestableDeclGroup
  }

  internal class MoreSignatures(val moreSignatures: List<Signature>) : MoreSignaturesOrBody() {
    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(
            (firstSignature plus moreSignatures).map {
              IncompleteNestableDecl(abstract, it)
            })
  }

  internal class Body(val elements: KClassMultimap<BodyElement>) : MoreSignaturesOrBody() {
    constructor(list: List<BodyElement> = listOf()) : this(KClassMultimap(list))

    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(abstract, firstSignature, this)

    private inline fun <reified E : BodyElement> getAll() = elements.get<E>()

    val invariants = getAll<InvariantElement>().map { it.invariant }
    val defaultses = getAll<DefaultsElement>().map { it.defaults }
    val effects = getAll<EffectElement>().map { it.effect }
    val actions = getAll<ActionElement>().map { it.action }
    val nestedGroups = getAll<NestedDeclGroup>().map { it.declGroup }

    sealed class BodyElement {
      internal class InvariantElement(val invariant: Requirement) : BodyElement()
      internal class DefaultsElement(val defaults: DefaultsDeclaration) : BodyElement()
      internal class EffectElement(val effect: Effect) : BodyElement()
      internal class ActionElement(val action: Action) : BodyElement()
      internal class NestedDeclGroup(val declGroup: NestableDeclGroup) : BodyElement()
    }
  }

  internal class NestableDeclGroup(val declList: List<NestableDecl>) {
    constructor(abstract: Boolean, signature: Signature, body: Body) :
        this(create(abstract, signature, body))

    fun unnestAllFrom(container: ClassName): List<NestableDecl> =
        declList.map { it.unnestOneFrom(container) }

    fun finishOnlyDecl() = declList.onlyElement().finishAtTopLevel()

    fun finishAll() = declList.map { it.finishAtTopLevel() }

    private companion object {
      private fun create(abstract: Boolean, signature: Signature, body: Body): List<NestableDecl> {
        val newDecl = signature.asClassDecl.copy(
            abstract = abstract,
            otherInvariants = body.invariants.toSetStrict(),
            effectsRaw = (body.effects + actionsToEffects(body.actions)).toSetStrict(),
            defaultsDeclaration = DefaultsDeclaration.merge(body.defaultses),
        )
        val unnested = body.nestedGroups.flatMap { it.unnestAllFrom(signature.asClassDecl.name) }
        return IncompleteNestableDecl(newDecl) plus unnested
      }
    }
  }

  internal sealed class NestableDecl {
    abstract val decl: ClassDeclaration
    abstract fun unnestOneFrom(container: ClassName): NestableDecl

    fun finishAtTopLevel(): ClassDeclaration { // TODO
      if (decl.name != COMPONENT && decl.supertypes.isEmpty()) {
        return decl.copy(supertypes = setOf(COMPONENT.type)).also { it.validate() }
      }
      return decl.also { it.validate() }
    }

    data class IncompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
      constructor(abstract: Boolean, signature: Signature) :
          this(signature.asClassDecl.copy(abstract = abstract))

      // This returns a new NestableDecl that looks like it could be a sibling to containingClass
      // instead of nested inside it
      override fun unnestOneFrom(container: ClassName): NestableDecl {
        return when {
          // the class name we'd insert is already there (TODO be even smarter)
          decl.supertypes.any { it.root == container } -> CompleteNestableDecl(decl)

          // jam the superclass in and mark it complete
          else -> CompleteNestableDecl(prependSuperclass(container))
        }
      }

      private fun prependSuperclass(superclassName: ClassName): ClassDeclaration {
        val allSupertypes = superclassName.type plus decl.supertypes
        return decl.copy(supertypes = allSupertypes.toSetStrict())
      }
    }

    data class CompleteNestableDecl(override val decl: ClassDeclaration) : NestableDecl() {
      init { decl.validate() }
      override fun unnestOneFrom(container: ClassName) = this
    }
  }
}
