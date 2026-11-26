/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package wvlet.airframe.rx.html

/**
  * Unified tags and attributes that resolves naming conflicts between HTML and SVG. For conflicting names, SVG versions
  * are prefixed with 'svg'.
  */
trait UnifiedTags extends HtmlTags with HtmlAttrs with RxEmbedding {

  // Import all non-conflicting SVG tags
  lazy val altGlyph            = svgTags.altGlyph
  lazy val altGlyphDef         = svgTags.altGlyphDef
  lazy val altGlyphItem        = svgTags.altGlyphItem
  lazy val animate             = svgTags.animate
  lazy val animateMotion       = svgTags.animateMotion
  lazy val animateTransform    = svgTags.animateTransform
  lazy val circle              = svgTags.circle
  lazy val `color-profile`     = svgTags.`color-profile`
  lazy val defs                = svgTags.defs
  lazy val desc                = svgTags.desc
  lazy val ellipse             = svgTags.ellipse
  lazy val feBlend             = svgTags.feBlend
  lazy val feColorMatrix       = svgTags.feColorMatrix
  lazy val feComponentTransfer = svgTags.feComponentTransfer
  lazy val feComposite         = svgTags.feComposite
  lazy val feConvolveMatrix    = svgTags.feConvolveMatrix
  lazy val feDiffuseLighting   = svgTags.feDiffuseLighting
  lazy val feDisplacementMap   = svgTags.feDisplacementMap
  lazy val feDistantLighting   = svgTags.feDistantLighting
  lazy val feFlood             = svgTags.feFlood
  lazy val feFuncA             = svgTags.feFuncA
  lazy val feFuncB             = svgTags.feFuncB
  lazy val feFuncG             = svgTags.feFuncG
  lazy val feFuncR             = svgTags.feFuncR
  lazy val feGaussianBlur      = svgTags.feGaussianBlur
  lazy val feImage             = svgTags.feImage
  lazy val feMerge             = svgTags.feMerge
  lazy val feMergeNode         = svgTags.feMergeNode
  lazy val feMorphology        = svgTags.feMorphology
  lazy val feOffset            = svgTags.feOffset
  lazy val fePointLight        = svgTags.fePointLight
  lazy val feSpecularLighting  = svgTags.feSpecularLighting
  lazy val feSpotlight         = svgTags.feSpotlight
  lazy val feTile              = svgTags.feTile
  lazy val feTurbulance        = svgTags.feTurbulance
  // font tag is deprecated in SVG
  // lazy val font                = svgTags.font
  lazy val `font-face`         = svgTags.`font-face`
  lazy val `font-face-format`  = svgTags.`font-face-format`
  lazy val `font-face-name`    = svgTags.`font-face-name`
  lazy val `font-face-src`     = svgTags.`font-face-src`
  lazy val `font-face-uri`     = svgTags.`font-face-uri`
  lazy val foreignObject       = svgTags.foreignObject
  lazy val g                   = svgTags.g
  lazy val glyph               = svgTags.glyph
  lazy val glyphRef            = svgTags.glyphRef
  lazy val hkern               = svgTags.hkern
  lazy val image               = svgTags.image
  lazy val line                = svgTags.line
  lazy val linearGradient      = svgTags.linearGradient
  lazy val marker              = svgTags.marker
  lazy val metadata            = svgTags.metadata
  lazy val `missing-glyph`     = svgTags.`missing-glyph`
  lazy val mpath               = svgTags.mpath
  lazy val path                = svgTags.path
  lazy val polygon             = svgTags.polygon
  lazy val polyline            = svgTags.polyline
  lazy val radialGradient      = svgTags.radialGradient
  lazy val rect                = svgTags.rect
  lazy val set                 = svgTags.set
  lazy val stop                = svgTags.stop
  lazy val svg                 = svgTags.svg
  lazy val switch              = svgTags.switch
  lazy val symbol              = svgTags.symbol
  lazy val text                = svgTags.text
  lazy val textPath            = svgTags.textPath
  lazy val tref                = svgTags.tref
  lazy val tspan               = svgTags.tspan
  lazy val use                 = svgTags.use
  lazy val view                = svgTags.view
  lazy val vkern               = svgTags.vkern

