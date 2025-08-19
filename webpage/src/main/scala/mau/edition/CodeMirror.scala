package mau.edition

import com.raquo.laminar.api.L.*
import org.scalajs.dom
import typings.codemirrorAutocomplete.mod.*
import typings.codemirrorCommands.mod.*
import typings.codemirrorLanguage.mod.*
import typings.codemirrorLint.mod.*
import typings.codemirrorSearch.mod.*
import typings.codemirrorState.mod.*
import typings.codemirrorView.mod.*

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.Dynamic.literal
import scala.util.Failure
import scala.util.Success

object CodeMirror:
  def apply(init: Future[SyntaxHighlighting], editable: Boolean = true)(using ExecutionContext): CodeMirror =
    val compartment = Compartment()
    val baseExtensions = js.Array[Any](
      lineNumbers(),
      lintGutter(),
      highlightSpecialChars(),
      bracketMatching(),
      EditorState.tabSize.of(2),
      EditorView.editable.of(editable),
      // EditorState.readOnly.of(readOnly),
      // Prec.highest(EditorKeymaps.keymapping(props)),
      // InteractiveProvider.interactive.of(InteractiveProvider(props).extension),
      SyntaxHighlighting.theme,
      lintGutter(LintGutterConfig().setTooltipFilter((_, _) => js.Array())),
      // OnChangeHandler(props.codeChange),
      compartment.of(SyntaxHighlighting.fallbackExtension),
    )
    val editionExtensions =
      if editable then
        js.Array[Any](
          history(),
          drawSelection(),
          indentOnInput(),
          closeBrackets(),
          highlightSelectionMatches(),
          keymap.of(closeBracketsKeymap ++ defaultKeymap ++ historyKeymap ++ foldKeymap ++ completionKeymap ++ lintKeymap ++ searchKeymap),
          Autocompletion.extension
        )
      else js.Array[Any]()
    val state = EditorState.create(EditorStateConfig().setExtensions(baseExtensions ++ editionExtensions))
    val view = EditorView(EditorViewConfig().setState(state))
    init.onComplete:
      case Failure(e) =>
        println(s"Failed loading tree-sitter parser: $e")
      case Success(init) =>
        init.reconfigure(compartment, view)
        println("Loaded tree-sitter parser")
    new CodeMirror(view)

class CodeMirror(view: EditorView):
  val element: HtmlElement = foreignHtmlElement(view.dom)
  def content: String = view.state.doc.toString
  
  def updateDiagnostics(compilation: CompilationReport): Unit =
    val diagnostics = compilation.errors.collect:
      case CompilationError.Diagnostic(start, end, message) => Diagnostic(start, message, Severity.error, end)
    view.dispatch(setDiagnostics(view.state, js.Array(diagnostics*)))

  def update(compilation: CompilationReport): Unit =
    val diagnostics = compilation.errors.collect:
      case CompilationError.Diagnostic(start, end, message) => Diagnostic(start, message, Severity.error, end)
    view.dispatch(
      TransactionSpec().setChanges(js.Array(literal(from = 0, to = view.state.doc.length, insert = compilation.code))),
      setDiagnostics(view.state, js.Array(diagnostics*))
    )
        
    
