package org.monke.lsm.skiplist;

/**
 * Represents a node in the skip list, which contains an entry as its value and references to the next nodes at each level.
 *
 * <p> Ex :
 * <pre>
 * Level 3:       3 ------------ 9 -------------- 21 -------- 26
 * Level 2:       3 ---6 ------- 9 -------- 19 -- 21 -- 25 -- 26
 * Level 1:       3 -- 6 -- 7 -- 9 -- 12 -- 19 -- 21 -- 25 -- 26
 *
 * next of head :      [ ->3, ->3, ->3 ]
 * next of Element 3 : [ ->6, ->6, ->9 ]
 * next of Element 6 : [ ->7, ->9 ]
 * </pre>
 */
public class Node {

    Entry entry;
    Node[] next;

    public Node(Entry entry, int level) {
        this.entry = entry;
        this.next = new Node[level + 1];
    }
}
