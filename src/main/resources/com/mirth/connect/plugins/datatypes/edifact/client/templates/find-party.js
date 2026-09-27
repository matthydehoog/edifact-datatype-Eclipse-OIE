// The name and address segment (NAD) of a party, by its qualifier:
// BY = buyer, SU = supplier, DP = delivery party, IV = invoicee, ...
function findParty(qualifier) {
    for each (var nad in msg['NAD']) {
        if (nad['NAD.01']['NAD.01.1'].toString() == qualifier) {
            return nad;
        }
    }
    return null;
}

var party = findParty('BY');
if (party != null) {
    var partyId = party['NAD.02']['NAD.02.1'].toString(); // e.g. the GLN
    var partyName = party['NAD.04']['NAD.04.1'].toString();
}
