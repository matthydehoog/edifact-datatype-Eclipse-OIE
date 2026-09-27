// Every test result (BEP) of a MEDLAB message, with its section (SEC) and remarks (OPB).
var results = [];
var section = '';
var result = null;
for each (var segment in msg.children()) {
    var tag = String(segment.localName());
    if (tag == 'IDE') {
        section = ''; // the next sample or request
    } else if (tag == 'SEC') {
        section = segment['SEC.01']['SEC.01.1'].toString();
    } else if (tag == 'BEP') {
        result = {
            section: section,
            name: segment['BEP.02']['BEP.02.1'].toString(),
            value: segment['BEP.03']['BEP.03.1'].toString(),
            changed: segment['BEP.04']['BEP.04.1'].toString() != '', // the result has been changed
            unit: segment['BEP.05']['BEP.05.1'].toString(),
            flag: segment['BEP.06']['BEP.06.1'].toString(), // '<' below, '>' above the reference range
            low: segment['BEP.07']['BEP.07.1'].toString(),
            high: segment['BEP.08']['BEP.08.1'].toString(),
            code: segment['BEP.09']['BEP.09.1'].toString(),
            remarks: []
        };
        results.push(result);
    } else if (tag == 'OPB' && result != null) {
        result.remarks.push(segment['OPB.01']['OPB.01.1'].toString());
    }
}

// For example: results[0].name + ' ' + results[0].value + ' ' + results[0].unit
