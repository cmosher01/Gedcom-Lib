#!/bin/sh

# dependencies: curl xmlstarlet

# Parses MARC specification XML from USA Library of Congress
# and extracts MARC hex values and corresponding Unicode hex values
# into file ./ansel.csv

url=http://www.loc.gov/marc/specifications/codetables.xml
xpath='/codeTables/codeTable[contains(@name,"Latin")]/characterSet[contains(@name,"ANSEL")]/code'
curl -s "$url" | xmlstarlet sel -t -m "$xpath" -v "marc" -o "," -v "ucs" -n >ansel.csv

# Adds some additional values from various GEDCOM (de-facto) standards:

echo "BE,25A1" >>ansel.csv
echo "BF,25A0" >>ansel.csv
echo "CD,0065" >>ansel.csv
echo "CE,006F" >>ansel.csv
echo "CF,00DF" >>ansel.csv
echo "FC,0338" >>ansel.csv
