/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits an EDIFACT message into segments, data elements and components, resolving the release
 * character. The message is read lazily, one segment per call, so metadata extraction can stop early.
 */
public class EDIFACTSegmentParser {

    /** One occurrence of a data element. Repeated elements (repetition separator) share their index. */
    public static class DataElement {
        public final int index;
        /** The components; a data element without content has a single empty component. */
        public final List<String> components;

        DataElement(int index, List<String> components) {
            this.index = index;
            this.components = components;
        }

        public boolean isEmpty() {
            return components.size() == 1 && components.get(0).isEmpty();
        }
    }

    public static class Segment {
        public final String tag;
        public final List<DataElement> elements = new ArrayList<DataElement>();

        Segment(String tag) {
            this.tag = tag;
        }

        /** The first occurrence of a data element, or null when it is absent. */
        public DataElement element(int index) {
            for (DataElement e : elements) {
                if (e.index == index) {
                    return e;
                }
            }
            return null;
        }

        /** Text of a component (both numbers start at 1), or null when the element or component is absent. */
        public String component(int elementIndex, int componentIndex) {
            DataElement e = element(elementIndex);
            if (e == null || componentIndex < 1 || componentIndex > e.components.size()) {
                return null;
            }
            return e.components.get(componentIndex - 1);
        }
    }

    public static class SyntaxException extends Exception {
        private static final long serialVersionUID = 1L;

        public SyntaxException(String message) {
            super(message);
        }
    }

    private final String message;
    private final EDIFACTDelimiters d;
    private int pos;

    public EDIFACTSegmentParser(String message, int start, EDIFACTDelimiters delimiters) {
        this.message = message;
        this.pos = start;
        this.d = delimiters;
    }

    /** @return the next segment, or null at the end of the message */
    public Segment next() throws SyntaxException {
        // Line breaks (and any other whitespace) between segments are not part of the data.
        while (pos < message.length() && (message.charAt(pos) <= ' ' || message.charAt(pos) == '\uFEFF')) {
            pos++;
        }
        if (pos >= message.length()) {
            return null;
        }

        int tagStart = pos;
        while (pos < message.length() && message.charAt(pos) != d.element && message.charAt(pos) != d.segment()) {
            pos++;
        }
        String tag = message.substring(tagStart, pos);
        if (!isValidTag(tag)) {
            throw new SyntaxException("Invalid segment tag \"" + printable(tag) + "\" at position " + tagStart + ": an EDIFACT segment starts with a tag of three letters or digits");
        }

        Segment segment = new Segment(tag);
        if (pos >= message.length()) {
            return segment;
        }
        if (message.charAt(pos) == d.segment()) {
            pos++;
            return segment;
        }

        // On the element separator that follows the tag: data element 1 starts here.
        pos++;
        int elementIndex = 1;
        List<String> components = new ArrayList<String>();
        StringBuilder current = new StringBuilder();

        while (true) {
            if (pos >= message.length()) {
                // The last segment may lack its terminator.
                components.add(current.toString());
                segment.elements.add(new DataElement(elementIndex, components));
                return segment;
            }

            char c = message.charAt(pos);
            if (c == d.release) {
                if (pos + 1 >= message.length()) {
                    throw new SyntaxException("The message ends with a release character (" + d.release + ") in segment " + tag);
                }
                current.append(message.charAt(pos + 1));
                pos += 2;
            } else if (c == d.component) {
                components.add(current.toString());
                current.setLength(0);
                pos++;
            } else if (c == d.element) {
                components.add(current.toString());
                segment.elements.add(new DataElement(elementIndex, components));
                elementIndex++;
                components = new ArrayList<String>();
                current.setLength(0);
                pos++;
            } else if (d.hasRepetition() && c == d.repetition) {
                components.add(current.toString());
                segment.elements.add(new DataElement(elementIndex, components));
                components = new ArrayList<String>();
                current.setLength(0);
                pos++;
            } else if (c == d.segment()) {
                components.add(current.toString());
                segment.elements.add(new DataElement(elementIndex, components));
                pos++;
                return segment;
            } else {
                current.append(c);
                pos++;
            }
        }
    }

    private static boolean isValidTag(String tag) {
        if (tag.length() != 3) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            char c = tag.charAt(i);
            boolean ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static String printable(String s) {
        String t = s.length() > 20 ? s.substring(0, 20) + "..." : s;
        return t.replace("\r", "\\r").replace("\n", "\\n");
    }
}
