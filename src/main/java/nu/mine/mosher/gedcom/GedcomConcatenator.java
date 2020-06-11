package nu.mine.mosher.gedcom;


import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.logging.Jul;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles CONT and CONC tags in a given <code>GedcomTree</code> by appending
 * their values to the previous <code>GedcomLine</code>.
 *
 * @author Chris Mosher
 */
public class GedcomConcatenator {
    private final GedcomTree tree;
    private int maxLength = 0;

    /**
     * @param tree
     */
    public GedcomConcatenator(final GedcomTree tree) {
        this.tree = tree;
    }

    public void concatenate() {
        concatenateHelper(this.tree.getRoot());

        if (this.maxLength < 1) {
            this.maxLength = GedcomUnconcatenator.DEFAULT_MAX_LENGTH;
            Jul.log().info("Did not detect any CONC or CONT lines in this file; defaulting to "+this.maxLength);
        }

        tree.setMaxLength(this.maxLength);
        Jul.log().info("Detected maximum length for CONC/CONT line breaking of: " + this.maxLength);
    }

    private void concatenateHelper(final TreeNode<GedcomLine> nodeParent) {
        final List<TreeNode<GedcomLine>> rToBeRemoved = new ArrayList<>();

        for (final TreeNode<GedcomLine> nodeChild : nodeParent) {
            concatenateHelper(nodeChild);

            final GedcomLine lineChild = nodeChild.getObject();

            final GedcomTag tag = lineChild.getTag();

            switch (tag) {
                case CONT: {
                    if (this.maxLength < lineChild.getValue().length()) {
                        this.maxLength = lineChild.getValue().length();
                    }
                    nodeParent.setObject(nodeParent.getObject().contValue(lineChild.getValue()));
                    rToBeRemoved.add(nodeChild);
                }
                break;

                case CONC: {
                    if (this.maxLength < lineChild.getValue().length()) {
                        this.maxLength = lineChild.getValue().length();
                    }
                    nodeParent.setObject(nodeParent.getObject().concValue(lineChild.getValue()));
                    rToBeRemoved.add(nodeChild);
                }
                break;

                default:
                    // we don't do anything with tags other than CONT or CONC
            }
        }

        rToBeRemoved.forEach(TreeNode::removeFromParent);
    }
}
