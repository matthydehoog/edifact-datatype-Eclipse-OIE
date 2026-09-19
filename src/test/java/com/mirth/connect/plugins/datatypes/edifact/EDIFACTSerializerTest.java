package com.mirth.connect.plugins.datatypes.edifact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.mirth.connect.donkey.model.message.MessageSerializerException;
import com.mirth.connect.model.datatype.SerializerProperties;
import com.mirth.connect.model.util.DefaultMetaData;

public class EDIFACTSerializerTest {

    static final String ORDERS = "UNA:+.? '" + "UNB+UNOC:3+SENDER:ZZ+RECEIVER:ZZ+230115:1200+REF001'" + "UNH+1+ORDERS:D:96A:UN:EAN008'" + "BGM+220+PO12345+9'" + "DTM+137:20230115:102'" + "NAD+BY+5412345000013::9'" + "NAD+SU+++Acme ?+ Co+Main Street 1+Amsterdam++1011AB+NL'" + "LIN+1++4000862141404:EN'" + "QTY+21:10'" + "FTX+AAI+++It?'s a test??'" + "UNS+S'" + "UNT+11+1'" + "UNZ+1+REF001'";

    private static EDIFACTSerializer serializer() {
        return serializer(new EDIFACTSerializationProperties());
    }

    private static EDIFACTSerializer serializer(EDIFACTSerializationProperties p) {
        SerializerProperties serializerProperties = new EDIFACTDataTypeProperties().getSerializerProperties();
        serializerProperties.setSerializationProperties(p);
        return new EDIFACTSerializer(serializerProperties);
    }

