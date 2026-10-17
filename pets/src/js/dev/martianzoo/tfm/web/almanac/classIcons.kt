package dev.martianzoo.tfm.web.almanac

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.types.Class
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLImageElement

/** Match Playground's inline icons while retaining text until artwork has loaded. */
internal fun appendClassIcon(button: Element, klass: Class) {
  val name = klass.className.toString()
  val table = klass.classTable
  if (
      name in setOf("Hand", "Production") ||
          (!klass.abstract && klass.isSubtypeOf(table.getClass(cn("CardFront")))) ||
          klass.isSubtypeOf(table.getClass(cn("StandardProject")))
  )
      return
  val label = document.createElement("span")
  label.className = "class-name"
  label.textContent = name
  button.textContent = ""
  button.appendChild(label)
  button.setAttribute("title", name)
  button.setAttribute("aria-label", name)
  val image = document.createElement("img") as HTMLImageElement
  image.className = "class-icon"
  image.alt = ""
  val sources =
      if (name == "MC") listOf("$IMAGE_ROOT/classes/MC.png")
      else listOf("$IMAGE_ROOT/classes/30px/$name.png", "$IMAGE_ROOT/classes/$name.png")
  var sourceIndex = 0
  image.addEventListener("load", { button.classList.add("has-icon") })
  image.addEventListener(
      "error",
      {
        sourceIndex++
        if (sourceIndex < sources.size) image.src = sources[sourceIndex] else image.remove()
      },
  )
  image.src = sources.first()
  button.appendChild(image)
}
