/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import com.mirth.connect.util.StringUtil;

/**
 * The service characters of an EDIFACT interchange: the UNA "service string advice" (or the
 * defaults when there is none) plus what the serializer should append after each segment.
 *
 * <pre>
 * UNA:+.? '
 *    | | | | |
 *    | | | | +-- segment terminator
 *    | | | +---- repetition separator (syntax version 4) or a space when unused
 *    | | +------ release character
 *    | +-------- decimal mark
 *    +---------- data element separator, and before it the component separator
 * </pre>
 */
public class EDIFACTDelimiters {
    public static final char DEFAULT_COMPONENT = ':';
    public static final char DEFAULT_ELEMENT = '+';
    public static final char DEFAULT_DECIMAL = '.';
    public static final char DEFAULT_RELEASE = '?';
    public static final char DEFAULT_SEGMENT = '\'';

    /** Marks "no repetition separator". */
    public static final char NONE = '\0';

    public static final String UNA = "UNA";
    private static final int UNA_LENGTH = 9;

    public char component = DEFAULT_COMPONENT;
    public char element = DEFAULT_ELEMENT;
    public char decimal = DEFAULT_DECIMAL;
    public char release = DEFAULT_RELEASE;
    public char repetition = NONE;

    /** Written after every segment; its first character is the segment terminator, any others (a newline) are cosmetic. */
    public String segmentOutput = String.valueOf(DEFAULT_SEGMENT);

    /** True when the message started with a UNA segment. */
    public boolean una = false;

    public EDIFACTDelimiters() {}

    public EDIFACTDelimiters(EDIFACTDelimiters other) {
        component = other.component;
        element = other.element;
        decimal = other.decimal;
        release = other.release;
        repetition = other.repetition;
        segmentOutput = other.segmentOutput;
        una = other.una;
    }

    /** Builds delimiters from the serialization properties (escape sequences such as \n are resolved). */
    public static EDIFACTDelimiters fromProperties(EDIFACTSerializationProperties p) {
        EDIFACTDelimiters d = new EDIFACTDelimiters();
        d.segmentOutput = orDefault(StringUtil.unescape(p.getSegmentDelimiter()), String.valueOf(DEFAULT_SEGMENT));
        d.element = firstChar(StringUtil.unescape(p.getElementDelimiter()), DEFAULT_ELEMENT);
        d.component = firstChar(StringUtil.unescape(p.getSubelementDelimiter()), DEFAULT_COMPONENT);
        d.release = firstChar(StringUtil.unescape(p.getReleaseCharacter()), DEFAULT_RELEASE);
        d.decimal = firstChar(StringUtil.unescape(p.getDecimalMark()), DEFAULT_DECIMAL);
        d.repetition = firstChar(StringUtil.unescape(p.getRepetitionSeparator()), NONE);
        return d;
    }

    /** The terminator character used when reading. */
    public char segment() {
        return segmentOutput.charAt(0);
    }

    public boolean hasRepetition() {
        return repetition != NONE;
    }

    /** True when reading or writing needs no UNA because every service character is the standard one. */
    public boolean isStandard() {
        return component == DEFAULT_COMPONENT && element == DEFAULT_ELEMENT && decimal == DEFAULT_DECIMAL && release == DEFAULT_RELEASE && repetition == NONE && segment() == DEFAULT_SEGMENT;
    }

    /**
     * Skips leading whitespace (and a byte order mark) and, when the message starts with UNA, reads the
     * service characters from it (only when infer is true; otherwise the UNA is just skipped).
     *
     * @return the index of the first character after the UNA, or after the leading whitespace when there is none
     */
    public int readUna(String message, boolean infer) {
        int start = 0;
        while (start < message.length() && (message.charAt(start) <= ' ' || message.charAt(start) == '﻿')) {
            start++;
        }

        if (message.startsWith(UNA, start) && message.length() >= start + UNA_LENGTH) {
            una = true;
            if (infer) {
                component = message.charAt(start + 3);
                element = message.charAt(start + 4);
                decimal = message.charAt(start + 5);
                release = message.charAt(start + 6);
                char reserved = message.charAt(start + 7);
                repetition = reserved == ' ' ? NONE : reserved;
                // Keep any cosmetic characters (a newline) of the configured output; only the terminator itself comes from the UNA.
                String tail = segmentOutput.length() > 1 ? segmentOutput.substring(1) : "";
                segmentOutput = message.charAt(start + 8) + tail;
            }
            return start + UNA_LENGTH;
        }

        return start;
    }

    /** The UNA segment for these delimiters, including the cosmetic characters after the terminator. */
    public String unaString() {
        return UNA + component + element + decimal + release + (hasRepetition() ? repetition : ' ') + segmentOutput;
    }

    private static String orDefault(String value, String def) {
        return value == null || value.isEmpty() ? def : value;
    }

    private static char firstChar(String value, char def) {
        return value == null || value.isEmpty() ? def : value.charAt(0);
    }
}
