# Specifications

The Dutch healthcare EDIFACT messages this data type has been checked against. The documents themselves were published by third parties and are not included in this repository; ask the publisher (or the maintainer of this repository) for a copy.

| Message | Content | Specification | Publisher, date | Example |
| --- | --- | --- | --- | --- |
| MEDLAB 1 | Laboratory results to the GP | *Laboratoriumbericht MEDLAB versie 1* | 2007 (MEDLAB is maintained by the WCIA of LHV and NHG) | [`examples/medlab.edi`](../../examples/medlab.edi) |
| MEDSPE 3.1 | Specialist letter: report from a hospital specialist to the GP | *MEDSPE 3.1*, Zorginhoudelijke berichten | CSIZ, May 1997 | [`examples/medspe.edi`](../../examples/medspe.edi) |
| MEDVRY 3.1 | Referral letter from the GP to a specialist | *MEDVRY 3.1*, Zorginhoudelijke berichten | CSIZ, May 1997 | |
| Patiënt Overdracht Bericht 1.1 (MEDEUR) | Transfer of a patient's medical record between GPs | *Patiënt Overdracht Bericht versie 1.1* | WCIA (LHV/NHG) with Medische Informatica, Erasmus Universiteit, June 2000 | |

What these messages have in common, and what the data type handles:

- **Components directly behind the segment tag** (MEDLAB: `ARA:1+…`, `BEP:1:1:3+…`): read as data element 0, see the README.
- **No UNA**, with the standard delimiters and `?` as release character, also inside free text (`FTX+GMR+1++Aan?:…` in MEDSPE).
- **`""` as an explicitly empty value** (MEDSPE `PID`): read and written as the two characters `""`.

The examples contain fictitious patients only.
