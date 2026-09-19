/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.mirth.connect.donkey.model.message.MessageSerializerException;
import com.mirth.connect.model.util.DefaultMetaData;

/** Dutch MEDLAB messages: components straight after the segment tag (ARA:1+..., BEP:1:1:1+...). */
public class EDIFACTDialectTest {

    private static EDIFACTSerializer serializer() {
        return new EDIFACTSerializer(new EDIFACTDataTypeProperties().getSerializerProperties());
    }

    private static Document dom(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String xpath(Document doc, String expression) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(expression, doc);
    }

    private static int count(Document doc, String expression) throws Exception {
        return ((Number) XPathFactory.newInstance().newXPath().evaluate("count(" + expression + ")", doc, XPathConstants.NUMBER)).intValue();
    }

    private static String medlab() throws Exception {
        return new String(Files.readAllBytes(Paths.get("examples/medlab.edi")), StandardCharsets.UTF_8);
    }

    private static String roundTrip(String message) throws Exception {
        EDIFACTSerializer s = serializer();
        return s.fromXML(s.toXML(message));
    }

    @Test
    public void medlabMessageIsConverted() throws Exception {
        Document doc = dom(serializer().toXML(medlab()));

        assertEquals("EDIFACTInterchange", doc.getDocumentElement().getNodeName());
        assertEquals(18, count(doc, "/EDIFACTInterchange/*"));
        assertEquals("MEDLAB", xpath(doc, "/EDIFACTInterchange/UNH/UNH.02/UNH.02.1"));
        assertEquals("Test Patient", xpath(doc, "/EDIFACTInterchange/PID/PID.03/PID.03.1"));
        assertEquals("P.", xpath(doc, "/EDIFACTInterchange/PID/PID.03/PID.03.6"));
    }

    @Test
    public void componentsAfterTheTagBecomeElementZero() throws Exception {
        Document doc = dom(serializer().toXML(medlab()));

        // ARA:1+J. Jansen+000-0000001'
        assertEquals("1", xpath(doc, "/EDIFACTInterchange/ARA/ARA.00/ARA.00.1"));
        assertEquals("J. Jansen", xpath(doc, "/EDIFACTInterchange/ARA/ARA.01/ARA.01.1"));
        assertEquals("000-0000001", xpath(doc, "/EDIFACTInterchange/ARA/ARA.02/ARA.02.1"));

        // SEC:1:1+MEETWAARDEN'
        assertEquals("1", xpath(doc, "/EDIFACTInterchange/SEC/SEC.00/SEC.00.1"));
        assertEquals("1", xpath(doc, "/EDIFACTInterchange/SEC/SEC.00/SEC.00.2"));
        assertEquals("MEETWAARDEN", xpath(doc, "/EDIFACTInterchange/SEC/SEC.01/SEC.01.1"));

        // BEP:1:1:3+0+Quet/BMI+23.1++kg/m2++10+50+QUET'
        assertEquals("3", xpath(doc, "/EDIFACTInterchange/BEP[3]/BEP.00/BEP.00.3"));
        assertEquals("Quet/BMI", xpath(doc, "/EDIFACTInterchange/BEP[3]/BEP.02/BEP.02.1"));
        assertEquals("23.1", xpath(doc, "/EDIFACTInterchange/BEP[3]/BEP.03/BEP.03.1"));
        assertEquals("kg/m2", xpath(doc, "/EDIFACTInterchange/BEP[3]/BEP.05/BEP.05.1"));
        assertEquals("QUET", xpath(doc, "/EDIFACTInterchange/BEP[3]/BEP.09/BEP.09.1"));
    }

    @Test
    public void medlabMessageSurvivesTheRoundTrip() throws Exception {
        String message = medlab();

        assertEquals(message.replaceAll("[\r\n]+", ""), roundTrip(message));
    }

