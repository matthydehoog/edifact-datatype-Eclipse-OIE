/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */

package com.mirth.connect.plugins.datatypes.edifact.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.mirth.connect.model.codetemplates.CodeTemplate;
import com.mirth.connect.plugins.datatypes.edifact.EDIFACTDataTypeProperties;
import com.mirth.connect.plugins.datatypes.edifact.EDIFACTSerializer;

/** Runs the code of the EDIFACT code templates in Rhino (with E4X), as a transformer would. */
public class EDIFACTCodeTemplatesTest {

    private static final String ORDERS = "UNA:+.? '\n"
            + "UNB+UNOC:3+SENDER:ZZ+RECEIVER:ZZ+230115:1200+REF001'\n"
            + "UNH+1+ORDERS:D:96A:UN:EAN008'\n"
            + "BGM+220+PO12345+9'\n"
            + "NAD+BY+5412345000013::9'\n"
            + "NAD+SU+++Acme ?+ Co+Main Street 1+Amsterdam++1011AB+NL'\n"
            + "LIN+1++4000862141404:EN'\n"
            + "IMD+F++:::Blue widget'\n"
            + "QTY+21:10'\n"
            + "PRI+AAA:2.50'\n"
            + "LIN+2++4000862141411:EN'\n"
            + "QTY+21:3'\n"
            + "QTY+59:1'\n"
            + "UNS+S'\n"
            + "CNT+2:2'\n"
            + "UNT+14+1'\n"
            + "UNZ+1+REF001'";

    private static String file(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static String toXml(String edifact) throws Exception {
        return new EDIFACTSerializer(new EDIFACTDataTypeProperties().getSerializerProperties()).toXML(edifact);
    }

    /** Runs a template with msg set to the message, then evaluates the expression. */
    private static String run(String resource, String edifact, String expression) throws Exception {
        for (int version : new int[] { Context.VERSION_1_8, Context.VERSION_ES6 }) {
            Context cx = Context.enter();
            try {
                cx.setLanguageVersion(version);
                Scriptable scope = cx.initStandardObjects();
                scope.put("xmlText", scope, edifact == null ? "" : toXml(edifact));
                cx.evaluateString(scope, "var msg = xmlText == '' ? null : new XML(xmlText);", "setup", 1, null);
                cx.evaluateString(scope, EDIFACTCodeTemplates.getCode(resource), resource, 1, null);
                String result = Context.toString(cx.evaluateString(scope, expression, "check", 1, null));
                if (version == Context.VERSION_ES6) {
                    return result;
                }
            } finally {
                Context.exit();
            }
        }
        throw new IllegalStateException();
    }

    @Test
    public void everyTemplateIsListed() {
        List<CodeTemplate> templates = EDIFACTCodeTemplates.getCodeTemplates();
        assertEquals(5, templates.size());
        for (CodeTemplate template : templates) {
            assertFalse(template.getName(), template.getCode().isEmpty());
            assertFalse(template.getName(), template.getDescription().isEmpty());
        }
    }

    @Test
    public void envelopeBuildsAValidInterchange() throws Exception {
        String edifact = run("envelope.js", null, "edifact");

        assertTrue(edifact.startsWith("UNA:+.? '\nUNB+UNOC:3+SENDER:ZZ+RECIPIENT:ZZ+"));
        assertTrue(edifact, edifact.contains("'\nUNH+1+ORDERS:D:96A:UN'\nBGM+220+PO12345+9'\nDTM+137:20230115:102'\nUNT+4+1'\nUNZ+1+"));
        assertTrue(edifact.endsWith("'"));
        // the data type reads it back
        assertTrue(toXml(edifact).contains("<UNT.01.1>4</UNT.01.1>"));
    }

    @Test
    public void lineItemsCollectTheirSegments() throws Exception {
        assertEquals("2", run("line-items.js", ORDERS, "lineItems.length"));
        assertEquals("4000862141404|10|2.50|Blue widget", run("line-items.js", ORDERS,
                "var l = lineItems[0]; [l.item, l.quantities['21'], l.prices['AAA'], l.description].join('|')"));
        assertEquals("4000862141411|3|1|0", run("line-items.js", ORDERS,
                "var l = lineItems[1]; [l.item, l.quantities['21'], l.quantities['59'], Object.keys(l.prices).length].join('|')"));
    }

    @Test
    public void findPartyByQualifier() throws Exception {
        assertEquals("5412345000013", run("find-party.js", ORDERS, "partyId"));
        assertEquals("Acme + Co", run("find-party.js", ORDERS, "findParty('SU')['NAD.04']['NAD.04.1'].toString()"));
        assertEquals("true", run("find-party.js", ORDERS, "findParty('DP') == null"));
    }

    @Test
    public void medlabResults() throws Exception {
        String medlab = file("examples/medlab.edi");

        assertEquals("4", run("medlab-results.js", medlab, "results.length"));
        assertEquals("MEETWAARDEN|Quet/BMI|23.1|kg/m2|10|50|QUET|false", run("medlab-results.js", medlab,
                "var r = results[2]; [r.section, r.name, r.value, r.unit, r.low, r.high, r.code, r.changed].join('|')"));

        String withRemark = medlab.replace("BEP:1:1:4+", "OPB+nuchter afgenomen'\nBEP:1:1:4+").replace("UNT+16", "UNT+17");
        assertEquals("nuchter afgenomen", run("medlab-results.js", withRemark, "results[2].remarks.join()"));
        assertEquals("0", run("medlab-results.js", withRemark, "results[3].remarks.length"));
    }

    @Test
    public void escapeAndUnescape() throws Exception {
        assertEquals("Acme ?+ Co?: it??s?'", run("escape.js", null, "edifactEscape(\"Acme + Co: it?s'\")"));
        assertEquals("Acme + Co: it?s'", run("escape.js", null, "edifactUnescape(edifactEscape(\"Acme + Co: it?s'\"))"));
    }
}