    private static Document dom(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

    private static String xpath(Document doc, String expression) throws Exception {
        return XPathFactory.newInstance().newXPath().evaluate(expression, doc);
    }

    private static int count(Document doc, String expression) throws Exception {
        return ((Number) XPathFactory.newInstance().newXPath().evaluate("count(" + expression + ")", doc, javax.xml.xpath.XPathConstants.NUMBER)).intValue();
    }

    private static String roundTrip(String message) throws Exception {
        EDIFACTSerializer s = serializer();
        return s.fromXML(s.toXML(message));
    }

    // ---- EDIFACT -> XML

    @Test
    public void toXmlUsesEdiStyleNames() throws Exception {
        Document doc = dom(serializer().toXML(ORDERS));

        assertEquals("EDIFACTInterchange", doc.getDocumentElement().getNodeName());
        assertEquals("ORDERS", xpath(doc, "/EDIFACTInterchange/UNH/UNH.02/UNH.02.1"));
        assertEquals("D", xpath(doc, "/EDIFACTInterchange/UNH/UNH.02/UNH.02.2"));
        assertEquals("96A", xpath(doc, "/EDIFACTInterchange/UNH/UNH.02/UNH.02.3"));
        assertEquals("SENDER", xpath(doc, "/EDIFACTInterchange/UNB/UNB.02/UNB.02.1"));
        assertEquals("220", xpath(doc, "/EDIFACTInterchange/BGM/BGM.01/BGM.01.1"));
        assertEquals("20230115", xpath(doc, "/EDIFACTInterchange/DTM/DTM.01/DTM.01.2"));
        assertEquals(12, count(doc, "/EDIFACTInterchange/*"));
    }

    @Test
    public void rootCarriesTheServiceCharacters() throws Exception {
        Document doc = dom(serializer().toXML(ORDERS));
        org.w3c.dom.Element root = doc.getDocumentElement();

        assertEquals("'", root.getAttribute("segmentDelimiter"));
        assertEquals("+", root.getAttribute("elementDelimiter"));
        assertEquals(":", root.getAttribute("subelementDelimiter"));
        assertEquals("?", root.getAttribute("releaseCharacter"));
        assertEquals(".", root.getAttribute("decimalMark"));
        assertEquals("", root.getAttribute("repetitionSeparator"));
        assertEquals("true", root.getAttribute("una"));
    }

    @Test
    public void releaseCharacterEscapesAreResolved() throws Exception {
        Document doc = dom(serializer().toXML(ORDERS));

        // NAD+SU+++Acme ?+ Co+...  and  FTX+AAI+++It?'s a test??'
        assertEquals("Acme + Co", xpath(doc, "/EDIFACTInterchange/NAD[2]/NAD.04/NAD.04.1"));
        assertEquals("It's a test?", xpath(doc, "/EDIFACTInterchange/FTX/FTX.04/FTX.04.1"));
    }

    @Test
    public void emptyElementsStayEmptyAndKeepTheirPosition() throws Exception {
        Document doc = dom(serializer().toXML(ORDERS));

        // NAD+SU+++Acme ?+ Co+Main Street 1+Amsterdam++1011AB+NL'
        assertEquals(1, count(doc, "/EDIFACTInterchange/NAD[2]/NAD.02"));
        assertEquals(0, count(doc, "/EDIFACTInterchange/NAD[2]/NAD.02/*"));
        assertEquals(0, count(doc, "/EDIFACTInterchange/NAD[2]/NAD.03/*"));
        assertEquals("Amsterdam", xpath(doc, "/EDIFACTInterchange/NAD[2]/NAD.06/NAD.06.1"));
        assertEquals(0, count(doc, "/EDIFACTInterchange/NAD[2]/NAD.07/*"));
        assertEquals("1011AB", xpath(doc, "/EDIFACTInterchange/NAD[2]/NAD.08/NAD.08.1"));
        assertEquals("NL", xpath(doc, "/EDIFACTInterchange/NAD[2]/NAD.09/NAD.09.1"));
    }

    @Test
    public void emptyComponentsAreKept() throws Exception {
        Document doc = dom(serializer().toXML(ORDERS));

        // NAD+BY+5412345000013::9'
        assertEquals("5412345000013", xpath(doc, "/EDIFACTInterchange/NAD[1]/NAD.02/NAD.02.1"));
        assertEquals(1, count(doc, "/EDIFACTInterchange/NAD[1]/NAD.02/NAD.02.2"));
        assertEquals("", xpath(doc, "/EDIFACTInterchange/NAD[1]/NAD.02/NAD.02.2"));
        assertEquals("9", xpath(doc, "/EDIFACTInterchange/NAD[1]/NAD.02/NAD.02.3"));
    }

    @Test
    public void xmlSpecialCharactersAreEncoded() throws Exception {
        String xml = serializer().toXML("UNB+UNOA:1+A&B+C+230101:1200+1'FTX+AAA+++<b> & \"q\"'");
        Document doc = dom(xml);

        assertEquals("A&B", xpath(doc, "/EDIFACTInterchange/UNB/UNB.02/UNB.02.1"));
        assertEquals("<b> & \"q\"", xpath(doc, "/EDIFACTInterchange/FTX/FTX.04/FTX.04.1"));
    }

    @Test
    public void newlinesBetweenSegmentsAreIgnored() throws Exception {
        String message = "UNA:+.? '\r\nUNB+UNOC:3+S+R+230101:1200+1'\r\n\r\nUNH+1+ORDERS:D:96A:UN'\nBGM+220+1+9'\r\nUNT+3+1'\nUNZ+1+1'\r\n";
        Document doc = dom(serializer().toXML(message));

        assertEquals(5, count(doc, "/EDIFACTInterchange/*"));
        assertEquals("220", xpath(doc, "/EDIFACTInterchange/BGM/BGM.01/BGM.01.1"));
    }

    @Test
    public void leadingWhitespaceAndByteOrderMarkAreIgnored() throws Exception {
        Document doc = dom(serializer().toXML("﻿  \nUNH+1+ORDERS:D:96A:UN'UNT+2+1'"));

        assertEquals("EDIFACTMessage", doc.getDocumentElement().getNodeName());
        assertEquals("ORDERS", xpath(doc, "/EDIFACTMessage/UNH/UNH.02/UNH.02.1"));
    }

    @Test
    public void lastSegmentMayLackItsTerminator() throws Exception {
        Document doc = dom(serializer().toXML("UNH+1+ORDERS:D:96A:UN'UNT+2+1"));

        assertEquals("1", xpath(doc, "/EDIFACTMessage/UNT/UNT.02/UNT.02.1"));
    }

    @Test
    public void withoutUnaTheStandardDelimitersApply() throws Exception {
        Document doc = dom(serializer().toXML("UNB+UNOA:1+SENDER+RECEIVER+230101:1200+1'UNZ+0+1'"));

        assertEquals("false", doc.getDocumentElement().getAttribute("una"));
        assertEquals("SENDER", xpath(doc, "/EDIFACTInterchange/UNB/UNB.02/UNB.02.1"));
    }

    @Test
    public void unaDefinesTheDelimiters() throws Exception {
        // component :, element *, decimal ',', release #, segment ~
        String message = "UNA:*,# ~UNB*UNOC:3*SEND#*ER*REC*230101:1200*1~FTX*AAA***a#~b#:c~UNZ*0*1~";
        Document doc = dom(serializer().toXML(message));

        assertEquals("~", doc.getDocumentElement().getAttribute("segmentDelimiter"));
        assertEquals("*", doc.getDocumentElement().getAttribute("elementDelimiter"));
        assertEquals(",", doc.getDocumentElement().getAttribute("decimalMark"));
        assertEquals("#", doc.getDocumentElement().getAttribute("releaseCharacter"));
        assertEquals("SEND*ER", xpath(doc, "/EDIFACTInterchange/UNB/UNB.02/UNB.02.1"));
        assertEquals("a~b:c", xpath(doc, "/EDIFACTInterchange/FTX/FTX.04/FTX.04.1"));
    }

    @Test
    public void propertiesApplyWhenInferringIsOffEvenIfThereIsAUna() throws Exception {
        EDIFACTSerializationProperties p = new EDIFACTSerializationProperties();
        p.setInferDelimiters(false);
        p.setElementDelimiter("|");
        p.setSubelementDelimiter("^");
        p.setSegmentDelimiter("~");
        p.setReleaseCharacter("\\");

        // The UNA is skipped, but the properties win.
        Document doc = dom(serializer(p).toXML("UNA:+.? 'UNB|UNOC^3|S\\|R|R|230101^1200|1~UNZ|0|1~"));

        assertEquals("S|R", xpath(doc, "/EDIFACTInterchange/UNB/UNB.02/UNB.02.1"));
        assertEquals("UNOC", xpath(doc, "/EDIFACTInterchange/UNB/UNB.01/UNB.01.1"));
    }

    @Test
    public void repetitionSeparatorSplitsRepeatedElements() throws Exception {
        String message = "UNA:+.?*'UNH+1+ORDERS:D:96A:UN'RFF+ON:1*ON:2*ON:3+X'UNT+3+1'";
        Document doc = dom(serializer().toXML(message));

        assertEquals("*", doc.getDocumentElement().getAttribute("repetitionSeparator"));
        assertEquals(3, count(doc, "/EDIFACTInterchange/RFF/RFF.01"));
        assertEquals("2", xpath(doc, "/EDIFACTInterchange/RFF/RFF.01[2]/RFF.01.2"));
        assertEquals("X", xpath(doc, "/EDIFACTInterchange/RFF/RFF.02/RFF.02.1"));
    }

    @Test
    public void invalidInputIsRejected() {
        String[] invalid = { "", "  ", "MSH|^~\\&|A|B|C|D|20230101||ADT^A01|1|P|2.3", "ISA*00*          *00*          *ZZ*A", "UNH+1+ORDERS?" };
        for (String message : invalid) {
            try {
                serializer().toXML(message);
                fail("Expected an error for: " + message);
            } catch (MessageSerializerException expected) {
                // fine
            }
        }
    }

    @Test
    public void errorMessageNamesTheProblem() {
        try {
            serializer().toXML("MSH|^~\\&|A|B");
            fail();
        } catch (MessageSerializerException e) {
            assertTrue(e.getMessage() + " / " + e.getCause(), String.valueOf(e.getCause().getMessage()).contains("Invalid segment tag"));
        }
    }

    // ---- XML -> EDIFACT

    @Test
    public void roundTripGivesTheSameMessage() throws Exception {
        assertEquals(ORDERS, roundTrip(ORDERS));
    }

    @Test
    public void roundTripWithoutUnaDoesNotAddOne() throws Exception {
        String message = "UNB+UNOA:1+SENDER+RECEIVER+230101:1200+1'UNH+1+ORDERS:D:96A:UN'BGM+220+1+9'UNT+3+1'UNZ+1+1'";
        assertEquals(message, roundTrip(message));
    }

    @Test
    public void roundTripWithCustomDelimiters() throws Exception {
        String message = "UNA:*,# ~UNB*UNOC:3*SEND#*ER*REC*230101:1200*1~FTX*AAA***a#~b#:c#*d~UNZ*0*1~";
        assertEquals(message, roundTrip(message));
    }

    @Test
    public void roundTripWithRepetition() throws Exception {
        String message = "UNA:+.?*'UNH+1+ORDERS:D:96A:UN'RFF+ON:1*ON:2*ON:3+X'UNT+3+1'";
        assertEquals(message, roundTrip(message));
    }

    @Test
    public void roundTripKeepsEmptyElementsAndComponents() throws Exception {
        String message = "UNH+1+ORDERS:D:96A:UN'NAD+BY+123:::+'NAD+SU++:X+++'TST+::'UNT+5+1'";
        assertEquals(message, roundTrip(message));
    }

    @Test
    public void roundTripWithElementNumbersAboveNine() throws Exception {
        String message = "UNH+1+ORDERS:D:96A:UN'TST+1+2+3+4+5+6+7+8+9+10+11+12'UNT+3+1'";
        Document doc = dom(serializer().toXML(message));

        assertEquals("10", xpath(doc, "/EDIFACTMessage/TST/TST.10/TST.10.1"));
        assertEquals(message, roundTrip(message));
    }

    @Test
    public void roundTripEscapesAllServiceCharacters() throws Exception {
        String message = "UNH+1+ORDERS:D:96A:UN'FTX+AAA+++a?+b?:c?'d??e'UNT+3+1'";
        assertEquals(message, roundTrip(message));
    }

    @Test
    public void segmentDelimiterPropertyWithNewlineIsWritten() throws Exception {
        EDIFACTSerializationProperties p = new EDIFACTSerializationProperties();
        p.setSegmentDelimiter("'\\n");
        EDIFACTSerializer s = serializer(p);

        String out = s.fromXML(s.toXML("UNH+1+ORDERS:D:96A:UN'BGM+220+1+9'UNT+3+1'"));

        assertEquals("UNH+1+ORDERS:D:96A:UN'\nBGM+220+1+9'\nUNT+3+1'\n", out);
        // and that output can be read again
        assertEquals(out, s.fromXML(s.toXML(out)));
    }

    @Test
    public void unaIsWrittenWithNewlineWhenTheSegmentDelimiterHasOne() throws Exception {
        EDIFACTSerializationProperties p = new EDIFACTSerializationProperties();
        p.setSegmentDelimiter("'\\n");
        EDIFACTSerializer s = serializer(p);

        String out = s.fromXML(s.toXML(ORDERS));

        assertTrue(out, out.startsWith("UNA:+.? '\nUNB+"));
        assertEquals(out, s.fromXML(s.toXML(out)));
    }

    @Test
    public void handWrittenXmlWithoutAttributesUsesThePropertyDelimiters() throws Exception {
        String xml = "<EDIFACTInterchange><UNH><UNH.01><UNH.01.1>1</UNH.01.1></UNH.01><UNH.02><UNH.02.1>ORDERS</UNH.02.1><UNH.02.2>D</UNH.02.2></UNH.02></UNH></EDIFACTInterchange>";

        assertEquals("UNH+1+ORDERS:D'", serializer().fromXML(xml));
    }

    @Test
    public void nonStandardPropertyDelimitersForceAUna() throws Exception {
        EDIFACTSerializationProperties p = new EDIFACTSerializationProperties();
        p.setElementDelimiter("*");
        p.setSubelementDelimiter(">");
        p.setSegmentDelimiter("~");
        p.setReleaseCharacter("#");
        String xml = "<EDIFACTInterchange><FTX><FTX.01><FTX.01.1>AAA</FTX.01.1></FTX.01><FTX.04><FTX.04.1>a*b</FTX.04.1><FTX.04.2>c</FTX.04.2></FTX.04></FTX></EDIFACTInterchange>";

        assertEquals("UNA>*.# ~FTX*AAA***a#*b>c~", serializer(p).fromXML(xml));
    }

    @Test
    public void unaAttributeAddsAUnaForStandardDelimiters() throws Exception {
        String xml = "<EDIFACTInterchange una=\"true\"><UNH><UNH.01><UNH.01.1>1</UNH.01.1></UNH.01></UNH></EDIFACTInterchange>";

        assertEquals("UNA:+.? 'UNH+1'", serializer().fromXML(xml));
    }

    @Test
    public void textDirectlyInADataElementIsAccepted() throws Exception {
        String xml = "<EDIFACTInterchange><BGM><BGM.01>220</BGM.01><BGM.02>PO?1</BGM.02></BGM></EDIFACTInterchange>";

        assertEquals("BGM+220+PO??1'", serializer().fromXML(xml));
    }

    @Test
    public void componentGapsBecomeEmptyComponents() throws Exception {
        String xml = "<EDIFACTInterchange><DTM><DTM.01><DTM.01.1>137</DTM.01.1><DTM.01.3>102</DTM.01.3></DTM.01></DTM></EDIFACTInterchange>";

        assertEquals("DTM+137::102'", serializer().fromXML(xml));
    }

    @Test
    public void elementGapsBecomeEmptyElements() throws Exception {
        String xml = "<EDIFACTInterchange><NAD><NAD.01><NAD.01.1>BY</NAD.01.1></NAD.01><NAD.04><NAD.04.1>X</NAD.04.1></NAD.04></NAD></EDIFACTInterchange>";

        assertEquals("NAD+BY+++X'", serializer().fromXML(xml));
    }

    @Test
    public void whitespaceBetweenTagsInPrettyPrintedXmlIsIgnored() throws Exception {
        String xml = "<EDIFACTInterchange>\n  <UNH>\n    <UNH.01>\n      <UNH.01.1>1</UNH.01.1>\n    </UNH.01>\n  </UNH>\n</EDIFACTInterchange>\n";

        assertEquals("UNH+1'", serializer().fromXML(xml));
    }

    @Test
    public void outOfOrderElementsAreRejected() {
        String xml = "<EDIFACTInterchange><BGM><BGM.02><BGM.02.1>1</BGM.02.1></BGM.02><BGM.01><BGM.01.1>220</BGM.01.1></BGM.01></BGM></EDIFACTInterchange>";
        try {
            serializer().fromXML(xml);
            fail();
        } catch (MessageSerializerException expected) {
            // fine
        }
    }

    @Test
    public void duplicateElementsWithoutRepetitionAreRejected() {
        String xml = "<EDIFACTInterchange><BGM><BGM.01><BGM.01.1>220</BGM.01.1></BGM.01><BGM.01><BGM.01.1>1</BGM.01.1></BGM.01></BGM></EDIFACTInterchange>";
        try {
            serializer().fromXML(xml);
            fail();
        } catch (MessageSerializerException expected) {
            // fine
        }
    }

    @Test
    public void unexpectedElementNamesAreRejected() {
        String xml = "<EDIFACTInterchange><BGM><oops/></BGM></EDIFACTInterchange>";
        try {
            serializer().fromXML(xml);
            fail();
        } catch (MessageSerializerException expected) {
            // fine
        }
    }

    @Test
    public void doctypeIsRejected() {
        String xml = "<!DOCTYPE x [<!ENTITY e \"boom\">]><EDIFACTInterchange><BGM><BGM.01><BGM.01.1>&e;</BGM.01.1></BGM.01></BGM></EDIFACTInterchange>";
        try {
            serializer().fromXML(xml);
            fail();
        } catch (MessageSerializerException expected) {
            // fine
        }
    }

    // ---- metadata

    @Test
    public void metadataComesFromUnbAndUnh() {
        Map<String, Object> map = serializer().getMetaDataFromMessage(ORDERS);

        assertEquals("SENDER", map.get(DefaultMetaData.SOURCE_VARIABLE_MAPPING));
        assertEquals("ORDERS", map.get(DefaultMetaData.TYPE_VARIABLE_MAPPING));
        assertEquals("D96A", map.get(DefaultMetaData.VERSION_VARIABLE_MAPPING));
    }

    @Test
    public void metadataOfAMessageWithoutEnvelope() {
        Map<String, Object> map = new HashMap<String, Object>();
        serializer().populateMetaData("UNH+1+INVOIC:D:01B:UN'UNT+2+1'", map);

        assertNull(map.get(DefaultMetaData.SOURCE_VARIABLE_MAPPING));
        assertEquals("INVOIC", map.get(DefaultMetaData.TYPE_VARIABLE_MAPPING));
        assertEquals("D01B", map.get(DefaultMetaData.VERSION_VARIABLE_MAPPING));
    }

    @Test
    public void metadataOfGarbageIsEmptyAndDoesNotThrow() {
        assertTrue(serializer().getMetaDataFromMessage("this is not EDIFACT").isEmpty());
        assertTrue(serializer().getMetaDataFromMessage("").isEmpty());
    }

    @Test
    public void metadataUsesTheDelimitersOfTheUna() {
        Map<String, Object> map = serializer().getMetaDataFromMessage("UNA:*,# ~UNB*UNOC:3*SND:ZZ*REC*230101:1200*1~UNH*1*ORDERS:D:96A:UN~");

        assertEquals("SND", map.get(DefaultMetaData.SOURCE_VARIABLE_MAPPING));
        assertEquals("ORDERS", map.get(DefaultMetaData.TYPE_VARIABLE_MAPPING));
    }

    // ---- the rest of the data type

    @Test
    public void serializationIsNeverRequiredWithoutXml() {
        assertFalse(serializer().isSerializationRequired(true));
        assertFalse(serializer().isSerializationRequired(false));
    }

    @Test
    public void delegateDescribesTheDataType() {
        EDIFACTDataTypeDelegate delegate = new EDIFACTDataTypeDelegate();

        assertEquals("EDIFACT", delegate.getName());
        assertFalse(delegate.isBinary());
        assertTrue(delegate.getDefaultProperties() instanceof EDIFACTDataTypeProperties);
        assertTrue(delegate.getSerializer(delegate.getDefaultProperties().getSerializerProperties()) instanceof EDIFACTSerializer);
    }

    @Test
    public void propertiesRoundTripThroughTheirDescriptors() {
        EDIFACTSerializationProperties p = new EDIFACTSerializationProperties();
        assertEquals(7, p.getPropertyDescriptors().size());

        Map<String, Object> values = new HashMap<String, Object>();
        values.put("segmentDelimiter", "~");
        values.put("elementDelimiter", "*");
        values.put("repetitionSeparator", "^");
        values.put("inferDelimiters", Boolean.FALSE);
        p.setProperties(values);

        assertEquals("~", p.getSegmentDelimiter());
        assertEquals("*", p.getElementDelimiter());
        assertEquals("^", p.getRepetitionSeparator());
        assertFalse(p.isInferDelimiters());
        assertEquals(":", p.getSubelementDelimiter());

        EDIFACTSerializationProperties copy = new EDIFACTSerializationProperties(p);
        assertEquals("~", copy.getSegmentDelimiter());
        assertEquals("^", copy.getRepetitionSeparator());
    }

    @Test
    public void vocabularyDescribesEnvelopeAndCommonSegments() {
        EDIFACTVocabulary v = new EDIFACTVocabulary("", "");

        assertEquals("Message header", v.getDescription("UNH"));
        assertEquals("Message identifier", v.getDescription("UNH.02"));
        assertEquals("Message type", v.getDescription("UNH.02.1"));
        assertEquals("Sender identification", v.getDescription("UNB.02.1"));
        assertEquals("Party function code qualifier", v.getDescription("NAD.01"));
        assertEquals("", v.getDescription("XYZ.99"));
        assertEquals("EDIFACT", v.getDataType());
    }
}
