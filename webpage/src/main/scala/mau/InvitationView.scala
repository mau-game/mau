package mau

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement
import io.circe.syntax.*
import mau.game.LoginParams

object InvitationView:
  def apply(play: Observer[Unit]): HtmlElement =
    val showPasswordBus = EventBus[Boolean]()
    val showPasswordVar = Var(false)
    val nameVar = Var("")
    val passwordVar = Var("")
    val joinBus = EventBus[(String, String, String)]()
    val loginStream: EventStream[Either[String, Unit]] =
      joinBus.events.flatMapSwitch:
        case (username, password, avatar) => Endpoints.login(username, password, avatar)
    val selectedAvatarVar = Var(scala.util.Random.shuffle(Assets.Avatars.all.keys.toSeq).head)
    val avatarOptions = Assets.Avatars.all.keys.toSeq.sorted
    div(
      idAttr := "invitation-container",
      div(
        idAttr := "invitation",
        div (
          idAttr := "invitation-title",
          h1("THE GAME OF"),
          h1(idAttr:= "mau", "MAU"),
          div(
            className := "release-version-display",
            "BETA"
          ),
        ),
        form(
          idAttr := "invitation-form",
          div(
            className := "avatar-navigator",
            img(
              className := "avatar-image",
              src <-- selectedAvatarVar.signal.map(Assets.Avatars.apply),
              alt <-- selectedAvatarVar.signal.map(id => s"Avatar $id"),
            ),
            div(
              className := "avatar-nav-button avatar-prev",
              "❮",
              onClick --> { _ =>
                val currentIndex = avatarOptions.indexOf(selectedAvatarVar.now())
                val prevIndex = if (currentIndex <= 0) avatarOptions.length - 1 else currentIndex - 1
                selectedAvatarVar.set(avatarOptions(prevIndex))
              }
            ),
            div(
              className := "avatar-nav-button avatar-next",
              "❯",
              onClick --> { _ =>
                val currentIndex = avatarOptions.indexOf(selectedAvatarVar.now())
                val nextIndex = if (currentIndex >= avatarOptions.length - 1) 0 else currentIndex + 1
                selectedAvatarVar.set(avatarOptions(nextIndex))
              }
            )
          ),
          
          input(
            placeholder := "Username",
            onInput.mapToValue --> nameVar
          ),
          div(
            className := "password-container",
            input(
              `type` <-- showPasswordVar.signal.map:
                case true => "text"
                case false => "password",
              placeholder := "Password",
              onInput.mapToValue --> passwordVar
            ),
            span(
              className := Seq("show-password", "material-symbols-outlined"),
              child <-- showPasswordVar.signal.map:
                case true => "visibility_off"
                case false => "visibility",
              onClick.mapTo(!showPasswordVar.now()) --> showPasswordBus,
              EventStream
                .merge(showPasswordBus.events, showPasswordBus.events.debounce(3000).mapTo(false))
                .-->(showPasswordVar.toObserver)
            ),
          ),
          button (
            `type` := "button",
            "Join the Game",
            onClick.mapTo((nameVar.now(), passwordVar.now(), selectedAvatarVar.now())) --> joinBus
          ),
          onKeyDown.filter(e => e.key == "Enter").mapTo((nameVar.now(), passwordVar.now(), selectedAvatarVar.now())) --> joinBus,
          loginStream.collect { case Right(_) => () } --> play
        ),
        child <-- loginStream.collect { case Left(error) => div(className := "error-message", error) }
      ),
    )
