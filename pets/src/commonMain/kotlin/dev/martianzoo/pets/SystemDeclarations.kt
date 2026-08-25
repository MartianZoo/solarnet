package dev.martianzoo.pets

import dev.martianzoo.data.ClassDeclaration
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.util.toSetStrict

/** Pets runtime declarations that are available to every Authority. */
// TODO: Replace this temporary tfm-canon seam with the generic Catalog contract.
public val systemClassDeclarations: Set<ClassDeclaration> by lazy {
  parseClasses(readPetsResource("system.pets")).toSetStrict()
}

internal expect fun readPetsResource(filename: String): String
