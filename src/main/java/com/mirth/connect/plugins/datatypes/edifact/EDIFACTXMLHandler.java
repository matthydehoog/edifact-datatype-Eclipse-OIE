/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Turns the XML produced by {@link EDIFACTReader} (or a transformer) back into EDIFACT. The service
 * characters come from the attributes of the root element; anything missing falls back to the
 * serialization properties. A UNA is written when the XML says so (una="true") or when the service
 * characters are not the standard ones, since the receiver could not read the message without it.
 */
public class EDIFACTXMLHandler extends DefaultHandler {
    private final EDIFACTDelimiters d;
    private boolean unaRequested = false;

    private int depth = 0;
    private int lastElement = 0;
    private int lastComponent = 0;
    private String segmentTag = null;
    private boolean writtenComponent = false;

    private final StringBuilder output = new StringBuilder();

    public EDIFACTXMLHandler(EDIFACTDelimiters defaults) {
        this.d = new EDIFACTDelimiters(defaults);
    }

    @Override
    public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
        depth++;

        if (depth == 1) {
            readRootAttributes(atts);
        } else if (depth == 2) {
            segmentTag = name;
            lastElement = 0;
            output.append(name);
        } else if (depth == 3) {
            int index = index(name, 1);
            if (d.hasRepetition() && index == lastElement) {
                output.append(d.repetition);
            } else if (index <= lastElement) {
                throw new SAXException("Data element " + name + " is out of order or occurs twice in segment " + segmentTag);
            } else {
                appendRepeated(d.element, index - lastElement);
            }
            lastElement = index;
            lastComponent = 0;
            writtenComponent = false;
        } else if (depth == 4) {
            int index = index(name, 2);
            if (index <= lastComponent) {
                throw new SAXException("Component " + name + " is out of order or occurs twice in segment " + segmentTag);
            }
            // The first written component only needs delimiters for components before it that are empty.
            appendRepeated(d.component, writtenComponent ? index - lastComponent : index - 1);
            lastComponent = index;
            writtenComponent = true;
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        if (depth == 2) {
            output.append(d.segmentOutput);
        }
        depth--;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        // Text of a component, or directly of a data element when the transformer left out the component level.
        if (depth == 3 || depth == 4) {
            escape(ch, start, length);
        }
    }

    public String getOutput() {
        return (unaRequested || !d.isStandard() ? d.unaString() : "") + output;
    }

    private void readRootAttributes(Attributes atts) {
        // The terminator comes from the XML; extra characters after it (a newline) stay as configured in the properties.
        String segment = atts.getValue("segmentDelimiter");
        if (segment != null && !segment.isEmpty()) {
            String tail = d.segmentOutput.length() > 1 ? d.segmentOutput.substring(1) : "";
            d.segmentOutput = segment.charAt(0) + tail;
        }
        d.element = charAttribute(atts, "elementDelimiter", d.element);
        d.component = charAttribute(atts, "subelementDelimiter", d.component);
        d.release = charAttribute(atts, "releaseCharacter", d.release);
        d.decimal = charAttribute(atts, "decimalMark", d.decimal);

        String repetition = atts.getValue("repetitionSeparator");
        if (repetition != null) {
            d.repetition = repetition.isEmpty() ? EDIFACTDelimiters.NONE : repetition.charAt(0);
        }

        unaRequested = "true".equalsIgnoreCase(atts.getValue("una"));
    }

    private static char charAttribute(Attributes atts, String name, char def) {
        String value = atts.getValue(name);
        return value == null || value.isEmpty() ? def : value.charAt(0);
    }

    /** Number in a name such as NAD.02 (part 1) or NAD.02.3 (part 2). */
    private int index(String name, int part) throws SAXException {
        String[] parts = name.split("\\.");
        if (parts.length != part + 1) {
            throw new SAXException("Unexpected element " + name + " in segment " + segmentTag);
        }
        try {
            int index = Integer.parseInt(parts[part]);
            if (index < 1) {
                throw new NumberFormatException();
            }
            return index;
        } catch (NumberFormatException e) {
            throw new SAXException("Unexpected element " + name + " in segment " + segmentTag + ": the number after the dot must be 1 or higher");
        }
    }

    private void appendRepeated(char c, int count) {
        for (int i = 0; i < count; i++) {
            output.append(c);
        }
    }

    /** Writes text with a release character in front of every service character it contains. */
    private void escape(char[] ch, int start, int length) {
        for (int i = start; i < start + length; i++) {
            char c = ch[i];
            if (c == d.release || c == d.element || c == d.component || c == d.segment() || (d.hasRepetition() && c == d.repetition)) {
                output.append(d.release);
            }
            output.append(c);
        }
    }
}
