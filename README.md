# EDIFACT Data Type for Eclipse OIE

An **EDIFACT data type** for [Eclipse Open Integration Engine](https://openintegrationengine.org/) (tested against **4.6.0**), modelled on the built-in *EDI / X12* data type. Once installed, "EDIFACT" shows up next to HL7 v2.x, EDI / X12 and the other data types in the Swing client and the web administrator. Incoming EDIFACT is converted to XML for your filters and transformers; XML is converted back to EDIFACT for the outbound side.

> Community extension. It is not part of, or endorsed by, the Eclipse OIE project.

The data type works at the level of the EDIFACT syntax (ISO 9735), so it can be used for any EDIFACT-based standard, including national ones such as the Dutch EDIFACT messages. It does not validate messages against a specific message definition (see *Not included*).

## What you get

- The **EDIFACT** data type for source and destination connectors, with a properties panel in the **Swing client** and the **web administrator**.
- Reads UN/EDIFACT interchanges: **UNA** service string advice, **release character** escapes (`?+`, `?:`, `?'`, `??`), a **repetition separator** (syntax version 4), line breaks between segments, a missing terminator on the last segment.
- Writes them back **byte for byte** (apart from line breaks between segments, which are configurable): an interchange survives `EDIFACT -> XML -> EDIFACT` unchanged.
- **Metadata** for the message list and searches: source = interchange sender (UNB), type = message type (UNH, e.g. `ORDERS`), version = version and release (e.g. `D96A`).
- **Message tree descriptions** for the service segments and the most common message segments (BGM, DTM, NAD, LIN, QTY, ...).
- **Batch** processing with a JavaScript splitter, like the EDI / X12 type.

## The XML

The naming follows the EDI / X12 data type: a segment is an element named after its tag, its data elements are `TAG.01`, `TAG.02`, ... and their components `TAG.01.1`, `TAG.01.2`, ... An empty data element is an empty element, so positions are preserved. The delimiters travel as attributes of the root element, which is how the XML can be turned back into the same message.

```
UNA:+.? '
UNB+UNOC:3+SENDER:ZZ+RECEIVER:ZZ+230115:1200+REF001'
UNH+1+ORDERS:D:96A:UN:EAN008'
BGM+220+PO12345+9'
NAD+SU+++Acme ?+ Co+Main Street 1+Amsterdam++1011AB+NL'
...
```

becomes

```xml
<EDIFACTInterchange segmentDelimiter="'" elementDelimiter="+" subelementDelimiter=":"
                    releaseCharacter="?" decimalMark="." repetitionSeparator="" una="true">
  <UNH>
    <UNH.01><UNH.01.1>1</UNH.01.1></UNH.01>
    <UNH.02>
      <UNH.02.1>ORDERS</UNH.02.1> <UNH.02.2>D</UNH.02.2> <UNH.02.3>96A</UNH.02.3> ...
    </UNH.02>
  </UNH>
  <NAD>
    <NAD.01><NAD.01.1>SU</NAD.01.1></NAD.01>
    <NAD.02/> <NAD.03/>
    <NAD.04><NAD.04.1>Acme + Co</NAD.04.1></NAD.04>
    ...
```

The root is `EDIFACTInterchange` when the message has a UNA or UNB, otherwise `EDIFACTMessage` (a bare UNH...UNT message). A complete example is in [`examples/orders.edi`](examples/orders.edi) and [`examples/orders.xml`](examples/orders.xml).

In a transformer (E4X):

```javascript
var type  = msg['UNH']['UNH.02']['UNH.02.1'].toString();      // ORDERS
var sender = msg['UNB']['UNB.02']['UNB.02.1'].toString();     // SENDER

// every buyer party
for each (var nad in msg['NAD']) {
    if (nad['NAD.01']['NAD.01.1'].toString() == 'BY') {
        channelMap.put('buyerGln', nad['NAD.02']['NAD.02.1'].toString());
    }
}
```

## Install

1. Download `datatype-edifact-<version>.zip` from the [Releases](../../releases) page (or build it, see below).
2. Settings -> Extensions -> **Install Extension**, choose the zip, restart the engine.
3. Restart the Swing client. In the web administrator do a hard refresh (Ctrl+F5).
4. In a channel, open the source connector's **Set Data Types** and choose **EDIFACT**.

## Settings

The delimiters are used when the message has no UNA, when *Infer Delimiters* is off, and when XML that carries no delimiters of its own is converted to EDIFACT.

| Setting | Meaning | Default |
|---|---|---|
| Segment Delimiter | Segment terminator. The first character is the terminator; further characters (`\n`) are written after every segment when producing EDIFACT | `'` |
| Element Delimiter | Separates the data elements | `+` |
| Component Delimiter | Separates the components of a composite | `:` |
| Release Character | Makes the next character plain data | `?` |
| Decimal Mark | Only used to write the UNA | `.` |
| Repetition Separator | Separates repeated data elements (syntax version 4, e.g. `*`). Empty = not used | *(empty)* |
| Infer Delimiters | Read the delimiters from the UNA when the message has one | on |
| Split Batch By | JavaScript: a script that returns the next message of a batch (`reader` is available) | JavaScript |

Building EDIFACT from XML:

- A **UNA** is written when the root element has `una="true"` or when the delimiters are not the standard ones (the receiver could not read the message otherwise).
- Text is escaped with the release character automatically.
- Data elements and components must appear in ascending order; gaps become empty elements/components. Elements that are out of order, occur twice (without a repetition separator) or have unexpected names are rejected with a clear error.

## Not included

- **No validation** against message definitions (ORDERS D.96A, INVOIC, ...): the type checks the syntax, not whether a message is valid for its type.
- **The counters are not recalculated.** UNT (segment count), UNE and UNZ (counts) keep the values you give them; update them yourself when you add or remove segments.
- Binary segments (UNO/UNP) are not supported.
- One built-in batch splitter only (JavaScript). Splitting an interchange into its messages is a matter of a short script.
- Character sets: the message is a Java string; choose the character encoding in the connector (UNOA/UNOB are ASCII, UNOC is ISO 8859-1, UNOY is UTF-8).

## Build

Requirements: a JDK 11+ (the runtime bundled with the engine, `<OIE_HOME>/jre`, includes `javac` and works as `JAVA_HOME`) and Maven 3.9+.

1. Install the engine jars into your local Maven repository under the coordinates `pom.xml` expects:

   ```bash
   mvn install:install-file -Dfile="<OIE_HOME>/server-lib/mirth-server.jar"        -DgroupId=com.mirth.connect -DartifactId=server-api    -Dversion=4.6.0 -Dpackaging=jar
   mvn install:install-file -Dfile="<OIE_HOME>/server-lib/donkey/donkey-server.jar" -DgroupId=com.mirth.connect -DartifactId=donkey-server -Dversion=4.6.0 -Dpackaging=jar
   mvn install:install-file -Dfile="<OIE_HOME>/server-lib/donkey/donkey-model.jar"  -DgroupId=com.mirth.connect -DartifactId=donkey-model  -Dversion=4.6.0 -Dpackaging=jar
   mvn install:install-file -Dfile="<OIE_HOME>/client-lib/mirth-client.jar"         -DgroupId=com.mirth.connect -DartifactId=client        -Dversion=4.6.0 -Dpackaging=jar
   mvn install:install-file -Dfile="<OIE_HOME>/client-lib/mirth-client-core.jar"    -DgroupId=com.mirth.connect -DartifactId=client-core   -Dversion=4.6.0 -Dpackaging=jar
   ```

2. Build (this also runs the unit tests):

   ```bash
   mvn clean package
   ```

   This produces `target/datatype-edifact-<version>.zip`:

   ```
   datatype-edifact/
   ├── plugin.xml
   ├── datatype-edifact-shared.jar    (serializer, reader, properties, vocabulary)
   ├── datatype-edifact-server.jar    (server plugin, batch adaptor)
   ├── datatype-edifact-client.jar    (Swing plugin, code template plugin)
   └── webadmin/                      (web administrator: plugin.json + web/plugin.js)
   ```

## Design notes (for developers)

- **Package name.** The classes live in `com.mirth.connect.plugins.datatypes.edifact`. The engine's XStream allow-list only accepts classes from the `com.mirth.connect.*` family when it reads a channel, so a package of your own would make the channel unreadable.
- **Folder name = `path`.** The `path` attribute in `plugin.xml` (`datatype-edifact`) must equal the extension folder name; uninstalling and the web administrator both build paths from it.
- **Null-safe properties.** The engine does not run constructors when it reads a saved channel, so a property added later is null there. The getters of `EDIFACTSerializationProperties` fall back to the defaults.
- `EDIFACTSegmentParser` reads the message lazily, one segment at a time, which is what lets the metadata extraction stop after the first UNH.

## License

[Mozilla Public License 2.0](LICENSE). The structure of this data type follows the EDI / X12 data type of Open Integration Engine (Mirth Connect, Copyright (c) Mirth Corporation), which is MPL 2.0 as well.
