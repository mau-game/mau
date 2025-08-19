package mau

import scala.Console.*

// Adapted from Scastie
object HtmlFormatter:
  private val escapeMap =
    Map('&' -> "&amp;", '"' -> "&quot;", '<' -> "&lt;", '>' -> "&gt;")

  private val colors = Map(
    BLACK -> "ansi-color-black",
    RED -> "ansi-color-red",
    "\u001b[90m" -> "ansi-color-gray",
    GREEN -> "ansi-color-green",
    YELLOW -> "ansi-color-yellow",
    BLUE -> "ansi-color-blue",
    MAGENTA -> "ansi-color-magenta",
    CYAN -> "ansi-color-cyan",
    WHITE -> "ansi-color-white",
    BLACK_B -> "ansi-bg-color-black",
    RED_B -> "ansi-bg-color-red",
    GREEN_B -> "ansi-bg-color-green",
    YELLOW_B -> "ansi-bg-color-yellow",
    BLUE_B -> "ansi-bg-color-blue",
    MAGENTA_B -> "ansi-bg-color-magenta",
    CYAN_B -> "ansi-bg-color-cyan",
    WHITE_B -> "ansi-bg-color-white",
    RESET -> "",
    BLINK -> "ansi-blink",
    BOLD -> "ansi-bold",
    REVERSED -> "ansi-reversed",
    INVISIBLE -> "ansi-invisible"
  )

  def format(raw: String) =
    escape(raw)
      .foldLeft("" -> 0) {
        case ((_r, d), c) =>
          val r = _r + c
          val replaced = colors.collectFirst {
            case (ansiCode, replacement) if r.endsWith(ansiCode) =>
              if (ansiCode == RESET) r.replace(ansiCode, "</span>" * d) -> 0
              else r.replace(ansiCode, s"""<span class="$replacement">""") -> (d + 1)
          }
          replaced.getOrElse(r -> d)
      }
      ._1

  private def escape(text: String): String =
    text.iterator
      .foldLeft(new StringBuilder()) { (s, c) =>
        escapeMap.get(c) match {
          case Some(str)                                   => s ++= str
          case _ if c >= ' ' || "\n\r\t\u001b".contains(c) => s += c
          case _                                           => s // noop
        }
      }
      .toString
