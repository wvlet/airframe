package wvlet.airframe.rx.html.widget

import scala.scalajs.js
import js.annotation._
import js.|

package editor {

  package monaco {

    import org.scalajs.dom.raw.{HTMLElement, MouseEvent, Worker}

    @js.native
    trait Environment extends js.Object {
      var baseUrl: String = js.native

      def getWorker(workerId: String, label: String): Worker = js.native

      def getWorkerUrl(workerId: String, label: String): String = js.native
    }

    @js.native
    trait IDisposable extends js.Object {
      def dispose(): Unit = js.native
    }

    @js.native
    trait IEvent[T] extends js.Object {
      def apply(listener: js.Function1[T, Any], thisArg: js.Any = ???): IDisposable = js.native
    }

    @js.native
    @JSGlobal("monaco.Emitter")
    class Emitter[T] extends js.Object {
      def event: IEvent[T] = js.native

      def fire(event: T): Unit = js.native

      def dispose(): Unit = js.native
    }

    @js.native
    sealed trait MarkerTag extends js.Object {}

    @js.native
    @JSGlobal("monaco.MarkerTag")
    object MarkerTag extends js.Object {
      var Unnecessary: MarkerTag = js.native
      var Deprecated: MarkerTag  = js.native

      @JSBracketAccess
      def apply(value: MarkerTag): String = js.native
    }

    @js.native
    sealed trait MarkerSeverity extends js.Object {}

    @js.native
    @JSGlobal("monaco.MarkerSeverity")
    object MarkerSeverity extends js.Object {
      var Hint: MarkerSeverity    = js.native
      var Info: MarkerSeverity    = js.native
      var Warning: MarkerSeverity = js.native
      var Error: MarkerSeverity   = js.native

      @JSBracketAccess
      def apply(value: MarkerSeverity): String = js.native
    }

    @js.native
    @JSGlobal("monaco.CancellationTokenSource")
    class CancellationTokenSource protected () extends js.Object {
      def this(parent: CancellationToken = ???) = this()

      def token(): CancellationToken = js.native

      def cancel(): Unit = js.native

      def dispose(cancel: Boolean = ???): Unit = js.native
    }

    @js.native
    trait CancellationToken extends js.Object {
      def isCancellationRequested: Boolean = js.native

      def onCancellationRequested: IEvent[js.Any] = js.native
    }

    @js.native
    @JSGlobal("monaco.Uri")
    class Uri extends UriComponents {
      //      def scheme: String                                = js.native
      //      def authority: String                             = js.native
      //      def path: String                                  = js.native
      //      def query: String                                 = js.native
      //      def fragment: String                              = js.native
      def fsPath(): String = js.native

      def `with`(change: js.Any): Uri = js.native

      def toString(skipEncoding: Boolean = ???): String = js.native

      def toJSON(): UriComponents = js.native
    }

    @js.native
    @JSGlobal("monaco.Uri")
    object Uri extends js.Object {
      def isUri(thing: js.Any): Boolean = js.native

      def parse(value: String, _strict: Boolean = ???): Uri = js.native

      def file(path: String): Uri = js.native

      def from(components: js.Any): Uri = js.native

      def revive(data: UriComponents | Uri): Uri = js.native

      //def revive(data: UriComponents | Uri | Unit): Uri | Unit               = js.native
      //def revive(data: UriComponents | Uri | Null): Uri | Null               = js.native
      //def revive(data: UriComponents | Uri | Unit | Null): Uri | Unit | Null = js.native
    }

    @js.native
    trait UriComponents extends js.Object {
      var scheme: String    = js.native
      var authority: String = js.native
      var path: String      = js.native
      var query: String     = js.native
      var fragment: String  = js.native
    }

    @js.native
    sealed trait KeyCode extends js.Object {}

    @js.native
    @JSGlobal("monaco.KeyCode")
    object KeyCode extends js.Object {
      var Unknown: KeyCode                 = js.native
      var Backspace: KeyCode               = js.native
      var Tab: KeyCode                     = js.native
      var Enter: KeyCode                   = js.native
      var Shift: KeyCode                   = js.native
      var Ctrl: KeyCode                    = js.native
      var Alt: KeyCode                     = js.native
      var PauseBreak: KeyCode              = js.native
      var CapsLock: KeyCode                = js.native
      var Escape: KeyCode                  = js.native
      var Space: KeyCode                   = js.native
      var PageUp: KeyCode                  = js.native
      var PageDown: KeyCode                = js.native
      var End: KeyCode                     = js.native
      var Home: KeyCode                    = js.native
      var LeftArrow: KeyCode               = js.native
      var UpArrow: KeyCode                 = js.native
      var RightArrow: KeyCode              = js.native
      var DownArrow: KeyCode               = js.native
      var Insert: KeyCode                  = js.native
      var Delete: KeyCode                  = js.native
      var KEY_0: KeyCode                   = js.native
      var KEY_1: KeyCode                   = js.native
      var KEY_2: KeyCode                   = js.native
      var KEY_3: KeyCode                   = js.native
      var KEY_4: KeyCode                   = js.native
      var KEY_5: KeyCode                   = js.native
      var KEY_6: KeyCode                   = js.native
      var KEY_7: KeyCode                   = js.native
      var KEY_8: KeyCode                   = js.native
      var KEY_9: KeyCode                   = js.native
      var KEY_A: KeyCode                   = js.native
      var KEY_B: KeyCode                   = js.native
      var KEY_C: KeyCode                   = js.native
      var KEY_D: KeyCode                   = js.native
      var KEY_E: KeyCode                   = js.native
      var KEY_F: KeyCode                   = js.native
      var KEY_G: KeyCode                   = js.native
      var KEY_H: KeyCode                   = js.native
      var KEY_I: KeyCode                   = js.native
      var KEY_J: KeyCode                   = js.native
      var KEY_K: KeyCode                   = js.native
      var KEY_L: KeyCode                   = js.native
      var KEY_M: KeyCode                   = js.native
      var KEY_N: KeyCode                   = js.native
      var KEY_O: KeyCode                   = js.native
      var KEY_P: KeyCode                   = js.native
      var KEY_Q: KeyCode                   = js.native
      var KEY_R: KeyCode                   = js.native
      var KEY_S: KeyCode                   = js.native
      var KEY_T: KeyCode                   = js.native
      var KEY_U: KeyCode                   = js.native
      var KEY_V: KeyCode                   = js.native
      var KEY_W: KeyCode                   = js.native
      var KEY_X: KeyCode                   = js.native
      var KEY_Y: KeyCode                   = js.native
      var KEY_Z: KeyCode                   = js.native
      var Meta: KeyCode                    = js.native
      var ContextMenu: KeyCode             = js.native
      var F1: KeyCode                      = js.native
      var F2: KeyCode                      = js.native
      var F3: KeyCode                      = js.native
      var F4: KeyCode                      = js.native
      var F5: KeyCode                      = js.native
      var F6: KeyCode                      = js.native
      var F7: KeyCode                      = js.native
      var F8: KeyCode                      = js.native
      var F9: KeyCode                      = js.native
      var F10: KeyCode                     = js.native
      var F11: KeyCode                     = js.native
      var F12: KeyCode                     = js.native
      var F13: KeyCode                     = js.native
      var F14: KeyCode                     = js.native
      var F15: KeyCode                     = js.native
      var F16: KeyCode                     = js.native
      var F17: KeyCode                     = js.native
      var F18: KeyCode                     = js.native
      var F19: KeyCode                     = js.native
      var NumLock: KeyCode                 = js.native
      var ScrollLock: KeyCode              = js.native
      var US_SEMICOLON: KeyCode            = js.native
      var US_EQUAL: KeyCode                = js.native
      var US_COMMA: KeyCode                = js.native
      var US_MINUS: KeyCode                = js.native
      var US_DOT: KeyCode                  = js.native
      var US_SLASH: KeyCode                = js.native
      var US_BACKTICK: KeyCode             = js.native
      var US_OPEN_SQUARE_BRACKET: KeyCode  = js.native
      var US_BACKSLASH: KeyCode            = js.native
      var US_CLOSE_SQUARE_BRACKET: KeyCode = js.native
      var US_QUOTE: KeyCode                = js.native
      var OEM_8: KeyCode                   = js.native
      var OEM_102: KeyCode                 = js.native
      var NUMPAD_0: KeyCode                = js.native
      var NUMPAD_1: KeyCode                = js.native
      var NUMPAD_2: KeyCode                = js.native
      var NUMPAD_3: KeyCode                = js.native
      var NUMPAD_4: KeyCode                = js.native
      var NUMPAD_5: KeyCode                = js.native
      var NUMPAD_6: KeyCode                = js.native
      var NUMPAD_7: KeyCode                = js.native
      var NUMPAD_8: KeyCode                = js.native
      var NUMPAD_9: KeyCode                = js.native
      var NUMPAD_MULTIPLY: KeyCode         = js.native
      var NUMPAD_ADD: KeyCode              = js.native
      var NUMPAD_SEPARATOR: KeyCode        = js.native
      var NUMPAD_SUBTRACT: KeyCode         = js.native
      var NUMPAD_DECIMAL: KeyCode          = js.native
      var NUMPAD_DIVIDE: KeyCode           = js.native
      var KEY_IN_COMPOSITION: KeyCode      = js.native
      var ABNT_C1: KeyCode                 = js.native
      var ABNT_C2: KeyCode                 = js.native
      var MAX_VALUE: KeyCode               = js.native

      @JSBracketAccess
      def apply(value: KeyCode): String = js.native
    }

    @js.native
    @JSGlobal("monaco.KeyMod")
    class KeyMod extends js.Object {}

    @js.native
    @JSGlobal("monaco.KeyMod")
    object KeyMod extends js.Object {
      def CtrlCmd: Double = js.native

      def Shift: Double = js.native

      def Alt: Double = js.native

      def WinCtrl: Double = js.native

      def chord(firstPart: Double, secondPart: Double): Double = js.native
    }

    @js.native
    trait IMarkdownString extends js.Object {
      def value: String = js.native

      def isTrusted: Boolean = js.native

      def supportThemeIcons: Boolean = js.native

      var uris: IMarkdownString.Uris = js.native
    }

    object IMarkdownString {

      @js.native
      trait Uris extends js.Object {
        @JSBracketAccess
        def apply(href: String): UriComponents = js.native

        @JSBracketAccess
        def update(href: String, v: UriComponents): Unit = js.native
      }

    }

    @js.native
    trait IKeyboardEvent extends js.Object {
      def _standardKeyboardEventBrand: Boolean = js.native

      def browserEvent: IKeyboardEvent = js.native

      def target: HTMLElement = js.native

      def ctrlKey: Boolean = js.native

      def shiftKey: Boolean = js.native

      def altKey: Boolean = js.native

      def metaKey: Boolean = js.native

      def keyCode: KeyCode = js.native

      def code: String = js.native

      def equals(keybinding: Double): Boolean = js.native

      def preventDefault(): Unit = js.native

      def stopPropagation(): Unit = js.native
    }

    @js.native
    trait IMouseEvent extends js.Object {
      def browserEvent: MouseEvent = js.native

      def leftButton: Boolean = js.native

      def middleButton: Boolean = js.native

      def rightButton: Boolean = js.native

      def buttons: Double = js.native

      def target: HTMLElement = js.native

      def detail: Double = js.native

      def posx: Double = js.native

      def posy: Double = js.native

      def ctrlKey: Boolean = js.native

      def shiftKey: Boolean = js.native

      def altKey: Boolean = js.native

      def metaKey: Boolean = js.native

      def timestamp: Double = js.native

      def preventDefault(): Unit = js.native

      def stopPropagation(): Unit = js.native
    }

    @js.native
    trait IScrollEvent extends js.Object {
      def scrollTop: Double = js.native

      def scrollLeft: Double = js.native

      def scrollWidth: Double = js.native

      def scrollHeight: Double = js.native

      def scrollTopChanged: Boolean = js.native

      def scrollLeftChanged: Boolean = js.native

      def scrollWidthChanged: Boolean = js.native

      def scrollHeightChanged: Boolean = js.native
    }

    @js.native
    trait IPosition extends js.Object {
      def lineNumber: Double = js.native

      def column: Double = js.native
    }

    @js.native
    @JSGlobal("monaco.Position")
    class Position protected () extends js.Object {
      def this(lineNumber: Double, column: Double) = this()

      def lineNumber: Double = js.native

      def column: Double = js.native

      def `with`(newLineNumber: Double = ???, newColumn: Double = ???): Position = js.native

      def delta(deltaLineNumber: Double = ???, deltaColumn: Double = ???): Position = js.native

      def equals(other: IPosition): Boolean = js.native

      def isBefore(other: IPosition): Boolean = js.native

      def isBeforeOrEqual(other: IPosition): Boolean = js.native

      override def clone(): Position = js.native

      override def toString(): String = js.native
    }

    @js.native
    @JSGlobal("monaco.Position")
    object Position extends js.Object {
      def equals(a: IPosition | Null, b: IPosition | Null): Boolean = js.native

      def isBefore(a: IPosition, b: IPosition): Boolean = js.native

      def isBeforeOrEqual(a: IPosition, b: IPosition): Boolean = js.native

      def compare(a: IPosition, b: IPosition): Double = js.native

      def lift(pos: IPosition): Position = js.native

      def isIPosition(obj: js.Any): Boolean = js.native
    }

    @js.native
    trait IRange extends js.Object {
      def startLineNumber: Double = js.native

      def startColumn: Double = js.native

      def endLineNumber: Double = js.native

      def endColumn: Double = js.native
    }

    @js.native
    @JSGlobal("monaco.Range")
    class Range protected () extends js.Object {
      def this(startLineNumber: Double, startColumn: Double, endLineNumber: Double, endColumn: Double) = this()

      def startLineNumber: Double = js.native

      def startColumn: Double = js.native

      def endLineNumber: Double = js.native

      def endColumn: Double = js.native

      def isEmpty(): Boolean = js.native

      def containsPosition(position: IPosition): Boolean = js.native

      def containsRange(range: IRange): Boolean = js.native

      def strictContainsRange(range: IRange): Boolean = js.native

      def plusRange(range: IRange): Range = js.native

      def intersectRanges(range: IRange): Range | Null = js.native

      def equalsRange(other: IRange | Null): Boolean = js.native

      def getEndPosition(): Position = js.native

      def getStartPosition(): Position = js.native

      override def toString(): String = js.native

      def setEndPosition(endLineNumber: Double, endColumn: Double): Range = js.native

      def setStartPosition(startLineNumber: Double, startColumn: Double): Range = js.native

      def collapseToStart(): Range = js.native
    }

    @js.native
    @JSGlobal("monaco.Range")
    object Range extends js.Object {
      def isEmpty(range: IRange): Boolean = js.native

      def containsPosition(range: IRange, position: IPosition): Boolean = js.native

      def containsRange(range: IRange, otherRange: IRange): Boolean = js.native

      def strictContainsRange(range: IRange, otherRange: IRange): Boolean = js.native

      def plusRange(a: IRange, b: IRange): Range = js.native

      def intersectRanges(a: IRange, b: IRange): Range | Null = js.native

      def equalsRange(a: IRange | Null, b: IRange | Null): Boolean = js.native

      def collapseToStart(range: IRange): Range = js.native

      def fromPositions(start: IPosition, end: IPosition = ???): Range = js.native

      def lift(range: Unit | Null): Null = js.native

      def lift(range: IRange): Range = js.native

      def isIRange(obj: js.Any): Boolean = js.native

      def areIntersectingOrTouching(a: IRange, b: IRange): Boolean = js.native

      def areIntersecting(a: IRange, b: IRange): Boolean = js.native

      def compareRangesUsingStarts(a: IRange | Null | Unit, b: IRange | Null | Unit): Double = js.native

      def compareRangesUsingEnds(a: IRange, b: IRange): Double = js.native

      def spansMultipleLines(range: IRange): Boolean = js.native
    }

    @js.native
    trait ISelection extends js.Object {
      def selectionStartLineNumber: Double = js.native

      def selectionStartColumn: Double = js.native

      def positionLineNumber: Double = js.native

      def positionColumn: Double = js.native
    }

    @js.native
    @JSGlobal("monaco.Selection")
    class Selection protected () extends Range {
      def this(
          selectionStartLineNumber: Double,
          selectionStartColumn: Double,
          positionLineNumber: Double,
          positionColumn: Double
      ) = this()

      def selectionStartLineNumber: Double = js.native

      def selectionStartColumn: Double = js.native

      def positionLineNumber: Double = js.native

      def positionColumn: Double = js.native

      override def toString(): String = js.native

      def equalsSelection(other: ISelection): Boolean = js.native

      def getDirection(): SelectionDirection = js.native

      override def setEndPosition(endLineNumber: Double, endColumn: Double): Selection = js.native

      def getPosition(): Position = js.native

      override def setStartPosition(startLineNumber: Double, startColumn: Double): Selection = js.native
    }

    @js.native
    @JSGlobal("monaco.Selection")
    object Selection extends js.Object {
      def selectionsEqual(a: ISelection, b: ISelection): Boolean = js.native

      def fromPositions(start: IPosition, end: IPosition = ???): Selection = js.native

      def liftSelection(sel: ISelection): Selection = js.native

      def selectionsArrEqual(a: js.Array[ISelection], b: js.Array[ISelection]): Boolean = js.native

      def isISelection(obj: js.Any): Boolean = js.native

      def createWithDirection(
          startLineNumber: Double,
          startColumn: Double,
          endLineNumber: Double,
          endColumn: Double,
          direction: SelectionDirection
      ): Selection = js.native
    }

    @js.native
    sealed trait SelectionDirection extends js.Object {}

    @js.native
    @JSGlobal("monaco.SelectionDirection")
    object SelectionDirection extends js.Object {
      var LTR: SelectionDirection = js.native
      var RTL: SelectionDirection = js.native

      @JSBracketAccess
      def apply(value: SelectionDirection): String = js.native
    }

    @js.native
    @JSGlobal("monaco.Token")
    class Token protected () extends js.Object {
      def this(offset: Double, `type`: String, language: String) = this()

      var _tokenBrand: Unit = js.native

      def offset: Double = js.native

      def `type`: String = js.native

      def language: String = js.native

      override def toString(): String = js.native
    }

    package editor {

      import org.scalajs.dom.raw.Element
      import wvlet.airframe.rx.html.widget.editor.monaco.editor.Editor.{
        BuiltinTheme,
        EditorAutoClosingOvertypeStrategy,
        EditorAutoClosingStrategy,
        EditorAutoSurroundStrategy,
        EditorCommentsOptions,
        EditorFindOptions,
        EditorHoverOptions,
        EditorLightbulbOptions,
        EditorMinimapOptions,
        GoToLocationOptions,
        GoToLocationValues,
        IColors,
        IEditorModel,
        IEditorViewState,
        InternalParameterHintOptions,
        InternalSuggestOptions,
        LineNumbersType,
        ValidQuickSuggestionsOptions
      }

      import scala.scalajs.js.Promise

      @js.native
      trait IDiffNavigator extends js.Object {
        def canNavigate(): Boolean = js.native

        def next(): Unit = js.native

        def previous(): Unit = js.native

        def dispose(): Unit = js.native
      }

      @js.native
      trait IDiffNavigatorOptions extends js.Object {
        def followsCaret: Boolean = js.native

        def ignoreCharChanges: Boolean = js.native

        def alwaysRevealFirst: Boolean = js.native
      }

      @js.native
      trait IStandaloneThemeData extends js.Object {
        var base: BuiltinTheme                    = js.native
        var inherit: Boolean                      = js.native
        var rules: js.Array[ITokenThemeRule]      = js.native
        var encodedTokensColors: js.Array[String] = js.native
        var colors: IColors                       = js.native
      }

      @js.native
      trait ITokenThemeRule extends js.Object {
        var token: String      = js.native
        var foreground: String = js.native
        var background: String = js.native
        var fontStyle: String  = js.native
      }

      @js.native
      trait MonacoWebWorker[T] extends js.Object {
        def dispose(): Unit = js.native

        def getProxy(): Promise[T] = js.native

        def withSyncedResources(resources: js.Array[Uri]): Promise[T] = js.native
      }

      @js.native
      trait IWebWorkerOptions extends js.Object {
        var moduleId: String        = js.native
        var createData: js.Any      = js.native
        var label: String           = js.native
        var host: js.Any            = js.native
        var keepIdleModels: Boolean = js.native
      }

      @js.native
      trait IActionDescriptor extends js.Object {
        var id: String                    = js.native
        var label: String                 = js.native
        var precondition: String          = js.native
        var keybindings: js.Array[Double] = js.native
        var keybindingContext: String     = js.native
        var contextMenuGroupId: String    = js.native
        var contextMenuOrder: Double      = js.native

        def run(editor: ICodeEditor, args: js.Any*): Promise[Unit] = js.native
      }

      @js.native
      trait IGlobalEditorOptions extends js.Object {
        var tabSize: Double                   = js.native
        var insertSpaces: Boolean             = js.native
        var detectIndentation: Boolean        = js.native
        var trimAutoWhitespace: Boolean       = js.native
        var largeFileOptimizations: Boolean   = js.native
        var wordBasedSuggestions: Boolean     = js.native
        var stablePeek: Boolean               = js.native
        var maxTokenizationLineLength: Double = js.native
      }

      @js.native
      trait IStandaloneEditorConstructionOptions extends IEditorConstructionOptions with IGlobalEditorOptions {
        var model: ITextModel | Null     = js.native
        var value: String                = js.native
        var language: String             = js.native
        var theme: String                = js.native
        var accessibilityHelpUrl: String = js.native
      }

      @js.native
      trait IDiffEditorConstructionOptions extends IDiffEditorOptions {
        var theme: String = js.native
      }

      @js.native
      trait IStandaloneCodeEditor extends ICodeEditor {
        //def updateOptions(newOptions: IEditorOptions with IGlobalEditorOptions): Unit             = js.native
        def addCommand(keybinding: Double, handler: ICommandHandler, context: String = ???): String | Null = js.native

        def createContextKey[T](key: String, defaultValue: T): IContextKey[T] = js.native

        def addAction(descriptor: IActionDescriptor): IDisposable = js.native
      }

      @js.native
      trait IStandaloneDiffEditor extends IDiffEditor {
        def addCommand(keybinding: Double, handler: ICommandHandler, context: String = ???): String | Null = js.native

        def createContextKey[T](key: String, defaultValue: T): IContextKey[T] = js.native

        def addAction(descriptor: IActionDescriptor): IDisposable = js.native

        override def getOriginalEditor(): IStandaloneCodeEditor = js.native

        override def getModifiedEditor(): IStandaloneCodeEditor = js.native
      }

      @js.native
      trait ICommandHandler extends js.Object {
        def apply(args: js.Any*): Unit = js.native
      }

      @js.native
      trait IContextKey[T] extends js.Object {
        def set(value: T): Unit = js.native

        def reset(): Unit = js.native

        def get(): T | Unit = js.native
      }

      @js.native
      trait IEditorOverrideServices extends js.Object {
        @JSBracketAccess
        def apply(index: String): js.Any = js.native

        @JSBracketAccess
        def update(index: String, v: js.Any): Unit = js.native
      }

      @js.native
      trait IMarker extends js.Object {
        var owner: String                                     = js.native
        var resource: Uri                                     = js.native
        var severity: MarkerSeverity                          = js.native
        var code: String | js.Any                             = js.native
        var message: String                                   = js.native
        var source: String                                    = js.native
        var startLineNumber: Double                           = js.native
        var startColumn: Double                               = js.native
        var endLineNumber: Double                             = js.native
        var endColumn: Double                                 = js.native
        var relatedInformation: js.Array[IRelatedInformation] = js.native
        var tags: js.Array[MarkerTag]                         = js.native
      }

      @js.native
      trait IMarkerData extends js.Object {
        var code: String | js.Any                             = js.native
        var severity: MarkerSeverity                          = js.native
        var message: String                                   = js.native
        var source: String                                    = js.native
        var startLineNumber: Double                           = js.native
        var startColumn: Double                               = js.native
        var endLineNumber: Double                             = js.native
        var endColumn: Double                                 = js.native
        var relatedInformation: js.Array[IRelatedInformation] = js.native
        var tags: js.Array[MarkerTag]                         = js.native
      }

      @js.native
      trait IRelatedInformation extends js.Object {
        var resource: Uri           = js.native
        var message: String         = js.native
        var startLineNumber: Double = js.native
        var startColumn: Double     = js.native
        var endLineNumber: Double   = js.native
        var endColumn: Double       = js.native
      }

      @js.native
      trait IColorizerOptions extends js.Object {
        var tabSize: Double = js.native
      }

      @js.native
      trait IColorizerElementOptions extends IColorizerOptions {
        var theme: String    = js.native
        var mimeType: String = js.native
      }

      @js.native
      sealed trait ScrollbarVisibility extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.ScrollbarVisibility")
      object ScrollbarVisibility extends js.Object {
        var Auto: ScrollbarVisibility    = js.native
        var Hidden: ScrollbarVisibility  = js.native
        var Visible: ScrollbarVisibility = js.native

        @JSBracketAccess
        def apply(value: ScrollbarVisibility): String = js.native
      }

      @js.native
      trait ThemeColor extends js.Object {
        var id: String = js.native
      }

      @js.native
      sealed trait OverviewRulerLane extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.OverviewRulerLane")
      object OverviewRulerLane extends js.Object {
        var Left: OverviewRulerLane   = js.native
        var Center: OverviewRulerLane = js.native
        var Right: OverviewRulerLane  = js.native
        var Full: OverviewRulerLane   = js.native

        @JSBracketAccess
        def apply(value: OverviewRulerLane): String = js.native
      }

      @js.native
      sealed trait MinimapPosition extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.MinimapPosition")
      object MinimapPosition extends js.Object {
        var Inline: MinimapPosition = js.native
        var Gutter: MinimapPosition = js.native

        @JSBracketAccess
        def apply(value: MinimapPosition): String = js.native
      }

      @js.native
      trait IDecorationOptions extends js.Object {
        var color: String | ThemeColor | Unit = js.native
        var darkColor: String | ThemeColor    = js.native
      }

      @js.native
      trait IModelDecorationOverviewRulerOptions extends IDecorationOptions {
        var position: OverviewRulerLane = js.native
      }

      @js.native
      trait IModelDecorationMinimapOptions extends IDecorationOptions {
        var position: MinimapPosition = js.native
      }

      @js.native
      trait IModelDecorationOptions extends js.Object {
        var stickiness: TrackedRangeStickiness                                          = js.native
        var className: String | Null                                                    = js.native
        var glyphMarginHoverMessage: IMarkdownString | js.Array[IMarkdownString] | Null = js.native
        var hoverMessage: IMarkdownString | js.Array[IMarkdownString] | Null            = js.native
        var isWholeLine: Boolean                                                        = js.native
        var zIndex: Double                                                              = js.native
        var overviewRuler: IModelDecorationOverviewRulerOptions | Null                  = js.native
        var minimap: IModelDecorationMinimapOptions | Null                              = js.native
        var glyphMarginClassName: String | Null                                         = js.native
        var linesDecorationsClassName: String | Null                                    = js.native
        var marginClassName: String | Null                                              = js.native
        var inlineClassName: String | Null                                              = js.native
        var inlineClassNameAffectsLetterSpacing: Boolean                                = js.native
        var beforeContentClassName: String | Null                                       = js.native
        var afterContentClassName: String | Null                                        = js.native
      }

      @js.native
      trait IModelDeltaDecoration extends js.Object {
        var range: IRange                    = js.native
        var options: IModelDecorationOptions = js.native
      }

      @js.native
      trait IModelDecoration extends js.Object {
        def id: String = js.native

        def ownerId: Double = js.native

        def range: Range = js.native

        def options: IModelDecorationOptions = js.native
      }

      @js.native
      trait IWordAtPosition extends js.Object {
        def word: String = js.native

        def startColumn: Double = js.native

        def endColumn: Double = js.native
      }

      @js.native
      sealed trait EndOfLinePreference extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.EndOfLinePreference")
      object EndOfLinePreference extends js.Object {
        var TextDefined: EndOfLinePreference = js.native
        var LF: EndOfLinePreference          = js.native
        var CRLF: EndOfLinePreference        = js.native

        @JSBracketAccess
        def apply(value: EndOfLinePreference): String = js.native
      }

      @js.native
      sealed trait DefaultEndOfLine extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.DefaultEndOfLine")
      object DefaultEndOfLine extends js.Object {
        var LF: DefaultEndOfLine   = js.native
        var CRLF: DefaultEndOfLine = js.native

        @JSBracketAccess
        def apply(value: DefaultEndOfLine): String = js.native
      }

      @js.native
      sealed trait EndOfLineSequence extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.EndOfLineSequence")
      object EndOfLineSequence extends js.Object {
        var LF: EndOfLineSequence   = js.native
        var CRLF: EndOfLineSequence = js.native

        @JSBracketAccess
        def apply(value: EndOfLineSequence): String = js.native
      }

      @js.native
      trait ISingleEditOperation extends js.Object {
        var range: IRange             = js.native
        var text: String | Null       = js.native
        var forceMoveMarkers: Boolean = js.native
      }

      @js.native
      trait IIdentifiedSingleEditOperation extends js.Object {
        var range: Range              = js.native
        var text: String | Null       = js.native
        var forceMoveMarkers: Boolean = js.native
      }

      @js.native
      trait ICursorStateComputer extends js.Object {
        def apply(inverseEditOperations: js.Array[IIdentifiedSingleEditOperation]): js.Array[Selection] | Null =
          js.native
      }

      @js.native
      @JSGlobal("monaco.editor.TextModelResolvedOptions")
      class TextModelResolvedOptions extends js.Object {
        var _textModelResolvedOptionsBrand: Unit = js.native

        def tabSize: Double = js.native

        def indentSize: Double = js.native

        def insertSpaces: Boolean = js.native

        def defaultEOL: DefaultEndOfLine = js.native

        def trimAutoWhitespace: Boolean = js.native
      }

      @js.native
      trait ITextModelUpdateOptions extends js.Object {
        var tabSize: Double             = js.native
        var indentSize: Double          = js.native
        var insertSpaces: Boolean       = js.native
        var trimAutoWhitespace: Boolean = js.native
      }

      @js.native
      @JSGlobal("monaco.editor.FindMatch")
      class FindMatch extends js.Object {
        var _findMatchBrand: Unit = js.native

        def range: Range = js.native

        def matches: js.Array[String] | Null = js.native
      }

      @js.native
      sealed trait TrackedRangeStickiness extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.TrackedRangeStickiness")
      object TrackedRangeStickiness extends js.Object {
        var AlwaysGrowsWhenTypingAtEdges: TrackedRangeStickiness = js.native
        var NeverGrowsWhenTypingAtEdges: TrackedRangeStickiness  = js.native
        var GrowsOnlyWhenTypingBefore: TrackedRangeStickiness    = js.native
        var GrowsOnlyWhenTypingAfter: TrackedRangeStickiness     = js.native

        @JSBracketAccess
        def apply(value: TrackedRangeStickiness): String = js.native
      }

      @js.native
      trait ITextModel extends js.Object {
        def uri: Uri = js.native

        def id: String = js.native

        def getOptions(): TextModelResolvedOptions = js.native

        def getVersionId(): Double = js.native

        def getAlternativeVersionId(): Double = js.native

        def setValue(newValue: String): Unit = js.native

        def getValue(eol: EndOfLinePreference = ???, preserveBOM: Boolean = ???): String = js.native

        def getValueLength(eol: EndOfLinePreference = ???, preserveBOM: Boolean = ???): Double = js.native

        def getValueInRange(range: IRange, eol: EndOfLinePreference = ???): String = js.native

        def getValueLengthInRange(range: IRange): Double = js.native

        def getCharacterCountInRange(range: IRange): Double = js.native

        def getLineCount(): Double = js.native

        def getLineContent(lineNumber: Double): String = js.native

        def getLineLength(lineNumber: Double): Double = js.native

        def getLinesContent(): js.Array[String] = js.native

        def getEOL(): String = js.native

        def getLineMinColumn(lineNumber: Double): Double = js.native

        def getLineMaxColumn(lineNumber: Double): Double = js.native

        def getLineFirstNonWhitespaceColumn(lineNumber: Double): Double = js.native

        def getLineLastNonWhitespaceColumn(lineNumber: Double): Double = js.native

        def validatePosition(position: IPosition): Position = js.native

        def modifyPosition(position: IPosition, offset: Double): Position = js.native

        def validateRange(range: IRange): Range = js.native

        def getOffsetAt(position: IPosition): Double = js.native

        def getPositionAt(offset: Double): Position = js.native

        def getFullModelRange(): Range = js.native

        def isDisposed(): Boolean = js.native

        def findMatches(
            searchString: String,
            searchOnlyEditableRange: Boolean,
            isRegex: Boolean,
            matchCase: Boolean,
            wordSeparators: String | Null,
            captureMatches: Boolean,
            limitResultCount: Double
        ): js.Array[FindMatch] = js.native

        def findMatches(
            searchString: String,
            searchScope: IRange,
            isRegex: Boolean,
            matchCase: Boolean,
            wordSeparators: String | Null,
            captureMatches: Boolean,
            limitResultCount: Double
        ): js.Array[FindMatch] = js.native

        def findNextMatch(
            searchString: String,
            searchStart: IPosition,
            isRegex: Boolean,
            matchCase: Boolean,
            wordSeparators: String | Null,
            captureMatches: Boolean
        ): FindMatch | Null = js.native

        def findPreviousMatch(
            searchString: String,
            searchStart: IPosition,
            isRegex: Boolean,
            matchCase: Boolean,
            wordSeparators: String | Null,
            captureMatches: Boolean
        ): FindMatch | Null = js.native

        def getModeId(): String = js.native

        def getWordAtPosition(position: IPosition): IWordAtPosition | Null = js.native

        def getWordUntilPosition(position: IPosition): IWordAtPosition = js.native

        def deltaDecorations(
            oldDecorations: js.Array[String],
            newDecorations: js.Array[IModelDeltaDecoration],
            ownerId: Double = ???
        ): js.Array[String] = js.native

        def getDecorationOptions(id: String): IModelDecorationOptions | Null = js.native

        def getDecorationRange(id: String): Range | Null = js.native

        def getLineDecorations(
            lineNumber: Double,
            ownerId: Double = ???,
            filterOutValidation: Boolean = ???
        ): js.Array[IModelDecoration] = js.native

        def getLinesDecorations(
            startLineNumber: Double,
            endLineNumber: Double,
            ownerId: Double = ???,
            filterOutValidation: Boolean = ???
        ): js.Array[IModelDecoration] = js.native

        def getDecorationsInRange(
            range: IRange,
            ownerId: Double = ???,
            filterOutValidation: Boolean = ???
        ): js.Array[IModelDecoration] = js.native

        def getAllDecorations(ownerId: Double = ???, filterOutValidation: Boolean = ???): js.Array[IModelDecoration] =
          js.native

        def getOverviewRulerDecorations(
            ownerId: Double = ???,
            filterOutValidation: Boolean = ???
        ): js.Array[IModelDecoration] = js.native

        def normalizeIndentation(str: String): String = js.native

        def updateOptions(newOpts: ITextModelUpdateOptions): Unit = js.native

        def detectIndentation(defaultInsertSpaces: Boolean, defaultTabSize: Double): Unit = js.native

        def pushStackElement(): Unit = js.native

        def pushEditOperations(
            beforeCursorState: js.Array[Selection],
            editOperations: js.Array[IIdentifiedSingleEditOperation],
            cursorStateComputer: ICursorStateComputer
        ): js.Array[Selection] | Null = js.native

        def pushEOL(eol: EndOfLineSequence): Unit = js.native

        def applyEdits(operations: js.Array[IIdentifiedSingleEditOperation]): js.Array[IIdentifiedSingleEditOperation] =
          js.native

        def setEOL(eol: EndOfLineSequence): Unit = js.native

        def onDidChangeContent(listener: js.Function1[IModelContentChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeDecorations(listener: js.Function1[IModelDecorationsChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeOptions(listener: js.Function1[IModelOptionsChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeLanguage(listener: js.Function1[IModelLanguageChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeLanguageConfiguration(
            listener: js.Function1[IModelLanguageConfigurationChangedEvent, Unit]
        ): IDisposable = js.native

        def onWillDispose(listener: js.Function0[Unit]): IDisposable = js.native

        def dispose(): Unit = js.native
      }

      @js.native
      trait IEditOperationBuilder extends js.Object {
        def addEditOperation(range: Range, text: String | Null, forceMoveMarkers: Boolean = ???): Unit = js.native

        def addTrackedEditOperation(range: Range, text: String | Null, forceMoveMarkers: Boolean = ???): Unit =
          js.native

        def trackSelection(selection: Selection, trackPreviousOnEmpty: Boolean = ???): String = js.native
      }

      @js.native
      trait ICursorStateComputerData extends js.Object {
        def getInverseEditOperations(): js.Array[IIdentifiedSingleEditOperation] = js.native

        def getTrackedSelection(id: String): Selection = js.native
      }

      @js.native
      trait ICommand extends js.Object {
        def getEditOperations(model: ITextModel, builder: IEditOperationBuilder): Unit = js.native

        def computeCursorState(model: ITextModel, helper: ICursorStateComputerData): Selection = js.native
      }

      @js.native
      trait IDiffEditorModel extends js.Object {
        var original: ITextModel = js.native
        var modified: ITextModel = js.native
      }

      @js.native
      trait IModelChangedEvent extends js.Object {
        def oldModelUrl: Uri | Null = js.native

        def newModelUrl: Uri | Null = js.native
      }

      @js.native
      trait IDimension extends js.Object {
        var width: Double  = js.native
        var height: Double = js.native
      }

      @js.native
      trait IChange extends js.Object {
        def originalStartLineNumber: Double = js.native

        def originalEndLineNumber: Double = js.native

        def modifiedStartLineNumber: Double = js.native

        def modifiedEndLineNumber: Double = js.native
      }

      @js.native
      trait ICharChange extends IChange {
        def originalStartColumn: Double = js.native

        def originalEndColumn: Double = js.native

        def modifiedStartColumn: Double = js.native

        def modifiedEndColumn: Double = js.native
      }

      @js.native
      trait ILineChange extends IChange {
        def charChanges: js.Array[ICharChange] | Unit = js.native
      }

      @js.native
      trait IContentSizeChangedEvent extends js.Object {
        def contentWidth: Double = js.native

        def contentHeight: Double = js.native

        def contentWidthChanged: Boolean = js.native

        def contentHeightChanged: Boolean = js.native
      }

      @js.native
      trait INewScrollPosition extends js.Object {
        var scrollLeft: Double = js.native
        var scrollTop: Double  = js.native
      }

      @js.native
      trait IEditorAction extends js.Object {
        def id: String = js.native

        def label: String = js.native

        def alias: String = js.native

        def isSupported(): Boolean = js.native

        def run(): Promise[Unit] = js.native
      }

      @js.native
      trait ICursorState extends js.Object {
        var inSelectionMode: Boolean  = js.native
        var selectionStart: IPosition = js.native
        var position: IPosition       = js.native
      }

      @js.native
      trait IViewState extends js.Object {
        var scrollTop: Double                 = js.native
        var scrollTopWithoutViewZones: Double = js.native
        var scrollLeft: Double                = js.native
        var firstPosition: IPosition          = js.native
        var firstPositionDeltaTop: Double     = js.native
      }

      @js.native
      trait ICodeEditorViewState extends js.Object {
        var cursorState: js.Array[ICursorState]                         = js.native
        var viewState: IViewState                                       = js.native
        var contributionsState: ICodeEditorViewState.ContributionsState = js.native
      }

      object ICodeEditorViewState {

        @js.native
        trait ContributionsState extends js.Object {
          @JSBracketAccess
          def apply(id: String): js.Any = js.native

          @JSBracketAccess
          def update(id: String, v: js.Any): Unit = js.native
        }

      }

      @js.native
      trait IDiffEditorViewState extends js.Object {
        var original: ICodeEditorViewState | Null = js.native
        var modified: ICodeEditorViewState | Null = js.native
      }

      @js.native
      sealed trait ScrollType extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.ScrollType")
      object ScrollType extends js.Object {
        var Smooth: ScrollType    = js.native
        var Immediate: ScrollType = js.native

        @JSBracketAccess
        def apply(value: ScrollType): String = js.native
      }

      @js.native
      trait IEditor extends js.Object {
        def onDidDispose(listener: js.Function0[Unit]): IDisposable = js.native

        def dispose(): Unit = js.native

        def getId(): String = js.native

        def getEditorType(): String = js.native

        def updateOptions(newOptions: IEditorOptions): Unit = js.native

        def layout(dimension: IDimension = ???): Unit = js.native

        def focus(): Unit = js.native

        def hasTextFocus(): Boolean = js.native

        def getSupportedActions(): js.Array[IEditorAction] = js.native

        def saveViewState(): IEditorViewState | Null = js.native

        def restoreViewState(state: IEditorViewState): Unit = js.native

        def getVisibleColumnFromPosition(position: IPosition): Double = js.native

        def getPosition(): Position | Null = js.native

        def setPosition(position: IPosition): Unit = js.native

        def revealLine(lineNumber: Double, scrollType: ScrollType = ???): Unit = js.native

        def revealLineInCenter(lineNumber: Double, scrollType: ScrollType = ???): Unit = js.native

        def revealLineInCenterIfOutsideViewport(lineNumber: Double, scrollType: ScrollType = ???): Unit = js.native

        def revealPosition(position: IPosition, scrollType: ScrollType = ???): Unit = js.native

        def revealPositionInCenter(position: IPosition, scrollType: ScrollType = ???): Unit = js.native

        def revealPositionInCenterIfOutsideViewport(position: IPosition, scrollType: ScrollType = ???): Unit = js.native

        def getSelection(): Selection | Null = js.native

        def getSelections(): js.Array[Selection] | Null = js.native

        def setSelection(selection: IRange): Unit = js.native

        def setSelection(selection: Range): Unit = js.native

        def setSelection(selection: ISelection): Unit = js.native

        def setSelection(selection: Selection): Unit = js.native

        def revealLines(startLineNumber: Double, endLineNumber: Double, scrollType: ScrollType = ???): Unit = js.native

        def revealLinesInCenter(lineNumber: Double, endLineNumber: Double, scrollType: ScrollType = ???): Unit =
          js.native

        def revealLinesInCenterIfOutsideViewport(
            lineNumber: Double,
            endLineNumber: Double,
            scrollType: ScrollType = ???
        ): Unit = js.native

        def revealRange(range: IRange, scrollType: ScrollType = ???): Unit = js.native

        def revealRangeInCenter(range: IRange, scrollType: ScrollType = ???): Unit = js.native

        def revealRangeAtTop(range: IRange, scrollType: ScrollType = ???): Unit = js.native

        def revealRangeInCenterIfOutsideViewport(range: IRange, scrollType: ScrollType = ???): Unit = js.native

        def trigger(source: String, handlerId: String, payload: js.Any): Unit = js.native

        def getModel(): IEditorModel | Null = js.native

        def setModel(model: IEditorModel | Null): Unit = js.native
      }

      @js.native
      trait IEditorContribution extends js.Object {
        def dispose(): Unit = js.native

        def saveViewState(): js.Dynamic = js.native

        def restoreViewState(state: js.Any): Unit = js.native
      }

      @js.native
      @JSGlobal("monaco.editor.EditorType")
      object EditorType extends js.Object {
        var ICodeEditor: String = js.native
        var IDiffEditor: String = js.native
      }

      @js.native
      trait IModelLanguageChangedEvent extends js.Object {
        def oldLanguage: String = js.native

        def newLanguage: String = js.native
      }

      @js.native
      trait IModelLanguageConfigurationChangedEvent extends js.Object {}

      @js.native
      trait IModelContentChange extends js.Object {
        def range: IRange = js.native

        def rangeOffset: Double = js.native

        def rangeLength: Double = js.native

        def text: String = js.native
      }

      @js.native
      trait IModelContentChangedEvent extends js.Object {
        def changes: js.Array[IModelContentChange] = js.native

        def eol: String = js.native

        def versionId: Double = js.native

        def isUndoing: Boolean = js.native

        def isRedoing: Boolean = js.native

        def isFlush: Boolean = js.native
      }

      @js.native
      trait IModelDecorationsChangedEvent extends js.Object {}

      @js.native
      trait IModelOptionsChangedEvent extends js.Object {
        def tabSize: Boolean = js.native

        def indentSize: Boolean = js.native

        def insertSpaces: Boolean = js.native

        def trimAutoWhitespace: Boolean = js.native
      }

      @js.native
      sealed trait CursorChangeReason extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.CursorChangeReason")
      object CursorChangeReason extends js.Object {
        var NotSet: CursorChangeReason             = js.native
        var ContentFlush: CursorChangeReason       = js.native
        var RecoverFromMarkers: CursorChangeReason = js.native
        var Explicit: CursorChangeReason           = js.native
        var Paste: CursorChangeReason              = js.native
        var Undo: CursorChangeReason               = js.native
        var Redo: CursorChangeReason               = js.native

        @JSBracketAccess
        def apply(value: CursorChangeReason): String = js.native
      }

      @js.native
      trait ICursorPositionChangedEvent extends js.Object {
        def position: Position = js.native

        def secondaryPositions: js.Array[Position] = js.native

        def reason: CursorChangeReason = js.native

        def source: String = js.native
      }

      @js.native
      trait ICursorSelectionChangedEvent extends js.Object {
        def selection: Selection = js.native

        def secondarySelections: js.Array[Selection] = js.native

        def modelVersionId: Double = js.native

        def oldSelections: js.Array[Selection] | Null = js.native

        def oldModelVersionId: Double = js.native

        def source: String = js.native

        def reason: CursorChangeReason = js.native
      }

      @js.native
      sealed trait AccessibilitySupport extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.AccessibilitySupport")
      object AccessibilitySupport extends js.Object {
        var Unknown: AccessibilitySupport  = js.native
        var Disabled: AccessibilitySupport = js.native
        var Enabled: AccessibilitySupport  = js.native

        @JSBracketAccess
        def apply(value: AccessibilitySupport): String = js.native
      }

      @js.native
      sealed trait EditorAutoIndentStrategy extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.EditorAutoIndentStrategy")
      object EditorAutoIndentStrategy extends js.Object {
        var None: EditorAutoIndentStrategy     = js.native
        var Keep: EditorAutoIndentStrategy     = js.native
        var Brackets: EditorAutoIndentStrategy = js.native
        var Advanced: EditorAutoIndentStrategy = js.native
        var Full: EditorAutoIndentStrategy     = js.native

        @JSBracketAccess
        def apply(value: EditorAutoIndentStrategy): String = js.native
      }

      @js.native
      trait IEditorOptions extends js.Object {
        var inDiffEditor: Boolean                                  = js.native
        var ariaLabel: String                                      = js.native
        var rulers: js.Array[Double]                               = js.native
        var wordSeparators: String                                 = js.native
        var selectionClipboard: Boolean                            = js.native
        var lineNumbers: LineNumbersType                           = js.native
        var cursorSurroundingLines: Double                         = js.native
        var cursorSurroundingLinesStyle: String                    = js.native
        var renderFinalNewline: Boolean                            = js.native
        var selectOnLineNumbers: Boolean                           = js.native
        var lineNumbersMinChars: Double                            = js.native
        var glyphMargin: Boolean                                   = js.native
        var lineDecorationsWidth: Double | String                  = js.native
        var revealHorizontalRightPadding: Double                   = js.native
        var roundedSelection: Boolean                              = js.native
        var extraEditorClassName: String                           = js.native
        var readOnly: Boolean                                      = js.native
        var renderValidationDecorations: String                    = js.native
        var scrollbar: IEditorScrollbarOptions                     = js.native
        var minimap: IEditorMinimapOptions                         = js.native
        var find: IEditorFindOptions                               = js.native
        var fixedOverflowWidgets: Boolean                          = js.native
        var overviewRulerLanes: Double                             = js.native
        var overviewRulerBorder: Boolean                           = js.native
        var cursorBlinking: String                                 = js.native
        var mouseWheelZoom: Boolean                                = js.native
        var mouseStyle: String                                     = js.native
        var cursorSmoothCaretAnimation: Boolean                    = js.native
        var cursorStyle: String                                    = js.native
        var cursorWidth: Double                                    = js.native
        var fontLigatures: Boolean | String                        = js.native
        var disableLayerHinting: Boolean                           = js.native
        var disableMonospaceOptimizations: Boolean                 = js.native
        var hideCursorInOverviewRuler: Boolean                     = js.native
        var scrollBeyondLastLine: Boolean                          = js.native
        var scrollBeyondLastColumn: Double                         = js.native
        var smoothScrolling: Boolean                               = js.native
        var automaticLayout: Boolean                               = js.native
        var wordWrap: String                                       = js.native
        var wordWrapColumn: Double                                 = js.native
        var wordWrapMinified: Boolean                              = js.native
        var wrappingIndent: String                                 = js.native
        var wrappingStrategy: String                               = js.native
        var wordWrapBreakBeforeCharacters: String                  = js.native
        var wordWrapBreakAfterCharacters: String                   = js.native
        var stopRenderingLineAfter: Double                         = js.native
        var hover: IEditorHoverOptions                             = js.native
        var links: Boolean                                         = js.native
        var colorDecorators: Boolean                               = js.native
        var comments: IEditorCommentsOptions                       = js.native
        var contextmenu: Boolean                                   = js.native
        var mouseWheelScrollSensitivity: Double                    = js.native
        var fastScrollSensitivity: Double                          = js.native
        var multiCursorModifier: String                            = js.native
        var multiCursorMergeOverlapping: Boolean                   = js.native
        var multiCursorPaste: String                               = js.native
        var accessibilitySupport: String                           = js.native
        var accessibilityPageSize: Double                          = js.native
        var suggest: ISuggestOptions                               = js.native
        var gotoLocation: IGotoLocationOptions                     = js.native
        var quickSuggestions: Boolean | IQuickSuggestionsOptions   = js.native
        var quickSuggestionsDelay: Double                          = js.native
        var parameterHints: IEditorParameterHintOptions            = js.native
        var autoClosingBrackets: EditorAutoClosingStrategy         = js.native
        var autoClosingQuotes: EditorAutoClosingStrategy           = js.native
        var autoClosingOvertype: EditorAutoClosingOvertypeStrategy = js.native
        var autoSurround: EditorAutoSurroundStrategy               = js.native
        var autoIndent: String                                     = js.native
        var formatOnType: Boolean                                  = js.native
        var formatOnPaste: Boolean                                 = js.native
        var dragAndDrop: Boolean                                   = js.native
        var suggestOnTriggerCharacters: Boolean                    = js.native
        var acceptSuggestionOnEnter: String                        = js.native
        var acceptSuggestionOnCommitCharacter: Boolean             = js.native
        var snippetSuggestions: String                             = js.native
        var emptySelectionClipboard: Boolean                       = js.native
        var copyWithSyntaxHighlighting: Boolean                    = js.native
        var suggestSelection: String                               = js.native
        var suggestFontSize: Double                                = js.native
        var suggestLineHeight: Double                              = js.native
        var tabCompletion: String                                  = js.native
        var selectionHighlight: Boolean                            = js.native
        var occurrencesHighlight: Boolean                          = js.native
        var codeLens: Boolean                                      = js.native
        var lightbulb: IEditorLightbulbOptions                     = js.native
        var codeActionsOnSaveTimeout: Double                       = js.native
        var folding: Boolean                                       = js.native
        var foldingStrategy: String                                = js.native
        var foldingHighlight: Boolean                              = js.native
        var showFoldingControls: String                            = js.native
        var matchBrackets: String                                  = js.native
        var renderWhitespace: String                               = js.native
        var renderControlCharacters: Boolean                       = js.native
        var renderIndentGuides: Boolean                            = js.native
        var highlightActiveIndentGuide: Boolean                    = js.native
        var renderLineHighlight: String                            = js.native
        var useTabStops: Boolean                                   = js.native
        var fontFamily: String                                     = js.native
        var fontWeight: String                                     = js.native
        var fontSize: Double                                       = js.native
        var lineHeight: Double                                     = js.native
        var letterSpacing: Double                                  = js.native
        var showUnused: Boolean                                    = js.native
        var peekWidgetDefaultFocus: String                         = js.native
      }

      @js.native
      trait IEditorConstructionOptions extends IEditorOptions {
        var dimension: IDimension = js.native
      }

      @js.native
      trait IDiffEditorOptions extends IEditorOptions {
        var enableSplitViewResizing: Boolean = js.native
        var renderSideBySide: Boolean        = js.native
        var maxComputationTime: Double       = js.native
        var ignoreTrimWhitespace: Boolean    = js.native
        var renderIndicators: Boolean        = js.native
        var originalEditable: Boolean        = js.native
      }

      @js.native
      @JSGlobal("monaco.editor.ConfigurationChangedEvent")
      class ConfigurationChangedEvent extends js.Object {
        def hasChanged(id: EditorOption): Boolean = js.native
      }

      @js.native
      trait IComputedEditorOptions extends js.Object {
        //def get[T <: EditorOption](id: T): FindComputedEditorOptionValueById[T] = js.native
      }

      @js.native
      trait IEditorOption[K1, V] extends js.Object {
        def id: K1 = js.native

        def name: String = js.native

        var defaultValue: V = js.native
      }

      @js.native
      trait IEditorCommentsOptions extends js.Object {
        var insertSpace: Boolean = js.native
      }

      @js.native
      sealed trait TextEditorCursorBlinkingStyle extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.TextEditorCursorBlinkingStyle")
      object TextEditorCursorBlinkingStyle extends js.Object {
        var Hidden: TextEditorCursorBlinkingStyle = js.native
        var Blink: TextEditorCursorBlinkingStyle  = js.native
        var Smooth: TextEditorCursorBlinkingStyle = js.native
        var Phase: TextEditorCursorBlinkingStyle  = js.native
        var Expand: TextEditorCursorBlinkingStyle = js.native
        var Solid: TextEditorCursorBlinkingStyle  = js.native

        @JSBracketAccess
        def apply(value: TextEditorCursorBlinkingStyle): String = js.native
      }

      @js.native
      sealed trait TextEditorCursorStyle extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.TextEditorCursorStyle")
      object TextEditorCursorStyle extends js.Object {
        var Line: TextEditorCursorStyle          = js.native
        var Block: TextEditorCursorStyle         = js.native
        var Underline: TextEditorCursorStyle     = js.native
        var LineThin: TextEditorCursorStyle      = js.native
        var BlockOutline: TextEditorCursorStyle  = js.native
        var UnderlineThin: TextEditorCursorStyle = js.native

        @JSBracketAccess
        def apply(value: TextEditorCursorStyle): String = js.native
      }

      @js.native
      trait IEditorFindOptions extends js.Object {
        var seedSearchStringFromSelection: Boolean = js.native
        var autoFindInSelection: String            = js.native
        var addExtraSpaceOnTop: Boolean            = js.native
      }

      @js.native
      trait IGotoLocationOptions extends js.Object {
        var multiple: GoToLocationValues                = js.native
        var multipleDefinitions: GoToLocationValues     = js.native
        var multipleTypeDefinitions: GoToLocationValues = js.native
        var multipleDeclarations: GoToLocationValues    = js.native
        var multipleImplementations: GoToLocationValues = js.native
        var multipleReferences: GoToLocationValues      = js.native
        var alternativeDefinitionCommand: String        = js.native
        var alternativeTypeDefinitionCommand: String    = js.native
        var alternativeDeclarationCommand: String       = js.native
        var alternativeImplementationCommand: String    = js.native
        var alternativeReferenceCommand: String         = js.native
      }

      @js.native
      trait IEditorHoverOptions extends js.Object {
        var enabled: Boolean = js.native
        var delay: Double    = js.native
        var sticky: Boolean  = js.native
      }

      @js.native
      trait OverviewRulerPosition extends js.Object {
        def width: Double = js.native

        def height: Double = js.native

        def top: Double = js.native

        def right: Double = js.native
      }

      @js.native
      sealed trait RenderMinimap extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.RenderMinimap")
      object RenderMinimap extends js.Object {
        var None: RenderMinimap   = js.native
        var Text: RenderMinimap   = js.native
        var Blocks: RenderMinimap = js.native

        @JSBracketAccess
        def apply(value: RenderMinimap): String = js.native
      }

      @js.native
      trait EditorLayoutInfo extends js.Object {
        def width: Double = js.native

        def height: Double = js.native

        def glyphMarginLeft: Double = js.native

        def glyphMarginWidth: Double = js.native

        def lineNumbersLeft: Double = js.native

        def lineNumbersWidth: Double = js.native

        def decorationsLeft: Double = js.native

        def decorationsWidth: Double = js.native

        def contentLeft: Double = js.native

        def contentWidth: Double = js.native

        def minimapLeft: Double = js.native

        def minimapWidth: Double = js.native

        def renderMinimap: RenderMinimap = js.native

        def viewportColumn: Double = js.native

        def verticalScrollbarWidth: Double = js.native

        def horizontalScrollbarHeight: Double = js.native

        def overviewRuler: OverviewRulerPosition = js.native
      }

      @js.native
      trait IEditorLightbulbOptions extends js.Object {
        var enabled: Boolean = js.native
      }

      @js.native
      trait IEditorMinimapOptions extends js.Object {
        var enabled: Boolean          = js.native
        var side: String              = js.native
        var showSlider: String        = js.native
        var renderCharacters: Boolean = js.native
        var maxColumn: Double         = js.native
        var scale: Double             = js.native
      }

      @js.native
      trait IEditorParameterHintOptions extends js.Object {
        var enabled: Boolean = js.native
        var cycle: Boolean   = js.native
      }

      @js.native
      trait IQuickSuggestionsOptions extends js.Object {
        var other: Boolean    = js.native
        var comments: Boolean = js.native
        var strings: Boolean  = js.native
      }

      @js.native
      sealed trait RenderLineNumbersType extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.RenderLineNumbersType")
      object RenderLineNumbersType extends js.Object {
        var Off: RenderLineNumbersType      = js.native
        var On: RenderLineNumbersType       = js.native
        var Relative: RenderLineNumbersType = js.native
        var Interval: RenderLineNumbersType = js.native
        var Custom: RenderLineNumbersType   = js.native

        @JSBracketAccess
        def apply(value: RenderLineNumbersType): String = js.native
      }

      @js.native
      trait InternalEditorRenderLineNumbersOptions extends js.Object {
        def renderType: RenderLineNumbersType = js.native

        def renderFn: js.Function1[Double, String] | Null = js.native
      }

      @js.native
      trait IEditorScrollbarOptions extends js.Object {
        var arrowSize: Double                = js.native
        var vertical: String                 = js.native
        var horizontal: String               = js.native
        var useShadows: Boolean              = js.native
        var verticalHasArrows: Boolean       = js.native
        var horizontalHasArrows: Boolean     = js.native
        var handleMouseWheel: Boolean        = js.native
        var alwaysConsumeMouseWheel: Boolean = js.native
        var horizontalScrollbarSize: Double  = js.native
        var verticalScrollbarSize: Double    = js.native
        var verticalSliderSize: Double       = js.native
        var horizontalSliderSize: Double     = js.native
      }

      @js.native
      trait InternalEditorScrollbarOptions extends js.Object {
        def arrowSize: Double = js.native

        def vertical: ScrollbarVisibility = js.native

        def horizontal: ScrollbarVisibility = js.native

        def useShadows: Boolean = js.native

        def verticalHasArrows: Boolean = js.native

        def horizontalHasArrows: Boolean = js.native

        def handleMouseWheel: Boolean = js.native

        def alwaysConsumeMouseWheel: Boolean = js.native

        def horizontalScrollbarSize: Double = js.native

        def horizontalSliderSize: Double = js.native

        def verticalScrollbarSize: Double = js.native

        def verticalSliderSize: Double = js.native
      }

      @js.native
      trait ISuggestOptions extends js.Object {
        var insertMode: String                       = js.native
        var insertHighlight: Boolean                 = js.native
        var filterGraceful: Boolean                  = js.native
        var snippetsPreventQuickSuggestions: Boolean = js.native
        var localityBonus: Boolean                   = js.native
        var shareSuggestSelections: Boolean          = js.native
        var showIcons: Boolean                       = js.native
        var maxVisibleSuggestions: Double            = js.native
        var showMethods: Boolean                     = js.native
        var showFunctions: Boolean                   = js.native
        var showConstructors: Boolean                = js.native
        var showFields: Boolean                      = js.native
        var showVariables: Boolean                   = js.native
        var showClasses: Boolean                     = js.native
        var showStructs: Boolean                     = js.native
        var showInterfaces: Boolean                  = js.native
        var showModules: Boolean                     = js.native
        var showProperties: Boolean                  = js.native
        var showEvents: Boolean                      = js.native
        var showOperators: Boolean                   = js.native
        var showUnits: Boolean                       = js.native
        var showValues: Boolean                      = js.native
        var showConstants: Boolean                   = js.native
        var showEnums: Boolean                       = js.native
        var showEnumMembers: Boolean                 = js.native
        var showKeywords: Boolean                    = js.native
        var showWords: Boolean                       = js.native
        var showColors: Boolean                      = js.native
        var showFiles: Boolean                       = js.native
        var showReferences: Boolean                  = js.native
        var showFolders: Boolean                     = js.native
        var showTypeParameters: Boolean              = js.native
        var showSnippets: Boolean                    = js.native
        var hideStatusBar: Boolean                   = js.native
      }

      @js.native
      sealed trait WrappingIndent extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.WrappingIndent")
      object WrappingIndent extends js.Object {
        var None: WrappingIndent       = js.native
        var Same: WrappingIndent       = js.native
        var Indent: WrappingIndent     = js.native
        var DeepIndent: WrappingIndent = js.native

        @JSBracketAccess
        def apply(value: WrappingIndent): String = js.native
      }

      @js.native
      trait EditorWrappingInfo extends js.Object {
        def isDominatedByLongLines: Boolean = js.native

        def isWordWrapMinified: Boolean = js.native

        def isViewportWrapping: Boolean = js.native

        def wrappingColumn: Double = js.native
      }

      @js.native
      sealed trait EditorOption extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.EditorOption")
      object EditorOption extends js.Object {
        var acceptSuggestionOnCommitCharacter: EditorOption = js.native
        var acceptSuggestionOnEnter: EditorOption           = js.native
        var accessibilitySupport: EditorOption              = js.native
        var accessibilityPageSize: EditorOption             = js.native
        var ariaLabel: EditorOption                         = js.native
        var autoClosingBrackets: EditorOption               = js.native
        var autoClosingOvertype: EditorOption               = js.native
        var autoClosingQuotes: EditorOption                 = js.native
        var autoIndent: EditorOption                        = js.native
        var automaticLayout: EditorOption                   = js.native
        var autoSurround: EditorOption                      = js.native
        var codeLens: EditorOption                          = js.native
        var colorDecorators: EditorOption                   = js.native
        var comments: EditorOption                          = js.native
        var contextmenu: EditorOption                       = js.native
        var copyWithSyntaxHighlighting: EditorOption        = js.native
        var cursorBlinking: EditorOption                    = js.native
        var cursorSmoothCaretAnimation: EditorOption        = js.native
        var cursorStyle: EditorOption                       = js.native
        var cursorSurroundingLines: EditorOption            = js.native
        var cursorSurroundingLinesStyle: EditorOption       = js.native
        var cursorWidth: EditorOption                       = js.native
        var disableLayerHinting: EditorOption               = js.native
        var disableMonospaceOptimizations: EditorOption     = js.native
        var dragAndDrop: EditorOption                       = js.native
        var emptySelectionClipboard: EditorOption           = js.native
        var extraEditorClassName: EditorOption              = js.native
        var fastScrollSensitivity: EditorOption             = js.native
        var find: EditorOption                              = js.native
        var fixedOverflowWidgets: EditorOption              = js.native
        var folding: EditorOption                           = js.native
        var foldingStrategy: EditorOption                   = js.native
        var foldingHighlight: EditorOption                  = js.native
        var fontFamily: EditorOption                        = js.native
        var fontInfo: EditorOption                          = js.native
        var fontLigatures: EditorOption                     = js.native
        var fontSize: EditorOption                          = js.native
        var fontWeight: EditorOption                        = js.native
        var formatOnPaste: EditorOption                     = js.native
        var formatOnType: EditorOption                      = js.native
        var glyphMargin: EditorOption                       = js.native
        var gotoLocation: EditorOption                      = js.native
        var hideCursorInOverviewRuler: EditorOption         = js.native
        var highlightActiveIndentGuide: EditorOption        = js.native
        var hover: EditorOption                             = js.native
        var inDiffEditor: EditorOption                      = js.native
        var letterSpacing: EditorOption                     = js.native
        var lightbulb: EditorOption                         = js.native
        var lineDecorationsWidth: EditorOption              = js.native
        var lineHeight: EditorOption                        = js.native
        var lineNumbers: EditorOption                       = js.native
        var lineNumbersMinChars: EditorOption               = js.native
        var links: EditorOption                             = js.native
        var matchBrackets: EditorOption                     = js.native
        var minimap: EditorOption                           = js.native
        var mouseStyle: EditorOption                        = js.native
        var mouseWheelScrollSensitivity: EditorOption       = js.native
        var mouseWheelZoom: EditorOption                    = js.native
        var multiCursorMergeOverlapping: EditorOption       = js.native
        var multiCursorModifier: EditorOption               = js.native
        var multiCursorPaste: EditorOption                  = js.native
        var occurrencesHighlight: EditorOption              = js.native
        var overviewRulerBorder: EditorOption               = js.native
        var overviewRulerLanes: EditorOption                = js.native
        var parameterHints: EditorOption                    = js.native
        var peekWidgetDefaultFocus: EditorOption            = js.native
        var quickSuggestions: EditorOption                  = js.native
        var quickSuggestionsDelay: EditorOption             = js.native
        var readOnly: EditorOption                          = js.native
        var renderControlCharacters: EditorOption           = js.native
        var renderIndentGuides: EditorOption                = js.native
        var renderFinalNewline: EditorOption                = js.native
        var renderLineHighlight: EditorOption               = js.native
        var renderValidationDecorations: EditorOption       = js.native
        var renderWhitespace: EditorOption                  = js.native
        var revealHorizontalRightPadding: EditorOption      = js.native
        var roundedSelection: EditorOption                  = js.native
        var rulers: EditorOption                            = js.native
        var scrollbar: EditorOption                         = js.native
        var scrollBeyondLastColumn: EditorOption            = js.native
        var scrollBeyondLastLine: EditorOption              = js.native
        var selectionClipboard: EditorOption                = js.native
        var selectionHighlight: EditorOption                = js.native
        var selectOnLineNumbers: EditorOption               = js.native
        var showFoldingControls: EditorOption               = js.native
        var showUnused: EditorOption                        = js.native
        var snippetSuggestions: EditorOption                = js.native
        var smoothScrolling: EditorOption                   = js.native
        var stopRenderingLineAfter: EditorOption            = js.native
        var suggest: EditorOption                           = js.native
        var suggestFontSize: EditorOption                   = js.native
        var suggestLineHeight: EditorOption                 = js.native
        var suggestOnTriggerCharacters: EditorOption        = js.native
        var suggestSelection: EditorOption                  = js.native
        var tabCompletion: EditorOption                     = js.native
        var useTabStops: EditorOption                       = js.native
        var wordSeparators: EditorOption                    = js.native
        var wordWrap: EditorOption                          = js.native
        var wordWrapBreakAfterCharacters: EditorOption      = js.native
        var wordWrapBreakBeforeCharacters: EditorOption     = js.native
        var wordWrapColumn: EditorOption                    = js.native
        var wordWrapMinified: EditorOption                  = js.native
        var wrappingIndent: EditorOption                    = js.native
        var wrappingStrategy: EditorOption                  = js.native
        var editorClassName: EditorOption                   = js.native
        var pixelRatio: EditorOption                        = js.native
        var tabFocusMode: EditorOption                      = js.native
        var layoutInfo: EditorOption                        = js.native
        var wrappingInfo: EditorOption                      = js.native

        @JSBracketAccess
        def apply(value: EditorOption): String = js.native
      }

      @js.native
      @JSGlobal("monaco.editor.EditorOptions")
      object EditorOptions extends js.Object {
        var acceptSuggestionOnCommitCharacter: IEditorOption[Int, Boolean] =
          js.native
        var acceptSuggestionOnEnter: IEditorOption[Int, String]                = js.native
        var accessibilitySupport: IEditorOption[Int, AccessibilitySupport]     = js.native
        var accessibilityPageSize: IEditorOption[Int, Double]                  = js.native
        var ariaLabel: IEditorOption[Int, String]                              = js.native
        var autoClosingBrackets: IEditorOption[Int, EditorAutoClosingStrategy] = js.native
        var autoClosingOvertype: IEditorOption[Int, EditorAutoClosingOvertypeStrategy] =
          js.native
        var autoClosingQuotes: IEditorOption[Int, EditorAutoClosingStrategy]  = js.native
        var autoIndent: IEditorOption[Int, EditorAutoIndentStrategy]          = js.native
        var automaticLayout: IEditorOption[Int, Boolean]                      = js.native
        var autoSurround: IEditorOption[Int, EditorAutoSurroundStrategy]      = js.native
        var codeLens: IEditorOption[Int, Boolean]                             = js.native
        var colorDecorators: IEditorOption[Int, Boolean]                      = js.native
        var comments: IEditorOption[Int, EditorCommentsOptions]               = js.native
        var contextmenu: IEditorOption[Int, Boolean]                          = js.native
        var copyWithSyntaxHighlighting: IEditorOption[Int, Boolean]           = js.native
        var cursorBlinking: IEditorOption[Int, TextEditorCursorBlinkingStyle] = js.native
        var cursorSmoothCaretAnimation: IEditorOption[Int, Boolean]           = js.native
        var cursorStyle: IEditorOption[Int, TextEditorCursorStyle]            = js.native
        var cursorSurroundingLines: IEditorOption[Int, Double]                = js.native
        var cursorSurroundingLinesStyle: IEditorOption[Int, String]           = js.native
        var cursorWidth: IEditorOption[Int, Double]                           = js.native
        var disableLayerHinting: IEditorOption[Int, Boolean]                  = js.native
        var disableMonospaceOptimizations: IEditorOption[Int, Boolean] =
          js.native
        var dragAndDrop: IEditorOption[Int, Boolean]                                = js.native
        var emptySelectionClipboard: IEditorOption[Int, Boolean]                    = js.native
        var extraEditorClassName: IEditorOption[Int, String]                        = js.native
        var fastScrollSensitivity: IEditorOption[Int, Double]                       = js.native
        var find: IEditorOption[Int, EditorFindOptions]                             = js.native
        var fixedOverflowWidgets: IEditorOption[Int, Boolean]                       = js.native
        var folding: IEditorOption[Int, Boolean]                                    = js.native
        var foldingStrategy: IEditorOption[Int, String]                             = js.native
        var foldingHighlight: IEditorOption[Int, Boolean]                           = js.native
        var fontFamily: IEditorOption[Int, String]                                  = js.native
        var fontInfo: IEditorOption[Int, FontInfo]                                  = js.native
        var fontLigatures2: IEditorOption[Int, String]                              = js.native
        var fontSize: IEditorOption[Int, Double]                                    = js.native
        var fontWeight: IEditorOption[Int, String]                                  = js.native
        var formatOnPaste: IEditorOption[Int, Boolean]                              = js.native
        var formatOnType: IEditorOption[Int, Boolean]                               = js.native
        var glyphMargin: IEditorOption[Int, Boolean]                                = js.native
        var gotoLocation: IEditorOption[Int, GoToLocationOptions]                   = js.native
        var hideCursorInOverviewRuler: IEditorOption[Int, Boolean]                  = js.native
        var highlightActiveIndentGuide: IEditorOption[Int, Boolean]                 = js.native
        var hover: IEditorOption[Int, EditorHoverOptions]                           = js.native
        var inDiffEditor: IEditorOption[Int, Boolean]                               = js.native
        var letterSpacing: IEditorOption[Int, Double]                               = js.native
        var lightbulb: IEditorOption[Int, EditorLightbulbOptions]                   = js.native
        var lineDecorationsWidth: IEditorOption[Int, String | Double]               = js.native
        var lineHeight: IEditorOption[Int, Double]                                  = js.native
        var lineNumbers: IEditorOption[Int, InternalEditorRenderLineNumbersOptions] = js.native
        var lineNumbersMinChars: IEditorOption[Int, Double]                         = js.native
        var links: IEditorOption[Int, Boolean]                                      = js.native
        var matchBrackets: IEditorOption[Int, String]                               = js.native
        var minimap: IEditorOption[Int, EditorMinimapOptions]                       = js.native
        var mouseStyle: IEditorOption[Int, String]                                  = js.native
        var mouseWheelScrollSensitivity: IEditorOption[Int, Double]                 = js.native
        var mouseWheelZoom: IEditorOption[Int, Boolean]                             = js.native
        var multiCursorMergeOverlapping: IEditorOption[Int, Boolean]                = js.native
        var multiCursorModifier: IEditorOption[Int, String]                         = js.native
        var multiCursorPaste: IEditorOption[Int, String]                            = js.native
        var occurrencesHighlight: IEditorOption[Int, Boolean]                       = js.native
        var overviewRulerBorder: IEditorOption[Int, Boolean]                        = js.native
        var overviewRulerLanes: IEditorOption[Int, Double]                          = js.native
        var parameterHints: IEditorOption[Int, InternalParameterHintOptions]        = js.native
        var peekWidgetDefaultFocus: IEditorOption[Int, String]                      = js.native
        var quickSuggestions: IEditorOption[Int, ValidQuickSuggestionsOptions]      = js.native
        var quickSuggestionsDelay: IEditorOption[Int, Double]                       = js.native
        var readOnly: IEditorOption[Int, Boolean]                                   = js.native
        var renderControlCharacters: IEditorOption[Int, Boolean]                    = js.native
        var renderIndentGuides: IEditorOption[Int, Boolean]                         = js.native
        var renderFinalNewline: IEditorOption[Int, Boolean]                         = js.native
        var renderLineHighlight: IEditorOption[Int, String]                         = js.native
        var renderValidationDecorations: IEditorOption[Int, String]                 = js.native
        var renderWhitespace: IEditorOption[Int, String]                            = js.native
        var revealHorizontalRightPadding: IEditorOption[Int, Double]                = js.native
        var roundedSelection: IEditorOption[Int, Boolean]                           = js.native
        var rulers: IEditorOption[Int, js.Any]                                      = js.native
        var scrollbar: IEditorOption[Int, InternalEditorScrollbarOptions]           = js.native
        var scrollBeyondLastColumn: IEditorOption[Int, Double]                      = js.native
        var scrollBeyondLastLine: IEditorOption[Int, Boolean]                       = js.native
        var selectionClipboard: IEditorOption[Int, Boolean]                         = js.native
        var selectionHighlight: IEditorOption[Int, Boolean]                         = js.native
        var selectOnLineNumbers: IEditorOption[Int, Boolean]                        = js.native
        var showFoldingControls: IEditorOption[Int, String]                         = js.native
        var showUnused: IEditorOption[Int, Boolean]                                 = js.native
        var snippetSuggestions: IEditorOption[Int, String]                          = js.native
        var smoothScrolling: IEditorOption[Int, Boolean]                            = js.native
        var stopRenderingLineAfter: IEditorOption[Int, Double]                      = js.native
        var suggest: IEditorOption[Int, InternalSuggestOptions]                     = js.native
        var suggestFontSize: IEditorOption[Int, Double]                             = js.native
        var suggestLineHeight: IEditorOption[Int, Double]                           = js.native
        var suggestOnTriggerCharacters: IEditorOption[Int, Boolean]                 = js.native
        var suggestSelection: IEditorOption[Int, String]                            = js.native
        var tabCompletion: IEditorOption[Int, String]                               = js.native
        var useTabStops: IEditorOption[Int, Boolean]                                = js.native
        var wordSeparators: IEditorOption[Int, String]                              = js.native
        var wordWrap: IEditorOption[Int, String]                                    = js.native
        var wordWrapBreakAfterCharacters: IEditorOption[Int, String]                = js.native
        var wordWrapBreakBeforeCharacters: IEditorOption[Int, String]               = js.native
        var wordWrapColumn: IEditorOption[Int, Double]                              = js.native
        var wordWrapMinified: IEditorOption[Int, Boolean]                           = js.native
        var wrappingIndent: IEditorOption[Int, WrappingIndent]                      = js.native
        var wrappingStrategy: IEditorOption[Int, String]                            = js.native
        var editorClassName: IEditorOption[Int, String]                             = js.native
        var pixelRatio: IEditorOption[Int, Double]                                  = js.native
        var tabFocusMode: IEditorOption[Int, Boolean]                               = js.native
        var layoutInfo: IEditorOption[Int, EditorLayoutInfo]                        = js.native
        var wrappingInfo: IEditorOption[Int, EditorWrappingInfo]                    = js.native
      }

      @js.native
      trait IViewZone extends js.Object {
        var afterLineNumber: Double                      = js.native
        var afterColumn: Double                          = js.native
        var suppressMouseDown: Boolean                   = js.native
        var heightInLines: Double                        = js.native
        var heightInPx: Double                           = js.native
        var minWidthInPx: Double                         = js.native
        var domNode: HTMLElement                         = js.native
        var marginDomNode: HTMLElement | Null            = js.native
        var onDomNodeTop: js.Function1[Double, Unit]     = js.native
        var onComputedHeight: js.Function1[Double, Unit] = js.native
      }

      @js.native
      trait IViewZoneChangeAccessor extends js.Object {
        def addZone(zone: IViewZone): String = js.native

        def removeZone(id: String): Unit = js.native

        def layoutZone(id: String): Unit = js.native
      }

      @js.native
      sealed trait ContentWidgetPositionPreference extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.ContentWidgetPositionPreference")
      object ContentWidgetPositionPreference extends js.Object {
        var EXACT: ContentWidgetPositionPreference = js.native
        var ABOVE: ContentWidgetPositionPreference = js.native
        var BELOW: ContentWidgetPositionPreference = js.native

        @JSBracketAccess
        def apply(value: ContentWidgetPositionPreference): String = js.native
      }

      @js.native
      trait IContentWidgetPosition extends js.Object {
        var position: IPosition | Null                            = js.native
        var range: IRange | Null                                  = js.native
        var preference: js.Array[ContentWidgetPositionPreference] = js.native
      }

      @js.native
      trait IContentWidget extends js.Object {
        var allowEditorOverflow: Boolean = js.native
        var suppressMouseDown: Boolean   = js.native

        def getId(): String = js.native

        def getDomNode(): HTMLElement = js.native

        def getPosition(): IContentWidgetPosition | Null = js.native
      }

      @js.native
      sealed trait OverlayWidgetPositionPreference extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.OverlayWidgetPositionPreference")
      object OverlayWidgetPositionPreference extends js.Object {
        var TOP_RIGHT_CORNER: OverlayWidgetPositionPreference    = js.native
        var BOTTOM_RIGHT_CORNER: OverlayWidgetPositionPreference = js.native
        var TOP_CENTER: OverlayWidgetPositionPreference          = js.native

        @JSBracketAccess
        def apply(value: OverlayWidgetPositionPreference): String = js.native
      }

      @js.native
      trait IOverlayWidgetPosition extends js.Object {
        var preference: OverlayWidgetPositionPreference | Null = js.native
      }

      @js.native
      trait IOverlayWidget extends js.Object {
        def getId(): String = js.native

        def getDomNode(): HTMLElement = js.native

        def getPosition(): IOverlayWidgetPosition | Null = js.native
      }

      @js.native
      sealed trait MouseTargetType extends js.Object {}

      @js.native
      @JSGlobal("monaco.editor.MouseTargetType")
      object MouseTargetType extends js.Object {
        var UNKNOWN: MouseTargetType                 = js.native
        var TEXTAREA: MouseTargetType                = js.native
        var GUTTER_GLYPH_MARGIN: MouseTargetType     = js.native
        var GUTTER_LINE_NUMBERS: MouseTargetType     = js.native
        var GUTTER_LINE_DECORATIONS: MouseTargetType = js.native
        var GUTTER_VIEW_ZONE: MouseTargetType        = js.native
        var CONTENT_TEXT: MouseTargetType            = js.native
        var CONTENT_EMPTY: MouseTargetType           = js.native
        var CONTENT_VIEW_ZONE: MouseTargetType       = js.native
        var CONTENT_WIDGET: MouseTargetType          = js.native
        var OVERVIEW_RULER: MouseTargetType          = js.native
        var SCROLLBAR: MouseTargetType               = js.native
        var OVERLAY_WIDGET: MouseTargetType          = js.native
        var OUTSIDE_EDITOR: MouseTargetType          = js.native

        @JSBracketAccess
        def apply(value: MouseTargetType): String = js.native
      }

      @js.native
      trait IMouseTarget extends js.Object {
        def element: Element | Null = js.native

        def `type`: MouseTargetType = js.native

        def position: Position | Null = js.native

        def mouseColumn: Double = js.native

        def range: Range | Null = js.native

        def detail: js.Any = js.native
      }

      @js.native
      trait IEditorMouseEvent extends js.Object {
        def event: IMouseEvent = js.native

        def target: IMouseTarget = js.native
      }

      @js.native
      trait IPartialEditorMouseEvent extends js.Object {
        def event: IMouseEvent = js.native

        def target: IMouseTarget | Null = js.native
      }

      @js.native
      trait IPasteEvent extends js.Object {
        def range: Range = js.native

        def mode: String | Null = js.native
      }

      @js.native
      trait ICodeEditor extends IEditor {
        def onDidChangeModelContent(listener: js.Function1[IModelContentChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeModelLanguage(listener: js.Function1[IModelLanguageChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeModelLanguageConfiguration(
            listener: js.Function1[IModelLanguageConfigurationChangedEvent, Unit]
        ): IDisposable = js.native

        def onDidChangeModelOptions(listener: js.Function1[IModelOptionsChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeConfiguration(listener: js.Function1[ConfigurationChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeCursorPosition(listener: js.Function1[ICursorPositionChangedEvent, Unit]): IDisposable =
          js.native

        def onDidChangeCursorSelection(listener: js.Function1[ICursorSelectionChangedEvent, Unit]): IDisposable =
          js.native

        def onDidChangeModel(listener: js.Function1[IModelChangedEvent, Unit]): IDisposable = js.native

        def onDidChangeModelDecorations(listener: js.Function1[IModelDecorationsChangedEvent, Unit]): IDisposable =
          js.native

        def onDidFocusEditorText(listener: js.Function0[Unit]): IDisposable = js.native

        def onDidBlurEditorText(listener: js.Function0[Unit]): IDisposable = js.native

        def onDidFocusEditorWidget(listener: js.Function0[Unit]): IDisposable = js.native

        def onDidBlurEditorWidget(listener: js.Function0[Unit]): IDisposable = js.native

        def onDidCompositionStart(listener: js.Function0[Unit]): IDisposable = js.native

        def onDidCompositionEnd(listener: js.Function0[Unit]): IDisposable = js.native

        def onDidPaste(listener: js.Function1[IPasteEvent, Unit]): IDisposable = js.native

        def onMouseUp(listener: js.Function1[IEditorMouseEvent, Unit]): IDisposable = js.native

        def onMouseDown(listener: js.Function1[IEditorMouseEvent, Unit]): IDisposable = js.native

        def onContextMenu(listener: js.Function1[IEditorMouseEvent, Unit]): IDisposable = js.native

        def onMouseMove(listener: js.Function1[IEditorMouseEvent, Unit]): IDisposable = js.native

        def onMouseLeave(listener: js.Function1[IPartialEditorMouseEvent, Unit]): IDisposable = js.native

        def onKeyUp(listener: js.Function1[IKeyboardEvent, Unit]): IDisposable = js.native

        def onKeyDown(listener: js.Function1[IKeyboardEvent, Unit]): IDisposable = js.native

        def onDidLayoutChange(listener: js.Function1[EditorLayoutInfo, Unit]): IDisposable = js.native

        def onDidContentSizeChange(listener: js.Function1[IContentSizeChangedEvent, Unit]): IDisposable = js.native

        def onDidScrollChange(listener: js.Function1[IScrollEvent, Unit]): IDisposable = js.native

        override def saveViewState(): IEditorViewState | Null = js.native

        def restoreViewState(state: ICodeEditorViewState): Unit = js.native

        def hasWidgetFocus(): Boolean = js.native

        def getContribution[T <: IEditorContribution](id: String): T = js.native

        override def getModel(): IEditorModel | Null = js.native

        //def setModel(model: ITextModel | Null): Unit                                                    = js.native
        def getOptions(): IComputedEditorOptions = js.native

        //def getOption[T <: EditorOption](id: T): FindComputedEditorOptionValueById[T]                   = js.native
        def getRawOptions(): IEditorOptions = js.native

        def getValue(options: js.Any = ???): String = js.native

        def setValue(newValue: String): Unit = js.native

        def getContentWidth(): Double = js.native

        def getScrollWidth(): Double = js.native

        def getScrollLeft(): Double = js.native

        def getContentHeight(): Double = js.native

        def getScrollHeight(): Double = js.native

        def getScrollTop(): Double = js.native

        def setScrollLeft(newScrollLeft: Double): Unit = js.native

        def setScrollTop(newScrollTop: Double): Unit = js.native

        def setScrollPosition(position: INewScrollPosition): Unit = js.native

        def getAction(id: String): IEditorAction = js.native

        def executeCommand(source: String, command: ICommand): Unit = js.native

        def pushUndoStop(): Boolean = js.native

        def executeEdits(
            source: String,
            edits: js.Array[IIdentifiedSingleEditOperation],
            endCursorState: ICursorStateComputer | js.Array[Selection] = ???
        ): Boolean = js.native

        def executeCommands(source: String, commands: js.Array[ICommand | Null]): Unit = js.native

        def getLineDecorations(lineNumber: Double): js.Array[IModelDecoration] | Null = js.native

        def deltaDecorations(
            oldDecorations: js.Array[String],
            newDecorations: js.Array[IModelDeltaDecoration]
        ): js.Array[String] = js.native

        def getLayoutInfo(): EditorLayoutInfo = js.native

        def getVisibleRanges(): js.Array[Range] = js.native

        def getTopForLineNumber(lineNumber: Double): Double = js.native

        def getTopForPosition(lineNumber: Double, column: Double): Double = js.native

        def getContainerDomNode(): HTMLElement = js.native

        def getDomNode(): HTMLElement | Null = js.native

        def addContentWidget(widget: IContentWidget): Unit = js.native

        def layoutContentWidget(widget: IContentWidget): Unit = js.native

        def removeContentWidget(widget: IContentWidget): Unit = js.native

        def addOverlayWidget(widget: IOverlayWidget): Unit = js.native

        def layoutOverlayWidget(widget: IOverlayWidget): Unit = js.native

        def removeOverlayWidget(widget: IOverlayWidget): Unit = js.native

        def changeViewZones(callback: js.Function1[IViewZoneChangeAccessor, Unit]): Unit = js.native

        def getOffsetForColumn(lineNumber: Double, column: Double): Double = js.native

        def render(forceRedraw: Boolean = ???): Unit = js.native

        def getTargetAtClientPoint(clientX: Double, clientY: Double): IMouseTarget | Null = js.native

        def getScrolledVisiblePosition(position: IPosition): js.Any | Null = js.native

        def applyFontInfo(target: HTMLElement): Unit = js.native
      }

      @js.native
      trait IDiffLineInformation extends js.Object {
        def equivalentLineNumber: Double = js.native
      }

      @js.native
      trait IDiffEditor extends IEditor {
        def getDomNode(): HTMLElement = js.native

        def onDidUpdateDiff(listener: js.Function0[Unit]): IDisposable = js.native

        override def saveViewState(): IEditorViewState | Null = js.native

        def restoreViewState(state: IDiffEditorViewState): Unit = js.native

        override def getModel(): IEditorModel | Null = js.native

        //def setModel(model: IDiffEditorModel | Null): Unit                                     = js.native
        def getOriginalEditor(): ICodeEditor = js.native

        def getModifiedEditor(): ICodeEditor = js.native

        def getLineChanges(): js.Array[ILineChange] | Null = js.native

        def getDiffLineInformationForOriginal(lineNumber: Double): IDiffLineInformation | Null = js.native

        def getDiffLineInformationForModified(lineNumber: Double): IDiffLineInformation | Null = js.native

        def updateOptions(newOptions: IDiffEditorOptions): Unit = js.native
      }

      @js.native
      @JSGlobal("monaco.editor.FontInfo")
      class FontInfo extends BareFontInfo {
        def _editorStylingBrand: Unit = js.native

        def isTrusted: Boolean = js.native

        def isMonospace: Boolean = js.native

        def typicalHalfwidthCharacterWidth: Double = js.native

        def typicalFullwidthCharacterWidth: Double = js.native

        def canUseHalfwidthRightwardsArrow: Boolean = js.native

        def spaceWidth: Double = js.native

        def middotWidth: Double = js.native

        def maxDigitWidth: Double = js.native
      }

      @js.native
      @JSGlobal("monaco.editor.BareFontInfo")
      class BareFontInfo extends js.Object {
        def _bareFontInfoBrand: Unit = js.native

        def zoomLevel: Double = js.native

        def fontFamily: String = js.native

        def fontWeight: String = js.native

        def fontSize: Double = js.native

        def fontFeatureSettings: String = js.native

        def lineHeight: Double = js.native

        def letterSpacing: Double = js.native
      }

      @js.native
      @JSGlobal("monaco.editor")
      object Editor extends js.Object {
        def create(
            domElement: HTMLElement,
            options: IStandaloneEditorConstructionOptions = ???,
            `override`: IEditorOverrideServices = ???
        ): IStandaloneCodeEditor = js.native

        def onDidCreateEditor(listener: js.Function1[ICodeEditor, Unit]): IDisposable = js.native

        def createDiffEditor(
            domElement: HTMLElement,
            options: IDiffEditorConstructionOptions = ???,
            `override`: IEditorOverrideServices = ???
        ): IStandaloneDiffEditor = js.native

        def createDiffNavigator(diffEditor: IStandaloneDiffEditor, opts: IDiffNavigatorOptions = ???): IDiffNavigator =
          js.native

        def createModel(value: String, language: String = ???, uri: Uri = ???): ITextModel = js.native

        def setModelLanguage(model: ITextModel, languageId: String): Unit = js.native

        def setModelMarkers(model: ITextModel, owner: String, markers: js.Array[IMarkerData]): Unit = js.native

        def getModelMarkers(filter: js.Any): js.Array[IMarker] = js.native

        def getModel(uri: Uri): ITextModel | Null = js.native

        def getModels(): js.Array[ITextModel] = js.native

        def onDidCreateModel(listener: js.Function1[ITextModel, Unit]): IDisposable = js.native

        def onWillDisposeModel(listener: js.Function1[ITextModel, Unit]): IDisposable = js.native

        def onDidChangeModelLanguage(listener: js.Function1[js.Any, Unit]): IDisposable = js.native

        def createWebWorker[T](opts: IWebWorkerOptions): MonacoWebWorker[T] = js.native

        def colorizeElement(domNode: HTMLElement, options: IColorizerElementOptions): Promise[Unit] = js.native

        def colorize(text: String, languageId: String, options: IColorizerOptions): Promise[String] = js.native

        def colorizeModelLine(model: ITextModel, lineNumber: Double, tabSize: Double = ???): String = js.native

        def tokenize(text: String, languageId: String): js.Array[js.Array[Token]] = js.native

        def defineTheme(themeName: String, themeData: IStandaloneThemeData): Unit = js.native

        def setTheme(themeName: String): Unit = js.native

        def remeasureFonts(): Unit = js.native

        type BuiltinTheme                      = String
        type IColors                           = js.Dictionary[String]
        type IEditorModel                      = ITextModel | IDiffEditorModel
        type IEditorViewState                  = ICodeEditorViewState | IDiffEditorViewState
        type EditorAutoClosingStrategy         = String
        type EditorAutoSurroundStrategy        = String
        type EditorAutoClosingOvertypeStrategy = String
        type EditorCommentsOptions             = IEditorCommentsOptions
        type EditorFindOptions                 = IEditorFindOptions
        type GoToLocationValues                = String
        type GoToLocationOptions               = IGotoLocationOptions
        type EditorHoverOptions                = IEditorHoverOptions
        type EditorLightbulbOptions            = IEditorLightbulbOptions
        type EditorMinimapOptions              = IEditorMinimapOptions
        type InternalParameterHintOptions      = IEditorParameterHintOptions
        type ValidQuickSuggestionsOptions      = Boolean | IQuickSuggestionsOptions
        type LineNumbersType                   = String | js.Function1[Double, String]
        type InternalSuggestOptions            = ISuggestOptions
        type EditorOptionsType                 = EditorOptions.type
        //type FindComputedEditorOptionValueById[T <: EditorOption] = ComputedEditorOptionValue[js.Any]
        type IReadOnlyModel = ITextModel
        type IModel         = ITextModel
      }

    }

    package languages {

      import wvlet.airframe.rx.html.widget.editor.monaco.languages.Languages.{
        IMonarchLanguageAction,
        IMonarchLanguageRule,
        ProviderResult
      }
      import wvlet.airframe.rx.html.widget.editor.monaco.Monaco.Thenable
      import wvlet.airframe.rx.html.widget.editor.monaco.languages.Languages.{
        CharacterPair,
        Definition,
        IMonarchLanguageAction,
        IMonarchLanguageRule,
        ProviderResult,
        TextEdit
      }

      import scala.scalajs.js.RegExp

      @js.native
      trait IToken extends js.Object {
        var startIndex: Double = js.native
        var scopes: String     = js.native
      }

      @js.native
      trait ILineTokens extends js.Object {
        var tokens: js.Array[IToken] = js.native
        var endState: IState         = js.native
      }

      @js.native
      trait IEncodedLineTokens extends js.Object {
        var tokens: js.typedarray.Uint32Array = js.native
        var endState: IState                  = js.native
      }

      @js.native
      trait TokensProvider extends js.Object {
        def getInitialState(): IState = js.native

        def tokenize(line: String, state: IState): ILineTokens = js.native
      }

      @js.native
      trait EncodedTokensProvider extends js.Object {
        def getInitialState(): IState = js.native

        def tokenizeEncoded(line: String, state: IState): IEncodedLineTokens = js.native
      }

      @js.native
      trait CodeActionContext extends js.Object {
        def markers: js.Array[editor.IMarkerData] = js.native

        def only: String = js.native
      }

      @js.native
      trait CodeActionProvider extends js.Object {
        def provideCodeActions(
            model: editor.ITextModel,
            range: Range,
            context: CodeActionContext,
            token: CancellationToken
        ): ProviderResult[CodeActionList] = js.native
      }

      @js.native
      trait CommentRule extends js.Object {
        var lineComment: String | Null         = js.native
        var blockComment: CharacterPair | Null = js.native
      }

      @js.native
      trait LanguageConfiguration extends js.Object {
        var comments: CommentRule                                                        = js.native
        var brackets: js.Array[CharacterPair]                                            = js.native
        var wordPattern: RegExp                                                          = js.native
        var indentationRules: IndentationRule                                            = js.native
        var onEnterRules: js.Array[OnEnterRule]                                          = js.native
        var autoClosingPairs: js.Array[IAutoClosingPairConditional]                      = js.native
        var surroundingPairs: js.Array[IAutoClosingPair]                                 = js.native
        var autoCloseBefore: String                                                      = js.native
        var folding: FoldingRules                                                        = js.native
        var __electricCharacterSupport: LanguageConfiguration.__electricCharacterSupport = js.native
      }

      object LanguageConfiguration {

        @js.native
        trait __electricCharacterSupport extends js.Object {
          var docComment: IDocComment = js.native
        }

      }

      @js.native
      trait IndentationRule extends js.Object {
        var decreaseIndentPattern: RegExp        = js.native
        var increaseIndentPattern: RegExp        = js.native
        var indentNextLinePattern: RegExp | Null = js.native
        var unIndentedLinePattern: RegExp | Null = js.native
      }

      @js.native
      trait FoldingMarkers extends js.Object {
        var start: RegExp = js.native
        var end: RegExp   = js.native
      }

      @js.native
      trait FoldingRules extends js.Object {
        var offSide: Boolean        = js.native
        var markers: FoldingMarkers = js.native
      }

      @js.native
      trait OnEnterRule extends js.Object {
        var beforeText: RegExp       = js.native
        var afterText: RegExp        = js.native
        var oneLineAboveText: RegExp = js.native
        var action: EnterAction      = js.native
      }

      @js.native
      trait IDocComment extends js.Object {
        var open: String  = js.native
        var close: String = js.native
      }

      @js.native
      trait IAutoClosingPair extends js.Object {
        var open: String  = js.native
        var close: String = js.native
      }

      @js.native
      trait IAutoClosingPairConditional extends IAutoClosingPair {
        var notIn: js.Array[String] = js.native
      }

      @js.native
      sealed trait IndentAction extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.IndentAction")
      object IndentAction extends js.Object {
        var None: IndentAction          = js.native
        var Indent: IndentAction        = js.native
        var IndentOutdent: IndentAction = js.native
        var Outdent: IndentAction       = js.native

        @JSBracketAccess
        def apply(value: IndentAction): String = js.native
      }

      @js.native
      trait EnterAction extends js.Object {
        var indentAction: IndentAction = js.native
        var appendText: String         = js.native
        var removeText: Double         = js.native
      }

      @js.native
      trait IState extends js.Object {
        override def clone(): IState = js.native

        def equals(other: IState): Boolean = js.native
      }

      @js.native
      trait Hover extends js.Object {
        var contents: js.Array[IMarkdownString] = js.native
        var range: IRange                       = js.native
      }

      @js.native
      trait HoverProvider extends js.Object {
        def provideHover(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken
        ): ProviderResult[Hover] = js.native
      }

      @js.native
      sealed trait CompletionItemKind extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.CompletionItemKind")
      object CompletionItemKind extends js.Object {
        var Method: CompletionItemKind        = js.native
        var Function: CompletionItemKind      = js.native
        var Constructor: CompletionItemKind   = js.native
        var Field: CompletionItemKind         = js.native
        var Variable: CompletionItemKind      = js.native
        var Class: CompletionItemKind         = js.native
        var Struct: CompletionItemKind        = js.native
        var Interface: CompletionItemKind     = js.native
        var Module: CompletionItemKind        = js.native
        var Property: CompletionItemKind      = js.native
        var Event: CompletionItemKind         = js.native
        var Operator: CompletionItemKind      = js.native
        var Unit: CompletionItemKind          = js.native
        var Value: CompletionItemKind         = js.native
        var Constant: CompletionItemKind      = js.native
        var Enum: CompletionItemKind          = js.native
        var EnumMember: CompletionItemKind    = js.native
        var Keyword: CompletionItemKind       = js.native
        var Text: CompletionItemKind          = js.native
        var Color: CompletionItemKind         = js.native
        var File: CompletionItemKind          = js.native
        var Reference: CompletionItemKind     = js.native
        var Customcolor: CompletionItemKind   = js.native
        var Folder: CompletionItemKind        = js.native
        var TypeParameter: CompletionItemKind = js.native
        var Snippet: CompletionItemKind       = js.native

        @JSBracketAccess
        def apply(value: CompletionItemKind): String = js.native
      }

      @js.native
      trait CompletionItemLabel extends js.Object {
        var name: String      = js.native
        var signature: String = js.native
        var qualifier: String = js.native
        var `type`: String    = js.native
      }

      @js.native
      sealed trait CompletionItemTag extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.CompletionItemTag")
      object CompletionItemTag extends js.Object {
        var Deprecated: CompletionItemTag = js.native

        @JSBracketAccess
        def apply(value: CompletionItemTag): String = js.native
      }

      @js.native
      sealed trait CompletionItemInsertTextRule extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.CompletionItemInsertTextRule")
      object CompletionItemInsertTextRule extends js.Object {
        var KeepWhitespace: CompletionItemInsertTextRule  = js.native
        var InsertAsSnippet: CompletionItemInsertTextRule = js.native

        @JSBracketAccess
        def apply(value: CompletionItemInsertTextRule): String = js.native
      }

      @js.native
      trait CompletionItem extends js.Object {
        var label: String | CompletionItemLabel                        = js.native
        var kind: CompletionItemKind                                   = js.native
        var tags: js.Array[_ <: CompletionItemTag]                     = js.native
        var detail: String                                             = js.native
        var documentation: String | IMarkdownString                    = js.native
        var sortText: String                                           = js.native
        var filterText: String                                         = js.native
        var preselect: Boolean                                         = js.native
        var insertText: String                                         = js.native
        var insertTextRules: CompletionItemInsertTextRule              = js.native
        var range: IRange | js.Any                                     = js.native
        var commitCharacters: js.Array[String]                         = js.native
        var additionalTextEdits: js.Array[editor.ISingleEditOperation] = js.native
        var command: Command                                           = js.native
      }

      @js.native
      trait CompletionList extends js.Object {
        var suggestions: js.Array[CompletionItem] = js.native
        var incomplete: Boolean                   = js.native

        def dispose(): Unit = js.native
      }

      @js.native
      sealed trait CompletionTriggerKind extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.CompletionTriggerKind")
      object CompletionTriggerKind extends js.Object {
        var Invoke: CompletionTriggerKind                          = js.native
        var TriggerCharacter: CompletionTriggerKind                = js.native
        var TriggerForIncompleteCompletions: CompletionTriggerKind = js.native

        @JSBracketAccess
        def apply(value: CompletionTriggerKind): String = js.native
      }

      @js.native
      trait CompletionContext extends js.Object {
        var triggerKind: CompletionTriggerKind = js.native
        var triggerCharacter: String           = js.native
      }

      @js.native
      trait CompletionItemProvider extends js.Object {
        var triggerCharacters: js.Array[String] = js.native

        def provideCompletionItems(
            model: editor.ITextModel,
            position: Position,
            context: CompletionContext,
            token: CancellationToken
        ): ProviderResult[CompletionList] = js.native

        def resolveCompletionItem(
            model: editor.ITextModel,
            position: Position,
            item: CompletionItem,
            token: CancellationToken
        ): ProviderResult[CompletionItem] = js.native
      }

      @js.native
      trait CodeAction extends js.Object {
        var title: String                             = js.native
        var command: Command                          = js.native
        var edit: WorkspaceEdit                       = js.native
        var diagnostics: js.Array[editor.IMarkerData] = js.native
        var kind: String                              = js.native
        var isPreferred: Boolean                      = js.native
        var disabled: String                          = js.native
      }

      @js.native
      trait CodeActionList extends IDisposable {
        def actions: js.Array[_ <: CodeAction] = js.native
      }

      @js.native
      trait ParameterInformation extends js.Object {
        var label: String | js.Tuple2[Double, Double] = js.native
        var documentation: String | IMarkdownString   = js.native
      }

      @js.native
      trait SignatureInformation extends js.Object {
        var label: String                              = js.native
        var documentation: String | IMarkdownString    = js.native
        var parameters: js.Array[ParameterInformation] = js.native
      }

      @js.native
      trait SignatureHelp extends js.Object {
        var signatures: js.Array[SignatureInformation] = js.native
        var activeSignature: Double                    = js.native
        var activeParameter: Double                    = js.native
      }

      @js.native
      trait SignatureHelpResult extends IDisposable {
        var value: SignatureHelp = js.native
      }

      @js.native
      sealed trait SignatureHelpTriggerKind extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.SignatureHelpTriggerKind")
      object SignatureHelpTriggerKind extends js.Object {
        var Invoke: SignatureHelpTriggerKind           = js.native
        var TriggerCharacter: SignatureHelpTriggerKind = js.native
        var ContentChange: SignatureHelpTriggerKind    = js.native

        @JSBracketAccess
        def apply(value: SignatureHelpTriggerKind): String = js.native
      }

      @js.native
      trait SignatureHelpContext extends js.Object {
        def triggerKind: SignatureHelpTriggerKind = js.native

        def triggerCharacter: String = js.native

        def isRetrigger: Boolean = js.native

        def activeSignatureHelp: SignatureHelp = js.native
      }

      @js.native
      trait SignatureHelpProvider extends js.Object {
        def signatureHelpTriggerCharacters: js.Array[_ <: String] = js.native

        def signatureHelpRetriggerCharacters: js.Array[_ <: String] = js.native

        def provideSignatureHelp(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken,
            context: SignatureHelpContext
        ): ProviderResult[SignatureHelpResult] = js.native
      }

      @js.native
      sealed trait DocumentHighlightKind extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.DocumentHighlightKind")
      object DocumentHighlightKind extends js.Object {
        var Text: DocumentHighlightKind  = js.native
        var Read: DocumentHighlightKind  = js.native
        var Write: DocumentHighlightKind = js.native

        @JSBracketAccess
        def apply(value: DocumentHighlightKind): String = js.native
      }

      @js.native
      trait DocumentHighlight extends js.Object {
        var range: IRange               = js.native
        var kind: DocumentHighlightKind = js.native
      }

      @js.native
      trait DocumentHighlightProvider extends js.Object {
        def provideDocumentHighlights(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken
        ): ProviderResult[js.Array[DocumentHighlight]] = js.native
      }

      @js.native
      trait ReferenceContext extends js.Object {
        var includeDeclaration: Boolean = js.native
      }

      @js.native
      trait ReferenceProvider extends js.Object {
        def provideReferences(
            model: editor.ITextModel,
            position: Position,
            context: ReferenceContext,
            token: CancellationToken
        ): ProviderResult[js.Array[Location]] = js.native
      }

      @js.native
      trait Location extends js.Object {
        var uri: Uri      = js.native
        var range: IRange = js.native
      }

      @js.native
      trait LocationLink extends js.Object {
        var originSelectionRange: IRange = js.native
        var uri: Uri                     = js.native
        var range: IRange                = js.native
        var targetSelectionRange: IRange = js.native
      }

      @js.native
      trait DefinitionProvider extends js.Object {
        def provideDefinition(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken
        ): ProviderResult[Definition | js.Array[LocationLink]] = js.native
      }

      @js.native
      trait DeclarationProvider extends js.Object {
        def provideDeclaration(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken
        ): ProviderResult[Definition | js.Array[LocationLink]] =
          js.native
      }

      @js.native
      trait ImplementationProvider extends js.Object {
        def provideImplementation(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken
        ): ProviderResult[Definition | js.Array[LocationLink]] =
          js.native
      }

      @js.native
      trait TypeDefinitionProvider extends js.Object {
        def provideTypeDefinition(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken
        ): ProviderResult[Definition | js.Array[LocationLink]] =
          js.native
      }

      @js.native
      sealed trait SymbolKind extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.SymbolKind")
      object SymbolKind extends js.Object {
        var File: SymbolKind          = js.native
        var Module: SymbolKind        = js.native
        var Namespace: SymbolKind     = js.native
        var Package: SymbolKind       = js.native
        var Class: SymbolKind         = js.native
        var Method: SymbolKind        = js.native
        var Property: SymbolKind      = js.native
        var Field: SymbolKind         = js.native
        var Constructor: SymbolKind   = js.native
        var Enum: SymbolKind          = js.native
        var Interface: SymbolKind     = js.native
        var Function: SymbolKind      = js.native
        var Variable: SymbolKind      = js.native
        var Constant: SymbolKind      = js.native
        var String: SymbolKind        = js.native
        var Number: SymbolKind        = js.native
        var Boolean: SymbolKind       = js.native
        var Array: SymbolKind         = js.native
        var Object: SymbolKind        = js.native
        var Key: SymbolKind           = js.native
        var Null: SymbolKind          = js.native
        var EnumMember: SymbolKind    = js.native
        var Struct: SymbolKind        = js.native
        var Event: SymbolKind         = js.native
        var Operator: SymbolKind      = js.native
        var TypeParameter: SymbolKind = js.native

        @JSBracketAccess
        def apply(value: SymbolKind): String = js.native
      }

      @js.native
      sealed trait SymbolTag extends js.Object {}

      @js.native
      @JSGlobal("monaco.languages.SymbolTag")
      object SymbolTag extends js.Object {
        var Deprecated: SymbolTag = js.native

        @JSBracketAccess
        def apply(value: SymbolTag): String = js.native
      }

      @js.native
      trait DocumentSymbol extends js.Object {
        var name: String                       = js.native
        var detail: String                     = js.native
        var kind: SymbolKind                   = js.native
        var tags: js.Array[_ <: SymbolTag]     = js.native
        var containerName: String              = js.native
        var range: IRange                      = js.native
        var selectionRange: IRange             = js.native
        var children: js.Array[DocumentSymbol] = js.native
      }

      @js.native
      trait DocumentSymbolProvider extends js.Object {
        var displayName: String = js.native

        def provideDocumentSymbols(
            model: editor.ITextModel,
            token: CancellationToken
        ): ProviderResult[js.Array[DocumentSymbol]] = js.native
      }

      @js.native
      trait FormattingOptions extends js.Object {
        var tabSize: Double       = js.native
        var insertSpaces: Boolean = js.native
      }

      @js.native
      trait DocumentFormattingEditProvider extends js.Object {
        def displayName: String = js.native

        def provideDocumentFormattingEdits(
            model: editor.ITextModel,
            options: FormattingOptions,
            token: CancellationToken
        ): ProviderResult[js.Array[TextEdit]] = js.native
      }

      @js.native
      trait DocumentRangeFormattingEditProvider extends js.Object {
        def displayName: String = js.native

        def provideDocumentRangeFormattingEdits(
            model: editor.ITextModel,
            range: Range,
            options: FormattingOptions,
            token: CancellationToken
        ): ProviderResult[js.Array[TextEdit]] =
          js.native
      }

      @js.native
      trait OnTypeFormattingEditProvider extends js.Object {
        var autoFormatTriggerCharacters: js.Array[String] = js.native

        def provideOnTypeFormattingEdits(
            model: editor.ITextModel,
            position: Position,
            ch: String,
            options: FormattingOptions,
            token: CancellationToken
        ): ProviderResult[js.Array[TextEdit]] = js.native
      }

      @js.native
      trait ILink extends js.Object {
        var range: IRange     = js.native
        var url: Uri | String = js.native
        var tooltip: String   = js.native
      }

      @js.native
      trait ILinksList extends js.Object {
        var links: js.Array[ILink] = js.native

        def dispose(): Unit = js.native
      }

      @js.native
      trait LinkProvider extends js.Object {
        def provideLinks(model: editor.ITextModel, token: CancellationToken): ProviderResult[ILinksList] = js.native

        var resolveLink: js.Function2[ILink, CancellationToken, ProviderResult[ILink]] = js.native
      }

      @js.native
      trait IColor extends js.Object {
        def red: Double = js.native

        def green: Double = js.native

        def blue: Double = js.native

        def alpha: Double = js.native
      }

      @js.native
      trait IColorPresentation extends js.Object {
        var label: String                           = js.native
        var textEdit: TextEdit                      = js.native
        var additionalTextEdits: js.Array[TextEdit] = js.native
      }

      @js.native
      trait IColorInformation extends js.Object {
        var range: IRange = js.native
        var color: IColor = js.native
      }

      @js.native
      trait DocumentColorProvider extends js.Object {
        def provideDocumentColors(
            model: editor.ITextModel,
            token: CancellationToken
        ): ProviderResult[js.Array[IColorInformation]] = js.native

        def provideColorPresentations(
            model: editor.ITextModel,
            colorInfo: IColorInformation,
            token: CancellationToken
        ): ProviderResult[js.Array[IColorPresentation]] =
          js.native
      }

      @js.native
      trait SelectionRange extends js.Object {
        var range: IRange = js.native
      }

      @js.native
      trait SelectionRangeProvider extends js.Object {
        def provideSelectionRanges(
            model: editor.ITextModel,
            positions: js.Array[Position],
            token: CancellationToken
        ): ProviderResult[js.Array[js.Array[SelectionRange]]] =
          js.native
      }

      @js.native
      trait FoldingContext extends js.Object {}

      @js.native
      trait FoldingRangeProvider extends js.Object {
        def provideFoldingRanges(
            model: editor.ITextModel,
            context: FoldingContext,
            token: CancellationToken
        ): ProviderResult[js.Array[FoldingRange]] = js.native
      }

      @js.native
      trait FoldingRange extends js.Object {
        var start: Double          = js.native
        var end: Double            = js.native
        var kind: FoldingRangeKind = js.native
      }

      @js.native
      @JSGlobal("monaco.languages.FoldingRangeKind")
      class FoldingRangeKind protected () extends js.Object {
        def this(value: String) = this()

        var value: String = js.native
      }

      @js.native
      @JSGlobal("monaco.languages.FoldingRangeKind")
      object FoldingRangeKind extends js.Object {
        def Comment: FoldingRangeKind = js.native

        def Imports: FoldingRangeKind = js.native

        def Region: FoldingRangeKind = js.native
      }

      @js.native
      trait WorkspaceEditMetadata extends js.Object {
        var needsConfirmation: Boolean = js.native
        var label: String              = js.native
        var description: String        = js.native
        var iconPath: js.Any           = js.native
      }

      @js.native
      trait WorkspaceFileEditOptions extends js.Object {
        var overwrite: Boolean         = js.native
        var ignoreIfNotExists: Boolean = js.native
        var ignoreIfExists: Boolean    = js.native
        var recursive: Boolean         = js.native
      }

      @js.native
      trait WorkspaceFileEdit extends js.Object {
        var oldUri: Uri                       = js.native
        var newUri: Uri                       = js.native
        var options: WorkspaceFileEditOptions = js.native
        var metadata: WorkspaceEditMetadata   = js.native
      }

      @js.native
      trait WorkspaceTextEdit extends js.Object {
        var resource: Uri                   = js.native
        var edit: TextEdit                  = js.native
        var modelVersionId: Double          = js.native
        var metadata: WorkspaceEditMetadata = js.native
      }

      @js.native
      trait WorkspaceEdit extends js.Object {
        var edits: js.Array[WorkspaceTextEdit | WorkspaceFileEdit] = js.native
      }

      @js.native
      trait Rejection extends js.Object {
        var rejectReason: String = js.native
      }

      @js.native
      trait RenameLocation extends js.Object {
        var range: IRange = js.native
        var text: String  = js.native
      }

      @js.native
      trait RenameProvider extends js.Object {
        def provideRenameEdits(
            model: editor.ITextModel,
            position: Position,
            newName: String,
            token: CancellationToken
        ): ProviderResult[WorkspaceEdit with Rejection] = js.native

        def resolveRenameLocation(
            model: editor.ITextModel,
            position: Position,
            token: CancellationToken
        ): ProviderResult[RenameLocation with Rejection] = js.native
      }

      @js.native
      trait Command extends js.Object {
        var id: String                  = js.native
        var title: String               = js.native
        var tooltip: String             = js.native
        var arguments: js.Array[js.Any] = js.native
      }

      @js.native
      trait CodeLens extends js.Object {
        var range: IRange    = js.native
        var id: String       = js.native
        var command: Command = js.native
      }

      @js.native
      trait CodeLensList extends js.Object {
        var lenses: js.Array[CodeLens] = js.native

        def dispose(): Unit = js.native
      }

      @js.native
      trait CodeLensProvider extends js.Object {
        var onDidChange: IEvent[this.type] = js.native

        def provideCodeLenses(model: editor.ITextModel, token: CancellationToken): ProviderResult[CodeLensList] =
          js.native

        def resolveCodeLens(
            model: editor.ITextModel,
            codeLens: CodeLens,
            token: CancellationToken
        ): ProviderResult[CodeLens] = js.native
      }

      @js.native
      trait SemanticTokensLegend extends js.Object {
        def tokenTypes: js.Array[String] = js.native

        def tokenModifiers: js.Array[String] = js.native
      }

      @js.native
      trait SemanticTokens extends js.Object {
        def resultId: String = js.native

        def data: js.typedarray.Uint32Array = js.native
      }

      @js.native
      trait SemanticTokensEdit extends js.Object {
        def start: Double = js.native

        def deleteCount: Double = js.native

        def data: js.typedarray.Uint32Array = js.native
      }

      @js.native
      trait SemanticTokensEdits extends js.Object {
        def resultId: String = js.native

        def edits: js.Array[SemanticTokensEdit] = js.native
      }

      @js.native
      trait DocumentSemanticTokensProvider extends js.Object {
        def getLegend(): SemanticTokensLegend = js.native

        def provideDocumentSemanticTokens(
            model: editor.ITextModel,
            lastResultId: String | Null,
            token: CancellationToken
        ): ProviderResult[SemanticTokens | SemanticTokensEdits] = js.native

        def releaseDocumentSemanticTokens(resultId: String | Unit): Unit = js.native
      }

      @js.native
      trait DocumentRangeSemanticTokensProvider extends js.Object {
        def getLegend(): SemanticTokensLegend = js.native

        def provideDocumentRangeSemanticTokens(
            model: editor.ITextModel,
            range: Range,
            token: CancellationToken
        ): ProviderResult[SemanticTokens] = js.native
      }

      @js.native
      trait ILanguageExtensionPoint extends js.Object {
        var id: String                         = js.native
        var extensions: js.Array[String]       = js.native
        var filenames: js.Array[String]        = js.native
        var filenamePatterns: js.Array[String] = js.native
        var firstLine: String                  = js.native
        var aliases: js.Array[String]          = js.native
        var mimetypes: js.Array[String]        = js.native
        var configuration: Uri                 = js.native
      }

      @js.native
      trait IMonarchLanguage extends js.Object {
        var tokenizer: IMonarchLanguage.Tokenizer       = js.native
        var ignoreCase: Boolean                         = js.native
        var defaultToken: String                        = js.native
        var brackets: js.Array[IMonarchLanguageBracket] = js.native
        var start: String                               = js.native
        var tokenPostfix: String                        = js.native
      }

      object IMonarchLanguage {

        @js.native
        trait Tokenizer extends js.Object {
          @JSBracketAccess
          def apply(name: String): js.Array[IMonarchLanguageRule] = js.native

          @JSBracketAccess
          def update(name: String, v: js.Array[IMonarchLanguageRule]): Unit = js.native
        }

      }

      @js.native
      trait IExpandedMonarchLanguageRule extends js.Object {
        var regex: String | RegExp         = js.native
        var action: IMonarchLanguageAction = js.native
        var include: String                = js.native
      }

      @js.native
      trait IExpandedMonarchLanguageAction extends js.Object {
        var group: js.Array[IMonarchLanguageAction] = js.native
        var cases: Object                           = js.native
        var token: String                           = js.native
        var next: String                            = js.native
        var switchTo: String                        = js.native
        var goBack: Double                          = js.native
        var bracket: String                         = js.native
        var nextEmbedded: String                    = js.native
        var log: String                             = js.native
      }

      @js.native
      trait IMonarchLanguageBracket extends js.Object {
        var open: String  = js.native
        var close: String = js.native
        var token: String = js.native
      }

      package typescript {

        import wvlet.airframe.rx.html.widget.editor.monaco.languages.typescript.Typescript.CompilerOptionsValue

        import scala.scalajs.js.Promise

        @js.native
        sealed trait ModuleKind extends js.Object {}

        @js.native
        @JSGlobal("monaco.languages.typescript.ModuleKind")
        object ModuleKind extends js.Object {
          var None: ModuleKind     = js.native
          var CommonJS: ModuleKind = js.native
          var AMD: ModuleKind      = js.native
          var UMD: ModuleKind      = js.native
          var System: ModuleKind   = js.native
          var ES2015: ModuleKind   = js.native
          var ESNext: ModuleKind   = js.native

          @JSBracketAccess
          def apply(value: ModuleKind): String = js.native
        }

        @js.native
        sealed trait JsxEmit extends js.Object {}

        @js.native
        @JSGlobal("monaco.languages.typescript.JsxEmit")
        object JsxEmit extends js.Object {
          var None: JsxEmit        = js.native
          var Preserve: JsxEmit    = js.native
          var React: JsxEmit       = js.native
          var ReactNative: JsxEmit = js.native

          @JSBracketAccess
          def apply(value: JsxEmit): String = js.native
        }

        @js.native
        sealed trait NewLineKind extends js.Object {}

        @js.native
        @JSGlobal("monaco.languages.typescript.NewLineKind")
        object NewLineKind extends js.Object {
          var CarriageReturnLineFeed: NewLineKind = js.native
          var LineFeed: NewLineKind               = js.native

          @JSBracketAccess
          def apply(value: NewLineKind): String = js.native
        }

        @js.native
        sealed trait ScriptTarget extends js.Object {}

        @js.native
        @JSGlobal("monaco.languages.typescript.ScriptTarget")
        object ScriptTarget extends js.Object {
          var ES3: ScriptTarget    = js.native
          var ES5: ScriptTarget    = js.native
          var ES2015: ScriptTarget = js.native
          var ES2016: ScriptTarget = js.native
          var ES2017: ScriptTarget = js.native
          var ES2018: ScriptTarget = js.native
          var ES2019: ScriptTarget = js.native
          var ES2020: ScriptTarget = js.native
          var ESNext: ScriptTarget = js.native
          var JSON: ScriptTarget   = js.native
          var Latest: ScriptTarget = js.native

          @JSBracketAccess
          def apply(value: ScriptTarget): String = js.native
        }

        @js.native
        sealed trait ModuleResolutionKind extends js.Object {}

        @js.native
        @JSGlobal("monaco.languages.typescript.ModuleResolutionKind")
        object ModuleResolutionKind extends js.Object {
          var Classic: ModuleResolutionKind = js.native
          var NodeJs: ModuleResolutionKind  = js.native

          @JSBracketAccess
          def apply(value: ModuleResolutionKind): String = js.native
        }

        @js.native
        trait MapLike[T] extends js.Object {
          @JSBracketAccess
          def apply(index: String): T = js.native

          @JSBracketAccess
          def update(index: String, v: T): Unit = js.native
        }

        @js.native
        trait CompilerOptions extends js.Object {
          var allowJs: Boolean                                 = js.native
          var allowSyntheticDefaultImports: Boolean            = js.native
          var allowUmdGlobalAccess: Boolean                    = js.native
          var allowUnreachableCode: Boolean                    = js.native
          var allowUnusedLabels: Boolean                       = js.native
          var alwaysStrict: Boolean                            = js.native
          var baseUrl: String                                  = js.native
          var charset: String                                  = js.native
          var checkJs: Boolean                                 = js.native
          var declaration: Boolean                             = js.native
          var declarationMap: Boolean                          = js.native
          var emitDeclarationOnly: Boolean                     = js.native
          var declarationDir: String                           = js.native
          var disableSizeLimit: Boolean                        = js.native
          var disableSourceOfProjectReferenceRedirect: Boolean = js.native
          var downlevelIteration: Boolean                      = js.native
          var emitBOM: Boolean                                 = js.native
          var emitDecoratorMetadata: Boolean                   = js.native
          var experimentalDecorators: Boolean                  = js.native
          var forceConsistentCasingInFileNames: Boolean        = js.native
          var importHelpers: Boolean                           = js.native
          var inlineSourceMap: Boolean                         = js.native
          var inlineSources: Boolean                           = js.native
          var isolatedModules: Boolean                         = js.native
          var jsx: JsxEmit                                     = js.native
          var keyofStringsOnly: Boolean                        = js.native
          var lib: js.Array[String]                            = js.native
          var locale: String                                   = js.native
          var mapRoot: String                                  = js.native
          var maxNodeModuleJsDepth: Double                     = js.native
          var module: ModuleKind                               = js.native
          var moduleResolution: ModuleResolutionKind           = js.native
          var newLine: NewLineKind                             = js.native
          var noEmit: Boolean                                  = js.native
          var noEmitHelpers: Boolean                           = js.native
          var noEmitOnError: Boolean                           = js.native
          var noErrorTruncation: Boolean                       = js.native
          var noFallthroughCasesInSwitch: Boolean              = js.native
          var noImplicitAny: Boolean                           = js.native
          var noImplicitReturns: Boolean                       = js.native
          var noImplicitThis: Boolean                          = js.native
          var noStrictGenericChecks: Boolean                   = js.native
          var noUnusedLocals: Boolean                          = js.native
          var noUnusedParameters: Boolean                      = js.native
          var noImplicitUseStrict: Boolean                     = js.native
          var noLib: Boolean                                   = js.native
          var noResolve: Boolean                               = js.native
          var out: String                                      = js.native
          var outDir: String                                   = js.native
          var outFile: String                                  = js.native
          var paths: MapLike[js.Array[String]]                 = js.native
          var preserveConstEnums: Boolean                      = js.native
          var preserveSymlinks: Boolean                        = js.native
          var project: String                                  = js.native
          var reactNamespace: String                           = js.native
          var jsxFactory: String                               = js.native
          var composite: Boolean                               = js.native
          var removeComments: Boolean                          = js.native
          var rootDir: String                                  = js.native
          var rootDirs: js.Array[String]                       = js.native
          var skipLibCheck: Boolean                            = js.native
          var skipDefaultLibCheck: Boolean                     = js.native
          var sourceMap: Boolean                               = js.native
          var sourceRoot: String                               = js.native
          var strict: Boolean                                  = js.native
          var strictFunctionTypes: Boolean                     = js.native
          var strictBindCallApply: Boolean                     = js.native
          var strictNullChecks: Boolean                        = js.native
          var strictPropertyInitialization: Boolean            = js.native
          var stripInternal: Boolean                           = js.native
          var suppressExcessPropertyErrors: Boolean            = js.native
          var suppressImplicitAnyIndexErrors: Boolean          = js.native
          var target: ScriptTarget                             = js.native
          var traceResolution: Boolean                         = js.native
          var resolveJsonModule: Boolean                       = js.native
          var types: js.Array[String]                          = js.native
          var typeRoots: js.Array[String]                      = js.native
          var esModuleInterop: Boolean                         = js.native
          var useDefineForClassFields: Boolean                 = js.native

          @JSBracketAccess
          def apply(option: String): CompilerOptionsValue | Unit = js.native

          @JSBracketAccess
          def update(option: String, v: CompilerOptionsValue | Unit): Unit = js.native
        }

        @js.native
        trait DiagnosticsOptions extends js.Object {
          var noSemanticValidation: Boolean             = js.native
          var noSyntaxValidation: Boolean               = js.native
          var noSuggestionDiagnostics: Boolean          = js.native
          var diagnosticCodesToIgnore: js.Array[Double] = js.native
        }

        @js.native
        trait IExtraLib extends js.Object {
          var content: String = js.native
          var version: Double = js.native
        }

        @js.native
        trait IExtraLibs extends js.Object {
          @JSBracketAccess
          def apply(path: String): IExtraLib = js.native

          @JSBracketAccess
          def update(path: String, v: IExtraLib): Unit = js.native
        }

        @js.native
        trait DiagnosticMessageChain extends js.Object {
          var messageText: String                    = js.native
          var category: Int                          = js.native
          var code: Double                           = js.native
          var next: js.Array[DiagnosticMessageChain] = js.native
        }

        @js.native
        trait Diagnostic extends DiagnosticRelatedInformation {
          var source: String                                             = js.native
          var relatedInformation: js.Array[DiagnosticRelatedInformation] = js.native
        }

        @js.native
        trait DiagnosticRelatedInformation extends js.Object {
          var category: Int                                = js.native
          var code: Double                                 = js.native
          var file: Unit                                   = js.native
          var start: Double | Unit                         = js.native
          var length: Double | Unit                        = js.native
          var messageText: String | DiagnosticMessageChain = js.native
        }

        @js.native
        trait EmitOutput extends js.Object {
          var outputFiles: js.Array[OutputFile] = js.native
          var emitSkipped: Boolean              = js.native
        }

        @js.native
        trait OutputFile extends js.Object {
          var name: String                = js.native
          var writeByteOrderMark: Boolean = js.native
          var text: String                = js.native
        }

        @js.native
        trait LanguageServiceDefaults extends js.Object {
          def onDidChange: IEvent[Unit] = js.native

          def onDidExtraLibsChange: IEvent[Unit] = js.native

          def getExtraLibs(): IExtraLibs = js.native

          def addExtraLib(content: String, filePath: String = ???): IDisposable = js.native

          def setExtraLibs(libs: js.Array[js.Any]): Unit = js.native

          def getCompilerOptions(): CompilerOptions = js.native

          def setCompilerOptions(options: CompilerOptions): Unit = js.native

          def getDiagnosticsOptions(): DiagnosticsOptions = js.native

          def setDiagnosticsOptions(options: DiagnosticsOptions): Unit = js.native

          def setMaximumWorkerIdleTime(value: Double): Unit = js.native

          def setEagerModelSync(value: Boolean): Unit = js.native

          def getEagerModelSync(): Boolean = js.native
        }

        @js.native
        trait TypeScriptWorker extends js.Object {
          def getSyntacticDiagnostics(fileName: String): Promise[js.Array[Diagnostic]] = js.native

          def getSemanticDiagnostics(fileName: String): Promise[js.Array[Diagnostic]] = js.native

          def getSuggestionDiagnostics(fileName: String): Promise[js.Array[Diagnostic]] = js.native

          def getCompilerOptionsDiagnostics(fileName: String): Promise[js.Array[Diagnostic]] = js.native

          def getCompletionsAtPosition(fileName: String, position: Double): Promise[js.Any | Unit] = js.native

          def getCompletionEntryDetails(fileName: String, position: Double, entry: String): Promise[js.Any | Unit] =
            js.native

          def getSignatureHelpItems(fileName: String, position: Double): Promise[js.Any | Unit] = js.native

          def getQuickInfoAtPosition(fileName: String, position: Double): Promise[js.Any | Unit] = js.native

          def getOccurrencesAtPosition(fileName: String, position: Double): Promise[js.Array[_ <: js.Any] | Unit] =
            js.native

          def getDefinitionAtPosition(fileName: String, position: Double): Promise[js.Array[_ <: js.Any] | Unit] =
            js.native

          def getReferencesAtPosition(fileName: String, position: Double): Promise[js.Array[js.Any] | Unit] = js.native

          def getNavigationBarItems(fileName: String): Promise[js.Array[js.Any]] = js.native

          def getFormattingEditsForDocument(fileName: String, options: js.Any): Promise[js.Array[js.Any]] = js.native

          def getFormattingEditsForRange(
              fileName: String,
              start: Double,
              end: Double,
              options: js.Any
          ): Promise[js.Array[js.Any]] = js.native

          def getFormattingEditsAfterKeystroke(
              fileName: String,
              postion: Double,
              ch: String,
              options: js.Any
          ): Promise[js.Array[js.Any]] = js.native

          def getRenameInfo(fileName: String, positon: Double, options: js.Any): Promise[js.Any] = js.native

          def getEmitOutput(fileName: String): Promise[js.Any] = js.native

          def getCodeFixesAtPosition(
              fileName: String,
              start: Double,
              end: Double,
              errorCodes: js.Array[Double],
              formatOptions: js.Any
          ): Promise[js.Array[_ <: js.Any]] = js.native
        }

        @js.native
        @JSGlobal("monaco.languages.typescript")
        object Typescript extends js.Object {
          type CompilerOptionsValue =
            String | Double | Boolean | js.Array[String | Double] | js.Array[String] | MapLike[
              js.Array[String]
            ] | Null | Unit
        }

      }

      package css {

        @js.native
        trait DiagnosticsOptions extends js.Object {
          def validate: Boolean = js.native

          def lint: DiagnosticsOptions.Lint = js.native
        }

        object DiagnosticsOptions {

          @js.native
          trait Lint extends js.Object {
            def compatibleVendorPrefixes: String = js.native

            def vendorPrefix: String = js.native

            def duplicateProperties: String = js.native

            def emptyRules: String = js.native

            def importStatement: String = js.native

            def boxModel: String = js.native

            def universalSelector: String = js.native

            def zeroUnits: String = js.native

            def fontFaceProperties: String = js.native

            def hexColorLength: String = js.native

            def argumentsInColorFunction: String = js.native

            def unknownProperties: String = js.native

            def ieHack: String = js.native

            def unknownVendorSpecificProperties: String = js.native

            def propertyIgnoredDueToDisplay: String = js.native

            def important: String = js.native

            def float: String = js.native

            def idSelector: String = js.native
          }

        }

        @js.native
        trait ModeConfiguration extends js.Object {
          def completionItems: Boolean = js.native

          def hovers: Boolean = js.native

          def documentSymbols: Boolean = js.native

          def definitions: Boolean = js.native

          def references: Boolean = js.native

          def documentHighlights: Boolean = js.native

          def rename: Boolean = js.native

          def colors: Boolean = js.native

          def foldingRanges: Boolean = js.native

          def diagnostics: Boolean = js.native

          def selectionRanges: Boolean = js.native
        }

        @js.native
        trait LanguageServiceDefaults extends js.Object {
          def onDidChange: IEvent[LanguageServiceDefaults] = js.native

          def diagnosticsOptions: DiagnosticsOptions = js.native

          def modeConfiguration: ModeConfiguration = js.native

          def setDiagnosticsOptions(options: DiagnosticsOptions): Unit = js.native

          def setModeConfiguration(modeConfiguration: ModeConfiguration): Unit = js.native
        }

        @js.native
        @JSGlobal("monaco.languages.css")
        object Css extends js.Object {
          var cssDefaults: LanguageServiceDefaults  = js.native
          var lessDefaults: LanguageServiceDefaults = js.native
          var scssDefaults: LanguageServiceDefaults = js.native
        }

      }

      package json {

        @js.native
        trait DiagnosticsOptions extends js.Object {
          def validate: Boolean = js.native

          def allowComments: Boolean = js.native

          def schemas: js.Array[js.Any] = js.native

          def enableSchemaRequest: Boolean = js.native
        }

        @js.native
        trait ModeConfiguration extends js.Object {
          def documentFormattingEdits: Boolean = js.native

          def documentRangeFormattingEdits: Boolean = js.native

          def completionItems: Boolean = js.native

          def hovers: Boolean = js.native

          def documentSymbols: Boolean = js.native

          def tokens: Boolean = js.native

          def colors: Boolean = js.native

          def foldingRanges: Boolean = js.native

          def diagnostics: Boolean = js.native

          def selectionRanges: Boolean = js.native
        }

        @js.native
        trait LanguageServiceDefaults extends js.Object {
          def onDidChange: IEvent[LanguageServiceDefaults] = js.native

          def diagnosticsOptions: DiagnosticsOptions = js.native

          def modeConfiguration: ModeConfiguration = js.native

          def setDiagnosticsOptions(options: DiagnosticsOptions): Unit = js.native

          def setModeConfiguration(modeConfiguration: ModeConfiguration): Unit = js.native
        }

        @js.native
        @JSGlobal("monaco.languages.json")
        object Json extends js.Object {
          var jsonDefaults: LanguageServiceDefaults = js.native
        }

      }

      package html {

        @js.native
        trait HTMLFormatConfiguration extends js.Object {
          def tabSize: Double = js.native

          def insertSpaces: Boolean = js.native

          def wrapLineLength: Double = js.native

          def unformatted: String = js.native

          def contentUnformatted: String = js.native

          def indentInnerHtml: Boolean = js.native

          def preserveNewLines: Boolean = js.native

          def maxPreserveNewLines: Double = js.native

          def indentHandlebars: Boolean = js.native

          def endWithNewline: Boolean = js.native

          def extraLiners: String = js.native

          def wrapAttributes: String = js.native
        }

        @js.native
        trait CompletionConfiguration extends js.Object {
          @JSBracketAccess
          def apply(provider: String): Boolean = js.native

          @JSBracketAccess
          def update(provider: String, v: Boolean): Unit = js.native
        }

        @js.native
        trait Options extends js.Object {
          def format: HTMLFormatConfiguration = js.native

          def suggest: CompletionConfiguration = js.native
        }

        @js.native
        trait ModeConfiguration extends js.Object {
          def completionItems: Boolean = js.native

          def hovers: Boolean = js.native

          def documentSymbols: Boolean = js.native

          def links: Boolean = js.native

          def documentHighlights: Boolean = js.native

          def rename: Boolean = js.native

          def colors: Boolean = js.native

          def foldingRanges: Boolean = js.native

          def diagnostics: Boolean = js.native

          def selectionRanges: Boolean = js.native

          def documentFormattingEdits: Boolean = js.native

          def documentRangeFormattingEdits: Boolean = js.native
        }

        @js.native
        trait LanguageServiceDefaults extends js.Object {
          def onDidChange: IEvent[LanguageServiceDefaults] = js.native

          def options: Options = js.native

          def setOptions(options: Options): Unit = js.native
        }

        @js.native
        @JSGlobal("monaco.languages.html")
        object Html extends js.Object {
          var htmlDefaults: LanguageServiceDefaults      = js.native
          var handlebarDefaults: LanguageServiceDefaults = js.native
          var razorDefaults: LanguageServiceDefaults     = js.native
        }

      }

      @js.native
      @JSGlobal("monaco.languages")
      object Languages extends js.Object {
        def register(language: ILanguageExtensionPoint): Unit = js.native

        def getLanguages(): js.Array[ILanguageExtensionPoint] = js.native

        def getEncodedLanguageId(languageId: String): Double = js.native

        def onLanguage(languageId: String, callback: js.Function0[Unit]): IDisposable = js.native

        def setLanguageConfiguration(languageId: String, configuration: LanguageConfiguration): IDisposable = js.native

        def setTokensProvider(
            languageId: String,
            provider: TokensProvider | EncodedTokensProvider | Thenable[TokensProvider | EncodedTokensProvider]
        ): IDisposable = js.native

        def setMonarchTokensProvider(
            languageId: String,
            languageDef: IMonarchLanguage | Thenable[IMonarchLanguage]
        ): IDisposable =
          js.native

        def registerReferenceProvider(languageId: String, provider: ReferenceProvider): IDisposable = js.native

        def registerRenameProvider(languageId: String, provider: RenameProvider): IDisposable = js.native

        def registerSignatureHelpProvider(languageId: String, provider: SignatureHelpProvider): IDisposable = js.native

        def registerHoverProvider(languageId: String, provider: HoverProvider): IDisposable = js.native

        def registerDocumentSymbolProvider(languageId: String, provider: DocumentSymbolProvider): IDisposable =
          js.native

        def registerDocumentHighlightProvider(languageId: String, provider: DocumentHighlightProvider): IDisposable =
          js.native

        def registerDefinitionProvider(languageId: String, provider: DefinitionProvider): IDisposable = js.native

        def registerImplementationProvider(languageId: String, provider: ImplementationProvider): IDisposable =
          js.native

        def registerTypeDefinitionProvider(languageId: String, provider: TypeDefinitionProvider): IDisposable =
          js.native

        def registerCodeLensProvider(languageId: String, provider: CodeLensProvider): IDisposable = js.native

        def registerCodeActionProvider(languageId: String, provider: CodeActionProvider): IDisposable = js.native

        def registerDocumentFormattingEditProvider(
            languageId: String,
            provider: DocumentFormattingEditProvider
        ): IDisposable = js.native

        def registerDocumentRangeFormattingEditProvider(
            languageId: String,
            provider: DocumentRangeFormattingEditProvider
        ): IDisposable =
          js.native

        def registerOnTypeFormattingEditProvider(
            languageId: String,
            provider: OnTypeFormattingEditProvider
        ): IDisposable = js.native

        def registerLinkProvider(languageId: String, provider: LinkProvider): IDisposable = js.native

        def registerCompletionItemProvider(languageId: String, provider: CompletionItemProvider): IDisposable =
          js.native

        def registerColorProvider(languageId: String, provider: DocumentColorProvider): IDisposable = js.native

        def registerFoldingRangeProvider(languageId: String, provider: FoldingRangeProvider): IDisposable = js.native

        def registerDeclarationProvider(languageId: String, provider: DeclarationProvider): IDisposable = js.native

        def registerSelectionRangeProvider(languageId: String, provider: SelectionRangeProvider): IDisposable =
          js.native

        def registerDocumentSemanticTokensProvider(
            languageId: String,
            provider: DocumentSemanticTokensProvider
        ): IDisposable = js.native

        def registerDocumentRangeSemanticTokensProvider(
            languageId: String,
            provider: DocumentRangeSemanticTokensProvider
        ): IDisposable =
          js.native

        type CharacterPair              = js.Tuple2[String, String]
        type ProviderResult[T]          = T | Unit | Null | Thenable[T | Unit | Null]
        type Definition                 = Location | js.Array[Location] | js.Array[LocationLink]
        type TextEdit                   = js.Any
        type IShortMonarchLanguageRule1 = js.Tuple2[RegExp, IMonarchLanguageAction]
        type IShortMonarchLanguageRule2 = js.Tuple3[RegExp, IMonarchLanguageAction, String]
        type IMonarchLanguageRule =
          IShortMonarchLanguageRule1 | IShortMonarchLanguageRule2 | IExpandedMonarchLanguageRule
        type IShortMonarchLanguageAction = String
        type IMonarchLanguageAction = IShortMonarchLanguageAction | IExpandedMonarchLanguageAction | js.Array[
          IShortMonarchLanguageAction
        ] | js.Array[IExpandedMonarchLanguageAction]
      }

    }

    package worker {

      @js.native
      trait IMirrorModel extends js.Object {
        def uri: Uri = js.native

        def version: Double = js.native

        def getValue(): String = js.native
      }

      @js.native
      trait IWorkerContext[H] extends js.Object {
        var host: H = js.native

        def getMirrorModels(): js.Array[IMirrorModel] = js.native
      }

    }

    @js.native
    @JSGlobal("monaco")
    object Monaco extends js.Object {
      type Thenable[T] = js.Thenable[T]
    }

  }

  @js.native
  @JSGlobalScope
  object Importedjs extends js.Object {
    def MonacoEnvironment: monaco.Environment | Unit = js.native
  }

}
