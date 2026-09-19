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
import java.util.Map;

import com.mirth.connect.model.util.MessageVocabulary;

/**
 * Descriptions for the message tree of the administrator: the service segments (UNA/UNB/UNG/UNH/UNT/UNE/UNZ)
 * completely, and the most common message segments (BGM, DTM, NAD, LIN, ...) down to the data elements.
 * Names follow UN/EDIFACT, ISO 9735. Ids look like UNH, UNH.02 and UNH.02.1.
 */
public class EDIFACTVocabulary extends MessageVocabulary {
    private static final Map<String, String> DESCRIPTIONS = new HashMap<String, String>();

    static {
        // Segments without data element descriptions
        String[][] segments = { { "ALC", "Allowance or charge" }, { "ALI", "Additional information" }, { "APR", "Additional price information" }, { "ATT", "Attribute" }, { "BGM", "Beginning of message" }, { "CAV", "Characteristic value" }, { "CCI", "Characteristic/class id" }, { "CNT", "Control total" }, { "COM", "Communication contact" }, { "CPS", "Consignment packing sequence" }, { "CTA", "Contact information" }, { "CUX", "Currencies" }, { "DGS", "Dangerous goods" }, { "DLM", "Delivery limitations" }, { "DOC", "Document/message details" }, { "DTM", "Date/time/period" }, { "EQD", "Equipment details" }, { "FII", "Financial institution information" }, { "FTX", "Free text" }, { "GIN", "Goods identity number" }, { "GIR", "Related identification numbers" }, { "HAN", "Handling instructions" }, { "IMD", "Item description" }, { "LIN", "Line item" }, { "LOC", "Place/location identification" }, { "MEA", "Measurements" }, { "MOA", "Monetary amount" }, { "NAD", "Name and address" }, { "PAC", "Package" }, { "PAI", "Payment instructions" }, { "PAT", "Payment terms basis" }, { "PCD", "Percentage details" }, { "PCI", "Package identification" }, { "PIA", "Additional product id" }, { "PRI", "Price details" }, { "PYT", "Payment terms" }, { "QTY", "Quantity" }, { "RFF", "Reference" }, { "RNG", "Range details" }, { "SEQ", "Sequence details" }, { "TAX", "Duty/tax/fee details" }, { "TDT", "Details of transport" }, { "TOD", "Terms of delivery or transport" }, { "TSR", "Transport service requirements" }, { "UNS", "Section control" }, { "UNA", "Service string advice" },
                { "UNB", "Interchange header" }, { "UNG", "Functional group header" }, { "UNH", "Message header" }, { "UNT", "Message trailer" }, { "UNE", "Functional group trailer" }, { "UNZ", "Interchange trailer" } };
        for (String[] s : segments) {
            DESCRIPTIONS.put(s[0], s[1]);
        }

        // Each entry is "element" or "element|component 1|component 2|...", numbered in order for each segment.
        elements("UNB", "Syntax identifier|Syntax identifier|Syntax version number", "Interchange sender|Sender identification|Partner identification code qualifier|Address for reverse routing", "Interchange recipient|Recipient identification|Partner identification code qualifier|Routing address", "Date and time of preparation|Date|Time", "Interchange control reference", "Recipient's reference/password|Recipient's reference/password|Recipient's reference/password qualifier", "Application reference", "Processing priority code", "Acknowledgement request", "Interchange agreement identifier", "Test indicator");
        elements("UNG", "Functional group identification", "Application sender identification|Application sender identification|Partner identification code qualifier", "Application recipient identification|Application recipient identification|Partner identification code qualifier", "Date and time of preparation|Date|Time", "Functional group reference number", "Controlling agency, coded", "Message version|Message version number|Message release number|Association assigned code", "Application password");
        elements("UNH", "Message reference number", "Message identifier|Message type|Message version number|Message release number|Controlling agency, coded|Association assigned code|Code list directory version number|Message type sub-function identification", "Common access reference", "Status of the transfer|Sequence of transfers|First and last transfer", "Message subset identification", "Message implementation guideline identification", "Scenario identification");
        elements("UNT", "Number of segments in a message", "Message reference number");
        elements("UNE", "Number of messages", "Functional group reference number");
        elements("UNZ", "Interchange control count", "Interchange control reference");
        elements("UNS", "Section identification");

        elements("BGM", "Document/message name|Document name code|Code list identification code|Code list responsible agency code|Document name", "Document/message identification|Document identifier|Version identifier|Revision identifier", "Message function code", "Response type code");
        elements("DTM", "Date/time/period|Date or time or period function code qualifier|Date or time or period text|Date or time or period format code");
        elements("NAD", "Party function code qualifier", "Party identification details|Party identifier|Code list identification code|Code list responsible agency code", "Name and address|Name and address line 1|Name and address line 2|Name and address line 3|Name and address line 4|Name and address line 5", "Party name|Party name 1|Party name 2|Party name 3|Party name 4|Party name 5|Party name format code", "Street|Street and number or post office box 1|Street and number or post office box 2|Street and number or post office box 3|Street and number or post office box 4", "City name", "Country sub-entity identification|Country sub-entity identifier|Code list identification code|Code list responsible agency code|Country sub-entity name", "Postal identification code", "Country identifier");
        elements("LIN", "Line item identifier", "Action request/notification description code", "Item identifier|Item identifier|Item type identification code|Code list identification code|Code list responsible agency code", "Sub-line information|Sub-line indicator code|Sub-line item identifier", "Configuration level number", "Configuration operation code");
        elements("QTY", "Quantity details|Quantity type code qualifier|Quantity|Measurement unit code");
        elements("PRI", "Price information|Price code qualifier|Price amount|Price type code|Price specification code|Unit price basis value|Measurement unit code");
        elements("MOA", "Monetary amount|Monetary amount type code qualifier|Monetary amount|Currency identification code|Currency type code qualifier|Status description code");
        elements("RFF", "Reference|Reference code qualifier|Reference identifier|Document line identifier|Reference version identifier|Revision identifier");
        elements("CUX", "Currency details|Currency usage code qualifier|Currency identification code|Currency type code qualifier|Currency rate", "Currency details|Currency usage code qualifier|Currency identification code|Currency type code qualifier|Currency rate", "Currency exchange rate", "Exchange rate currency market identifier");
        elements("CNT", "Control|Control total type code qualifier|Control total quantity|Measurement unit code");
        elements("CTA", "Contact function code", "Contact details|Contact identifier|Contact name");
        elements("COM", "Communication contact|Communication address identifier|Communication address code qualifier");
        elements("LOC", "Location function code qualifier", "Location identification|Location identifier|Code list identification code|Code list responsible agency code|Location name", "Related location one identification", "Related location two identification", "Relation code");
        elements("IMD", "Description format code", "Item characteristic code", "Item description identification|Item description identifier|Code list identification code|Code list responsible agency code|Item description|Language name code");
        elements("FTX", "Text subject code qualifier", "Free text function code", "Text reference|Free text description code|Code list identification code|Code list responsible agency code", "Text literal|Free text 1|Free text 2|Free text 3|Free text 4|Free text 5", "Language name code", "Free text format code");
        elements("PIA", "Product identifier code qualifier", "Item identification 1|Item identifier|Item type identification code|Code list identification code|Code list responsible agency code", "Item identification 2", "Item identification 3", "Item identification 4", "Item identification 5");
        elements("MEA", "Measurement purpose code qualifier", "Measurement details|Measured attribute code|Measurement significance code|Non-discrete measurement name code|Non-discrete measurement name", "Value/range|Measurement unit code|Measure|Range minimum quantity|Range maximum quantity|Significant digits quantity", "Surface or layer code");
        elements("TAX", "Duty or tax or fee function code qualifier", "Duty or tax or fee type|Duty or tax or fee type name code|Code list identification code|Code list responsible agency code|Duty or tax or fee type name", "Duty or tax or fee account detail|Duty or tax or fee account identifier|Code list identification code|Code list responsible agency code", "Duty or tax or fee assessment basis quantity", "Duty or tax or fee detail|Duty or tax or fee rate code|Code list identification code|Code list responsible agency code|Duty or tax or fee rate|Duty or tax or fee rate basis", "Duty or tax or fee category code", "Party tax identifier", "Calculation sequence code");
    }

    private static void elements(String tag, String... elements) {
        int number = 1;
        for (String element : elements) {
            String[] parts = element.split("\\|");
            String elementId = tag + "." + (number < 10 ? "0" + number : String.valueOf(number));
            DESCRIPTIONS.put(elementId, parts[0]);
            for (int i = 1; i < parts.length; i++) {
                DESCRIPTIONS.put(elementId + "." + i, parts[i]);
            }
            number++;
        }
    }

    public EDIFACTVocabulary(String version, String type) {
        super(version, type);
    }

    @Override
    public String getDescription(String elementId) {
        String description = elementId == null ? null : DESCRIPTIONS.get(elementId);
        return description == null ? "" : description;
    }

    @Override
    public String getDataType() {
        return EDIFACTDataTypeDelegate.NAME;
    }
}
