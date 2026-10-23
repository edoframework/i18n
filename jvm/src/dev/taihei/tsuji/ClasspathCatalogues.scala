package dev.taihei.tsuji

/**
 * The catalogue files of families on the JVM classpath, at
 * `META-INF/taihei-tsuji/<prefix>/<language tag>.json`: where a library ships its translations,
 * and where [[Catalogues]] reads them from by itself. This object reads them for a test.
 *
 * {{{
 * assertTrue(ClasspathCatalogues.verify(Seq(Orders), Seq(Dutch, French), required = Set(Dutch)).isEmpty)
 * }}}
 */
object ClasspathCatalogues:

  /**
   * The family's catalogue for the language: None when the classpath has no such file, a problem
   * when the file is not a catalogue of that language. Keys of another family are left out and
   * named in the catalogue's problems.
   */
  def read(family: MessageFamily, language: Language): Option[Either[CatalogueProblem, Catalogue]] =
    PlatformCatalogueResources
      .read(family, language.tag)
      .map: content =>
        for
          text      <- content.left.map(CatalogueProblem(_, Some(language)))
          catalogue <- Catalogue.parse(text).left.map(_.copy(language = Some(language)))
          _         <- Either.cond(
            catalogue.language == language,
            (),
            CatalogueProblem(
              s"the file of ${family.prefix} says it is ${catalogue.language.tag}",
              Some(language),
            ),
          )
        yield catalogue.ofFamily(family.prefix)

  /**
   * [[Catalogues.verify]] over the families' catalogue files in these languages and the required
   * ones, with what is wrong with the files themselves. Empty means all is well.
   */
  def verify(
    families: Seq[MessageFamily],
    languages: Seq[Language],
    required: Set[Language] = Set.empty,
  ): List[CatalogueProblem] =
    val results    =
      for
        family   <- families.distinct
        language <- (languages ++ required.toList.sortBy(_.tag)).distinct
        result   <- read(family, language).toList
      yield result
    val unreadable = results.collect { case Left(p) => p }
    (unreadable.toList ++
    Catalogues.verify(families, results.collect { case Right(c) => c }, required)).distinct

end ClasspathCatalogues
