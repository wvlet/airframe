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
  * Unified tags and attributes that resolves naming conflicts between HTML and SVG.
  * For conflicting names, SVG versions are prefixed with 'svg'.
  * 
  * Due to complex inheritance conflicts where some names exist as both tags and attributes,
  * we selectively import and re-export members rather than extending all traits.
  */
trait UnifiedTags extends HtmlTags with HtmlAttrs with RxEmbedding {
  // Import SVG tags except the conflicting ones
  export svgTags.{
    title => _, pattern => _, filter => _, mask => _, clipPath => _,
    *
  }
  
  // Import SVG attributes except the conflicting ones  
  export svgAttrs.{
    `class` => _, style => _, `type` => _, xmlns => _, max => _, min => _,
    height => _, width => _, id => _, filter => _, mask => _, clipPath => _,
    *
  }
  
  // SVG attributes with prefixed names for conflicts
  lazy val svgClass = new HtmlAttributeOf("class", Namespace.svg)
  lazy val svgStyle = new HtmlAttributeOf("style", Namespace.svg)
  lazy val svgType = new HtmlAttributeOf("type", Namespace.svg)
  lazy val svgXmlns = new HtmlAttributeOf("xmlns", Namespace.svg)
  lazy val svgMax = new HtmlAttributeOf("max", Namespace.svg)
  lazy val svgMin = new HtmlAttributeOf("min", Namespace.svg)
  lazy val svgHeight = new HtmlAttributeOf("height", Namespace.svg)
  lazy val svgWidth = new HtmlAttributeOf("width", Namespace.svg)
  lazy val svgId = new HtmlAttributeOf("id", Namespace.svg)
  
  // SVG tags/attributes that conflict with HTML attributes
  lazy val svgTitle = svgTags.title        // SVG title tag
  lazy val svgPattern = svgTags.pattern    // SVG pattern tag
  lazy val svgFilter = svgTags.filter      // SVG filter tag
  lazy val svgMask = svgTags.mask          // SVG mask tag
  lazy val svgClipPath = svgTags.clipPath  // SVG clipPath tag
  
  // SVG attributes for the conflicting tag names
  lazy val svgFilterAttr = new HtmlAttributeOf("filter", Namespace.svg)
  lazy val svgMaskAttr = new HtmlAttributeOf("mask", Namespace.svg)
  lazy val svgClipPathAttr = new HtmlAttributeOf("clip-path", Namespace.svg)
}

// Object moved to syntaxes.scala