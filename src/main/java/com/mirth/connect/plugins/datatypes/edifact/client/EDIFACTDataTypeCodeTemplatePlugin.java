/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact.client;

import java.util.List;
import java.util.Map;

import com.mirth.connect.model.codetemplates.CodeTemplate;
import com.mirth.connect.model.datatype.DataTypeDelegate;
import com.mirth.connect.plugins.DataTypeCodeTemplatePlugin;
import com.mirth.connect.plugins.datatypes.edifact.EDIFACTDataTypeDelegate;

public class EDIFACTDataTypeCodeTemplatePlugin extends DataTypeCodeTemplatePlugin {

    public EDIFACTDataTypeCodeTemplatePlugin(String name) {
        super(name);
    }

    @Override
    protected DataTypeDelegate getDataTypeDelegate() {
        return new EDIFACTDataTypeDelegate();
    }

    @Override
    protected String getDisplayName() {
        return "EDIFACT";
    }

    /** The conversion templates of every data type, plus an EDIFACT category with EDIFACT tasks. */
    @Override
    public Map<String, List<CodeTemplate>> getReferenceItems() {
        Map<String, List<CodeTemplate>> items = super.getReferenceItems();
        items.put(EDIFACTCodeTemplates.CATEGORY, EDIFACTCodeTemplates.getCodeTemplates());
        return items;
    }
}
