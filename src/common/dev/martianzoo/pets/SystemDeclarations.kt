package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.toSetStrict

/**
 * Pets runtime declarations that are available to every Catalog, as required by
 * [rule L1-13](https://github.com/MartianZoo/solarnet/blob/main/docs/pets-language-spec.md#1-source-and-declarations):
 * the classes this language and the type system depend on, including `Component` and `Class`, the
 * ownership vocabulary `Anyone`, `Owner` and `Owned`, the actor root `Actor`, the signals `Ok` and
 * `Die`, and `Atomized` and `Custom`. A catalog's own source is loaded alongside them.
 *
 * Which of these a particular *game* then contains is `OPTIONS.md`'s question, not this module's.
 */
// TODO: Replace this temporary tfm-canon seam with the generic Catalog contract.
public val systemClassDeclarations: Set<ClassDeclaration> by lazy {
  parseClasses(systemDeclarationsSource).toSetStrict()
}

private val systemDeclarationsSource =
    """
    "The single root of the class hierarchy"
    ABSTRACT CLASS Component {
      DEFAULT +Component!
      DEFAULT -Component!
    }

    "Magic rules: only a class name can go inside `<>`; `Class<Foo>` is concrete iff `Foo` is"
    CLASS Class<Component> : System { HAS =1 This }

    "Instances of this type never exist; Kotlin can instead define its instruction or metric behavior"
    ABSTRACT CLASS Custom

    "Extend this to have plural instructions automatically split into individual instructions"
    ABSTRACT CLASS Atomized

    "Implementation detail normally omitted from user-facing output"
    ABSTRACT CLASS Hidden

    "No one but Admin can create these"
    ABSTRACT CLASS System : Hidden {
      This BY Actor(NOT Admin): Die
    }

    "Anything that cannot remain once its owning operation or scope completes"
    ABSTRACT CLASS MustCleanUp : Hidden

    "Instances are removed at an empty task queue once no dependent Temporary or MustCleanUp remains"
    ABSTRACT CLASS Temporary

    "A lifetime anchor for components that depend on it"
    ABSTRACT CLASS Scope

    "A child Scope removed after queued work and dependent cleanup finish"
    ABSTRACT CLASS TemporaryScope<Scope> : Scope, Temporary, MustCleanUp {
      HAS MAX 1 This
    }

    "Something the player must remove to unblock some other task (i.e., `MAX 0 Barrier:` is common"
    ABSTRACT CLASS Barrier : MustCleanUp

    "An unscoped point event that removes itself immediately after triggering effects"
    ABSTRACT CLASS Signal : MustCleanUp {
      This:: -This!
    }

    "An entity that can initiate or continue game operations"
    ABSTRACT CLASS Actor

    "The unrestricted target for an ownership dependency"
    ABSTRACT CLASS Anyone

    "An entity that can own Components"
    ABSTRACT CLASS Owner : Anyone

    "A Component whose Type carries an ownership dependency"
    ABSTRACT CLASS Owned<Anyone> {
      DEFAULT Owned<Owner>
    }

    "The neutral table administrator created first to perform system operations"
    CLASS Admin : System, Actor { HAS =1 This }

    "Gaining `Ok` is the standard 'do-nothing' instruction; can't trigger anything"
    CLASS Ok : Signal

    "A component you can't create; the task queue will refuse to enqueue an attempt to"
    CLASS Die : Signal { HAS MAX 0 This }
    """
        .trimIndent() + "\n"
