package mau

import com.raquo.laminar.api.L.*

def navbar(exitGame: Observer[Unit]): Div =
  div(
    className := "navbar",
    navbarButton(
      icon = "door_open", 
      caption = "Exit",
      onClick.flatMapStream(_ => Endpoints.logout) --> exitGame
    )
  )

def navbarButton(icon: String, caption: String, mods: Modifier[Div]*): Div = div(
  className := "navbar-button",
  div(
    className := "navbar-icon",
    span(className := "material-symbols-outlined", icon)
  ),
  div(className := "navbar-caption", caption),
  mods
)
