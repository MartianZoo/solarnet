package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.oneOrMore
import com.github.h0tk3y.betterParse.combinators.optional
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.separatedTerms
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.tfm.data.ClassDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DefaultsDeclaration
import dev.martianzoo.tfm.data.ClassDeclaration.DependencyDeclaration
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Declarations.ActionElement
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Declarations.BodyElement
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Declarations.DefaultsElement
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Declarations.EffectElement
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Declarations.InvariantElement
import dev.martianzoo.tfm.pets.ClassDeclarationParser.Declarations.NestableDeclsElement
import dev.martianzoo.tfm.pets.PetParser.Actions.action
import dev.martianzoo.tfm.pets.PetParser.Effects.effect
import dev.martianzoo.tfm.pets.PetParser.Instructions
import dev.martianzoo.tfm.pets.PetParser.Requirements.requirement
import dev.martianzoo.tfm.pets.PetParser.Types
import dev.martianzoo.tfm.pets.PetParser.Types.typeExpression
import dev.martianzoo.tfm.pets.PetParser._abstract
import dev.martianzoo.tfm.pets.PetParser._class
import dev.martianzoo.tfm.pets.PetParser._default
import dev.martianzoo.tfm.pets.PetParser._has
import dev.martianzoo.tfm.pets.PetParser.char
import dev.martianzoo.tfm.pets.PetParser.commaSeparated
import dev.martianzoo.tfm.pets.PetParser.nls
import dev.martianzoo.tfm.pets.PetParser.optionalList
import dev.martianzoo.tfm.pets.PetParser.skipChar
import dev.martianzoo.tfm.pets.PetParser.tokenizer
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.SpecialClassNames.THIS
import dev.martianzoo.tfm.pets.ast.Action
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.Effect
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import dev.martianzoo.util.KClassMultimap
import dev.martianzoo.util.toSetStrict

/** Parses the Petaform language. */
object ClassDeclarationParser {

  /**
   * Parses an entire PETS class declarations source file.
   */
  fun parseClassDeclarations(text: String): List<ClassDeclaration> {
    val tokens = tokenizer.tokenize(stripLineComments(text))
    return parseRepeated(Declarations.nestedDecls, tokens)
  }

  // TODO move
  internal fun stripLineComments(text: String) = Regex(""" *(//[^\n]*)*\n""").replace(text, "\n")

  private object Declarations { // -------------------------------------------------------

    private val isAbstract: Parser<Boolean> = optional(_abstract) and skip(_class) map { it != null }

    private val dependency: Parser<DependencyDeclaration> = typeExpression map ::DependencyDeclaration

    private val dependencies: Parser<List<DependencyDeclaration>> =
        optionalList(skipChar('<') and commaSeparated(dependency) and skipChar('>'))

    private val supertypes: Parser<List<GenericTypeExpression>> =
        optionalList(skipChar(':') and commaSeparated(parser { Types.genericType }))

    private val signature: Parser<Signature> =
        parser { Types.classFullName } and
        dependencies and
        parser { Types.optlRefinement } and
        // optional(skipChar('[') and parser {Types.classShortName} and skipChar(']')) and
        supertypes map {
          (c, d, r, s) -> Signature(c, d, r, s)
        }

    private val moreSignatures: Parser<List<Signature>> =
        skipChar(',') and separatedTerms(signature, char(','))

    private val gainOnlyDefaults: Parser<DefaultsDeclaration> =
        skipChar('+') and Types.genericType and Instructions.intensity map { (type, int) ->
          require(type.className == THIS)
          require(type.refinement == null)
          DefaultsDeclaration(gainOnlySpecs = type.specs, gainIntensity = int)
        }

    private val allCasesDefault: Parser<DefaultsDeclaration> = Types.genericType map {
      require(it.className == THIS)
      require(it.refinement == null)
      DefaultsDeclaration(universalSpecs = it.specs)
    }

    private val default: Parser<DefaultsElement> =
        skip(_default) and (gainOnlyDefaults or allCasesDefault) map ::DefaultsElement

    private val invariant: Parser<InvariantElement> = skip(_has) and requirement map ::InvariantElement

    sealed class BodyElement
    class InvariantElement(val invariant: Requirement) : BodyElement()
    class DefaultsElement(val defaults: DefaultsDeclaration) : BodyElement()
    class ActionElement(val action: Action) : BodyElement()
    class EffectElement(val effect: Effect) : BodyElement()
    class NestableDeclsElement(val decls: List<NestableDecl>) : BodyElement()

    private val bodyElementNoClass: Parser<BodyElement> =
        default or
        invariant or
        (action map ::ActionElement) or
        (effect map ::EffectElement)

