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
import dev.martianzoo.tfm.data.shortName
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.ActionElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.DefaultsElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.EffectElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.InvariantElement
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Body.BodyElement.NestedDeclGroup
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.BodyElements.bodyElementExceptNestedClasses
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.NestableDecl.IncompleteNestableDecl
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Signatures.moreSignatures
import dev.martianzoo.tfm.pets.ClassDeclarationParsers.Signatures.signature
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Parsing.classFullName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.pets.ast.TypeExpr.TypeParsers.refinement
import dev.martianzoo.tfm.pets.ast.TypeExpr.TypeParsers.typeExpr
import dev.martianzoo.util.KClassMultimap
import dev.martianzoo.util.plus
import dev.martianzoo.util.toSetStrict

/** Parses the PETS language. */
public object ClassDeclarationParsers : PetParser() {

  /** Parses a section of `components.pets` etc. */
  public val topLevelDeclarationGroup by lazy { Declarations.topLevelGroup }

  /** Parses a one-line declaration such as found in `cards.json5` for cards like B10 (UNMI). */
  public val oneLineDeclaration by lazy { Declarations.oneLineDecl }

  // end public API

  internal val nls = zeroOrMore(char('\n'))

  // hack
  init {
    ClassName.Parsing.className
  }

  /*
   * These objects like [Signatures] are purely for grouping and to limit visibility of the
   * fine-grained details never needed again.
   */

  internal object Signatures {

    private val dependencies: Parser<List<DependencyDeclaration>> =
        optionalList(
            skipChar('<') and
                commaSeparated(typeExpr map ::DependencyDeclaration) and
                skipChar('>'))

    private val supertypeList: Parser<List<TypeExpr>> =
        optionalList(skipChar(':') and commaSeparated(typeExpr))

    /*
     * TODO I want to support letting classes declare a shortName as well, like
     *   CLASS OxygenStep[O2] ...
     * That's commented out below because it's blowing everything up
     */
    val signature: Parser<Signature> =
        classFullName and
            dependencies and
            optional(refinement) and
            // optional(skipChar('[') and parser { classShortName } and skipChar(']')) and
            supertypeList map
            { (name, deps, ref, /*short,*/ supes) ->
              Signature(name, deps, ref, /*short,*/ supes)
            }

    // This should only be included in the bodiless case
    val moreSignatures: Parser<MoreSignatures> =
        zeroOrMore(skipChar(',') and signature) map ::MoreSignatures
  }

  internal object BodyElements {
    private val invariant: Parser<Requirement> = skip(_has) and Requirement.parser()

    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and
            typeExpr and
            intensity map
            { (typeExpr, int) ->
              require(typeExpr.root == THIS)
              require(typeExpr.refinement == null)
              DefaultsDeclaration(gainOnlySpecs = typeExpr.args, gainIntensity = int)
            }

    private val allCasesDefault: Parser<DefaultsDeclaration> by lazy {
      typeExpr map
          {
            require(it.root == THIS)
            require(it.refinement == null)
            DefaultsDeclaration(universalSpecs = it.args)
          }
    }

    private val default: Parser<DefaultsDeclaration> =
        skip(_default) and (gainOnlyDefaults or allCasesDefault)

    val bodyElementExceptNestedClasses: Parser<BodyElement> =
        (invariant map ::InvariantElement) or
            (default map ::DefaultsElement) or
            (Effect.parser() map ::EffectElement) or
            (Action.parser() map ::ActionElement)
  }

  internal object Declarations {
    private val isAbstract: Parser<Boolean> =
        optional(_abstract) and skip(_class) map { it != null }

    private val bodyElement = parser { bodyElementExceptNestedClasses or nestedGroup }

    private val multilineBodyInterior: Parser<Body> =
        separatedTerms(bodyElement, oneOrMore(char('\n')), acceptZero = true) map ::Body

    private val multilineBody: Parser<Body> =
        skipChar('{') and skip(nls) and multilineBodyInterior and skip(nls) and skipChar('}')

    private val nestableGroup: Parser<NestableDeclGroup> =
        skip(nls) and
            isAbstract and
            signature and
            (multilineBody or moreSignatures) map
            { (abs, sig, bodyOrSigs) ->
              bodyOrSigs.convert(abs, sig)
            }

    // a declaration group that can be nested, that in this case *IS* nested
    private val nestedGroup: Parser<NestedDeclGroup> = nestableGroup map ::NestedDeclGroup

    // a declaration group that could've been nested but is *NOT*
    val topLevelGroup: Parser<List<ClassDeclaration>> = nestableGroup map { it.finishAll() }

    // For CardDefinition

    private val oneLineBody: Parser<Body> =
        skipChar('{') and
            separatedTerms(bodyElementExceptNestedClasses, char(';')) and
            skipChar('}') map
            ::Body

    val oneLineDecl: Parser<ClassDeclaration> =
        isAbstract and
            signature and
            optional(oneLineBody) map
            { (abs, sig, body) ->
              NestableDeclGroup(abs, sig, body ?: Body()).finishOnlyDecl()
            }
  }

  /*
   * The rest of the file is temporary types used only during parsing
   */

  internal data class Signature(val asClassDecl: ClassDeclaration) {
    constructor(
        className: ClassName,
        dependencies: List<DependencyDeclaration>,
        topInvariant: Requirement?,
        supertypes: List<TypeExpr>,
    ) : this(
        ClassDeclaration(
            name = className,
            id = shortName(className),
            dependencies = dependencies,
            supertypes = supertypes.toSetStrict(),
            topInvariant = topInvariant))
  }

  internal sealed class MoreSignaturesOrBody {
    abstract fun convert(abstract: Boolean, firstSignature: Signature): NestableDeclGroup
  }

  internal class MoreSignatures(private val moreSignatures: List<Signature>) :
      MoreSignaturesOrBody() {
    override fun convert(abstract: Boolean, firstSignature: Signature) =
        NestableDeclGroup(
            (firstSignature plus moreSignatures).map { IncompleteNestableDecl(abstract, it) })
  }

  internal class Body(private val elements: KClassMultimap<BodyElement>) : MoreSignaturesOrBody() {
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

  internal class NestableDeclGroup(private val declList: List<NestableDecl>) {
    constructor(
        abstract: Boolean,
        signature: Signature,
        body: Body
    ) : this(create(abstract, signature, body))

    fun unnestAllFrom(container: ClassName): List<NestableDecl> =
        declList.map { it.unnestOneFrom(container) }

    fun finishOnlyDecl() = declList.single().finishAtTopLevel()

    fun finishAll() = declList.map { it.finishAtTopLevel() }

    private companion object {
      private fun create(abstract: Boolean, signature: Signature, body: Body): List<NestableDecl> {
        val newDecl =
            signature.asClassDecl.copy(
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
      constructor(
          abstract: Boolean,
          signature: Signature
      ) : this(signature.asClassDecl.copy(abstract = abstract))

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
      init {
        decl.validate()
      }
      override fun unnestOneFrom(container: ClassName) = this
    }
  }
}
