/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import java.io.BufferedReader;
import java.io.IOException;

import org.openintegrationengine.engine.plugins.datatypes.AbstractXMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.mirth.connect.plugins.datatypes.edifact.EDIFACTSegmentParser.DataElement;
import com.mirth.connect.plugins.datatypes.edifact.EDIFACTSegmentParser.Segment;

/**
 * Turns an EDIFACT message into SAX events, using the same naming as the EDI/X12 data type:
 * a segment is an element named after its tag, its data elements are TAG.01, TAG.02, ... and
 * their components TAG.01.1, TAG.01.2, ...
 *
 * <pre>
 * &lt;EDIFACTInterchange segmentDelimiter="'" elementDelimiter="+" subelementDelimiter=":" ...&gt;
 *   &lt;UNH&gt;
 *     &lt;UNH.01&gt;&lt;UNH.01.1&gt;1&lt;/UNH.01.1&gt;&lt;/UNH.01&gt;
 *     &lt;UNH.02&gt;&lt;UNH.02.1&gt;ORDERS&lt;/UNH.02.1&gt;&lt;UNH.02.2&gt;D&lt;/UNH.02.2&gt;...&lt;/UNH.02&gt;
 * </pre>
 *
 * The service characters are written as attributes of the root element so that the XML can be turned
 * back into the same EDIFACT message.
 */
public class EDIFACTReader extends AbstractXMLReader {
    public static final String ROOT_INTERCHANGE = "EDIFACTInterchange";
    public static final String ROOT_MESSAGE = "EDIFACTMessage";

    private final EDIFACTDelimiters defaults;
    private final boolean inferDelimiters;

    public EDIFACTReader(EDIFACTDelimiters defaults, boolean inferDelimiters) {
        this.defaults = defaults;
        this.inferDelimiters = inferDelimiters;
    }

    @Override
    public void parse(InputSource input) throws SAXException, IOException {
        ensureHandlerSet();

        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(input.getCharacterStream());
        char[] buffer = new char[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            sb.append(buffer, 0, read);
        }
        String message = sb.toString();

        if (message.trim().length() < 3) {
            throw new SAXException("Unable to parse, message is empty or too short: " + message.trim());
        }

        EDIFACTDelimiters d = new EDIFACTDelimiters(defaults);
        d.una = false;
        int start = d.readUna(message, inferDelimiters);
        EDIFACTSegmentParser parser = new EDIFACTSegmentParser(message, start, d);

        ContentHandler handler = getContentHandler();
        handler.startDocument();

        try {
            String root = null;
            Segment segment;
            while ((segment = parser.next()) != null) {
                if (root == null) {
                    root = segment.tag.equalsIgnoreCase("UNB") || d.una ? ROOT_INTERCHANGE : ROOT_MESSAGE;
                    handler.startElement("", root, "", rootAttributes(d));
                }
                fireSegment(handler, segment);
            }
            if (root == null) {
                throw new SAXException("Unable to parse, the message contains no segments");
            }
            handler.endElement("", root, "");
        } catch (EDIFACTSegmentParser.SyntaxException e) {
            throw new SAXException(e.getMessage(), e);
        }

        handler.endDocument();
    }

    private AttributesImpl rootAttributes(EDIFACTDelimiters d) {
        AttributesImpl attributes = getEmptyAttributes();
        // Only the terminator: whitespace in an attribute value is normalized away by XML parsers.
        attributes.addAttribute("", "segmentDelimiter", "", "", String.valueOf(d.segment()));
        attributes.addAttribute("", "elementDelimiter", "", "", String.valueOf(d.element));
        attributes.addAttribute("", "subelementDelimiter", "", "", String.valueOf(d.component));
        attributes.addAttribute("", "releaseCharacter", "", "", String.valueOf(d.release));
        attributes.addAttribute("", "decimalMark", "", "", String.valueOf(d.decimal));
        attributes.addAttribute("", "repetitionSeparator", "", "", d.hasRepetition() ? String.valueOf(d.repetition) : "");
        attributes.addAttribute("", "una", "", "", String.valueOf(d.una));
        return attributes;
    }

    private void fireSegment(ContentHandler handler, Segment segment) throws SAXException {
        handler.startElement("", segment.tag, "", getEmptyAttributes());

        for (DataElement element : segment.elements) {
            String elementName = segment.tag + "." + pad(element.index);
            handler.startElement("", elementName, "", getEmptyAttributes());

            // An empty data element stays an empty element, as in the EDI/X12 data type. Element 0 (ARA:1+...)
            // always lists its components: without them the separator behind the tag would be lost.
            if (!element.isEmpty() || element.index == 0) {
                int number = 1;
                for (String component : element.components) {
                    String componentName = elementName + "." + number++;
                    handler.startElement("", componentName, "", getEmptyAttributes());
                    if (!component.isEmpty()) {
                        handler.characters(component.toCharArray(), 0, component.length());
                    }
                    handler.endElement("", componentName, "");
                }
            }

            handler.endElement("", elementName, "");
        }

        handler.endElement("", segment.tag, "");
    }

    private static String pad(int index) {
        return index < 10 ? "0" + index : String.valueOf(index);
    }
}