  // Import all non-conflicting SVG attributes
  lazy val accentHeight              = svgAttrs.accentHeight
  lazy val accumulate                = svgAttrs.accumulate
  lazy val additive                  = svgAttrs.additive
  lazy val alignmentBaseline         = svgAttrs.alignmentBaseline
  lazy val ascent                    = svgAttrs.ascent
  lazy val attributeName             = svgAttrs.attributeName
  lazy val attributeType             = svgAttrs.attributeType
  lazy val azimuth                   = svgAttrs.azimuth
  lazy val baseFrequency             = svgAttrs.baseFrequency
  lazy val baselineShift             = svgAttrs.baselineShift
  lazy val begin                     = svgAttrs.begin
  lazy val bias                      = svgAttrs.bias
  lazy val calcMode                  = svgAttrs.calcMode
  lazy val clip                      = svgAttrs.clip
  lazy val clipPathUnits             = svgAttrs.clipPathUnits
  lazy val clipRule                  = svgAttrs.clipRule
  lazy val color                     = svgAttrs.color
  lazy val colorInterpolation        = svgAttrs.colorInterpolation
  lazy val colorInterpolationFilters = svgAttrs.colorInterpolationFilters
  lazy val colorProfile              = svgAttrs.colorProfile
  lazy val colorRendering            = svgAttrs.colorRendering
  lazy val contentScriptType         = svgAttrs.contentScriptType
  lazy val contentStyleType          = svgAttrs.contentStyleType
  lazy val cursor                    = svgAttrs.cursor
  lazy val cx                        = svgAttrs.cx
  lazy val cy                        = svgAttrs.cy
  lazy val d                         = svgAttrs.d
  lazy val diffuseConstant           = svgAttrs.diffuseConstant
  lazy val direction                 = svgAttrs.direction
  lazy val display                   = svgAttrs.display
  lazy val divisor                   = svgAttrs.divisor
  lazy val dominantBaseline          = svgAttrs.dominantBaseline
  lazy val dur                       = svgAttrs.dur
  lazy val dx                        = svgAttrs.dx
  lazy val dy                        = svgAttrs.dy
  lazy val edgeMode                  = svgAttrs.edgeMode
  lazy val elevation                 = svgAttrs.elevation
  lazy val end                       = svgAttrs.end
  lazy val externalResourcesRequired = svgAttrs.externalResourcesRequired
  lazy val fill                      = svgAttrs.fill
  lazy val fillOpacity               = svgAttrs.fillOpacity
  lazy val fillRule                  = svgAttrs.fillRule
  lazy val filterRes                 = svgAttrs.filterRes
  lazy val filterUnits               = svgAttrs.filterUnits
  lazy val floodColor                = svgAttrs.floodColor
  lazy val floodOpacity              = svgAttrs.floodOpacity
  lazy val fontFamily                = svgAttrs.fontFamily
  lazy val fontSize                  = svgAttrs.fontSize
  lazy val fontSizeAdjust            = svgAttrs.fontSizeAdjust
  lazy val fontStretch               = svgAttrs.fontStretch
  lazy val fontVariant               = svgAttrs.fontVariant
  lazy val fontWeight                = svgAttrs.fontWeight
  lazy val from                      = svgAttrs.from
  lazy val fx                        = svgAttrs.fx
  lazy val fy                        = svgAttrs.fy
  lazy val gradientTransform         = svgAttrs.gradientTransform
  lazy val gradientUnits             = svgAttrs.gradientUnits
  lazy val imageRendering            = svgAttrs.imageRendering
  lazy val in                        = svgAttrs.in
  lazy val in2                       = svgAttrs.in2
  lazy val k1                        = svgAttrs.k1
  lazy val k2                        = svgAttrs.k2
  lazy val k3                        = svgAttrs.k3
  lazy val k4                        = svgAttrs.k4
  lazy val kernelMatrix              = svgAttrs.kernelMatrix
  lazy val kernelUnitLength          = svgAttrs.kernelUnitLength
  lazy val kerning                   = svgAttrs.kerning
  lazy val keySplines                = svgAttrs.keySplines
  lazy val keyTimes                  = svgAttrs.keyTimes
  lazy val letterSpacing             = svgAttrs.letterSpacing
  lazy val lightingColor             = svgAttrs.lightingColor
  lazy val limitingConeAngle         = svgAttrs.limitingConeAngle
  lazy val local                     = svgAttrs.local
  lazy val markerEnd                 = svgAttrs.markerEnd
  lazy val markerMid                 = svgAttrs.markerMid
  lazy val markerStart               = svgAttrs.markerStart
  lazy val markerHeight              = svgAttrs.markerHeight
  lazy val markerUnits               = svgAttrs.markerUnits
  lazy val markerWidth               = svgAttrs.markerWidth
  lazy val maskContentUnits          = svgAttrs.maskContentUnits
  lazy val maskUnits                 = svgAttrs.maskUnits
  lazy val mode                      = svgAttrs.mode
  lazy val numOctaves                = svgAttrs.numOctaves
  lazy val offset                    = svgAttrs.offset
  lazy val orient                    = svgAttrs.orient
  lazy val opacity                   = svgAttrs.opacity
  lazy val operator                  = svgAttrs.operator
  lazy val order                     = svgAttrs.order
  lazy val overflow                  = svgAttrs.overflow
  lazy val paintOrder                = svgAttrs.paintOrder
  lazy val pathLength                = svgAttrs.pathLength
  lazy val patternContentUnits       = svgAttrs.patternContentUnits
  lazy val patternTransform          = svgAttrs.patternTransform
  lazy val patternUnits              = svgAttrs.patternUnits
  lazy val pointerEvents             = svgAttrs.pointerEvents
  lazy val points                    = svgAttrs.points
  lazy val pointsAtX                 = svgAttrs.pointsAtX
  lazy val pointsAtY                 = svgAttrs.pointsAtY
  lazy val pointsAtZ                 = svgAttrs.pointsAtZ
  lazy val preserveAlpha             = svgAttrs.preserveAlpha
  lazy val preserveAspectRatio       = svgAttrs.preserveAspectRatio
  lazy val primitiveUnits            = svgAttrs.primitiveUnits
  lazy val r                         = svgAttrs.r
  lazy val radius                    = svgAttrs.radius
  lazy val refX                      = svgAttrs.refX
  lazy val refY                      = svgAttrs.refY
  lazy val repeatCount               = svgAttrs.repeatCount
  lazy val repeatDur                 = svgAttrs.repeatDur
  lazy val requiredFeatures          = svgAttrs.requiredFeatures
  lazy val restart                   = svgAttrs.restart
  lazy val result                    = svgAttrs.result
  lazy val rx                        = svgAttrs.rx
  lazy val ry                        = svgAttrs.ry
  lazy val scale                     = svgAttrs.scale
  lazy val seed                      = svgAttrs.seed
  lazy val shapeRendering            = svgAttrs.shapeRendering
  lazy val specularConstant          = svgAttrs.specularConstant
  lazy val specularExponent          = svgAttrs.specularExponent
  lazy val spreadMethod              = svgAttrs.spreadMethod
  lazy val stdDeviation              = svgAttrs.stdDeviation
  lazy val stitchTiles               = svgAttrs.stitchTiles
  lazy val stopColor                 = svgAttrs.stopColor
  lazy val stopOpacity               = svgAttrs.stopOpacity
  lazy val stroke                    = svgAttrs.stroke
  lazy val strokeDasharray           = svgAttrs.strokeDasharray
  lazy val strokeDashoffset          = svgAttrs.strokeDashoffset
  lazy val strokeLinecap             = svgAttrs.strokeLinecap
  lazy val strokeLinejoin            = svgAttrs.strokeLinejoin
  lazy val strokeMiterlimit          = svgAttrs.strokeMiterlimit
  lazy val strokeOpacity             = svgAttrs.strokeOpacity
  lazy val strokeWidth               = svgAttrs.strokeWidth
  lazy val surfaceScale              = svgAttrs.surfaceScale
  lazy val targetX                   = svgAttrs.targetX
  lazy val targetY                   = svgAttrs.targetY
  lazy val textAnchor                = svgAttrs.textAnchor
  lazy val textDecoration            = svgAttrs.textDecoration
  lazy val textRendering             = svgAttrs.textRendering
  lazy val to                        = svgAttrs.to
  lazy val transform                 = svgAttrs.transform
  lazy val values                    = svgAttrs.values
  lazy val viewBox                   = svgAttrs.viewBox
  lazy val visibility                = svgAttrs.visibility
  lazy val wordSpacing               = svgAttrs.wordSpacing
  lazy val writingMode               = svgAttrs.writingMode
  lazy val x                         = svgAttrs.x
  lazy val x1                        = svgAttrs.x1
  lazy val x2                        = svgAttrs.x2
  lazy val xChannelSelector          = svgAttrs.xChannelSelector
  lazy val xLinkHref                 = svgAttrs.xLinkHref
  lazy val xLink                     = svgAttrs.xLink
  lazy val xLinkTitle                = svgAttrs.xLinkTitle
  lazy val xmlSpace                  = svgAttrs.xmlSpace
  lazy val xmlnsXlink                = svgAttrs.xmlnsXlink
  lazy val y                         = svgAttrs.y
  lazy val y1                        = svgAttrs.y1
  lazy val y2                        = svgAttrs.y2
  lazy val yChannelSelector          = svgAttrs.yChannelSelector
  lazy val z                         = svgAttrs.z

