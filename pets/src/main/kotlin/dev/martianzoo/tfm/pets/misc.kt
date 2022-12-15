package dev.martianzoo.tfm.pets

fun classNamePattern(): Regex {
  val CLASS_NAME_PATTERN by lazy { Regex("^[A-Z][a-z][A-Za-z0-9_]*$") }
  return CLASS_NAME_PATTERN
}

internal fun actionToEffect(action: Action, index: Int) : Effect  {
  val merged = if (action.cost == null) {
    action.instruction
  } else {
    Instruction.then(action.cost.toInstruction(), action.instruction)
  }
  return Effect(PetsParser.parse("UseAction${index}<This>"), merged)
}

fun pad(s: Any, width: Int) = ("$s" + " ".repeat(width)).substring(0, width)
