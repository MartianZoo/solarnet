package dev.martianzoo.tfm.cardgenerator

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.Parsing.parseOneLinerClass
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.data.ClassDeclaration
import dev.martianzoo.pets.data.ClassDeclaration.ClassKind.ABSTRACT
import dev.martianzoo.tfm.carddata.CardData
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

/** The authored class hierarchy needed while card declarations are still being generated. */
internal class RawClassIndex(declarations: List<ClassDeclaration>) {
  private val byName = declarations.associateBy(ClassDeclaration::className)

  internal fun isSubtypeOf(name: ClassName, parent: ClassName): Boolean =
      name == parent || byName[name]?.supertypes?.any { isSubtypeOf(it.className, parent) } == true

  internal fun concreteSubtypes(parent: ClassName): List<ClassName> =
      byName.values
          .filter { it.kind != ABSTRACT && isSubtypeOf(it.className, parent) }
          .map(ClassDeclaration::className)
          .sortedBy(ClassName::toString)

  companion object {
    internal fun fromSources(directory: Path): RawClassIndex {
      val authored =
          Files.walk(directory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) && it.extension == "pets" }
                .sorted()
                .toList()
                .flatMap { parseClasses(it.readText()) }
          }
      val components =
          CardData.bundleNames.flatMap { bundle ->
            CardData.definitions(bundle).flatMap { card ->
              card.components.map(::parseOneLinerClass)
            }
          }
      return RawClassIndex(authored + components)
    }
  }
}
