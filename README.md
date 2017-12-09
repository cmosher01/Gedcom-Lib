# Gedcom-Lib

**Gedcom-Lib** is a Java library and framework for parsing GEDCOM files.

This software is distributed under the [GPLv3](http://www.gnu.org/licenses/gpl-3.0-standalone.html) license.

## Usage

For `gradle` builds:

```groovy
repositories {
    maven {
        url 'http://mosher.mine.nu/nexus/repository/maven-public/'
    }
}

dependencies {
    compile group: 'nu.mine.mosher.gedcom', name: 'gedcom-lib', version: 'latest.integration'
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
