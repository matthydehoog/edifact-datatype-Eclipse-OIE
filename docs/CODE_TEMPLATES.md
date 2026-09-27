# EDIFACT Data Type — Code Templates

From version 1.0.4 the EDIFACT data type adds five ready-made code templates to the reference list of the filter and transformer editors, so common EDIFACT tasks no longer have to be written from scratch in every channel.

## Where to find them

The templates are in the **EDIFACT** category of the reference list in the Swing Administrator.

1. Open a channel and, on the Source or a Destination tab, click **Edit Filter** or **Edit Transformer**.
2. On the right, open the **Reference** tab.
3. Choose **EDIFACT** in the category list. The five templates appear below it; hover over one to read its description.
4. Drag a template into a JavaScript step (or double-click it to insert it where the cursor is).
5. Change the values the template asks you to fill in, and use the variables it creates in the rest of your step.

The standard *Convert EDIFACT to XML* and *Convert XML to EDIFACT* templates that every data type has stay in their usual place, the **Conversion Functions** category.

## The five templates at a glance

| Template | What you get | Where it works |
| --- | --- | --- |
| Build an interchange envelope | The text `edifact`: UNA, UNB, UNH, your segments, UNT, UNZ, with references and counters filled in | Every script |
| Walk the line items (LIN with QTY, PRI, IMD) | The list `lineItems`: each LIN with its quantities, prices and description | Filters and transformers (uses `msg`) |
| Find a party by qualifier (NAD) | The function `findParty(qualifier)` and, for the buyer, `partyId` and `partyName` | Filters and transformers (uses `msg`) |
| Read MEDLAB results (BEP) | The list `results`: each test result with section, value, unit, reference range and remarks | Filters and transformers (uses `msg`) |
| Escape and unescape text | The functions `edifactEscape(text)` and `edifactUnescape(text)` | Every script |

## Build an interchange envelope

This template wraps your message segments in a complete interchange and fills in the date, the references and the counters. UNT gets the number of segments of the message (UNH and UNT included), UNZ the number of messages (1).

Change these four values at the top:

| Variable | Meaning | Example |
| --- | --- | --- |
| `sender` | Interchange sender (UNB.02), with its qualifier | `'500000001:14'` |
| `recipient` | Interchange recipient (UNB.03) | `'500000002:14'` |
| `messageType` | Message identifier (UNH.02) | `'ORDERS:D:96A:UN'`, `'MEDLAB:1'` |
| `segments` | Your segments, without terminator | `['BGM+220+PO12345+9', 'DTM+137:20230115:102']` |

The template creates the interchange reference from the current time (at most 14 characters) and uses `1` as the message reference. The result is the text variable `edifact`, one segment per line:

```
UNA:+.? '
UNB+UNOC:3+SENDER:ZZ+RECIPIENT:ZZ+260927:1530+90518458123456'
UNH+1+ORDERS:D:96A:UN'
BGM+220+PO12345+9'
DTM+137:20230115:102'
UNT+4+1'
UNZ+1+90518458123456'
```

Two ways to send it:

- As text: `channelMap.put('edifact', edifact);` and `${edifact}` as the template of a destination.
- As the outbound message of a transformer whose outbound data type is EDIFACT: `tmp = new XML(SerializerFactory.getSerializer('EDIFACT').toXML(edifact));`

Values that can contain `+`, `:`, `'` or `?` (names, addresses, free text) must be escaped first with `edifactEscape()`, see *Escape and unescape text*.

## Walk the line items (LIN with QTY, PRI, IMD)

This template collects every line item of an order, invoice or despatch advice into the list `lineItems`. A line item is a LIN segment plus the QTY, PRI and IMD segments that follow it, up to the next LIN, UNS or UNT.

| Field | Taken from | Example |
| --- | --- | --- |
| `number` | LIN.01.1, line item number | `1` |
| `item` | LIN.03.1, item number (e.g. the GTIN/EAN) | `4000862141404` |
| `quantities` | QTY.01.2 per qualifier QTY.01.1 | `quantities['21']` = ordered quantity `10` |
| `prices` | PRI.01.2 per qualifier PRI.01.1 | `prices['AAA']` = net price `2.50` |
| `description` | IMD.03.4, item description | `Blue widget` |

