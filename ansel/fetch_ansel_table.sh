#!/bin/sh

# dependencies: curl xmlstarlet

# Parses MARC specification XML from USA Library of Congress
# and extracts MARC hex values and corresponding Unicode hex values
# into file ./ansel.csv

url=http://www.loc.gov/marc/specifications/codetables.xml
xpath='/codeTables/codeTable[contains(@name,"Latin")]/characterSet[contains(@name,"ANSEL")]/code'
curl -s $url | xmlstarlet sel -t -m "$xpath" -v "marc" -o "," -v "ucs" -n >ansel.csv
