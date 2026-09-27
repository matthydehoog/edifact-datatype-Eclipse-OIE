/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */

package com.mirth.connect.plugins.datatypes.edifact.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.mirth.connect.model.codetemplates.CodeTemplate;
import com.mirth.connect.model.codetemplates.CodeTemplateContextSet;
import com.mirth.connect.model.codetemplates.CodeTemplateProperties.CodeTemplateType;

/**
 * The code templates of the EDIFACT category in the reference list. The code of each template is a
 * resource in templates/ next to this class, so the tests can run exactly what the user gets.
 */
public final class EDIFACTCodeTemplates {

    public static final String CATEGORY = "EDIFACT";

    /** Name, resource, uses msg (connector scripts only), description. */
    private static final String[][] TEMPLATES = {
            { "Build an interchange envelope", "envelope.js", "false", "Builds an EDIFACT interchange (UNA, UNB, UNH ... UNT, UNZ) around message segments, with the references and counters filled in." },
            { "Walk the line items (LIN with QTY, PRI, IMD)", "line-items.js", "true", "Collects every line item (LIN) with the quantities (QTY), prices (PRI) and description (IMD) that belong to it." },
            { "Find a party by qualifier (NAD)", "find-party.js", "true", "Finds the name and address segment (NAD) of a party by its qualifier, such as BY (buyer), SU (supplier) or DP (delivery party)." },
            { "Read MEDLAB results (BEP)", "medlab-results.js", "true", "Collects every test result (BEP) of a MEDLAB laboratory message with its section, value, unit, reference range and remarks." },
            { "Escape and unescape text", "escape.js", "false", "Functions that escape text with the release character (?+ ?: ?' ??) for a raw EDIFACT message, and undo that." } };

    private EDIFACTCodeTemplates() {}

    public static List<CodeTemplate> getCodeTemplates() {
        List<CodeTemplate> templates = new ArrayList<CodeTemplate>();
        for (String[] t : TEMPLATES) {
            CodeTemplateContextSet context = Boolean.parseBoolean(t[2]) ? CodeTemplateContextSet.getConnectorContextSet() : CodeTemplateContextSet.getGlobalContextSet();
            templates.add(new CodeTemplate(t[0], CodeTemplateType.DRAG_AND_DROP_CODE, context, getCode(t[1]), t[3]));
        }
        return templates;
    }

    /** The code of a template, by its resource name (envelope.js, ...). */
    public static String getCode(String resource) {
        try (InputStream in = EDIFACTCodeTemplates.class.getResourceAsStream("templates/" + resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing code template " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").trim();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read code template " + resource, e);
        }
    }
}