    private val oneLineBody: Parser<KClassMultimap<BodyElement>> =
        skipChar('{') and
        separatedTerms(bodyElementNoClass, char(';')) and
        skipChar('}') map {
          KClassMultimap(it)
        }

    val singleDecl: Parser<ClassDeclaration> =
        isAbstract and signature and optional(oneLineBody) map { (abs, sig, body) ->
          createIncomplete(abs, sig, body ?: KClassMultimap()).first().finish()
        }

    val bodyElement: Parser<BodyElement> = bodyElementNoClass or parser { nestableDecls }

    private val multilineBodyInterior: Parser<List<BodyElement>> =
        separatedTerms(bodyElement, oneOrMore(char('\n')), acceptZero = true)

    private val multilineBody: Parser<Body> =
        skipChar('{') and
        skip(nls) and
        multilineBodyInterior and
        skip(nls) and
        skipChar('}') map ::Body

    class Body(val elements: KClassMultimap<BodyElement>) {
      constructor(list: List<BodyElement>) : this(KClassMultimap(list))
    }
    class Signatures(val signatures: List<Signature>)

    val signatures: Parser<Signatures> = optionalList(moreSignatures) map :: Signatures

    private val nestableDecls: Parser<NestableDeclsElement> =
        skip(nls) and
        isAbstract and
        signature and
        (multilineBody or signatures) map { (abs, sig, bodyOrSigs: Any) ->
          when (bodyOrSigs) {
            is Body -> {
              NestableDeclsElement(createIncomplete(abs, sig, bodyOrSigs.elements))
            }
            is Signatures -> {
              val combinedSigs: List<Signature> = listOf(sig) + bodyOrSigs.signatures
              NestableDeclsElement(combinedSigs.flatMap {
                createIncomplete(abs, it)
              })
            }
            else -> error("")
          }
        }

    val nestedDecls: Parser<List<ClassDeclaration>> =
        nestableDecls map { decls -> decls.decls.map { it.finish() } }
  }

  private fun createIncomplete(
      abst: Boolean,
      sig: Signature,
      contents: KClassMultimap<BodyElement> = KClassMultimap()
  ): List<NestableDecl> {
    val invs = contents.get<InvariantElement>().map { it.invariant }.toSetStrict()
    val defs = contents.get<DefaultsElement>().map { it.defaults }.toSetStrict()
    val effs = contents.get<EffectElement>().map { it.effect }.toSetStrict()
    val acts = contents.get<ActionElement>().map { it.action }.toSetStrict()
    val subs = contents.get<NestableDeclsElement>().map { it.decls }.toSetStrict()

    val mergedDefaults = DefaultsDeclaration(
        universalSpecs = defs.firstNotNullOfOrNull { it.universalSpecs } ?: listOf(),
        gainOnlySpecs = defs.firstNotNullOfOrNull { it.gainOnlySpecs } ?: listOf(),
        gainIntensity = defs.firstNotNullOfOrNull { it.gainIntensity },
    )

    val comp = ClassDeclaration(
        id = sig.className, // TODO
        name = sig.className,
        abstract = abst,
        dependencies = sig.dependencies,
        supertypes = sig.supertypes.toSetStrict(),
        topInvariant = sig.topInvariant,
        otherInvariants = invs,
        effectsRaw = effs + actionsToEffects(acts),
        defaultsDeclaration = mergedDefaults,
    )
    return listOf(NestableDecl(comp, false)) + subs.flatten().map { it.withSuperclass(sig.className) }
  }

  val oneLineClassDeclaration = Declarations.singleDecl

  private class Signature(
      val className: ClassName,
      val dependencies: List<DependencyDeclaration>,
      val topInvariant: Requirement?,
      val supertypes: List<GenericTypeExpression>,
  )

  internal data class NestableDecl(
      private val declaration: ClassDeclaration,
      internal val isComplete: Boolean,
  ) {
    fun withSuperclass(className: ClassName): NestableDecl =
        if (className == COMPONENT || isComplete) {
          this
        } else if (declaration.supertypes.any { it.className == className }) {
          this // it's redundant to add it again
        } else {
          val allSupertypes = listOf(className.type) + declaration.supertypes
          val fixedDeclaration = declaration.copy(supertypes = allSupertypes.toSetStrict())
          NestableDecl(fixedDeclaration, true)
        }

    internal fun finish(): ClassDeclaration { // TODO
      val supes = declaration.supertypes
      return when {
        declaration.name == COMPONENT -> declaration.also { require(supes.isEmpty()) }
        supes.isEmpty() -> declaration.copy(supertypes = setOf(COMPONENT.type))
        else -> declaration.also {
          require(COMPONENT.type !in supes) {
            "${declaration.name}: ${declaration.supertypes}"
          }
        }
      }
    }
  }
}
