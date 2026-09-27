// Build an EDIFACT interchange around message segments: UNA, UNB, UNH ... UNT, UNZ.
// The references and counters (UNT segment count, UNZ message count) are filled in.
// Escape your own values first with edifactEscape() (template "Escape and unescape text").
var sender = 'SENDER:ZZ';
var recipient = 'RECIPIENT:ZZ';
var messageType = 'ORDERS:D:96A:UN';
var segments = [
    'BGM+220+PO12345+9',
    'DTM+137:20230115:102'
];

var interchangeReference = String(new Date().getTime()).slice(-14); // at most 14 characters
var messageReference = '1';
var prepared = new java.text.SimpleDateFormat('yyMMdd:HHmm').format(new java.util.Date());

var edifactSegments = ['UNB+UNOC:3+' + sender + '+' + recipient + '+' + prepared + '+' + interchangeReference,
    'UNH+' + messageReference + '+' + messageType]
    .concat(segments)
    .concat(['UNT+' + (segments.length + 2) + '+' + messageReference, 'UNZ+1+' + interchangeReference]);
var edifact = "UNA:+.? '\n" + edifactSegments.join("'\n") + "'";

// For example channelMap.put('edifact', edifact); with ${edifact} as a destination's template,
// or, with EDIFACT as the outbound data type: tmp = new XML(SerializerFactory.getSerializer('EDIFACT').toXML(edifact));
