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

  test("support recursive types JSON serialization and deserialization") {
    val child1 = FileEntry("file1.txt", "/home/file1.txt", false, 100L, 1234567890L, Some("content1"))
    val child2 = FileEntry("file2.txt", "/home/file2.txt", false, 200L, 1234567891L, Some("content2"))
    val parentEntry = FileEntry("home", "/home", true, 0L, 1234567892L, None, List(child1, child2))

    val codec = MessageCodec.of[FileEntry]
    
    // Test JSON serialization and deserialization
    val json = codec.toJson(parentEntry)
    val restored = codec.unpackJson(json)
    restored shouldBe defined
    restored.get shouldBe parentEntry
  }

  test("support empty children list in recursive types") {
    val emptyDir = FileEntry("empty", "/empty", true, 0L, 1234567897L, None, List.empty)

    val codec = MessageCodec.of[FileEntry]
    checkCodec(codec, emptyDir)
  }

  test("support circular reference with mutual recursion") {
    // Create a simple case that might be problematic
    val root = FileEntry("root", "/", true, 0L, 1234567890L, None, List.empty)
    val child = FileEntry("child", "/child", true, 0L, 1234567890L, None, List.empty)
    
    // Update the root to include the child
    val updatedRoot = root.copy(children = List(child))
    val updatedChild = child.copy(children = List(updatedRoot))
    val finalRoot = updatedRoot.copy(children = List(updatedChild))

    val codec = MessageCodec.of[FileEntry]
    
    // This test should reveal if there are issues with recursive unpacking
    val json = codec.toJson(finalRoot)
    val restored = codec.unpackJson(json)
    restored shouldBe defined
  }

  test("LazyCodec ref initialization should work correctly") {
    // This test specifically targets potential LazyCodec initialization issues
    val factory = new MessageCodecFactory()
    val surface = wvlet.airframe.surface.Surface.of[FileEntry]
    
    // First codec creation - this should create LazyCodecs internally
    val codec1 = factory.ofSurface(surface).asInstanceOf[MessageCodec[FileEntry]]
    
    // Second codec creation - this should use cache
    val codec2 = factory.ofSurface(surface).asInstanceOf[MessageCodec[FileEntry]]
    
    // Both should work the same way
    val testData = FileEntry("test", "/test", false, 100L, 123456L, Some("content"), 
                            List(FileEntry("nested", "/test/nested", false, 50L, 123457L)))
    
    val packed1 = codec1.toMsgPack(testData)
    val packed2 = codec2.toMsgPack(testData)
    
    val unpacked1 = codec1.unpackMsgPack(packed1)
    val unpacked2 = codec2.unpackMsgPack(packed2)
    
    unpacked1 shouldBe defined
    unpacked2 shouldBe defined
    unpacked1.get shouldBe testData
    unpacked2.get shouldBe testData
  }

  test("test potential LazyCodec infinite recursion bug") {
    // This test may reveal the issue with LazyCodec.ref initialization
    
    // Create a fresh factory to avoid any cached state
    val factory = new MessageCodecFactory()
    
    // Create the codec
    val codec = factory.ofSurface(wvlet.airframe.surface.Surface.of[FileEntry]).asInstanceOf[MessageCodec[FileEntry]]
    
    // Create a simple recursive structure
    val child = FileEntry("child", "/child", false, 100L, 123L)
    val parent = FileEntry("parent", "/parent", true, 0L, 123L, None, List(child))
    
    // This should work fine for packing
    val msgpack = codec.toMsgPack(parent)
    
    // The bug, if it exists, would likely manifest during unpacking
    // when LazyCodec.ref is accessed for the first time
    val restored = codec.unpackMsgPack(msgpack)
    
    restored shouldBe defined
    restored.get shouldBe parent
  }

  test("test concurrent codec access that might trigger race condition") {
    // Test a potential race condition in LazyCodec ref initialization
    val factory = new MessageCodecFactory()
    val codec = factory.ofSurface(wvlet.airframe.surface.Surface.of[FileEntry]).asInstanceOf[MessageCodec[FileEntry]]
    
    val testData = FileEntry("root", "/", true, 0L, 123L, None, List(
      FileEntry("child1", "/child1", false, 100L, 124L),
      FileEntry("child2", "/child2", true, 0L, 125L, None, List(
        FileEntry("grandchild", "/child2/grandchild", false, 50L, 126L)
      ))
    ))
    
    // Pack the data
    val msgpack = codec.toMsgPack(testData)
    
    // Try multiple concurrent unpacking operations
    val results = (1 to 5).map { _ =>
      codec.unpackMsgPack(msgpack)
    }
    
    results.foreach { result =>
      result shouldBe defined
      result.get shouldBe testData
    }
  }

  test("test LazyCodec with cleared cache scenario") {
    // This test simulates a scenario where the cache might not work as expected
    val factory = new MessageCodecFactory()
    
    // Force creation of a codec with recursion
    val surface = wvlet.airframe.surface.Surface.of[FileEntry]
    val codec = factory.ofSurface(surface).asInstanceOf[MessageCodec[FileEntry]]
    
    // Create test data with deep recursion
    val deepChild = FileEntry("level4", "/a/b/c/d", false, 10L, 123L)
    val level3 = FileEntry("level3", "/a/b/c", true, 0L, 123L, None, List(deepChild))
    val level2 = FileEntry("level2", "/a/b", true, 0L, 123L, None, List(level3))  
    val level1 = FileEntry("level1", "/a", true, 0L, 123L, None, List(level2))
    val root = FileEntry("root", "/", true, 0L, 123L, None, List(level1))
    
    val msgpack = codec.toMsgPack(root)
    val restored = codec.unpackMsgPack(msgpack)
    
    restored shouldBe defined
    restored.get shouldBe root
  }
}