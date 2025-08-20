package mau.exec


import dotty.tools.dotc
import dotty.tools.pc.ScalaPresentationCompiler
import mau.*
import mau.edition.*
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit

import java.net.URI
import java.nio.file.Path
import java.util.concurrent.Executors
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContextExecutorService
import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.jdk.OptionConverters.*
import scala.meta.internal.metals.CompilerOffsetParams
import scala.meta.internal.metals.EmptyCancelToken
import scala.meta.internal.pc.PresentationCompilerConfigImpl
import scala.meta.pc.CompletionItemPriority
import scala.meta.pc.PresentationCompiler
import dotty.tools.dotc.reporting.StoreReporter

object RuleCompiler:
  // private[mau] def default: RuleCompiler =
  //   val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
  //   RuleCompiler(ec)

  def apply(workspace: Workspace, classpath: Seq[Path], ec: ExecutionContextExecutorService): RuleCompiler =
    // val search = StandaloneSymbolSearch(workspace, classpath)
    val completionItemPriority: CompletionItemPriority = (_: String) => 0
    val pcConfig = new PresentationCompilerConfigImpl()
    val pc = ScalaPresentationCompiler(classpath = classpath)
      .withSearch(SymbolSearchImpl)
      .withExecutorService(ec)
      .withCompletionItemPriority(completionItemPriority)
      .withWorkspace(workspace.root)
      .withScheduledExecutorService(Executors.newSingleThreadScheduledExecutor)
      .withReportsLoggerLevel("info")
      // .withReportContext(rc)
      .withConfiguration(pcConfig)
    new RuleCompiler(pc, classpath)(using ec)

class RuleCompiler(pc: PresentationCompiler, classpath: Seq[Path])(using ExecutionContext):
  private val classLoader = getClass.getClassLoader
  private val exclusions = Set(
    "FIXME",
    "unapply",
    "fromOrdinal",
    "ordinal",
    "->",
    "→",
    "ensuring",
    "nn",
    "runtimeChecked",
    "formatted",
    "asInstanceOf",
    "equals",
    "getClass",
    "hashCode",
    "isInstanceOf",
    "synchronized",
    "toString",
    "wait",
    "canEqual",
    "copy",
    "productArity",
    "productElement",
    "productElementName",
    "productPrefix",
    "productElementNames",
    "productIterator"
  )

  def compile(source: RuleSource, targetDir: Path, sourceFile: Path): CompilationReport =
    val args = Array("-cp", classpath.mkString(":"), "-d", targetDir.toString, sourceFile.toString)
    val reporter = StoreReporter()
    dotc.Main.process(args, reporter = reporter)
    CompilationReport(source.code, reporter.allErrors.map(source.toDiagnostic))

  def complete(source: RuleSource, ruleUri: URI, position: Int): Future[Seq[Completion]] =
    // TODO cancel completion
    val offsetParams = CompilerOffsetParams(ruleUri, source.wrappedImpl, source.fromPosition(position), EmptyCancelToken)
    for list <- pc.complete(offsetParams).asScala
    yield list.getItems.asScala.toSeq
      .flatMap(toCompletion(source, _))
      .filterNot: c =>
        exclusions.contains(c.label)
        || c.label.startsWith("given")
        || c.label.startsWith("_")
        || c.label.startsWith("derived$")
        || c.tpe == "module"

  def info(completion: Completion, disabled: Boolean = true): Option[String] =
    // TODO fix by implementing documentation in SymbolSearchImpl
    if disabled then None
    else
      for
        symbol <- completion.symbol
        resp = Await.result(pc.completionItemResolve(toItem(completion), symbol).asScala, 5.seconds)
        doc <- Option(resp.getDocumentation)
      yield if doc.isLeft then doc.getLeft else doc.getRight.getValue

  private def toItem(completion: Completion): CompletionItem =
    val item = new CompletionItem()
    item.setLabel(completion.label)
    item
  
  private def toCompletion(source: RuleSource, item: CompletionItem): Option[Completion] =
    for 
      label <- Option(item.getFilterText)
      detail <- Option(item.getDetail)
      tpe <- Option(item.getKind).map(_.toString.toLowerCase)
      order <- Option(item.getSortText).map(_.toIntOption)
      insert <- createInsert(source, item)
      additionalInserts = createAdditionalInserts(source, item)
      symbol = parseCompletionData(item)
    yield
      // println(s"$label $detail $tpe $order")
      Completion(label, detail, tpe, order, insert, additionalInserts, symbol)

  private def createInsert(source: RuleSource, item: CompletionItem): Option[Insert] =
    if item.getTextEdit.isLeft then Some(toInsert(source, item.getTextEdit.getLeft))
    else
      println("unexpected InsertReplaceEdit")
      None

  private def createAdditionalInserts(source: RuleSource, item: CompletionItem): Seq[Insert] = 
    Option(item.getAdditionalTextEdits).toSeq.flatMap(_.asScala).map(toInsert(source, _))

  private def toInsert(source: RuleSource, textEdit: TextEdit): Insert =
    val range = textEdit.getRange
    val start = range.getStart
    val end = range.getEnd
    Insert(
      textEdit.getNewText,
      source.toLine(start.getLine),
      start.getCharacter + 1,
      source.toLine(end.getLine),
      end.getCharacter + 1
    )

  private def parseCompletionData(item: CompletionItem): Option[String] =
    for
      rawData <- Option(item.getData)
      json <- io.circe.parser.parse(rawData.toString).toOption
      objJson <- json.asObject
      symbolJson <- objJson("symbol")
      symbol <- symbolJson.asString
    yield symbol
