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
package wvlet.airframe.codec

/**
  * Test for recursive type codec handling
  */
class RecursiveCodecTest extends CodecSpec {

  case class FileEntry(
      name: String,
      path: String,
      isDirectory: Boolean,
      size: Long,
      lastUpdatedAtMillis: Long,
      content: Option[String] = None,
      children: List[FileEntry] = List.empty
  )

  test("support recursive types serialization and deserialization") {
    val child1 = FileEntry("file1.txt", "/home/file1.txt", false, 100L, 1234567890L, Some("content1"))
    val child2 = FileEntry("file2.txt", "/home/file2.txt", false, 200L, 1234567891L, Some("content2"))
    val parent = FileEntry("home", "/home", true, 0L, 1234567892L, None, List(child1, child2))

    val codec = MessageCodec.of[FileEntry]
    
    // Test serialization and deserialization
    checkCodec(codec, parent)
  }

  test("support deeply nested recursive types") {
    val deepChild = FileEntry("deep.txt", "/a/b/c/deep.txt", false, 50L, 1234567893L, Some("deep content"))
    val midChild = FileEntry("c", "/a/b/c", true, 0L, 1234567894L, None, List(deepChild))
    val topChild = FileEntry("b", "/a/b", true, 0L, 1234567895L, None, List(midChild))
    val root = FileEntry("a", "/a", true, 0L, 1234567896L, None, List(topChild))

    val codec = MessageCodec.of[FileEntry]
    checkCodec(codec, root)
  }

  test("support empty children list in recursive types") {
    val emptyDir = FileEntry("empty", "/empty", true, 0L, 1234567897L, None, List.empty)

    val codec = MessageCodec.of[FileEntry]
    checkCodec(codec, emptyDir)
  }
}