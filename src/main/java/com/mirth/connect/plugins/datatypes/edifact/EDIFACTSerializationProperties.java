/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.mirth.connect.donkey.util.DonkeyElement;
import com.mirth.connect.model.datatype.DataTypePropertyDescriptor;
import com.mirth.connect.model.datatype.PropertyEditorType;
import com.mirth.connect.model.datatype.SerializationProperties;

/**
 * The delimiters used when the message has no UNA (or when inferring is switched off), and the ones
 * used when a message is built from XML that does not carry its own.
 */
public class EDIFACTSerializationProperties extends SerializationProperties {
    private String segmentDelimiter = "'";
    private String elementDelimiter = "+";
    private String subelementDelimiter = ":";
    private String releaseCharacter = "?";
    private String decimalMark = ".";
    private String repetitionSeparator = "";
    private boolean inferDelimiters = true;

    public EDIFACTSerializationProperties() {

    }

    public EDIFACTSerializationProperties(EDIFACTSerializationProperties properties) {
        this.segmentDelimiter = properties.getSegmentDelimiter();
        this.elementDelimiter = properties.getElementDelimiter();
        this.subelementDelimiter = properties.getSubelementDelimiter();
        this.releaseCharacter = properties.getReleaseCharacter();
        this.decimalMark = properties.getDecimalMark();
        this.repetitionSeparator = properties.getRepetitionSeparator();
        this.inferDelimiters = properties.isInferDelimiters();
    }

    @Override
    public Map<String, DataTypePropertyDescriptor> getPropertyDescriptors() {
        Map<String, DataTypePropertyDescriptor> properties = new LinkedHashMap<String, DataTypePropertyDescriptor>();

        properties.put("segmentDelimiter", new DataTypePropertyDescriptor(segmentDelimiter, "Segment Delimiter", "Segment terminator of the message. The first character is the terminator; further characters (for example \\n) are only written after each segment when converting XML to EDIFACT.", PropertyEditorType.STRING));
        properties.put("elementDelimiter", new DataTypePropertyDescriptor(elementDelimiter, "Element Delimiter", "Character that separates the data elements of a segment.", PropertyEditorType.STRING));
        properties.put("subelementDelimiter", new DataTypePropertyDescriptor(subelementDelimiter, "Component Delimiter", "Character that separates the components of a composite data element.", PropertyEditorType.STRING));
        properties.put("releaseCharacter", new DataTypePropertyDescriptor(releaseCharacter, "Release Character", "Character that makes the next character plain data instead of a delimiter, for example ?+ for a plus sign.", PropertyEditorType.STRING));
        properties.put("decimalMark", new DataTypePropertyDescriptor(decimalMark, "Decimal Mark", "Decimal mark of the message. Only used to write the UNA segment.", PropertyEditorType.STRING));
        properties.put("repetitionSeparator", new DataTypePropertyDescriptor(repetitionSeparator, "Repetition Separator", "Character that separates repeated data elements (EDIFACT syntax version 4, for example *). Leave empty when the message does not use repetition.", PropertyEditorType.STRING));
        properties.put("inferDelimiters", new DataTypePropertyDescriptor(inferDelimiters, "Infer Delimiters", "If checked and the message starts with a UNA segment, the delimiters are read from the UNA and the delimiter properties above are not used.", PropertyEditorType.BOOLEAN));

        return properties;
    }

    @Override
    public void setProperties(Map<String, Object> properties) {
        if (properties != null) {
            if (properties.get("segmentDelimiter") != null) {
                this.segmentDelimiter = (String) properties.get("segmentDelimiter");
            }
            if (properties.get("elementDelimiter") != null) {
                this.elementDelimiter = (String) properties.get("elementDelimiter");
            }
            if (properties.get("subelementDelimiter") != null) {
                this.subelementDelimiter = (String) properties.get("subelementDelimiter");
            }
            if (properties.get("releaseCharacter") != null) {
                this.releaseCharacter = (String) properties.get("releaseCharacter");
            }
            if (properties.get("decimalMark") != null) {
                this.decimalMark = (String) properties.get("decimalMark");
            }
            if (properties.get("repetitionSeparator") != null) {
                this.repetitionSeparator = (String) properties.get("repetitionSeparator");
            }
            if (properties.get("inferDelimiters") != null) {
                this.inferDelimiters = ((Boolean) properties.get("inferDelimiters"));
            }
        }
    }

    // The engine does not run this constructor when it reads a saved channel, so a property
    // that was added later can be null; the getters fall back to the defaults.
    public String getSegmentDelimiter() {
        return segmentDelimiter == null ? "'" : segmentDelimiter;
    }

    public void setSegmentDelimiter(String segmentDelimiter) {
        this.segmentDelimiter = segmentDelimiter;
    }

    public String getElementDelimiter() {
        return elementDelimiter == null ? "+" : elementDelimiter;
    }

    public void setElementDelimiter(String elementDelimiter) {
        this.elementDelimiter = elementDelimiter;
    }

    public String getSubelementDelimiter() {
        return subelementDelimiter == null ? ":" : subelementDelimiter;
    }

    public void setSubelementDelimiter(String subelementDelimiter) {
        this.subelementDelimiter = subelementDelimiter;
    }

    public String getReleaseCharacter() {
        return releaseCharacter == null ? "?" : releaseCharacter;
    }

    public void setReleaseCharacter(String releaseCharacter) {
        this.releaseCharacter = releaseCharacter;
    }

    public String getDecimalMark() {
        return decimalMark == null ? "." : decimalMark;
    }

    public void setDecimalMark(String decimalMark) {
        this.decimalMark = decimalMark;
    }

    public String getRepetitionSeparator() {
        return repetitionSeparator == null ? "" : repetitionSeparator;
    }

    public void setRepetitionSeparator(String repetitionSeparator) {
        this.repetitionSeparator = repetitionSeparator;
    }

    public boolean isInferDelimiters() {
        return inferDelimiters;
    }

    public void setInferDelimiters(boolean inferDelimiters) {
        this.inferDelimiters = inferDelimiters;
    }

    // @formatter:off
    @Override public void migrate3_0_1(DonkeyElement element) {}
    @Override public void migrate3_0_2(DonkeyElement element) {}
    @Override public void migrate3_1_0(DonkeyElement element) {}
    @Override public void migrate3_2_0(DonkeyElement element) {}
    @Override public void migrate3_3_0(DonkeyElement element) {}
    @Override public void migrate3_4_0(DonkeyElement element) {}
    @Override public void migrate3_5_0(DonkeyElement element) {}
    @Override public void migrate3_6_0(DonkeyElement element) {}
    @Override public void migrate3_7_0(DonkeyElement element) {}
    @Override public void migrate3_9_0(DonkeyElement element) {}
    @Override public void migrate3_11_0(DonkeyElement element) {}
    @Override public void migrate3_11_1(DonkeyElement element) {}
    @Override public void migrate3_12_0(DonkeyElement element) {}
    // @formatter:on

    @Override
    public Map<String, Object> getPurgedProperties() {
        Map<String, Object> purgedProperties = new HashMap<String, Object>();
        purgedProperties.put("inferDelimiters", inferDelimiters);
        return purgedProperties;
    }
}
