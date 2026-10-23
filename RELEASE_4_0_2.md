# Release 4.0.2

**Release Date:** 2026-10-23

This patch release fixes a denial of service when a `Message` read from JSON is decoded or
formatted (see Security below). `MessageFormat2.format` now stops the text of one template at a
million characters and returns the template instead, and the JSON decoder of `Message` counts
its templates and arguments before it builds any map, and refuses a message beyond fixed limits
on their number and length. Messages and
templates within the limits behave exactly as in 4.0.1. The public API is unchanged, and the
release is binary compatible with 4.0.1.

## Security

In 1.0.0 and 2.0.0 (as `fish.genius::genius-i18n`), 3.0.0 (as `com.edoframework::edo-i18n`),
4.0.0 and 4.0.1, formatting a `Message` had no bound on the length of the text it produced. A
template can repeat a placeholder many times, and each repetition copies the argument, so a
`Message` of a few hundred kilobytes of JSON could format to gigabytes and end in
`OutOfMemoryError` on the JVM. On Scala.js formatting either threw a JavaScript `RangeError`
where a `String` is promised or exhausted the engine's memory, depending on the sizes.
`translate`, `render` and `toString` are all affected, so logging a received message is enough;
encoding it back to JSON is not. Decoding itself could also take minutes of CPU: the arguments
and templates were put into maps before they were counted, and many keys that share a hash code
make building a map quadratic. An application that decodes `Message` JSON from an untrusted
source should upgrade. Advisory:
[GHSA-6h29-v54g-8337](https://github.com/taiheiframework/i18n/security/advisories/GHSA-6h29-v54g-8337). If you cannot
upgrade, do not decode `Message` JSON from an untrusted source; if you must, cap the size of the
document, read its objects as a list of pairs (zio-json's `JsonDecoder.keyValueChunk`) and check
their number and length before you build a map or a `Message`.

## Upgrading

Change the version to 4.0.2. No code changes are needed:

```scala
mvn"dev.taihei::taihei-i18n:4.0.2"       // Mill, JVM
mvn"dev.taihei::taihei-i18n::4.0.2"      // Mill, Scala.js
"dev.taihei" %%% "taihei-i18n" % "4.0.2" // sbt
```

Users of `com.edoframework::edo-i18n:3.0.0` or `fish.genius::genius-i18n` move to
`dev.taihei::taihei-i18n:4.0.2` as described in the README.

## Changelog

### Fixed
- `MessageFormat2.format` builds the text of a template within 1,000,000 characters. Past that it
  returns the template as it was written. The check runs before each piece is appended, so the
  text never grows past the limit; a lone placeholder whose value is longer than the limit also
  gives the template, and a `.match` variant is held to the same limit. A 4.x message formats
  exactly one template, so this also bounds `Message.translate`, `render` and `toString`.
- The JSON decoder of `Message` reads `namedArgs` and `translationTemplates` as lists of pairs
  and counts them before it builds a map, so reading stays linear in the size of the document.
  A missing field, a built-in language name as a key, and a duplicate key (the last value wins)
  read as in 4.0.1. A template key that is not a language is refused with an error that leaves
  the key out.
- The JSON decoder of `Message` refuses a message that holds:
  - more than 1000 templates;
  - more than 1000 arguments, positional and named together;
  - a template, an argument value or an argument name longer than 65536 characters.

  Templates and arguments are counted as they appear in the document, before duplicate keys
  collapse. These errors name the limit and do not echo the input. The JSON shape and the encoder are
  unchanged, so a message built in code beyond these limits (for example `Message.of` with an
  exception message longer than 65536 characters) is still written, but no longer read back.

### Changed
- `make release-finalize` names the current branch in the push it asks for, and
  `make release-prepare` no longer warns on a maintenance branch (`<major>.<minor>.x`, cut from a
  release tag), so that a patch can also be released from one.

## Module Changes

- **i18n.jvm** and **i18n.js** (`dev.taihei::taihei-i18n`): shared sources `Message` and
  `MessageFormat2`.
- **Tests**: extended `MessageFormat2Spec` and `MessageSpec`, on both platforms.
- **Build and tooling**: `Makefile` (release hint only; nothing published changes).

## Compatibility

- Scala 3.9.0
- Scala.js 1.22.0
- zio-json 1.1.1
- scala-java-time 2.8.0 (Scala.js, with tzdb)
- ZIO Test 2.1.27 (tests)
- Binary compatible with 4.0.1 (checked by MiMa). The only behavioural restrictions are the limits
  above: Message JSON beyond them is refused, and a text over a million characters is replaced by
  its template.
