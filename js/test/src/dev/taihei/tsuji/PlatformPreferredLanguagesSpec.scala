package dev.taihei.tsuji

import zio.test.*

// The selection is tested on supplied values, so the result does not depend on whether the Node
// that runs CI has a navigator.
object PlatformPreferredLanguagesSpec extends ZIOSpecDefault:

  import PlatformPreferredLanguages.select

  def spec = suite("PlatformPreferredLanguages on Scala.js")(
    test("navigator.languages wins when it has any") {
      assertTrue(select(Vector("nl-BE", "en"), Some("fr"), Some("de")) == Vector("nl-BE", "en"))
    },
    test("navigator.language comes next, then the Intl locale") {
      assertTrue(
        select(Vector.empty, Some("fr"), Some("de")) == Vector("fr"),
        select(Vector.empty, None, Some("de")) == Vector("de"),
        select(Vector.empty, None, None).isEmpty,
      )
    },
    test("the chosen tags are filtered as the shared code does") {
      val tags = select(Vector("nl-BE", "und", "nl-be", "bad tag", "en"), None, None)
      assertTrue(Language.fromPlatformTags(tags) == Vector(Language("nl-BE"), Language.English))
    },
    test("the runtime's own tags are read without throwing") {
      val tags = PlatformPreferredLanguages.tags
      assertTrue(tags.forall(_ != null))
    },
  )

end PlatformPreferredLanguagesSpec
