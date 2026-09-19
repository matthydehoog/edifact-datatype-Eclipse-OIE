// EDIFACT data type - properties panel for the web administrator (equivalent of the Swing
// DataTypeClientPlugin properties). Same shape as the built-in EDI/X12 data type plugin.
const PKG = "com.mirth.connect.plugins.datatypes.edifact";

const text = (key, label, def, hint) => ({ key, label, type: "text", default: def, hint });
const bool = (key, label, def, hint) => ({ key, label, type: "checkbox", default: def, hint });
const opt = (key, label, options, def, hint) => ({ key, label, type: "select", options, default: def, hint });
const code = (key, label, def, hint) => ({ key, label, type: "code", default: def, hint });

const BATCH_SCRIPT_HINT =
  "JavaScript that splits the batch and returns the next message. Has access to 'reader' (a Java BufferedReader); return null/empty to signal end of input. Only used when Process Batch is enabled in the connector.";

const DEF = {
  name: "EDIFACT",
  label: "EDIFACT",
  order: 71,
  propertiesClass: `${PKG}.EDIFACTDataTypeProperties`,
  groups: [
    {
      key: "serializationProperties",
      label: "Serialization",
      class: `${PKG}.EDIFACTSerializationProperties`,
      fields: [
        text("segmentDelimiter", "Segment Delimiter", "'", "Segment terminator of the message. The first character is the terminator; further characters (for example \\n) are only written after each segment when converting XML to EDIFACT."),
        text("elementDelimiter", "Element Delimiter", "+", "Character that separates the data elements of a segment."),
        text("subelementDelimiter", "Component Delimiter", ":", "Character that separates the components of a composite data element."),
        text("releaseCharacter", "Release Character", "?", "Character that makes the next character plain data instead of a delimiter, for example ?+ for a plus sign."),
        text("decimalMark", "Decimal Mark", ".", "Decimal mark of the message. Only used to write the UNA segment."),
        text("repetitionSeparator", "Repetition Separator", "", "Character that separates repeated data elements (EDIFACT syntax version 4, for example *). Leave empty when the message does not use repetition."),
        bool("inferDelimiters", "Infer Delimiters", true, "If checked and the message starts with a UNA segment, the delimiters are read from the UNA and the delimiter properties above are not used.")
      ]
    },
    {
      key: "batchProperties",
      label: "Batch",
      class: `${PKG}.EDIFACTBatchProperties`,
      fields: [
        opt(
          "splitType",
          "Split Batch By",
          [{ value: "JavaScript", label: "JavaScript" }],
          "JavaScript",
          "Method for splitting the batch message. Only used when Process Batch is enabled in the connector."
        ),
        code("batchScript", "JavaScript", null, BATCH_SCRIPT_HINT)
      ]
    }
  ]
};

DEF.defaults = (version) => {
  const props = { "@class": DEF.propertiesClass, "@version": version };
  for (const group of DEF.groups) {
    const obj = { "@class": group.class, "@version": version };
    for (const f of group.fields) obj[f.key] = f.default ?? null;
    props[group.key] = obj;
  }
  return props;
};

export function register(platform) {
  platform.registerDataType(DEF.name, DEF);
}
