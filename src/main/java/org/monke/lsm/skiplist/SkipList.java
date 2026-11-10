package org.monke.lsm.skiplist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkipList {

    /**
     * Maximum number of levels.
     * A base linked list is considered level 0, and each additional level is a "shortcut" to nodes further down the list.
     */
    private final int maxLevel;

    /**
     * Probability of a node being promoted to a higher level.
     * For p = 0.5, a linked list with 10 nodes at the base level will have approximately 5 nodes in the next level of indices.
     */
    private final double p;

    /**
     * Current number of levels in the skip list.
     */
    private int levels;

    /**
     * Number of bytes occupied by the stored key-value pairs in the list.
     * Used to determine if the Memtable exceeds its size threshold.
     */
    private int size;

    /**
     * Head node, which holds references to the first node in each level.
     */
    protected Node head;


    public SkipList(int maxLevel, double p) {
        this.maxLevel = maxLevel;
        this.p = p;
        this.levels = 0;
        this.size = 0;
        this.head = new Node(null, maxLevel);
    }

    /**
     * Inserts a new entry into the skip list.
     */
    public void insert(Entry entry) {
        Node[] update = new Node[maxLevel + 1];
        Node current = head;

        // For each level, starting from highest.
        for (int i = maxLevel; i >= 0; i--) {
            // While the next node exists and its key is smaller than the key to be inserted, moves to the next node.
            while (current.next[i] != null && current.next[i].entry.key < entry.key) {
                current = current.next[i];
            }
            // Saves the last smaller node in the update array, which will be used to insert the new node.
            update[i] = current;
        }

        // Since current is the last smaller node, the next node is either null or has a key greater than or equal to the key to be inserted.
        current = current.next[0];

        // If the next node is not null and has the same key, update its value and tombstone status.
        if (current != null && current.entry.key == entry.key) {
            current.entry.value = entry.value;
            current.entry.tombstone = entry.tombstone;

            // Increases or decreases skip list size based on new value's size versus previous one.
            size += entry.value.length - current.entry.value.length;

        } else { // If the next node is null or has a different key, inserts a new node.

            // Calculates a new level for the node based on probability.
            int newLevel = randomLevel();

            // If the new level is greater than the current number of levels,
            // updates the update array to point to the head node for the new levels (see schema on node class) and updates the current number of levels.
            if (newLevel > levels) {
                for (int i = levels + 1; i <= newLevel; i++) {
                    update[i] = head;
                }
                levels = newLevel;
            }

            // Creates a new node with the entry and the calculated level.
            Node newNode = new Node(entry, newLevel);

            // Updates next references up to the new level of the new node.
            // update[i] is the last smaller node at level i, so its next reference is set to the new node,
            // and the new node's next reference is set to the next node in the list.
            for (int i = 0; i <= newLevel; i++) {
                newNode.next[i] = update[i].next[i];
                update[i].next[i] = newNode;
            }

            // Increases the size of the skip list by the size of the new entry's value.
            size += entry.value.length;
        }
    }

    /**
     * Performs a search for an entry with the specified key in the skip list.
     *
     * <p> Search starts from the highest levels and traverses each level.
     * Higher levels act as indexes, allowing the search to skip over multiple nodes at once :
     *
     * <ul>
     *     <li> If next node's key is smaller than the target key, moves to the next node at the same level.
     *     <li> If next node's key is greater than or equal to the target key, moves down to the next lower level and repeat the process.
     *     <li> After reaching the bottom level, last smaller node has been found.
     *     <li> If the next node is null or has a greater key, entry does not exist in the skip list. Else, returns entry of the next node.
     * </ul>
     */
    public Entry find(int key) {
        Node current = head;

        // For each level, starting from highest.
        for (int i = maxLevel; i >= 0; i--) {
            // While the next node exists and its key is smaller than the target key, moves to the next node.
            while (current.next[i] != null && current.next[i].entry.key < key) {
                current = current.next[i];
            }
        }

        // The last smallest entry has been found at bottom level. Moves to the next node.
        current = current.next[0];

        // If the next node is not null and has the same key, returns its entry.
        if (current != null && current.entry.key == key) {
            return current.entry;

        } else { // Entry does not exist.
            return null;
        }
    }

    /**
     * Returns a list of all entries in the skip list.
     * Convenience of the skip list resides in the fact that all entries are stored in a linked list at the base level,
     * so they can be traversed in order.
     */
    public List<Entry> all() {
        List<Entry> entries = new ArrayList<>();
        Node current = head.next[0];

        // Traverses the base level of the skip list and adds each entry to the list.
        while (current != null) {
            entries.add(current.entry);
            current = current.next[0];
        }
        return entries;
    }

    /**
     * Since entries handle tombstones, there is no explicit delete method.
     * Tombstones can be removed all at once from the skip list to free up space.
     *
     * <p> This method traverses the base level and removes nodes with tombstone entries.
     *
     * <ul>
     *     <li> The next references of the previous nodes are updated to skip over the removed nodes.
     *     <li> The size of the skip list is decreased by the size of the removed entries' values.
     *     <li> The number of levels is updated if the highest level becomes empty after removals.
     * </ul>
     *
     * <p> This method is useful for maintaining the skip list's efficiency and preventing it from growing too large due to tombstone entries.
     * Can be called periodically or from a background thread.
     */
    public void clear() {
        // TODO
    }

    /**
     * Clearing all entries is equivalent to clearing head since head is the root.
     */
    public void reset() {
        levels = 0;
        size = 0;
        head = new Node(null, maxLevel);
    }

    /**
     * Generates a random level for a new node based on the probability.
     * The node is promoted to a higher level with a probability of p, meaning that the generated random number must be in [0, p).
     *
     * <p> The process continues until the random number is greater than p or the maximum level is reached.
     *
     * <p> This ensures that the number of nodes decreases exponentially with each level, because probability to hit a higher level n is p^n
     * (n successful hits in the interval [0, p)).
     */
    public int randomLevel() {
        int level = 0;
        while (Math.random() < p && level < maxLevel) {
            level++;
        }
        return level;
    }

    /**
     * Prints a representation of the skip list for visualization and debugging purposes.
     *
     * <p> Each level is printed on a separate line, with nodes represented by their keys and connections represented by dashes.
     * Number of dashes increase to keep keys in the same vertical column in display.
     *
     * <p> Leading spaces may be used to move heads to correct position.
     *
     * <p> Ex :
     * <pre>
     * Level 0:       3 -- 6 -- 7 -- 9 -- 12 -- 19 -- 21 -- 25 -- 26
     * Level 1:       3 ---6 ------- 9 -------- 19 -- 21 -- 25 -- 26
     * Level 2:       3 ------------ 9 -------------- 21 -------- 26
     * </pre>
     */
    public void print() {
        StringBuilder sb = new StringBuilder();

        sb.append(System.lineSeparator());
        sb.append("Skip List :");
        sb.repeat(System.lineSeparator(), 2);

        Map<Integer, Integer> keyPositionsAtBottomLevel = new HashMap<>();

        for (int i = 0; i < levels; i++) {
            StringBuilder line = new StringBuilder();
            sb.append("Level ").append(i).append(":      ");

            boolean eol = false;
            int position = 0;
            Node current = head.next[i];

            // Head node only - Adds position if bottom level.
            if (i == 0) {
                keyPositionsAtBottomLevel.put(current.entry.key, 0);
            }

            int positionAtBottomLevel = keyPositionsAtBottomLevel.get(current.entry.key);

            // Head node only - Calculates leading spaces if the head node of the line is not the head node of bottom level.
            if (i > 0 && positionAtBottomLevel > 0) {
                line.repeat(" ", positionAtBottomLevel);
                position = positionAtBottomLevel;
            }

            while (!eol) {
                line.append(current.entry.key);
                Node next = current.next[i];

                if (next != null) {
                    // Moves position at the end of value.
                    int keySize = String.valueOf(current.entry.key).length();
                    position += keySize;

                    // Calculates number of trailing dashes to next node.
                    int trailingDashes;

                    if (i == 0) {
                        trailingDashes = 2;

                    } else {
                        positionAtBottomLevel = keyPositionsAtBottomLevel.get(next.entry.key);
                        trailingDashes = positionAtBottomLevel - position - 2; // Spaces between dashes and values.
                    }
                    // Moves after trailing dashes : the position of the next node.
                    position += trailingDashes;
                    position += 2; // Spaces between dashes and values.

                    keyPositionsAtBottomLevel.put(next.entry.key, position);
                    line.append(" ").repeat("-", Math.max(0, trailingDashes)).append(" ");

                    current = next;

                } else { // End of level reached, go to next.
                    eol = true;
                }
            }
            sb.append(line);
            sb.append(System.lineSeparator());
        }
        System.out.print(sb);
    }
}
