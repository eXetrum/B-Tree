package edu.berkeley.cs186.database.index;

import edu.berkeley.cs186.database.DatabaseException;
import edu.berkeley.cs186.database.datatypes.DataType;
import edu.berkeley.cs186.database.io.Page;

import java.util.List;

/**
 * A B+ tree inner node. An inner node header contains the page number of the
 * parent node (or -1 if no parent exists), and the page number of the first
 * child node (or -1 if no child exists). An inner node contains InnerEntry's.
 * Note that an inner node can have duplicate keys if a key spans multiple leaf
 * pages.
 *
 * Inherits all the properties of a BPlusNode.
 */
public class InnerNode extends BPlusNode {
  public InnerNode(BPlusTree tree) {
    super(tree, false);
    getPage().writeByte(0, (byte) 0);
    setFirstChild(-1);
    setParent(-1);
  }
  
  public InnerNode(BPlusTree tree, int pageNum) {
    super(tree, pageNum, false);
    if (getPage().readByte(0) != (byte) 0) {
      throw new BPlusTreeException("Page is not Inner Node!");
    }
  }

  @Override
  public boolean isLeaf() {
    return false;
  }

  public int getFirstChild() {
    return getPage().readInt(5);
  }
  
  public void setFirstChild(int val) {
    getPage().writeInt(5, val);
  }

  /**
   * See BPlusNode#locateLeaf documentation.
   */
  static int locate = 0;
  @Override
  public LeafNode locateLeaf(DataType key, boolean findFirst) {

    if(findFirst) {
      List<BEntry> entries = getAllValidEntries();
      int index = -1;

      for (int i = 0; i < entries.size(); ++i) {
        if (entries.get(i).getKey().compareTo(key) == 0) {
          index = i;
          break;
        }
      }

      int pageNum = index == -1 ? getFirstChild() : entries.get(index).getPageNum();
      if(index == 0 && getFirstChild() != -1) {

        BPlusNode bnd = BPlusNode.getBPlusNode(getTree(), pageNum);
        List<BEntry> bndlst = bnd.getAllValidEntries();
        for(int i = 0; i < bndlst.size(); ++i) {
          if(bndlst.get(i).getKey().compareTo(key) == 0)
            pageNum = getFirstChild();
        }
      }

      return BPlusNode.getBPlusNode(getTree(), pageNum).locateLeaf(key, findFirst);

    } else {

      List<BEntry> entries = getAllValidEntries();
      int index = -1;

      for (int i = 0; i < entries.size(); ++i) {
        if (entries.get(i).getKey().compareTo(key) <= 0) {
          index = i;
        } else {
          break;
        }
      }

      int pageNum = index == -1 ? getFirstChild() : entries.get(index).getPageNum();

      return BPlusNode.getBPlusNode(getTree(), pageNum).locateLeaf(key, findFirst);
    }

  }

  /**
   * Splits this node and pushes up the middle key. Note that we split this node
   * immediately after it becomes full rather than when trying to insert an
   * entry into a full node. Thus a full inner node of 2d entries will be split
   * into a left node with d entries and a right node with d-1 entries, with the
   * middle key pushed up.
   */
  @Override
  public void splitNode() {
    InnerNode parentNode = null;
    if(getParent() == -1) {
      parentNode = new InnerNode(getTree());
      parentNode.setFirstChild(getPageNum());
    } else {
      parentNode = (InnerNode)getBPlusNode(getTree(), getParent());
    }

    List<BEntry> entries = getAllValidEntries();
    int median = entries.size() / 2;

    BEntry bEntry = entries.get(median);
    InnerNode rightNode = new InnerNode(getTree());

    for (int i = median + 1; i < entries.size(); ++i)
      rightNode.insertBEntry(entries.get(i));

    while (entries.size() > median) entries.remove(entries.size() - 1);
    overwriteBNodeEntries(entries);

    this.setParent(parentNode.getPageNum());
    rightNode.setParent(parentNode.getPageNum());

    getTree().updateRoot(parentNode.getPageNum());
    parentNode.insertBEntry(bEntry);
  }
}
