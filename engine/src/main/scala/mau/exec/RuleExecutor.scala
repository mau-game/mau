package mau.exec

import io.circe.*
import io.circe.syntax.*
import mau.edition.Completion
import mau.engine.ExternalRule
import mau.round.Action
import mau.round.Decision
import mau.round.RulingState

import java.io.*
import java.lang.ProcessBuilder.Redirect
import java.nio.file.Path
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.*

class RuleExecutor(workspace: Workspace, classpath: Seq[Path])(using ExecutionContext):
  private var worker: WorkerClient = startWorker()
  
  def run(rule: ExternalRule, state: RulingState, action: Action, timeout: Duration = 3.seconds): WithUntrustedRules[Option[Decision]] =
    synchronized:
      try WithUntrustedRules(Await.result(worker.run(rule, state, action), timeout))
      catch case cause: Throwable =>
        println(s"restarting worker because of $cause")
        // cause.printStackTrace()
        restartWorker()
        WithUntrustedRules(None, Set(rule))

  def compile(code: String, timeout: Duration = 10.seconds): RuleCompilation =
    synchronized:
      try Await.result(worker.compile(code), timeout)
      catch case cause: Throwable =>
        println(s"restarting worker because of $cause")
        // cause.printStackTrace()
        restartWorker()
        RuleCompilation.fatal(code, cause.toString)

  def complete(code: String, position: Int, timeout: Duration = 10.seconds): Seq[Completion] =
    synchronized:
      try Await.result(worker.complete(code, position), timeout)
      catch case cause: Throwable =>
        println(s"restarting worker because of $cause")
        // cause.printStackTrace()
        restartWorker()
        Seq.empty

  private def restartWorker(): Unit =
    synchronized:
      worker.shutdown()
      worker = startWorker()

  private def startWorker(): WorkerClient = 
    synchronized:
      val processBuilder = new ProcessBuilder("java", "-cp", classpath.mkString(":"), classOf[RuleWorker].getName)
      processBuilder.environment.remove("JAVA_JDK_OPTIONS")
      val process = processBuilder.redirectError(Redirect.INHERIT).start()
      println(s"starting worker")
      WorkerClient(process)

  private class WorkerClient(process: Process):
    private val writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(process.getOutputStream)))
    private val reader = new BufferedReader(new InputStreamReader(process.getInputStream))

    def run(rule: ExternalRule, state: RulingState, action: Action): Future[Option[Decision]] =
      import mau.utils.given // codec for Option is defined in codecs.scala
      val command = ExecCommand.Run(rule.uri, rule.name, state, action)
      exec[Option[Decision]](command)(_ => None)

    def compile(code: String): Future[RuleCompilation] =
      val command = ExecCommand.Compile(code)
      exec[RuleCompilation](command)(RuleCompilation.fatal(code, _))

    def complete(code: String, position: Int): Future[Seq[Completion]] =
      import mau.utils.given // codec for Seq is defined in codecs.scala
      val command = ExecCommand.Complete(code, position)
      exec[Seq[Completion]](command)(_ => Seq.empty)
    
    def exec[T: Codec](command: ExecCommand)(handleError: String => T): Future[T] =
      writer.println(Printer.noSpaces.print(command.asJson))
      writer.flush()
      Future:
        
        val line = readSanitizedLine()
        parser.decode[ExecResult[T]](line) match
          case Right(ExecResult.Success(value)) => value
          case Right(ExecResult.Error(error)) =>
            // we are tolerant on rules that throw exception
            println(s"Rule execution failed with $error")
            handleError(error)
          case Right(ExecResult.Fatal(error)) =>
            // something's wrong, restart the worker
            throw new Exception(s"Rule execution failed with fatal error $error")
          case Left(error) => throw error

    private def readSanitizedLine(): String =
      val line = reader.readLine()
      if line.contains("org.jmxtrans") then
        println(s"skipped $line")
        readSanitizedLine()
      else
        val CleverCloudLog = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z (.*)".r
        line match
          case CleverCloudLog(line) =>
            println(s"Received and sanitized: $line")
            line
          case line =>
            println(s"Received $line")
            line

    def shutdown(): Unit =
      try process.destroyForcibly()
      catch case e: Throwable =>
        println(e)
        e.printStackTrace()
      


