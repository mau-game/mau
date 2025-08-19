package mau.exec

import dotty.tools.dotc
import mau.edition.CompilationError

import java.util.UUID
import scala.jdk.OptionConverters.*

case class RuleSource(uid: String, code: String):
    val className: String = s"Rule$uid"
    val prefix: String =
      s"""|package mau.round
          |import mau.engine.Rule
          |import mau.engine.Macro
          |import mau.round.Rank.*
          |import mau.round.Suit.*
          |object $className:
          |  def rule: Rule = Macro.rule({ state =>
          |    {
          |""".stripMargin
    val prefixLines = prefix.count(_ == '\n')
    val wrappedImpl: String = s"$prefix$code}}, \"$className\")"

    def fromPosition(pos: Int): Int = prefix.size + pos
    def toPosition(offset: Int): Option[Int] =
      Option.when(offset >= prefix.size && offset <= prefix.size + code.size)(offset - prefix.size)

    def toLine(line: Int): Int = line - prefixLines

    def toDiagnostic(error: dotc.reporting.Diagnostic.Error): CompilationError =
      val start = error.position.asScala.flatMap(p => toPosition(p.start))
      val end = error.position.asScala.flatMap(p => toPosition(p.end))
      start.zip(end) match
        case Some((start, end)) => CompilationError.Diagnostic(start, end, error.message)
        case None => CompilationError.Fatal("Compilation Error", Some(error.message), Seq.empty)

object RuleSource:
  def apply(code: String): RuleSource =
    val uid = UUID.randomUUID.toString.replace("-", "")
    RuleSource(uid, code)
