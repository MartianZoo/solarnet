package dev.martianzoo.pets

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.util.toSetStrict

/** Pets runtime declarations that are available to every Catalog. */
// TODO: Replace this temporary tfm-canon seam with the generic Catalog contract.
public val systemClassDeclarations: Set<ClassDeclaration> by lazy {
  parseClasses(readPetsResource("system.pets")).toSetStrict()
}

internal expect fun readPetsResource(filename: String): String