  // SVG attributes with prefixed names for conflicts
  lazy val svgClass  = new HtmlAttributeOf("class", Namespace.svg)
  lazy val svgStyle  = new HtmlAttributeOf("style", Namespace.svg)
  lazy val svgType   = new HtmlAttributeOf("type", Namespace.svg)
  lazy val svgXmlns  = new HtmlAttributeOf("xmlns", Namespace.svg)
  lazy val svgMax    = new HtmlAttributeOf("max", Namespace.svg)
  lazy val svgMin    = new HtmlAttributeOf("min", Namespace.svg)
  lazy val svgHeight = new HtmlAttributeOf("height", Namespace.svg)
  lazy val svgWidth  = new HtmlAttributeOf("width", Namespace.svg)
  lazy val svgId     = new HtmlAttributeOf("id", Namespace.svg)

  // SVG tags with prefixed names for conflicts
  lazy val svgTitle    = svgTags.title
  lazy val svgPattern  = svgTags.pattern
  lazy val svgFilter   = svgTags.filter
  lazy val svgMask     = svgTags.mask
  lazy val svgClipPath = svgTags.clipPath

  // SVG attributes for the conflicting tag names
  lazy val svgFilterAttr   = new HtmlAttributeOf("filter", Namespace.svg)
  lazy val svgMaskAttr     = new HtmlAttributeOf("mask", Namespace.svg)
  lazy val svgClipPathAttr = new HtmlAttributeOf("clip-path", Namespace.svg)
}

// Object moved to syntaxes.scala
