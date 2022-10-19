package wvlet.airframe.json

import wvlet.airspec.AirSpec

case class Test(json: String, yaml: String)

class YAMLFormatterTest extends AirSpec {

  val testData = Seq(
    // json, yaml
    Test("""{}""", """"""),
    Test("""{"a":1}""", """a: 1"""),
    Test("""{"a":true}""", """a: true"""),
    Test("""{"a":false}""", """a: false"""),
    Test("""{"a":null}""", """a: null"""),
    Test("""{"a":1.1}""", """a: 1.1"""),
    Test("""{"a":"hello"}""", """a: hello"""),
    Test("""{"a":"hello world"}""", """a: 'hello world'"""),
    Test(
      json = """{"a":[1, 2]}""",
      yaml = """a:
               |  - 1
               |  - 2""".stripMargin
    ),
    Test(
      json = """{"a":[true, false]}""",
      yaml = """a:
               |  - true
               |  - false""".stripMargin
    ),
    Test(
      json = """{"a":[1, true, false, null, 1.1, "hello", "hello world"]}""",
      yaml = """a:
               |  - 1
               |  - true
               |  - false
               |  - null
               |  - 1.1
               |  - hello
               |  - 'hello world'""".stripMargin
    ),
    Test(
      json = """{"a":{}}""",
      yaml = """""".stripMargin
    ),
    Test(
      json = """{"a":{"b":1}}""",
      yaml = """a:
               |  b: 1""".stripMargin
    ),
    Test(
      json = """{"a":{"b":1, "c":true}}""",
      yaml = """a:
               |  b: 1
               |  c: true""".stripMargin
    ),
    Test(
      json = """{"a":{"b":1, "c":true, "d":[1, 2, 3], "e":{"f":1}}}""",
      yaml = """a:
               |  b: 1
               |  c: true
               |  d:
               |    - 1
               |    - 2
               |    - 3
               |  e:
               |    f: 1""".stripMargin
    ),
    Test(
      json = """{"a":{"b":{"c":{"d":[1, 2, 3]}}}}""",
      yaml = """a:
               |  b:
               |    c:
               |      d:
               |        - 1
               |        - 2
               |        - 3""".stripMargin
    ),
    Test(
      json = """{"200":"ok"}}""",
      yaml = """'200': ok""".stripMargin
    ),
    Test(
      json = """{"a":[{"b":1, "c":2}, {"b":3, "c":4}]}""",
      yaml = """a:
               |  - b: 1
               |    c: 2
               |  - b: 3
               |    c: 4""".stripMargin
    ),
    Test(
      json = """{"description":"multi-line\nstrings"}""",
      yaml = """description: |
          |  multi-line
          |  strings""".stripMargin
    ),
    Test(
      json = """{"description":"with quote(')"}""",
      yaml = """description: |
          |  with quote(')""".stripMargin
    )
  )

  test("format json into YAML") {
    testData.foreach { x =>
      val json         = x.json
      val expectedYaml = x.yaml

      val yaml = YAMLFormatter.toYaml(json)
      yaml shouldBe expectedYaml
    }
  }
}
