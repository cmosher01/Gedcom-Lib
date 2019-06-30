# Gedcom-Lib

**Gedcom-Lib** is a Java library and framework for parsing GEDCOM files.

Copyright © 2004–2019, Christopher Alan Mosher, Shelton, Connecticut, USA, <cmosher01@gmail.com>.

[![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=CVSSQ2BWDCKQ2)
[![License](https://img.shields.io/github/license/cmosher01/Gedcom-Lib.svg)](https://www.gnu.org/licenses/gpl.html)


## Usage

For `gradle` builds:

```groovy
repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation group: 'nu.mine.mosher.gedcom', name: 'gedcom-lib', version: 'latest.integration'
}
```

Simple example of processing a GEDCOM file, just counting the individuals:

```java
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gedcom.exception.InvalidLevel;
import nu.mine.mosher.mopper.ArgParser;

import java.io.IOException;
import java.util.stream.*;

public class Foobar implements Gedcom.Processor {
    public static void main(String... args) throws InvalidLevel, IOException {
        GedcomOptions options = new ArgParser<>(new GedcomOptions()).parse(args);
        new Gedcom(options, new Foobar()).main();
    }

    @Override
    public boolean process(GedcomTree tree) {
        long c = stream(tree)
            .filter(line -> line.getObject().getTag().equals(GedcomTag.INDI))
            .count();

        System.out.format("found %d individuals%n", c);

        // Return true to write the changed GEDCOM file
        // to standard output, or false not to:
        return false;
    }

    private static Stream<TreeNode<GedcomLine>> stream(GedcomTree tree) {
        return StreamSupport.stream(tree.getRoot().spliterator(), false);
    }
}
```
