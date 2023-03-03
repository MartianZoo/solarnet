package dev.martianzoo.tfm.pets.ast

import com.github.h0tk3y.betterParse.combinators.map
import dev.martianzoo.tfm.pets.PetParser

public data class ClassName(private val asString: String) : PetNode(), Comparable<ClassName> {
  public companion object {
    public fun cn(name: String) = ClassName(name)

    private const val CLASS_NAME_PATTERN = "\\b[A-Z]([a-z][A-Za-z0-9_]*|[A-Z0-9]{0,4})\\b"
    private val classNameRegex = Regex(CLASS_NAME_PATTERN)
  }

  init {
    require(asString.matches(classNameRegex)) { "Bad class name: $asString" }
  }

  public val type = TypeExpr(this)

  public fun addArgs(specs: List<TypeExpr>) = type.addArgs(specs)
  public fun addArgs(vararg specs: TypeExpr) = addArgs(specs.toList())

  @JvmName("addArgsFromClassNames")
  public fun addArgs(specs: List<ClassName>) = addArgs(specs.map { it.type })
  public fun addArgs(vararg specs: ClassName) = addArgs(specs.toList())

  fun refine(requirement: Requirement?) = type.refine(requirement)

  public fun matches(regex: Regex) = asString.matches(regex)
  override fun toString() = asString

  override fun compareTo(other: ClassName) = asString.compareTo(other.asString)

  override val kind = ClassName::class.simpleName!!
  override fun visitChildren(visitor: Visitor) = Unit

  public object Parsing : PetParser() {
    val classShortName = _allCapsWordRE map { cn(it.text) } // currently unused
    val classFullName = _upperCamelRE map { cn(it.text) }
    val className = classFullName // or classShortName -- why does that break everything?
  }
}
