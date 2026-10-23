package dev.taihei.i18n

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.ByteBuffer
import java.nio.charset.{CharacterCodingException, CodingErrorAction, StandardCharsets}

import scala.util.control.NonFatal

// Catalogue files on the classpath, read with the class loader of the family that owns them.
private[i18n] object PlatformCatalogueResources:

  /** Whether there is a classpath to read catalogues from. */
  val available: Boolean = true

  // UTF-8 takes at most four bytes per character.
  private val MaxBytes = Catalogue.MaxLength.toLong * 4

  def path(prefix: String, tag: String): String = s"META-INF/taihei-i18n/$prefix/$tag.json"

  /**
   * The family's catalogue file for the language tag: None when there is none, Left when it
   * cannot be read (too large, not UTF-8, an I/O error), Right with its text.
   */
  def read(family: MessageFamily, tag: String): Option[Either[String, String]] =
    val name   = path(family.prefix, tag)
    val loader = Option(
      family.getClass.getClassLoader
    ).orElse(Option(Thread.currentThread.getContextClassLoader))
    try
      loader.flatMap(l => Option(l.getResourceAsStream(name))).map: in =>
        try readBounded(in, name)
        finally in.close()
    catch
      case NonFatal(e) => Some(Left(s"cannot read $name: ${Printable(String.valueOf(e.getMessage))}"))

  private[i18n] def readBounded(in: InputStream, name: String): Either[String, String] =
    val out    = new ByteArrayOutputStream()
    val buffer = new Array[Byte](8192)
    var total  = 0L
    var done   = false
    // One byte past the limit is enough to know the file is too large.
    while !done && total <= MaxBytes do
      val n = in.read(buffer, 0, math.min(buffer.length.toLong, MaxBytes + 1 - total).toInt)
      if n < 0 then done = true
      else
        out.write(buffer, 0, n)
        total += n
    if total > MaxBytes then Left(s"$name is larger than $MaxBytes bytes")
    else
      val decoder = StandardCharsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
      try Right(decoder.decode(ByteBuffer.wrap(out.toByteArray)).toString)
      catch case _: CharacterCodingException => Left(s"$name is not UTF-8")

  end readBounded

end PlatformCatalogueResources
