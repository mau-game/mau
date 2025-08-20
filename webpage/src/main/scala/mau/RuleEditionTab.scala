package mau

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement
import io.circe.syntax.*
import mau.edition.*
import mau.round.Player
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js

import game.Command
import mau.Endpoints.submit

class RuleEditionTab(you: Player, initCompilation: CompilationReport, send: Observer[Command], syntaxHighlighting: Future[SyntaxHighlighting])(using ExecutionContext):
  private val codeMirror = CodeMirror(syntaxHighlighting)
  private val compilationVar = Var(initCompilation)
  private val submitErrorBus = EventBus[CompilationReport]()
  private val inputBus = EventBus[Unit]()
  private val liveCompilationStream = inputBus.events
    .debounce(1000)
    .flatMapSwitch(_ => Endpoints.compile(codeMirror.content))
    .mergeWith(submitErrorBus.events)

  def apply(init: Option[CompilationReport]): HtmlElement =
    init.foreach(compilationVar.set)
    val loadingVar = Var(false)
    val modalVisibleVar = Var(false)
    div(
      className := "tab-content",
      initBar,
      div(
        className := Seq("deliberation-input", "cm-s-solarized"),
        codeMirror.element,
        onKeyDown.mapTo(())--> inputBus,
        liveCompilationStream --> codeMirror.updateDiagnostics,
        compilationVar --> codeMirror.update
      ),
      div(
        className := "commands",
        commandButton(
          "Submit",
          onClick.flatMapStream(_ => Endpoints.submit(codeMirror.content)) --> submitErrorBus,
          onClick.mapTo(true) --> loadingVar,
          submitErrorBus.events.mapTo(true) --> modalVisibleVar,
          submitErrorBus.events.mapTo(false) --> loadingVar
        )
      ),
      child <-- liveCompilationStream
        .map(_.errors.collect { case fatal: CompilationError.Fatal => fatal })
        .map: fatalErrors =>
          if fatalErrors.nonEmpty then
            div(
              className := "error-message",
              fatalErrors.map { e =>
                val className = e.className
                val message = e.message.getOrElse("")
                val stackTrace = e.stackTrace.mkString("\n  ")
                p(
                  className + 
                    (if message.nonEmpty then s": " + message else "") +
                    (if stackTrace.nonEmpty then s"\n At " + stackTrace else "")
                )
              }
            )
          else emptyNode,
      child <-- loadingVar.signal.map: loading =>
        if loading then
          div(
            className := "modal-overlay modal-overlay-fade-in",
            div(className := "spinner"),
            div(
              className := "loading-text",
              "Compiling rule..."
            )
          )
        else emptyNode,

      // Modal for generation results
      child <-- modalVisibleVar.signal.map: visible =>
        if visible then
          div(
            className := "modal-overlay",
            onClick.mapTo(false) --> modalVisibleVar,
            div(
              className := "modal-window",
              onClick.stopPropagation --> Observer.empty, // Prevent closing when clicking inside modal
              div(
                className := "modal-header",
                div(
                  className := "modal-header-icon",
                  span(className := "material-symbols-outlined", "error")
                ),
                div(
                  className := "modal-header-title",
                  h3("Compilation Error"),
                ),
                div(
                  className := "navbar-button",
                  marginLeft.auto,
                  span(className := "material-symbols-outlined", "close"),
                  onClick.mapTo(false) --> modalVisibleVar
                ),
              ),
              div(
                className := "modal-body",
                p(
                  className := "error-message",
                  "Failed compiling rule."
                )
              ),
              div(
                className := "modal-footer",
                commandButton(
                  className := "button-primary",
                  "Close",
                  onClick.mapTo(false) --> modalVisibleVar
                )
              )
            )
          )
        else emptyNode,
    )

  private def initBar: Div = div(
    className := "navbar",
    div(
      className := "navbar-button",
      marginLeft.auto,
      span(className := "material-symbols-outlined", "refresh"),
      div(className := "navbar-caption", "Init"),
      onClick.mapTo(initCompilation) --> Observer(codeMirror.update)
    ),
  )
