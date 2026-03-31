# Best Friends Plugin Rebrand Checklist

Use this checklist to remove legacy `MarriageMaster` branding and correctly credit original developers while rebranding to **Best Friends Plugin**.

## 1) Replace legacy branding strings

Run these searches and update all matches:

```bash
rg -n "MarriageMaster|marriage master|marriagemaster|marriage" src/ pom.xml README.md *.md
```

Update these common locations:

- `src/main/resources/plugin.yml`
  - `name:` -> `BestFriendsPlugin`
  - `description:` -> updated Best Friends wording
  - command descriptions/usages containing old branding
  - permission descriptions containing old branding
- `pom.xml`
  - `<artifactId>` / `<name>` values
- `README.md`
  - title, feature copy, install/config instructions, command examples
- Java package/classes (if currently brand-specific)
  - rename packages/classes only if you intend a full namespace migration

## 2) Validate command + permission naming strategy

Decide whether to keep legacy command/permission nodes for compatibility or move fully.

- Recommended migration:
  - Keep existing aliases for one release cycle (deprecated)
  - Add new Best Friends command/permission names now
  - Log deprecation warnings for old nodes

## 3) Credit original developers (required)

Preserve credit in **both** plugin metadata and docs.

### plugin.yml

Use `authors:` to include both current maintainers and original developers.

```yaml
authors:
  - CurrentMaintainer
  - Original MarriageMaster Developers
```

### README / CREDITS

Add an explicit acknowledgment section:

- “Originally based on MarriageMaster.”
- “Original developers: <names/organization>.”
- “Rebrand and continued maintenance by: <your team>.”

If exact names are unknown, collect from:

```bash
git shortlog -sne
```

Also review commit history for original upstream attribution before release.

## 4) Optional: backward compatibility safeguards

- Keep old config keys and map to new names at load time.
- Support legacy permission nodes as aliases.
- Add upgrade notes in release changelog.

## 5) Final verification commands

```bash
# confirm no old brand strings remain
rg -n "MarriageMaster|marriage master|marriagemaster" src/ pom.xml README.md *.md

# build check
mvn -q -DskipTests package
```

If the final search returns matches, verify they are only in intentional historical credit notes.
