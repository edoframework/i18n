# Taihei Framework i18n skill docs

This directory is the source of the `taihei-i18n` Claude Code skill: how to use the library,
written next to the code it describes.

```
docs/skill/
  SKILL.md            # the skill itself (frontmatter, a lean overview and an index)
  reference/*.md      # detailed reference per area (core, catalogues, messageformat2, coremessages, formatting, migration)
```

## Editing

Update these in the same pull request that changes the public API they describe. Keep `SKILL.md`
lean (overview and index) and put the detail in `reference/*.md`, so the skill loads cheaply and
Claude reads the depth only when it needs it.

## Using the skill

Copy `SKILL.md` and `reference/` into a `skills/taihei-i18n/` directory of your own plugin, or into
`.claude/skills/taihei-i18n/` of a project that uses the library. Nothing in this repository
publishes the skill anywhere.
