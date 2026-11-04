# airframe-rx-html

A reactive HTML/SVG rendering library for Scala.

## Import Options

### Separate Imports (Traditional approach)

When using HTML and SVG separately, you can import them individually:

```scala
import wvlet.airframe.rx.html.all.*      // HTML tags and attributes
import wvlet.airframe.rx.html.svgTags.*  // SVG tags
import wvlet.airframe.rx.html.svgAttrs.* // SVG attributes
```

### Unified Import (New approach)

To use both HTML and SVG together without naming conflicts:

```scala
import wvlet.airframe.rx.html.unifiedAll.*
```

## Handling Naming Conflicts

The unified import resolves naming conflicts between HTML and SVG by prefixing SVG versions with `svg`:

### Simple Attribute Conflicts
These attributes exist in both HTML and SVG namespaces:

| Attribute | HTML Version | SVG Version |
|-----------|--------------|-------------|
| class     | `class` or `cls` | `svgClass` |
| style     | `style` | `svgStyle` |
| type      | `type` | `svgType` |
| xmlns     | `xmlns` | `svgXmlns` |
| max       | `max` | `svgMax` |
| min       | `min` | `svgMin` |
| height    | `height` | `svgHeight` |
| width     | `width` | `svgWidth` |
| id        | `id` | `svgId` |

### Tag/Attribute Conflicts
These names exist as both tags and attributes:

| Name | HTML Attribute | SVG Tag | SVG Attribute |
|------|----------------|---------|---------------|
| title | `title` | `svgTitle` | N/A |
| pattern | `pattern` | `svgPattern` | N/A |
| filter | N/A | `svgFilter` | `svgFilterAttr` |
| mask | N/A | `svgMask` | `svgMaskAttr` |
| clipPath | N/A | `svgClipPath` | `svgClipPathAttr` |

## Examples

### Using Unified Import

```scala
import wvlet.airframe.rx.html.unifiedAll.*

val mixedContent = div(
  cls -> "container",           // HTML class attribute
  style -> "padding: 20px;",    // HTML style attribute
  h1("Data Visualization"),
  svg(
    svgWidth -> "400",          // SVG width attribute
    svgHeight -> "300",         // SVG height attribute
    svgClass -> "chart",        // SVG class attribute
    svgStyle -> "border: 1px solid #ccc;",  // SVG style attribute
    g(
      transform -> "translate(50, 50)",
      rect(
        x -> "0",
        y -> "0",
        svgWidth -> "300",
        svgHeight -> "200",
        fill -> "steelblue"
      ),
      text(
        x -> "150",
        y -> "100",
        textAnchor -> "middle",
        svgStyle -> "fill: white; font-size: 24px;",
        "Chart Title"
      )
    )
  )
)
```

### Traditional Separate Imports

If you prefer to keep HTML and SVG separate:

```scala
import wvlet.airframe.rx.html.all.*

val htmlContent = div(
  cls -> "container",
  style -> "padding: 20px;",
  h1("Hello World")
)
```

```scala
import wvlet.airframe.rx.html.svgTags.*
import wvlet.airframe.rx.html.svgAttrs.*

val svgContent = svg(
  width -> "100",
  height -> "100",
  circle(
    cx -> "50",
    cy -> "50",
    r -> "40",
    fill -> "red"
  )
)
```

## Namespace Handling

All SVG attributes and tags are automatically created with the correct SVG namespace (`http://www.w3.org/2000/svg`), ensuring proper rendering in browsers.