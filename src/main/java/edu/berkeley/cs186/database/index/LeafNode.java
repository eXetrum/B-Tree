package edu.berkeley.cs186.database.index;

import edu.berkeley.cs186.database.datatypes.DataType;
import edu.berkeley.cs186.database.io.Page;
import edu.berkeley.cs186.database.table.RecordID;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * A B+ tree leaf node. A leaf node header contains the page number of the
 * parent node (or -1 if no parent exists), the page number of the previous leaf
 * node (or -1 if no previous leaf exists), and the page number of the next leaf
 * node (or -1 if no next leaf exists). A leaf node contains LeafEntry's.
 *
 * Inherits all the properties of a BPlusNode.
 */
public class LeafNode extends BPlusNode {
  public LeafNode(BPlusTree tree) {
    super(tree, true);
    getPage().writeByte(0, (byte) 1);
    setPrevLeaf(-1);
    setParent(-1);
    setNextLeaf(-1);
  }
  
  public LeafNode(BPlusTree tree, int pageNum) {
    super(tree, pageNum, true);
    if (getPage().readByte(0) != (byte) 1) {
      throw new BPlusTreeException("Page is not Leaf Node!");
    }
  }

  @Override
  public boolean isLeaf() {
    return true;
  }

  /**
   * See BPlusNode#locateLeaf documentation.
   */
  static int locate = 0;
  @Override
  public LeafNode locateLeaf(DataType key, boolean findFirst) {
    if(this.isRoot() && isLeaf()) {
      return this;
    }
    List<BEntry> entries = getAllValidEntries();

    if(findFirst) {
      for(int i = 0; i < entries.size(); ++i) if(entries.get(i).getKey().compareTo(key) == 0) {
        BPlusNode n = BPlusNode.getBPlusNode(getTree(), getPageNum());
        int prev = getPrevLeaf();
        if(prev != -1) {
          LeafNode prevNode = (LeafNode) getBPlusNode(getTree(), getPrevLeaf());
          List<BEntry> prevEntries = prevNode.getEntries();
            if(prevEntries.get(prevEntries.size() - 1).getKey().compareTo(key) == 0) return prevNode.locateLeaf(key, findFirst);
        }
        return this;
      }

      if(getNextLeaf() == -1) return this;

      if(getNextLeaf() != -1) {
        return (LeafNode) getBPlusNode(getTree(), getNextLeaf()).locateLeaf(key, findFirst);
      }

    } else {

      if (getPrevLeaf() == -1 && getNextLeaf() == -1) {
        return this;
      }

      if(getNextLeaf() == -1) return this;

      BPlusNode nextNode = (LeafNode) getBPlusNode(getTree(), getNextLeaf());
      if(nextNode.getAllValidEntries().get(0).getKey().compareTo(key) <= 0) return nextNode.locateLeaf(key, findFirst);
      return  this;
    }
    return null;
  }

  /**
   * Splits this node and copies up the middle key. Note that we split this node
   * immediately after it becomes full rather than when trying to insert an
   * entry into a full node. Thus a full leaf node of 2d entries will be split
   * into a left node with d entries and a right node with d entries, with the
   * leftmost key of the right node copied up.
   */
  @Override
  public void splitNode() {
    List<BEntry> leafEntries = getAllValidEntries();

    int median = leafEntries.size() / 2;

    InnerNode parentNode = null;
    if(getParent() == -1) {
      parentNode = new InnerNode(getTree());
      parentNode.setFirstChild(getPageNum());
    }
    else {
      parentNode = (InnerNode)getBPlusNode(getTree(), getParent());
    }

    int oldParent = getParent();

    LeafNode rightLeaf = new LeafNode(getTree());
    // Populate right side
    for(int i = median; i < leafEntries.size(); ++i) rightLeaf.insertBEntry(leafEntries.get(i));
    // Get lefts most on the right node
    BEntry pushUp = leafEntries.get(median);
    // Erase half element for left node (current leaf will be left leaf node)
    while(leafEntries.size() > median) leafEntries.remove(leafEntries.size() - 1);
    // Setup new values of this node
    overwriteBNodeEntries(leafEntries);
    int leftPageNum = this.getPageNum();
    int rightPageNum = rightLeaf.getPageNum();
    // Connect leaves
    int leftOldNext = getNextLeaf();

    this.setNextLeaf(rightPageNum);
    rightLeaf.setPrevLeaf(leftPageNum);

    this.setParent(parentNode.getPageNum());
    rightLeaf.setParent(parentNode.getPageNum());


    if(oldParent == -1) {
      parentNode.setFirstChild(getPageNum());
    } else {
      if(leftOldNext != -1) {
        LeafNode next = (LeafNode) BPlusNode.getBPlusNode(getTree(), leftOldNext);
        next.setPrevLeaf(rightLeaf.getPageNum());
      }

      rightLeaf.setNextLeaf(leftOldNext);
    }


    BEntry bEntry = new InnerEntry(pushUp.getKey(), rightPageNum);
    getTree().updateRoot(parentNode.getPageNum());
    parentNode.insertBEntry(bEntry);

  }
  
  public int getPrevLeaf() {
    return getPage().readInt(5);
  }

  public int getNextLeaf() {
    return getPage().readInt(9);
  }
  
  public void setPrevLeaf(int val) {
    getPage().writeInt(5, val);
  }

  public void setNextLeaf(int val) {
    getPage().writeInt(9, val);
  }

  /**
   * Creates an iterator of RecordID's for all entries in this node.
   *
   * @return an iterator of RecordID's
   */
  public Iterator<RecordID> scan() {
    List<BEntry> validEntries = getAllValidEntries();
    List<RecordID> rids = new ArrayList<RecordID>();

    for (BEntry le : validEntries) {
      rids.add(le.getRecordID());
    }

    return rids.iterator();
  }

  /**
   * Creates an iterator of RecordID's whose keys are greater than or equal to
   * the given start value key.
   *
   * @param startValue the start value key
   * @return an iterator of RecordID's
   */
  public Iterator<RecordID> scanFrom(DataType startValue) {
    List<BEntry> validEntries = getAllValidEntries();
    List<RecordID> rids = new ArrayList<RecordID>();

    for (BEntry le : validEntries) {
      if (startValue.compareTo(le.getKey()) < 1) { 
        rids.add(le.getRecordID());
      }
    }
    return rids.iterator();
  }

  /**
   * Creates an iterator of RecordID's that correspond to the given key.
   *
   * @param key the search key
   * @return an iterator of RecordID's
   */
  public Iterator<RecordID> scanForKey(DataType key) {
    List<BEntry> validEntries = getAllValidEntries();
    List<RecordID> rids = new ArrayList<RecordID>();

    for (BEntry le : validEntries) {
      if (key.compareTo(le.getKey()) == 0) { 
        rids.add(le.getRecordID());
      }
    }
    return rids.iterator();
  }
}