Example, below the template:

```javascript
for (var i = 0; i < lineItems.length; i++) {
    var line = lineItems[i];
    logger.info(line.item + ': ' + line.quantities['21'] + ' x ' + line.prices['AAA']);
}
```

A quantity or price that is not in the message is `undefined`. To also collect other segments of the group (for example MOA or FTX), add an `else if` for their tag inside the loop.

## Find a party by qualifier (NAD)

`findParty(qualifier)` returns the NAD segment whose first element (NAD.01.1) is that qualifier, or `null` when there is none. The template reads the buyer as an example:

```javascript
var party = findParty('BY');
if (party != null) {
    var partyId = party['NAD.02']['NAD.02.1'].toString(); // e.g. the GLN
    var partyName = party['NAD.04']['NAD.04.1'].toString();
}
```

| Qualifier | Party |
| --- | --- |
| `BY` | Buyer |
| `SU` | Supplier |
| `DP` | Delivery party |
| `IV` | Invoicee |
| `SE` | Seller |

Other useful NAD elements: NAD.05.1 street, NAD.06.1 city, NAD.08.1 postal code, NAD.09.1 country. Release characters are already removed: `Acme ?+ Co` in the message reads as `Acme + Co`.

## Read MEDLAB results (BEP)

This template collects every test result (BEP) of a Dutch MEDLAB laboratory message into the list `results`, in message order. Each result gets the name of the section (SEC) it belongs to; a new sample or request (IDE) starts without a section. Remarks (OPB) are added to the result they follow.

| Field | Taken from | Example |
| --- | --- | --- |
| `section` | SEC.01.1, section name | `MEETWAARDEN` |
| `name` | BEP.02.1, test name | `Quet/BMI` |
| `value` | BEP.03.1, result | `23.1` |
| `changed` | BEP.04.1 is filled: the result has been changed | `false` |
| `unit` | BEP.05.1, unit | `kg/m2` |
| `flag` | BEP.06.1: `<` below or `>` above the reference range | `<` |
| `low` | BEP.07.1, lower limit | `10` |
| `high` | BEP.08.1, upper limit | `50` |
| `code` | BEP.09.1, test code | `QUET` |
| `remarks` | OPB.01.1 of the OPB segments after the BEP | `['nuchter afgenomen']` |

Example, below the template:

```javascript
logger.info(results.length + ' results, first: ' + results[0].name + ' ' + results[0].value + ' ' + results[0].unit);
```

With [`examples/medlab.edi`](../examples/medlab.edi) this logs `4 results, first: gewicht 70.0 kg`. All fields are text; use `parseFloat(result.value)` to calculate with a value. The field names follow *Berichtdefinitie Laboratoriumbericht MEDLAB 1*.

## Escape and unescape text

In EDIFACT, `+`, `:` and `'` are delimiters and `?` is the release character. Inside a value, each of these four must be preceded by `?`. The two functions do that for you:

| Call | Result |
| --- | --- |
| `edifactEscape("Acme + Co: it?s")` | `Acme ?+ Co?: it??s` |
| `edifactUnescape("Acme ?+ Co")` | `Acme + Co` |

You only need them for **raw EDIFACT text** that you build or take apart yourself, for example the segments of *Build an interchange envelope*. Values in `msg` and `tmp` are already unescaped and escaped by the data type.

## Good to know

- **Version:** the templates come with the EDIFACT data type 1.0.4 and later. Install the zip from the [releases page](https://github.com/matthydehoog/edifact-datatype-Eclipse-OIE/releases) with Settings → Extensions → Install Extension, then restart the engine and the Administrator.
- **Swing Administrator:** the reference list with the EDIFACT category is part of the Swing client.
- **`msg` only in filters and transformers:** three templates read `msg`, so they are offered only where `msg` exists. The envelope and escape templates work in every script.
- **Delimiters:** the templates assume the standard delimiters (`'` `+` `:` `?`). The XML the data type makes is the same whatever delimiters the message used, so the reading templates work for every message.
- **A starting point:** a template is copied into your step. Change it freely; updating the data type does not change code that is already in a channel.
