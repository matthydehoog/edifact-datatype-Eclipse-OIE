/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.mirth.connect.donkey.model.message.MessageSerializer;
import com.mirth.connect.donkey.model.message.MessageSerializerException;
import com.mirth.connect.model.converters.IMessageSerializer;
import com.mirth.connect.model.converters.XMLPrettyPrinter;
import com.mirth.connect.model.datatype.SerializerProperties;
import com.mirth.connect.model.util.DefaultMetaData;
import com.mirth.connect.plugins.datatypes.edifact.EDIFACTSegmentParser.Segment;
import com.mirth.connect.util.ErrorMessageBuilder;

public class EDIFACTSerializer implements IMessageSerializer {
    private Logger logger = LogManager.getLogger(this.getClass());

    private final EDIFACTSerializationProperties serializationProperties;
    private final EDIFACTDelimiters defaultDelimiters;

    // Removes the whitespace between tags of pretty-printed XML, like the EDI/X12 data type does.
    private static Pattern prettyPattern1 = Pattern.compile("\\s*<([^/][^>]*)>");
    private static Pattern prettyPattern2 = Pattern.compile("<([^>]*/|/[^>]*)>\\s*");

    public EDIFACTSerializer(SerializerProperties properties) {
        EDIFACTSerializationProperties p = null;
        if (properties != null) {
            p = (EDIFACTSerializationProperties) properties.getSerializationProperties();
        }
        serializationProperties = p != null ? p : new EDIFACTSerializationProperties();
        defaultDelimiters = EDIFACTDelimiters.fromProperties(serializationProperties);
    }

    @Override
    public boolean isSerializationRequired(boolean toXml) {
        return false;
    }

    @Override
    public String transformWithoutSerializing(String message, MessageSerializer outboundSerializer) throws MessageSerializerException {
        return null;
    }

    @Override
    public String fromXML(String source) throws MessageSerializerException {
        XMLReader xr;
        try {
            xr = XMLReaderFactory.createXMLReader();
            xr.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (SAXException e) {
            throw new MessageSerializerException("Error converting XML to EDIFACT", e, ErrorMessageBuilder.buildErrorMessage(this.getClass().getSimpleName(), "Error converting XML to EDIFACT", e));
        }

        EDIFACTXMLHandler handler = new EDIFACTXMLHandler(defaultDelimiters);
        xr.setContentHandler(handler);
        xr.setErrorHandler(handler);

        try {
            xr.parse(new InputSource(new StringReader(prettyPattern2.matcher(prettyPattern1.matcher(source).replaceAll("<$1>")).replaceAll("<$1>"))));
        } catch (Exception e) {
            throw new MessageSerializerException("Error converting XML to EDIFACT", e, ErrorMessageBuilder.buildErrorMessage(this.getClass().getSimpleName(), "Error converting XML to EDIFACT", e));
        }

        return handler.getOutput();
    }

    @Override
    public String toXML(String source) throws MessageSerializerException {
        try {
            EDIFACTReader reader = new EDIFACTReader(defaultDelimiters, serializationProperties.isInferDelimiters());
            StringWriter stringWriter = new StringWriter();
            XMLPrettyPrinter serializer = new XMLPrettyPrinter(stringWriter);
            serializer.setEncodeEntities(true);
            reader.setContentHandler(serializer);
            reader.parse(new InputSource(new StringReader(source)));
            return stringWriter.toString();
        } catch (Exception e) {
            throw new MessageSerializerException("Error converting EDIFACT to XML", e, ErrorMessageBuilder.buildErrorMessage(this.getClass().getSimpleName(), "Error converting EDIFACT to XML", e));
        }
    }

    @Override
    public Map<String, Object> getMetaDataFromMessage(String message) {
        Map<String, Object> map = new HashMap<String, Object>();
        populateMetaData(message, map);
        return map;
    }

    /**
     * Source is the sender of the interchange (UNB, S002 sender identification), type the message type
     * (UNH, S009 message identifier, for example ORDERS) and version the message version and release
     * number (for example D96A).
     */
    @Override
    public void populateMetaData(String message, Map<String, Object> map) {
        try {
            EDIFACTDelimiters d = new EDIFACTDelimiters(defaultDelimiters);
            d.una = false;
            int start = d.readUna(message, serializationProperties.isInferDelimiters());
            EDIFACTSegmentParser parser = new EDIFACTSegmentParser(message, start, d);

            String source = null;
            String type = null;
            String version = null;
            Segment segment;

            while ((segment = parser.next()) != null) {
                if (source == null && segment.tag.equals("UNB")) {
                    source = segment.component(2, 1);
                } else if (segment.tag.equals("UNH")) {
                    type = segment.component(2, 1);
                    String versionNumber = segment.component(2, 2);
                    String releaseNumber = segment.component(2, 3);
                    if (versionNumber != null || releaseNumber != null) {
                        version = (versionNumber != null ? versionNumber : "") + (releaseNumber != null ? releaseNumber : "");
                    }
                    // The first message header is enough.
                    break;
                }
            }

            if (source != null && !source.isEmpty()) {
                map.put(DefaultMetaData.SOURCE_VARIABLE_MAPPING, source);
            }
            if (type != null && !type.isEmpty()) {
                map.put(DefaultMetaData.TYPE_VARIABLE_MAPPING, type);
            }
            if (version != null && !version.isEmpty()) {
                map.put(DefaultMetaData.VERSION_VARIABLE_MAPPING, version);
            }
        } catch (Exception e) {
            logger.warn("Error populating EDIFACT metadata: " + e.getMessage());
        }
    }

    @Override
    public String toJSON(String message) throws MessageSerializerException {
        return null;
    }

    @Override
    public String fromJSON(String message) throws MessageSerializerException {
        return null;
    }
}
