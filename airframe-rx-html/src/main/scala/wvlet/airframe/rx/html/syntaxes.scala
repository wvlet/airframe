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

object tags extends HtmlTags

object tags_extra extends HtmlTagsExtra

object attrs extends HtmlAttrs

object all extends HtmlTags with HtmlAttrs with RxEmbedding

// Note: SVG tags and attributes are defined separately to resolve naming conflicts with regular HTML tags and attributes
object svgTags extends HtmlSvgTags

object svgAttrs extends HtmlSvgAttrs

// Unified import that includes both HTML and SVG with conflict resolution
// For conflicting names, SVG versions are prefixed with 'svg' (e.g., svgStyle, svgClass)
object unifiedAll extends UnifiedTags
