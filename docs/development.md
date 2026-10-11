# Development guide

This guide is for people who build, change or release the EDIFACT Data Type. For using the data type in a channel, see the [README](../README.md); for the code templates, see [CODE_TEMPLATES.md](CODE_TEMPLATES.md).

## Contents

- [Repository layout](#repository-layout)
- [Building](#building)
- [How the plugin is put together](#how-the-plugin-is-put-together)
- [Message flow](#message-flow)
- [Service characters: EDIFACTDelimiters](#service-characters-edifactdelimiters)
- [Reading EDIFACT: EDIFACTSegmentParser](#reading-edifact-edifactsegmentparser)
- [EDIFACT to XML: EDIFACTReader](#edifact-to-xml-edifactreader)
- [XML to EDIFACT: EDIFACTXMLHandler](#xml-to-edifact-edifactxmlhandler)
- [Dialects: data element 0](#dialects-data-element-0)
- [Message tree descriptions: EDIFACTVocabulary](#message-tree-descriptions-edifactvocabulary)
- [Code templates](#code-templates)
- [Batch processing](#batch-processing)
- [Properties and saved channels](#properties-and-saved-channels)
- [The web administrator](#the-web-administrator)
- [Testing](#testing)
- [Trying it in OIE](#trying-it-in-oie)
- [Continuous integration](#continuous-integration)
- [Releasing](#releasing)
- [Conventions](#conventions)

## Repository layout

| Path | What it is |
| --- | --- |
| `src/main/java/.../edifact/` | Shared classes: serializer, delimiters, parser, reader, XML handler, properties, vocabulary. Goes into `datatype-edifact-shared.jar`. |
| `src/main/java/.../edifact/server/` | Server-only classes: server plugin and batch adaptor. `datatype-edifact-server.jar`. |
| `src/main/java/.../edifact/client/` | Swing Administrator classes: client plugin and the code templates. `datatype-edifact-client.jar`. |
| `src/main/resources/.../edifact/client/templates/` | The JavaScript of the five code templates, one file each. Packed into the client jar. |
| `src/main/resources/plugin.xml` | The extension descriptor the engine reads. |
| `src/test/` | JUnit 4 tests. |
| `webadmin/` | The properties panel for the OIE web administrator (`plugin.json`, `web/plugin.js`). |
| `examples/` | Sample messages (`orders.edi` with its XML, `medlab.edi`, `medspe.edi`), all with fictitious data. |
| `docs/` | This guide, the code template guide and the list of [specifications](specs/README.md) the dialects were checked against. |
| `assembly.xml` | How the extension zip is laid out. |
| `oie.json` | The manifest for the OIE Community Store catalog. |
| `.github/` | Build, release, catalog and Jira workflows and their scripts. |

`...` is `com/mirth/connect/plugins/datatypes`. The package must be in the `com.mirth.connect` family: the engine's XStream allow-list only accepts classes from there when it reads a channel.

The structure follows the engine's built-in EDI/X12 data type, and so does the XML naming; several classes say so in their header.

## Building

### Prerequisites

- **JDK 11 or later.** The engine's own runtime (`<OIE_HOME>/jre`) includes `javac` and works as `JAVA_HOME`; the CI uses Temurin 17.
- **Maven 3.9** or later.
- **The OIE engine jars** in your local Maven repository, version `oie.version` from `pom.xml` (now 4.6.0). They are not in Maven Central. The script the CI uses installs them from the engine's GitHub release, checked against its published SHA-256:

```bash
bash .github/scripts/install-oie-jars.sh 4.6.0
```

It needs `curl`, `tar`, `sha256sum` and `mvn` (Git Bash on Windows has them). The README shows the `mvn install:install-file` commands to install the same jars from a local OIE installation instead.

| Maven artifact (`com.mirth.connect`) | File in the engine |
| --- | --- |
| `server-api` | `server-lib/mirth-server.jar` |
| `donkey-server` | `server-lib/donkey/donkey-server.jar` |
| `donkey-model` | `server-lib/donkey/donkey-model.jar` |
| `client` | `client-lib/mirth-client.jar` |
| `client-core` | `client-lib/mirth-client-core.jar` |

### Build

```bash
mvn clean package
```

The result is `target/datatype-edifact-<version>.zip`. `package` runs the tests first; add `-DskipTests` to skip them, or `-o` to build offline.

### What is in the zip

`maven-jar-plugin` makes three jars out of one source tree, by package; `assembly.xml` puts them in the zip:

```
datatype-edifact/
├── plugin.xml
├── datatype-edifact-shared.jar   edifact/*.class
├── datatype-edifact-server.jar   edifact/server/**
├── datatype-edifact-client.jar   edifact/client/** (classes and templates/*.js)
└── webadmin/                     the web administrator panel
```

The folder name `datatype-edifact` must equal the `path` attribute in `plugin.xml`: the engine and the web administrator build URLs from it. The plugin has no runtime dependencies of its own; everything it uses is on the engine's classpath.

## How the plugin is put together

| Plugin point | Class | Purpose |
| --- | --- | --- |
| `DataTypeDelegate` | `EDIFACTDataTypeDelegate` | Name (`EDIFACT`), serializer, default properties; text, not binary; the transformer works on XML. |
| `DataTypeServerPlugin` | `server.EDIFACTDataTypeServerPlugin` | Registers the data type on the server, plus the batch adaptor. |
| `DataTypeClientPlugin` | `client.EDIFACTDataTypeClientPlugin` | Registers the data type in the Swing Administrator. |
| `DataTypeCodeTemplatePlugin` | `client.EDIFACTDataTypeCodeTemplatePlugin` | The *EDIFACT* category in the reference list, plus the standard conversion entries. |
| `IMessageSerializer` | `EDIFACTSerializer` | EDIFACT to XML, XML to EDIFACT, metadata. |
| `DataTypeProperties` | `EDIFACTDataTypeProperties` | Holds `EDIFACTSerializationProperties` and `EDIFACTBatchProperties`. |
| `MessageVocabulary` | `EDIFACTVocabulary` | Descriptions in the message tree. |

The work is done by four classes that know nothing about the engine apart from SAX and the engine's `AbstractXMLReader`:

| Class | Role |
| --- | --- |
| `EDIFACTDelimiters` | The service characters: from the UNA, from the properties, or the defaults. |
| `EDIFACTSegmentParser` | Text to segments, data elements and components, one segment at a time. |
| `EDIFACTReader` | Segments to SAX events, so to XML. |
| `EDIFACTXMLHandler` | SAX events back to EDIFACT, with escaping and an optional UNA. |

## Message flow

```
inbound EDIFACT text
   ├─ populateMetaData()   EDIFACTSegmentParser, stops after UNH →
   │                       source = UNB sender, type = UNH message type (ORDERS), version = version + release (D96A)
   └─ toXML()              EDIFACTReader → SAX → XMLPrettyPrinter → XML for the transformer
                              ↓ transformer (E4X on msg)
outbound XML
   └─ fromXML()            strip pretty-print whitespace → SAX parser (DOCTYPE refused) → EDIFACTXMLHandler → EDIFACT text
```

- `isSerializationRequired` is `false` and `transformWithoutSerializing` returns `null`, so without filter or transformer steps a message passes through unchanged, as in the EDI/X12 data type.
- `populateMetaData` never throws: a message that cannot be read leaves the metadata empty, and `toXML` reports the real error.
- `fromXML` sets the SAX feature `disallow-doctype-decl`, so XML with a DOCTYPE is refused before entities can be resolved.
- Whitespace between tags of pretty-printed XML is removed first (two patterns, as in EDI/X12); text inside a component is kept.

The design goal is an exact round trip: `EDIFACT → XML → EDIFACT` gives the same bytes, apart from line breaks between segments, which follow the *Segment Delimiter* property.

## Service characters: EDIFACTDelimiters

```
UNA:+.? '
   │││││└─ segment terminator
   ││││└── repetition separator (syntax version 4), or a space when unused
   │││└─── release character
   ││└──── decimal mark
   │└───── data element separator
   └────── component separator
```

- `fromProperties` builds the defaults from the properties. The properties are strings and go through `StringUtil.unescape`, so `'\n` in *Segment Delimiter* means a terminator followed by a newline.
- `segmentOutput` is what is written after every segment. Its **first** character is the terminator; the rest (a newline) is cosmetic and never read.
- `readUna(message, infer)` skips leading whitespace and a byte order mark. When the message starts with `UNA`, it sets `una = true` and, if `infer` is on, takes the six service characters from it. The terminator comes from the UNA, but the cosmetic tail of the configured `segmentOutput` is kept.
- `repetition` is `NONE` (`'\0'`) when not used.
- `isStandard()` tells whether all service characters are the defaults (`:+.? '`), which decides whether a UNA must be written.

## Reading EDIFACT: EDIFACTSegmentParser

`EDIFACTSegmentParser` returns one `Segment` per call to `next()`, lazily, so the metadata extraction can stop after UNH without reading the whole interchange.

A `Segment` has a tag and a list of `DataElement`s; each has an index and its components. Some details:

- Whitespace (and a byte order mark) **between** segments is skipped. Inside a value it is kept.
- The tag must be three letters or digits. It ends at the element separator, the segment terminator **or the component separator**. The last case is for dialects such as MEDLAB; see [Dialects: data element 0](#dialects-data-element-0).
- The release character takes the next character literally. A message that ends on a release character is an error.
- With a repetition separator, a repeated data element gets a new `DataElement` with the **same index**.
- An empty data element is one element with one empty component, so its position is kept.
- The last segment may lack its terminator.

Errors are `SyntaxException`s with the position and the offending text, made printable (`\r`, `\n`), for example *Invalid segment tag "…" at position 42*.

## EDIFACT to XML: EDIFACTReader

`EDIFACTReader` (an `AbstractXMLReader`) reads the whole message, applies `readUna`, and emits:

```xml
<EDIFACTInterchange segmentDelimiter="'" elementDelimiter="+" subelementDelimiter=":"
                    releaseCharacter="?" decimalMark="." repetitionSeparator="" una="true">
  <NAD>
    <NAD.01><NAD.01.1>SU</NAD.01.1></NAD.01>
    <NAD.02/>
    ...
```

- The root is `EDIFACTInterchange` when the first segment is UNB or the message had a UNA, otherwise `EDIFACTMessage`.
- The root attributes carry the service characters, so the XML can be turned back into the same message. Only the terminator is in `segmentDelimiter`, because XML parsers normalize whitespace in attribute values.
- Element names follow EDI/X12: `TAG`, `TAG.NN` (two digits, `TAG.10` and up as they are), `TAG.NN.N`.
- An empty data element is written as an empty element without components. Data element 0 always lists its components; otherwise the separator behind the tag would be lost.
- A repeated data element is a repeated `TAG.NN` element.
- Release characters are resolved: the XML holds plain text.

## XML to EDIFACT: EDIFACTXMLHandler

`EDIFACTXMLHandler` (a SAX `DefaultHandler`) writes while it reads, by depth: root (1), segment (2), data element (3), component (4).

- **Root attributes** override the defaults from the properties; a missing attribute keeps the property value. `una="true"` asks for a UNA.
- **Segments**: the element name is written as the tag, followed by `segmentOutput`.
- **Data elements** must come in ascending order. A gap writes the element separators of the empty elements in between. The same index twice is a repetition when a repetition separator is set, and an error otherwise. Element 0 is written directly behind the tag and must come first.
- **Components** must come in ascending order too; gaps become empty components.
- **Text directly in a data element** (without component elements) is accepted, for a transformer that leaves out the component level.
- **Escaping**: every service character in the text gets the release character in front of it.
- **UNA**: written when the XML asked for it (`una="true"`) or when the service characters are not the standard ones, since the receiver could not read the message otherwise.
- Element names that do not match `TAG.NN` or `TAG.NN.N` are refused with the name and the segment in the message.

Counters (UNT, UNE, UNZ) are **not** recalculated; the README says so, and the *Build an interchange envelope* template computes them for messages built from scratch.

## Dialects: data element 0

Dutch healthcare messages such as MEDLAB put components directly behind the tag, with a component separator instead of an element separator:

```
BEP:1:1:3+0+Quet/BMI+23.1++kg/m2++10+50+QUET'
```

The parser starts such a segment with data element **0**; the next element separator starts data element 1 as usual. So `BEP.02` is still the second element behind the `+`, and `BEP.00.1` … `BEP.00.3` hold `1`, `1`, `3`. On the way back, `TAG.00` is written straight behind the tag with a component separator before each component.

The specifications these dialects were checked against are listed in [specs/README.md](specs/README.md); the documents themselves are not in this repository. MEDSPE also uses `""` as an explicitly empty value; it is read and written as those two characters, without special handling.

When adding support for another dialect, add a fictitious example to `examples/` and round-trip tests to `EDIFACTDialectTest`.

## Message tree descriptions: EDIFACTVocabulary

`EDIFACTVocabulary` describes the nodes of the message tree, with ids such as `UNH`, `UNH.02` and `UNH.02.1`:

- the service segments (UNA, UNB, UNG, UNH, UNT, UNE, UNZ) completely, after ISO 9735;
- the common message segments (BGM, DTM, NAD, LIN, QTY, PRI, MOA, ...), down to the data elements;
- for messages whose type (UNH.02.1, passed in by the engine as the vocabulary's `type`) is **MEDLAB**, the MEDLAB segments instead, after *Berichtdefinitie Laboratoriumbericht MEDLAB 1*, including data element 0. A segment with the same tag in another message (COM, for example) keeps its UN/EDIFACT description.

The tables are built in a static block. A segment is listed as `elements("TAG", "element", "element|component|component", ...)`: each argument is one data element, in order, and `|` separates the components.

## Code templates

`client.EDIFACTCodeTemplates` lists the five templates of the *EDIFACT* category: a name, a JavaScript resource in `templates/`, whether it uses `msg`, and a description.

- Templates that use `msg` are offered in connector scripts (filters and transformers) only; the others in every script.
- The code lives in `src/main/resources/.../client/templates/*.js`, so it is real JavaScript with syntax highlighting, and the tests run exactly what the user gets.
- `EDIFACTCodeTemplatesTest` runs each template in Rhino with E4X against a test message (for MEDLAB, `examples/medlab.edi`), as a transformer would, and checks the variables it creates.

**Adding a template:** add the `.js` file, a row in `TEMPLATES`, a test in `EDIFACTCodeTemplatesTest`, and a section in `docs/CODE_TEMPLATES.md` and the README's template table.

## Batch processing

With *Process Batch Files* on in the source connector, the engine asks `EDIFACTDataTypeServerPlugin.getBatchAdaptorFactory` for an adaptor. `EDIFACTBatchAdaptor` supports one split type, **JavaScript**: the user's script gets `reader` and returns the next message, or nothing at the end. It is modelled on the engine's other batch adaptors. There is no built-in splitter by message (UNH … UNT); that takes a short script.

## Properties and saved channels

Channels are stored with XStream, which **does not run constructors**. A property added in a later version is missing (`null` for objects, the type's default for primitives) in a channel saved before. The rules that follow:

- String getters must fall back to the default when the field is `null`; `EDIFACTDelimiters.fromProperties` also falls back for empty values.
- `inferDelimiters` is a primitive `boolean` that defaults to `true`. It has existed since the first version, so it is safe; a **new** setting that defaults to `true` must be a `Boolean` whose getter returns `true` for `null`.

### Adding a property

1. Add the field and a null-safe getter to `EDIFACTSerializationProperties` (or `EDIFACTBatchProperties`).
2. Add it to `getPropertyDescriptors()` (label and tooltip in the Swing Administrator), `setProperties()` and the copy constructor.
3. Add it to `webadmin/web/plugin.js` with the same key, label, default and tooltip.
4. Document it in the README's settings table.
5. Choose the default so that existing channels behave as before.
6. Test that a channel saved with the previous version still deploys; `propertiesRoundTripThroughTheirDescriptors` checks the descriptors.

## The web administrator

`webadmin/plugin.json` registers the panel; `webadmin/web/plugin.js` describes the fields of both property groups with their keys, defaults and tooltips. It is a static copy of `getPropertyDescriptors()`: keep both in sync by hand. After installing a new version, a hard refresh (Ctrl+F5) loads the new panel.

The web administrator has no hook for reference list items, so the code templates only appear in the Swing Administrator.

## Testing

```bash
mvn test
```

There are 62 tests (JUnit 4). They need no running engine and no network.

| Test | Covers |
| --- | --- |
| `EDIFACTSerializerTest` (43) | XML naming, root attributes, release characters, empty elements and components, whitespace and byte order mark, UNA and properties, repetitions, round trips (custom delimiters, element numbers above 9, escaping), UNA writing rules, gaps, order and name errors, DOCTYPE refusal, metadata, delegate, property descriptors, vocabulary |
| `EDIFACTDialectTest` (13) | MEDLAB and MEDSPE: element 0, round trips of `examples/medlab.edi` and `examples/medspe.edi`, MEDLAB descriptions and metadata, element 0 edge cases |
| `client/EDIFACTCodeTemplatesTest` (6) | Each code template run in Rhino with E4X |

Run the tests from the repository root: some read files from `examples/`. `commons-text` is a test dependency only because `CodeTemplate` needs it at runtime; the engine provides it in production.

## Trying it in OIE

1. `mvn clean package`.
2. Install `target/datatype-edifact-<version>.zip` on a **test** server (*Settings > Extensions > Install Extension*) and restart the service. Restart the Swing Administrator; in the web administrator, refresh with Ctrl+F5.
3. Make a channel with inbound data type **EDIFACT** and send the files in `examples/`. Set the connector's character encoding to match the syntax identifier in UNB (UNOA/UNOB: ASCII, UNOC: ISO 8859-1, UNOY: UTF-8).
4. Check the XML and the message tree descriptions in the message browser, and the EDIFACT that a destination with outbound data type EDIFACT writes.

## Continuous integration

`.github/workflows/build.yml` runs on every push to `main`, every pull request and every `v*` tag:

1. Java 17 (Temurin) and a Maven cache keyed on the engine version and `pom.xml`.
2. `install-oie-jars.sh` when the engine jars are not cached. `OIE_VERSION` in the workflow must equal `oie.version` in `pom.xml`.
3. `check-versions.sh`: the version in `pom.xml`, `plugin.xml`, `oie.json` and `webadmin/plugin.json` must be the same, and on a tag equal to the tag.
4. `mvn -B package`, and the zip is kept as a build artifact.
5. On a tag: `release.sh` creates the release, then the `catalog` job calls `catalog-pr.yml`.

Dependabot (`.github/dependabot.yml`) proposes updates weekly for Maven and monthly for GitHub Actions. It ignores the engine jars and the libraries the engine ships (`log4j-api`, `commons-lang3`, `commons-text`, `rhino`): those must match the engine, not the newest version.

| Workflow | Does |
| --- | --- |
| `catalog-pr.yml` | Files a pull request to the [OIE Community Store catalog](https://github.com/gibson9583/oie-community-catalog) from the fork `<owner>/oie-community-catalog`. Needs the secret `CATALOG_TOKEN` (a classic PAT with `public_repo`); skipped without it. |
| `jira-issue.yml` | Creates a Jira issue for every new GitHub issue and comments its key. Needs the variables `JIRA_BASE_URL`, `JIRA_EMAIL`, `JIRA_PROJECT_KEY` and the secret `JIRA_API_TOKEN`. |

`main` is protected against force pushes and deletion (ruleset *Protect main*).

## Releasing

1. **Branch and pull request.** Reference the issue (`Closes #n`); the build must be green.
2. **Version.** Set the new version in `pom.xml`, `src/main/resources/plugin.xml` (`pluginVersion`), `oie.json` and `webadmin/plugin.json`. The build fails when they differ.
3. **Engine versions.** `<mirthVersion>` in `plugin.xml` lists every OIE version the extension may load on (now `4.6.0,4.7.0`). OIE only loads an extension whose list contains its **exact** version, so a new OIE release needs a new plugin release.
4. **Release notes (optional).** To write them yourself, create the release on GitHub first, without a zip. Otherwise `release.sh` writes short notes with the install steps and the SHA-256, plus GitHub's generated notes.
5. **Tag** the merged commit on `main` and push the tag:

   ```bash
   git tag v1.0.6
   git push origin v1.0.6
   ```

   The build creates the release with `datatype-edifact-<version>.zip` and its `.sha256`, or adds them to the release you wrote. A release that already has a zip is left alone: a new build gives a different file, whose checksum would not match.
6. **Catalog.** Check the pull request the catalog job filed, and close earlier ones of this plugin that are still open.
7. **Jira.** Closing a GitHub issue does not close its Jira issue: set it to *Done* yourself.
8. **Documentation.** Update the README and `docs/CODE_TEMPLATES.md` when behaviour, settings or templates change.

`release.sh` can be tried locally with `DRY_RUN=1 bash .github/scripts/release.sh v1.0.6` after a build.

## Conventions

- Every file in `src/main` starts with the MPL 2.0 header; files based on the engine's EDI/X12 data type also credit it.
- Java 11 language level; no runtime dependencies outside the engine's classpath.
- Keep the round trip exact: XML to EDIFACT must give the same message as came in, apart from the configured line breaks.
- Examples contain fictitious data only.
- Error messages name the problem and where it is, for example *"Data element NAD.03 is out of order or occurs twice in segment NAD"*.
- Comments explain *why*; keep them short.
