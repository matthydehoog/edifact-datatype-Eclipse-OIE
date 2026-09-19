/*
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * https://mozilla.org/MPL/2.0/.
 *
 * Modelled on the EDI/X12 data type of Open Integration Engine (Mirth Connect),
 * Copyright (c) Mirth Corporation.
 */

package com.mirth.connect.plugins.datatypes.edifact;

import com.mirth.connect.donkey.model.message.SerializationType;
import com.mirth.connect.model.converters.IMessageSerializer;
import com.mirth.connect.model.datatype.DataTypeDelegate;
import com.mirth.connect.model.datatype.DataTypeProperties;
import com.mirth.connect.model.datatype.SerializerProperties;

public class EDIFACTDataTypeDelegate implements DataTypeDelegate {
    public static final String NAME = "EDIFACT";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public IMessageSerializer getSerializer(SerializerProperties properties) {
        return new EDIFACTSerializer(properties);
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public SerializationType getDefaultSerializationType() {
        return SerializationType.XML;
    }

    @Override
    public DataTypeProperties getDefaultProperties() {
        return new EDIFACTDataTypeProperties();
    }
}
