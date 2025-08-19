package mau.exec

import io.circe.*
import io.circe.syntax.*
import mau.*
import mau.edition.*
import mau.engine.ExternalRule
import mau.engine.InMemoryRule
import mau.round.*
import mau.utils.given

import java.net.URI
import java.net.URLClassLoader
import java.nio.file.Paths
import java.util.concurrent.Executors
import scala.collection.concurrent.TrieMap
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.duration.*
import scala.io.StdIn
import scala.util.Properties

final class RuleWorker(compiler: RuleCompiler, workspace: Workspace):
  private val classLoaders = TrieMap.empty[URI, ClassLoader]

  def run(ruleUri: URI, ruleName: String, state: RulingState, action: Action): ExecResult[Option[Decision]] =
    loadRule(ruleUri, ruleName)
      .flatMap(rule => ExecResult.tryOrFatal(rule(state).unapply(action)))

  def compile(code: String): ExecResult[RuleCompilation] =
    val source = RuleSource(code)
    val (targetDir, sourceFile) = workspace.createDirectoryAndSourceFile(source)
    val ruleUri = workspace.getRuleUri(source.uid)
    val ruleName = source.className
    ExecResult.tryOrError(compiler.compile(source, targetDir, sourceFile))
      .flatMap: report =>
        if report.isEmpty then
          loadRule(ruleUri, ruleName).map(rule => RuleCompilation.Success(ExternalRule(ruleUri, ruleName, rule.buttons, code)))
        else ExecResult.Success(RuleCompilation.Report(report))
  
  def complete(code: String, position: Int): ExecResult[Seq[Completion]] =
    ExecResult.tryOrError:
      val source = RuleSource(code)
      val uri = workspace.getVirtualSourceFile(source.className)
      Await.result(compiler.complete(source, uri, position), 10.seconds)

  private def loadRule(ruleUri: URI, ruleName: String): ExecResult[InMemoryRule] =
    ExecResult
      .tryOrFatal:
        val classLoader = classLoaders.getOrElseUpdate(ruleUri, RuleClassLoader(ruleUri, getClass.getClassLoader))
        val clazz = classLoader.loadClass(s"mau.round.$ruleName")
        val method = clazz.getMethod("rule")
        method.invoke(null).asInstanceOf[InMemoryRule]
      .onFailure(() => classLoaders.remove(ruleUri))

object RuleWorker:
  def main(args: Array[String]): Unit =
    import mau.given // codec for Option is defined in codecs.scala
    val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
    val workspace = Workspace.init()
    val classpath = Properties.javaClassPath.split(':').map(Paths.get(_))
    val compiler = RuleCompiler(workspace, classpath, ec)
    val worker = RuleWorker(compiler, workspace)
    while true do
      val line = StdIn.readLine()
      parser.decode[ExecCommand](line) match
        case Left(error) => respond[String](ExecResult.Fatal(error.toString))
        case Right(ExecCommand.Run(ruleUri, ruleName, state, action)) => respond(worker.run(ruleUri, ruleName, state, action))
        case Right(ExecCommand.Compile(code)) => respond(worker.compile(code))
        case Right(ExecCommand.Complete(code, position)) => respond(worker.complete(code, position))

  private def respond[T: Codec](value: ExecResult[T]): Unit =
    Console.println(Printer.noSpaces.print(value.asJson))
    Console.flush()

