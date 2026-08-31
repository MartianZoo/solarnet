package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.toSetStrict

/** Pets runtime declarations that are available to every Catalog. */
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

    "No one but Engine can create these"
    ABSTRACT CLASS System : Hidden {
      This BY !Engine: Die
    }

    // Anything that cannot exist after the task queue clears (i.e., the action ends)
    ABSTRACT CLASS MustCleanUp : Hidden

    "Instances are removed automatically whenever every task queue is empty"
    ABSTRACT CLASS Temporary : Hidden

    "Something the player must remove to unblock some other task (i.e., `MAX 0 Barrier:` is common"
    ABSTRACT CLASS Barrier : MustCleanUp

    "A type that immediately deletes itself; you'll never observe it existing, but it's used to trigger things"
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

    "The very first component created, which kicks the rest off and performs system operations"
    CLASS Engine : System, Actor { HAS MAX 1 This }

    "Gaining `Ok` is the standard 'do-nothing' instruction; can't trigger anything"
    CLASS Ok : Signal

    "A component you can't create; the task queue will refuse to enqueue an attempt to"
    CLASS Die : Signal { HAS MAX 0 This }
    """
        .trimIndent() + "\n"