    @Test
    public void medlabMetadata() throws Exception {
        Map<String, Object> map = serializer().getMetaDataFromMessage(medlab());

        assertEquals("500000001", map.get(DefaultMetaData.SOURCE_VARIABLE_MAPPING));
        assertEquals("MEDLAB", map.get(DefaultMetaData.TYPE_VARIABLE_MAPPING));
        assertEquals("1", map.get(DefaultMetaData.VERSION_VARIABLE_MAPPING));
    }

    @Test
    public void elementZeroRoundTripsInAllShapes() throws Exception {
        String[] messages = {
                // only element 0, one component
                "UNH+1+MEDLAB:1'ARA:1'UNT+3+1'",
                // element 0 followed by nothing
                "UNH+1+MEDLAB:1'DET:1+'UNT+3+1'",
                // empty components inside element 0
                "UNH+1+MEDLAB:1'BEP::1+0'BEP:::+0'UNT+4+1'",
                // a separator directly behind the tag, without content
                "UNH+1+MEDLAB:1'ARA:+x'UNT+3+1'",
                // element 0 with several components, elements with gaps behind it
                "UNH+1+MEDLAB:1'BEP:1:1:1+0+gewicht++kg++0'UNT+3+1'" };

        for (String message : messages) {
            assertEquals(message, roundTrip(message));
        }
    }

    @Test
    public void elementZeroKeepsEmptyComponents() throws Exception {
        Document doc = dom(serializer().toXML("UNH+1+MEDLAB:1'BEP::1+0'UNT+3+1'"));

        assertEquals(2, count(doc, "/EDIFACTMessage/BEP/BEP.00/*"));
        assertEquals("", xpath(doc, "/EDIFACTMessage/BEP/BEP.00/BEP.00.1"));
        assertEquals("1", xpath(doc, "/EDIFACTMessage/BEP/BEP.00/BEP.00.2"));
    }

    @Test
    public void handWrittenXmlWithElementZero() throws Exception {
        String xml = "<EDIFACTInterchange><SEC><SEC.00><SEC.00.1>1</SEC.00.1><SEC.00.2>1</SEC.00.2></SEC.00><SEC.01><SEC.01.1>MEETWAARDEN</SEC.01.1></SEC.01></SEC></EDIFACTInterchange>";

        assertEquals("SEC:1:1+MEETWAARDEN'", serializer().fromXML(xml));
    }

    @Test
    public void elementZeroMustComeFirst() {
        String xml = "<EDIFACTInterchange><SEC><SEC.01><SEC.01.1>x</SEC.01.1></SEC.01><SEC.00><SEC.00.1>1</SEC.00.1></SEC.00></SEC></EDIFACTInterchange>";
        try {
            serializer().fromXML(xml);
            fail();
        } catch (MessageSerializerException expected) {
            // fine
        }
    }

    @Test
    public void elementZeroDoesNotCountAsARepetition() throws Exception {
        // With a repetition separator two element-0 occurrences are still an error, not a repetition.
        EDIFACTSerializationProperties p = new EDIFACTSerializationProperties();
        p.setRepetitionSeparator("*");
        com.mirth.connect.model.datatype.SerializerProperties sp = new EDIFACTDataTypeProperties().getSerializerProperties();
        sp.setSerializationProperties(p);
        String xml = "<EDIFACTInterchange><SEC><SEC.00><SEC.00.1>1</SEC.00.1></SEC.00><SEC.00><SEC.00.1>2</SEC.00.1></SEC.00></SEC></EDIFACTInterchange>";
        try {
            new EDIFACTSerializer(sp).fromXML(xml);
            fail();
        } catch (MessageSerializerException expected) {
            // fine
        }
    }

    @Test
    public void garbageAfterTheTagIsStillRejected() {
        String[] invalid = { "AB:1+x'", "ABCD:1+x'", "A B+x'", ":1+x'" };
        for (String message : invalid) {
            try {
                serializer().toXML(message);
                fail("Expected an error for: " + message);
            } catch (MessageSerializerException expected) {
                assertTrue(String.valueOf(expected.getCause().getMessage()).contains("Invalid segment tag"));
            }
        }
    }
}
