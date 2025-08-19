package mau.edition

import mau.Assets
import org.scalajs.dom
import typings.codemirrorLanguage.mod.*
import typings.codemirrorLegacyModes.modeClikeMod.scala_
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.*
import typings.lezerHighlight.mod.tags.*
import typings.webTreeSitter.mod.*
import typings.webTreeSitter.mod.{ init as initTreeSitter, * }

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.*

final class SyntaxHighlighting(parser: Parser, query: Query):
  def reconfigure(compartment: Compartment, view: EditorView): Unit =
    val plugin = ViewPlugin.define(
      editorView => new SyntaxHighlighting.Handler(parser, query, editorView.state.doc.toString),
      PluginSpec().setDecorations(_.decorations)
    )
    view.dispatch(TransactionSpec().setEffects(compartment.reconfigure(plugin.extension)))

object SyntaxHighlighting:
  val theme = syntaxHighlighting(style)
  val fallbackExtension = StreamLanguage.define(scala_).extension

  def init(using ExecutionContext): Future[SyntaxHighlighting] =
    val options = new js.Object:
      def locateFile(scriptName: String, scriptDirectory: String): String = Assets.TreeSitter.wasm
    // start loading query file
    val queryContentF =
      for
        queryFile <- dom.fetch(Assets.TreeSitter.highlights).toFuture
        queryContent <- queryFile.text().toFuture
      yield queryContent
    for
      _ <- initTreeSitter(options).toFuture
      language <- Language.load(Assets.TreeSitter.scalaWasm).toFuture
      queryContent <- queryContentF
    yield
      val parser: Parser = new TreeSitter.Parser()
      parser.setLanguage(language)
      val query = language.query(queryContent)
      SyntaxHighlighting(parser, query)

  @js.native @JSImport("web-tree-sitter", JSImport.Namespace)
  object TreeSitter extends js.Any:
    @js.native @JSName("default")
    class Parser() extends typings.webTreeSitter.mod.Parser

  // Copied from Scastie's SyntaxHighlightingHandler
  private class Handler(parser: Parser, query: Query, initialContent: String) extends js.Object:
    private var tree = parser.parse(initialContent)
    var decorations: DecorationSet = computeDecorations

    def update(viewUpdate: ViewUpdate): Unit =
      if (viewUpdate.docChanged) then
        val newText = viewUpdate.state.doc.toString
        val edits = toTreeSitterEdits(viewUpdate.changes, viewUpdate.startState.doc, viewUpdate.state.doc)
        edits.foreach(tree.edit)
        tree = parser.parse(newText, tree)
        decorations = computeDecorations

    private def computeDecorations: DecorationSet =
      val rangeSetBuilder = new RangeSetBuilder[Decoration]()
      val captures = query.captures(tree.rootNode)
      captures.foldLeft(Option.empty[QueryCapture]){ (previous, current) =>
        if (!previous.exists(_ == current)) then
          val spec = MarkDecorationSpec().setInclusive(true).setClass(current.name.replace(".", "-"))
          val mark = Decoration.mark(spec)
          rangeSetBuilder.add(current.node.startIndex, current.node.endIndex, mark)
        Some(current)
      }
      rangeSetBuilder.finish()

    private def toTreeSitterEdits(changes: ChangeSet, originalText: Text, newText: Text): List[Edit] =
      val buffer = new ListBuffer[Edit]()
      changes.iterChanges { (fromA: Double, toA: Double, _, toB: Double, _) =>
        val oldEndPosition = toTreeSitterPoint(originalText, toA)
        val newEndPosition = toTreeSitterPoint(newText, toB)
        val startPosition = toTreeSitterPoint(originalText, fromA)
        buffer.addOne(Edit(toB, newEndPosition, toA, oldEndPosition, fromA, startPosition))
      }
      buffer.toList

    private def toTreeSitterPoint(text: Text, index: Double): Point =
      val line = text.lineAt(index)
      Point(index - line.from, line.number - 1)
  end Handler

  // Copied from Scastie's SyntaxHighlightingTheme
  private def style = HighlightStyle.define(
    js.Array(
      TagStyle(annotation).setClass("attribute"),
      TagStyle(arithmeticOperator).setClass("operator"),
      TagStyle(attributeName).setClass("tag-attribute"),
      TagStyle(attributeValue).setClass("string"),
      TagStyle(bitwiseOperator).setClass("operator"),
      TagStyle(blockComment).setClass("comment"),
      TagStyle(bool).setClass("boolean"),
      TagStyle(brace).setClass("punctuation-bracket"),
      TagStyle(bracket).setClass("punctuation-bracket"),
      TagStyle(character).setClass("character"),
      TagStyle(className).setClass("type"),
      TagStyle(comment).setClass("comment"),
      TagStyle(compareOperator).setClass("operator"),
      TagStyle(content).setClass("text"),
      TagStyle(contentSeparator).setClass("punctuation-delimiter"),
      TagStyle(controlKeyword).setClass("keyword"),
      TagStyle(controlOperator).setClass("operator"),
      TagStyle(definitionKeyword).setClass("keyword"),
      TagStyle(definitionOperator).setClass("keyword-operator"),
      TagStyle(derefOperator).setClass("operator"),
      TagStyle(docComment).setClass("comment"),
      TagStyle(docString).setClass("string"),
      TagStyle(emphasis).setClass("text-emphasis"),
      TagStyle(escape).setClass("string-escape"),
      TagStyle(float).setClass("float"),
      TagStyle(integer).setClass("number"),
      TagStyle(invalid).setClass("error"),
      TagStyle(keyword).setClass("keyword"),
      TagStyle(labelName).setClass("label"),
      TagStyle(lineComment).setClass("comment"),
      TagStyle(literal).setClass("text-literal"),
      TagStyle(logicOperator).setClass("operator"),
      TagStyle(macroName).setClass("function-macro"),
      TagStyle(modifier).setClass("keyword"),
      TagStyle(moduleKeyword).setClass("keyword"),
      TagStyle(namespace).setClass("namespace"),
      TagStyle(number).setClass("number"),
      TagStyle(operator).setClass("operator"),
      TagStyle(operatorKeyword).setClass("keyword-operator"),
      TagStyle(paren).setClass("punctuation-bracket"),
      TagStyle(propertyName).setClass("property"),
      TagStyle(punctuation).setClass("punctuation-delimiter"),
      TagStyle(quote).setClass("punctuation-special"),
      TagStyle(regexp).setClass("string-regex"),
      TagStyle(self).setClass("variablee"),
      TagStyle(separator).setClass("punctuation-delimiter"),
      TagStyle(squareBracket).setClass("punctuation-bracket"),
      TagStyle(strikethrough).setClass("text-strike"),
      TagStyle(string).setClass("string"),
      TagStyle(strong).setClass("text-strong"),
      TagStyle(tagName).setClass("tag"),
      TagStyle(typeName).setClass("type"),
      TagStyle(typeOperator).setClass("type-qualifier"),
      TagStyle(unit).setClass("none"),
      TagStyle(variableName).setClass("function"),
    )
  )
