package mau.exec

import io.circe.Codec
import mau.edition.*
import mau.engine.ExternalRule

import java.net.URI

enum RuleCompilation derives Codec:
  case Success(rule: ExternalRule)
  case Report(report: CompilationReport)

  def toReport: CompilationReport = this match
    case Success(rule) => CompilationReport(rule.code, Seq.empty)
    case Report(report) => report

object RuleCompilation:
  def fatal(code: String, cause: Throwable): RuleCompilation =
    RuleCompilation.Report(CompilationReport(code, Seq(CompilationError.fatal(cause))))

  def fatal(code: String, message: String): RuleCompilation =
    RuleCompilation.Report(CompilationReport(code, Seq(CompilationError.Fatal("Compilation Error", Some(message), Seq.empty))))
