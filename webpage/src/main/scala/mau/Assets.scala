package mau

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Assets:
  object Avatars:
    val default = "avatar-0"

    val all = Map(
      "avatar-01" -> avatar1,
      "avatar-02" -> avatar2,
      "avatar-03" -> avatar3,
      "avatar-04" -> avatar4,
      "avatar-05" -> avatar5,
      "avatar-06" -> avatar6,
      "avatar-07" -> avatar7,
      "avatar-08" -> avatar8,
      "avatar-09" -> avatar9,
      "avatar-10" -> avatar10,
      "avatar-11" -> avatar11,
      "avatar-12" -> avatar12
    )

    def apply(avatarId: String): String = all.getOrElse(avatarId, avatar0)

    @js.native @JSImport("/assets/avatars/avatar-00.png", JSImport.Default)
    val avatar0: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-01.png", JSImport.Default)
    val avatar1: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-02.png", JSImport.Default)
    val avatar2: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-03.png", JSImport.Default)
    val avatar3: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-04.png", JSImport.Default)
    val avatar4: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-05.png", JSImport.Default)
    val avatar5: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-06.png", JSImport.Default)
    val avatar6: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-07.png", JSImport.Default)
    val avatar7: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-08.png", JSImport.Default)
    val avatar8: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-09.png", JSImport.Default)
    val avatar9: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-10.png", JSImport.Default)
    val avatar10: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-11.png", JSImport.Default)
    val avatar11: String = js.native

    @js.native @JSImport("/assets/avatars/avatar-12.png", JSImport.Default)
    val avatar12: String = js.native

  end Avatars

  object TreeSitter:
    @js.native @JSImport("web-tree-sitter/tree-sitter.wasm?url", JSImport.Default)
    val wasm: String = js.native

    @js.native @JSImport("/assets/tree-sitter-scala.wasm?url", JSImport.Default)
    val scalaWasm: String = js.native

    @js.native @JSImport("/assets/highlights.scm", JSImport.Default)
    val highlights: String = js.native
  end TreeSitter

  object Cards:
    @js.native @JSImport("/assets/cards/card-back.png", JSImport.Default)
    val back: String = js.native

    val all: Map[String, String] = Clubs.all ++ Diamonds.all ++ Hearts.all ++ Spades.all

    def apply(id: String): String = all(id)

    object Clubs:
      val all = Map(
        "ace-of-clubs" -> ace,
        "two-of-clubs" -> two, 
        "three-of-clubs" -> three,
        "four-of-clubs" -> four,
        "five-of-clubs" -> five,
        "six-of-clubs" -> six,
        "seven-of-clubs" -> seven,
        "eight-of-clubs" -> eight,
        "nine-of-clubs" -> nine,
        "ten-of-clubs" -> ten,
        "jack-of-clubs" -> jack,
        "queen-of-clubs" -> queen,
        "king-of-clubs" -> king
      )

      @js.native @JSImport("/assets/cards/ace-of-clubs.png", JSImport.Default)
      val ace: String = js.native

      @js.native @JSImport("/assets/cards/two-of-clubs.png", JSImport.Default)
      val two: String = js.native

      @js.native @JSImport("/assets/cards/three-of-clubs.png", JSImport.Default)
      val three: String = js.native

      @js.native @JSImport("/assets/cards/four-of-clubs.png", JSImport.Default)
      val four: String = js.native

      @js.native @JSImport("/assets/cards/five-of-clubs.png", JSImport.Default)
      val five: String = js.native

      @js.native @JSImport("/assets/cards/six-of-clubs.png", JSImport.Default)
      val six: String = js.native

      @js.native @JSImport("/assets/cards/seven-of-clubs.png", JSImport.Default)
      val seven: String = js.native

      @js.native @JSImport("/assets/cards/eight-of-clubs.png", JSImport.Default)
      val eight: String = js.native

      @js.native @JSImport("/assets/cards/nine-of-clubs.png", JSImport.Default)
      val nine: String = js.native

      @js.native @JSImport("/assets/cards/ten-of-clubs.png", JSImport.Default)
      val ten: String = js.native

      @js.native @JSImport("/assets/cards/jack-of-clubs.png", JSImport.Default)
      val jack: String = js.native

      @js.native @JSImport("/assets/cards/queen-of-clubs.png", JSImport.Default)
      val queen: String = js.native

      @js.native @JSImport("/assets/cards/king-of-clubs.png", JSImport.Default)
      val king: String = js.native
    end Clubs

    object Diamonds:
      val all = Map(
        "ace-of-diamonds" -> ace,
        "two-of-diamonds" -> two, 
        "three-of-diamonds" -> three,
        "four-of-diamonds" -> four,
        "five-of-diamonds" -> five,
        "six-of-diamonds" -> six,
        "seven-of-diamonds" -> seven,
        "eight-of-diamonds" -> eight,
        "nine-of-diamonds" -> nine,
        "ten-of-diamonds" -> ten,
        "jack-of-diamonds" -> jack,
        "queen-of-diamonds" -> queen,
        "king-of-diamonds" -> king
      )
      @js.native @JSImport("/assets/cards/ace-of-diamonds.png", JSImport.Default)
      val ace: String = js.native

      @js.native @JSImport("/assets/cards/two-of-diamonds.png", JSImport.Default)
      val two: String = js.native

      @js.native @JSImport("/assets/cards/three-of-diamonds.png", JSImport.Default)
      val three: String = js.native

      @js.native @JSImport("/assets/cards/four-of-diamonds.png", JSImport.Default)
      val four: String = js.native

      @js.native @JSImport("/assets/cards/five-of-diamonds.png", JSImport.Default)
      val five: String = js.native

      @js.native @JSImport("/assets/cards/six-of-diamonds.png", JSImport.Default)
      val six: String = js.native

      @js.native @JSImport("/assets/cards/seven-of-diamonds.png", JSImport.Default)
      val seven: String = js.native

      @js.native @JSImport("/assets/cards/eight-of-diamonds.png", JSImport.Default)
      val eight: String = js.native

      @js.native @JSImport("/assets/cards/nine-of-diamonds.png", JSImport.Default)
      val nine: String = js.native

      @js.native @JSImport("/assets/cards/ten-of-diamonds.png", JSImport.Default)
      val ten: String = js.native

      @js.native @JSImport("/assets/cards/jack-of-diamonds.png", JSImport.Default)
      val jack: String = js.native

      @js.native @JSImport("/assets/cards/queen-of-diamonds.png", JSImport.Default)
      val queen: String = js.native

      @js.native @JSImport("/assets/cards/king-of-diamonds.png", JSImport.Default)
      val king: String = js.native
    end Diamonds

    object Hearts:
      val all = Map(
        "ace-of-hearts" -> ace,
        "two-of-hearts" -> two, 
        "three-of-hearts" -> three,
        "four-of-hearts" -> four,
        "five-of-hearts" -> five,
        "six-of-hearts" -> six,
        "seven-of-hearts" -> seven,
        "eight-of-hearts" -> eight,
        "nine-of-hearts" -> nine,
        "ten-of-hearts" -> ten,
        "jack-of-hearts" -> jack,
        "queen-of-hearts" -> queen,
        "king-of-hearts" -> king
      )

      @js.native @JSImport("/assets/cards/ace-of-hearts.png", JSImport.Default)
      val ace: String = js.native

      @js.native @JSImport("/assets/cards/two-of-hearts.png", JSImport.Default)
      val two: String = js.native

      @js.native @JSImport("/assets/cards/three-of-hearts.png", JSImport.Default)
      val three: String = js.native

      @js.native @JSImport("/assets/cards/four-of-hearts.png", JSImport.Default)
      val four: String = js.native

      @js.native @JSImport("/assets/cards/five-of-hearts.png", JSImport.Default)
      val five: String = js.native

      @js.native @JSImport("/assets/cards/six-of-hearts.png", JSImport.Default)
      val six: String = js.native

      @js.native @JSImport("/assets/cards/seven-of-hearts.png", JSImport.Default)
      val seven: String = js.native

      @js.native @JSImport("/assets/cards/eight-of-hearts.png", JSImport.Default)
      val eight: String = js.native

      @js.native @JSImport("/assets/cards/nine-of-hearts.png", JSImport.Default)
      val nine: String = js.native

      @js.native @JSImport("/assets/cards/ten-of-hearts.png", JSImport.Default)
      val ten: String = js.native

      @js.native @JSImport("/assets/cards/jack-of-hearts.png", JSImport.Default)
      val jack: String = js.native

      @js.native @JSImport("/assets/cards/queen-of-hearts.png", JSImport.Default)
      val queen: String = js.native

      @js.native @JSImport("/assets/cards/king-of-hearts.png", JSImport.Default)
      val king: String = js.native
    end Hearts

    object Spades:
      val all = Map(
        "ace-of-spades" -> ace,
        "two-of-spades" -> two, 
        "three-of-spades" -> three,
        "four-of-spades" -> four,
        "five-of-spades" -> five,
        "six-of-spades" -> six,
        "seven-of-spades" -> seven,
        "eight-of-spades" -> eight,
        "nine-of-spades" -> nine,
        "ten-of-spades" -> ten,
        "jack-of-spades" -> jack,
        "queen-of-spades" -> queen,
        "king-of-spades" -> king
      )

      @js.native @JSImport("/assets/cards/ace-of-spades.png", JSImport.Default)
      val ace: String = js.native

      @js.native @JSImport("/assets/cards/two-of-spades.png", JSImport.Default)
      val two: String = js.native

      @js.native @JSImport("/assets/cards/three-of-spades.png", JSImport.Default)
      val three: String = js.native

      @js.native @JSImport("/assets/cards/four-of-spades.png", JSImport.Default)
      val four: String = js.native

      @js.native @JSImport("/assets/cards/five-of-spades.png", JSImport.Default)
      val five: String = js.native

      @js.native @JSImport("/assets/cards/six-of-spades.png", JSImport.Default)
      val six: String = js.native

      @js.native @JSImport("/assets/cards/seven-of-spades.png", JSImport.Default)
      val seven: String = js.native

      @js.native @JSImport("/assets/cards/eight-of-spades.png", JSImport.Default)
      val eight: String = js.native

      @js.native @JSImport("/assets/cards/nine-of-spades.png", JSImport.Default)
      val nine: String = js.native

      @js.native @JSImport("/assets/cards/ten-of-spades.png", JSImport.Default)
      val ten: String = js.native

      @js.native @JSImport("/assets/cards/jack-of-spades.png", JSImport.Default)
      val jack: String = js.native

      @js.native @JSImport("/assets/cards/queen-of-spades.png", JSImport.Default)
      val queen: String = js.native

      @js.native @JSImport("/assets/cards/king-of-spades.png", JSImport.Default)
      val king: String = js.native
    end Spades
  end Cards
