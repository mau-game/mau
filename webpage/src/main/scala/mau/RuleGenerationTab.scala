package mau

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement
import io.circe.parser
import io.circe.syntax.*
import mau.edition.*
import mau.round.Player
import org.scalajs.dom

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JavaScriptException

import game.Command

class RuleGenerationTab(you: Player, send: Observer[Command], syntaxHighlighting: Future[SyntaxHighlighting], edit: Observer[CompilationReport])(using ExecutionContext):
  private val inputVar = Var("")
  // private val lines = inputVar.signal.map(textContent => textContent.count(_ == '\n') + 1)
  private val promptBus = EventBus[String]()
  private val generationVar = Var[Option[Generation]](None)
  private val codeMirror = CodeMirror(syntaxHighlighting, editable = false)

  def elem =
    val loadingVar = Var(false)
    val abortBus = EventBus[Unit]()
    val modalVisibleVar = Var(false)
    div(
      className := "tab-content",
      div(
        className := Seq("deliberation-input"),
        textArea(
          className := Seq("prompt-input"),
          disabled <-- loadingVar.signal,
          placeholder := "Describe the rule in natural language",
          rows := 3,
          // rows <-- lines.map(lines => lines.max(5).min(10)),
          // overflow <-- lines.map(lines => if lines > 10 then "auto" else "hidden"),
          inputVar.now(),
          inContext(e => onKeyUp.mapTo(e.ref.value) --> inputVar)
        ),
      ),
      div(
        className := "commands",
        commandButton(
          "Generate",
          disabled <-- loadingVar.signal,
          onClick.mapTo(inputVar.now()) --> promptBus
        )
      ),

      child <-- loadingVar.signal.map: loading =>
        if loading then
          div(
            className := "modal-overlay modal-overlay-fade-in",
            div(className := "spinner"),
            div(
              className := "loading-text",
              "Checking the rules with the Party officials..."
            ),
            button("Abort", onClick.mapTo(()) --> abortBus)
          )
        else emptyNode,

      // Modal for generation results
      child <-- modalVisibleVar.signal.combineWith(generationVar.signal).map: (visible, generation) =>
        if visible then
          div(
            className := "modal-overlay",
            onClick.mapTo(false) --> modalVisibleVar,
            div(
              className := "modal-window",
              onClick.stopPropagation --> Observer.empty, // Prevent closing when clicking inside modal
              generation match
                case Some(Generation.Failure(error)) =>
                  Seq(
                    div(
                      className := "modal-header",
                      div(
                        className := "modal-header-icon",
                        span(className := "material-symbols-outlined", "error")
                      ),
                      div(
                        className := "modal-header-title",
                        h3("Generation Error")
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
                      p(className := "error-message", error)
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
                case Some(Generation.Success(compilation, rephrasedRule)) =>
                  codeMirror.update(compilation)
                  Seq(
                    div(
                      className := "modal-header",
                      div(
                        className := "modal-header-icon",
                        span(className := "material-symbols-outlined", "task_alt")
                      ),
                      div(
                        className := "modal-header-title",
                        h3("Generation Success!")
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
                      div(
                        className := "modal-body-section",
                        h4("Rule Description"),
                        p(rephrasedRule),
                      ),
                      div(
                        className := "modal-body-section",
                        h4("Generated Code"),
                        div(
                          className := Seq("deliberation-input", "readonly", "cm-s-solarized"),
                          codeMirror.element
                        )
                      )
                    ),
                    div(
                      className := "modal-footer",
                      commandButton(
                        className := "button-secondary",
                        "Edit Code",
                        disabled <-- loadingVar.signal,
                        onClick.mapTo(compilation) --> edit,
                        onClick.mapTo(false) --> modalVisibleVar
                      ),
                      commandButton(
                        className := "button-primary",
                        "Submit",
                        disabled <-- loadingVar.signal.map(_ || compilation.errors.nonEmpty),
                        onClick.flatMapStream(_ => Endpoints.submit(codeMirror.content)) --> Observer(_ => ()),
                        onClick.mapTo(false) --> modalVisibleVar
                      ),

                    )
                  )
                case None => Seq.empty[Modifier[Div]]
            )
          )
        else emptyNode,
      // all of this to ensure the FetchStream is not being replayed after a tab change
        promptBus.events
          .throttle(1000)
          .flatMapSwitch: prompt =>
            loadingVar.set(true)
            Endpoints
              .generate(prompt, abortBus.events)
              .recover:
                case e: JavaScriptException if e.getMessage.startsWith("AbortError") =>
                  loadingVar.set(false)
                  None
                case e: Throwable => Some(Some(Generation.Failure(s"Client-side error: ${e.toString}")))
              .map: genOpt =>
                loadingVar.set(false) // Set loading to false when response arrives
                // Show modal when generation completes (success or failure)
                genOpt.foreach(_ => modalVisibleVar.set(true))
                genOpt
          .-->(generationVar.writer)
    )
