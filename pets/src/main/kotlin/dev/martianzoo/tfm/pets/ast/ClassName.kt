package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.map
import dev.martianzoo.tfm.pets.PetParser
import dev.martianzoo.tfm.pets.ast.TypeExpr.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpr.GenericTypeExpr

data class ClassName(private val asString: String) : PetNode(), Comparable<ClassName> {
  companion object {
    fun cn(name: String) = ClassName(name)

    private const val CLASS_NAME_PATTERN = "\\b[A-Z]([a-z][A-Za-z0-9_]*|[A-Z0-9]{0,4})\\b"
    private val classNameRegex = Regex(CLASS_NAME_PATTERN)
  }

  init {
    require(asString.matches(classNameRegex)) { "Bad class name: $asString" }
  }

  val ptype = GenericTypeExpr(this)
  val literal = ClassLiteral(this)

  fun addArgs(specs: List<TypeExpr>) = ptype.addArgs(specs)
  fun addArgs(vararg specs: TypeExpr) = addArgs(specs.toList())

  @JvmName("addArgsFromClassNames")
  fun addArgs(specs: List<ClassName>) = addArgs(specs.map { it.ptype })
  fun addArgs(vararg specs: ClassName) = addArgs(specs.toList())

  fun matches(regex: Regex) = asString.matches(regex)

  override fun toString() = asString
  override fun compareTo(other: ClassName) = asString.compareTo(other.asString)

  override val kind = "ClassName"

  object Parsing : PetParser() {
    val classShortName = _allCapsWordRE map { cn(it.text) } // currently unused
    val classFullName = _upperCamelRE map { cn(it.text) }
    val className = classFullName // or classShortName -- why does that break everything?
  }
}
