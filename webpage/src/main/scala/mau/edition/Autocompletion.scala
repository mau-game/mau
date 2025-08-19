package mau.edition

import org.scalajs.dom
import typings.codemirrorAutocomplete.anon
import typings.codemirrorAutocomplete.mod.{Completion => CMCompletion, *}
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.*
// import typings.highlightJs.mod.{HighlightOptions => HLJSOptions}
// import typings.markedHighlight.mod.*
// import typings.marked.mod.marked
// import typings.marked.mod.marked.MarkedExtension
// "@types/marked": "^5.0.0",
// "highlight.js": "^11.8.0",
// "marked": "^5.1.2",
// "marked-highlight": "^2.1.0",

import scala.collection.mutable.HashMap
import scala.concurrent.Future

import scalajs.js
import scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalajs.js.Thenable.Implicits.*
import js.JSConverters.*
import scala.util.Try
import io.circe.* 
import io.circe.syntax.*

object Autocompletion:
  val selectionPattern = "\\$\\{\\d+:(.*?)\\}".r

  // var wasPreviousIncomplete = true
  // var previousWord = ""

  private val autocompletionConfig = CompletionConfig()
    .setInteractionDelay(500) // we want completions to work instantly
    .setOverrideVarargs(ctx => complete(ctx).toJSPromise)
    // .setActivateOnTyping(false) // we use our own autocompletion trigger with working debounce MetalsAutocompletion.autocompletionTrigger
    .setIcons(true)
    .setDefaultKeymap(true)

  val extension: Extension = autocompletion(autocompletionConfig)
    // autocompletionTrigger

  // private val autocompletionTrigger = onChangeCallback((code, view) => {
  //   val matchesPreviousToken = view.matchesPreviousToken(previousWord)
  //   if (wasPreviousIncomplete || !matchesPreviousToken) startCompletion(view)
  //   if (!matchesPreviousToken) wasPreviousIncomplete = true
  // })

  private def complete(ctx: CompletionContext): Future[CompletionResult] =
    val word = ctx.matchBefore(js.RegExp("\\.?\\w*")).asInstanceOf[anon.Text]
    if word == null || word.text.isEmpty || word.from == word.to then 
      Future.successful(null)
    else
      // previousWord = word.text
      val params = CompletionParams(ctx.state.doc.toString, ctx.pos.toInt)
      sendRequest[CompletionParams, Seq[Completion]]("/api/completion", params)
        .map: completionsOpt =>
          val completions = completionsOpt.toSeq.flatten
          val from = if word.text.headOption.contains('.') then word.from + 1 else word.from
          CompletionResult(from, completions.map(toCodeMirror).toJSArray).setValidForFunction4((_, _, _, _) => true)
  
  private def sendRequest[Req: Encoder, Resp: Decoder](endpoint: String, params: Req): Future[Option[Resp]] =
    val request = new dom.RequestInit:
        body = Printer.noSpaces.print(params.asJson)
        method = dom.HttpMethod.POST
    for
      response <- dom.fetch(endpoint, request)
      data <- response.text()
    yield
      if response.ok then
        parser.decode[Resp](data) match
          case Left(error) =>
            println(error) 
            None
          case Right(resp) => Some(resp)
      else None

  private def toCodeMirror(c: Completion): CMCompletion =
    CMCompletion(c.label.stripSuffix(c.detail))
      .setDetail(c.detail)
      .setInfo(getCompletionInfo(c))
      .setType(c.tpe)
      .setBoost(-c.order.getOrElse(-99).toDouble)
      .setApplyFunction4: (view, _, _, to) =>
        // wasPreviousIncomplete = false
        // TODO this should be a callback
        view.dispatch(createEditTransaction(view, c, to.toInt))

  // marked.use(markedHighlight(SynchronousOptions(highlightF)).asInstanceOf[MarkedExtension])
  // marked.setOptions(typings.marked.mod.marked.MarkedOptions().setHeaderIds(false).setMangle(false))
  // private def highlightF(str: String, lang: String, a: String): String =
  //   val highlightJS = typings.highlightJs.mod.default
  //   if lang != null && highlightJS.getLanguage(lang) != null && lang != "" then
  //     Try(highlightJS.highlight(str, HLJSOptions(lang)).value).getOrElse(str)
  //   else str

  /*
   * Fetches documentation for selected completion
   */
  private def getCompletionInfo(c: Completion): js.Function1[CMCompletion, js.Promise[dom.Node]] =
    _ =>  Future.successful(null).toJSPromise
      // sendRequest[Completion, String]("/api/completion/info", c)
      //   .map: respOpt =>
      //     respOpt
      //       .filter(_.nonEmpty)
      //       .map: completionInfo =>
      //         val node = dom.document.createElement("div")
      //         node.innerHTML = completionInfo // marked(completionInfo)
      //         node
      //       .getOrElse(null)
      //   .toJSPromise

    /*
   * Creates edit transaction for completion. This enables cursor to be in proper position after completion is accepted
   */
  private def createEditTransaction(view: EditorView, completion: Completion, currentCursorPosition: Int): TransactionSpec =
    // println(completion.insert)
    val startLinePos = view.state.doc.line(completion.insert.startLine + 1).from
    val endLinePos = view.state.doc.line(completion.insert.endLine + 1).from
    // println("startLinePos: " + startLinePos)
    // println("endLinePos: " + endLinePos)
    val fromPos = startLinePos + completion.insert.startChar - 1
    val toPos = endLinePos + completion.insert.endChar - 1
    // println("fromPos: " + fromPos)
    // println("toPos: " + toPos)

    val newCursorStartLine = fromPos + completion.additionalInserts.map(_.text.size).sum
    // println("newCursorStartLine: " + newCursorStartLine)

    val (selection, insertText) = prepareInsertionText(completion, newCursorStartLine.toInt)
    val change = changeSpec(fromPos.toDouble, toPos.toDouble.max(currentCursorPosition), insertText)

    TransactionSpec()
      .setChangesVarargs(change +: createAdditionalTextEdits(completion.additionalInserts, view)*)
      .setSelection(selection)

  private def prepareInsertionText(completion: Completion, lineStart: Int): (EditorSelection, String) =
    val patternIndex = completion.insert.text.indexOf("$0")
    val partiallyCleanedPattern = completion.insert.text.replace("$0", "")
    val offset = if (patternIndex == -1) then partiallyCleanedPattern.length else patternIndex
    val simpleSelection = EditorSelection.single(lineStart + offset)

    selectionPattern
      .findFirstMatchIn(partiallyCleanedPattern)
      .map: regexMatch =>
        val offset = regexMatch.group(0).length - regexMatch.group(1).length
        val selection = EditorSelection.single(lineStart + regexMatch.start, lineStart + regexMatch.end - offset)
        val adjustedInsertString = partiallyCleanedPattern.substring(0, regexMatch.start) +
          regexMatch.group(1) +
          partiallyCleanedPattern.substring(regexMatch.end, partiallyCleanedPattern.length)
        (selection, adjustedInsertString)
      .getOrElse(simpleSelection, partiallyCleanedPattern)

  private def createAdditionalTextEdits(inserts: Seq[Insert], view: EditorView): Seq[ChangeSpec] =
    inserts.map: textEdit =>
      val startPos = view.state.doc.line(textEdit.startLine).from.toInt + textEdit.startChar
      val endPos = view.state.doc.line(textEdit.endLine).from.toInt + textEdit.endChar
      changeSpec(startPos.toDouble, endPos.toDouble, textEdit.text)

  private def changeSpec(from: Double, to: Double, insert: String): ChangeSpec =
    js.Dynamic.literal(from = from, to = to, insert = insert).asInstanceOf[ChangeSpec]
