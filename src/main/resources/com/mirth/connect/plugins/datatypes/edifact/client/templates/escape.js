// Escape text with the release character (? before ? + : and ') before you put it into a raw
// EDIFACT message, and undo that. Not needed for msg and tmp: the data type escapes and
// unescapes their values itself.
function edifactEscape(text) {
    return String(text).replace(/([?+:'])/g, '?$1');
}

function edifactUnescape(text) {
    return String(text).replace(/\?([\s\S])/g, '$1');
}
