// Every line item (LIN) with the QTY, PRI and IMD segments that belong to it:
// the segments after a LIN up to the next LIN, UNS or UNT.
var lineItems = [];
var lineItem = null;
for each (var segment in msg.children()) {
    var tag = String(segment.localName());
    if (tag == 'LIN') {
        lineItem = {
            number: segment['LIN.01']['LIN.01.1'].toString(),
            item: segment['LIN.03']['LIN.03.1'].toString(), // e.g. the GTIN/EAN
            quantities: {}, // qualifier -> quantity, e.g. 21 = ordered
            prices: {}, // qualifier -> price, e.g. AAA = net
            description: ''
        };
        lineItems.push(lineItem);
    } else if (tag == 'UNS' || tag == 'UNT') {
        lineItem = null;
    } else if (lineItem != null) {
        if (tag == 'QTY') {
            lineItem.quantities[segment['QTY.01']['QTY.01.1'].toString()] = segment['QTY.01']['QTY.01.2'].toString();
        } else if (tag == 'PRI') {
            lineItem.prices[segment['PRI.01']['PRI.01.1'].toString()] = segment['PRI.01']['PRI.01.2'].toString();
        } else if (tag == 'IMD') {
            lineItem.description += segment['IMD.03']['IMD.03.4'].toString();
        }
    }
}

// For example: lineItems[0].item, lineItems[0].quantities['21'], lineItems[0].prices['AAA']
