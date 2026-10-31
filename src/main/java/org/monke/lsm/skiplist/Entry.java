package org.monke.lsm.skiplist;

/**
 * Represents a key-value pair in the storage engine.
 * Each pair represents a snapshot, thus supports tombstones.
 */
public class Entry {

    int key;
    byte[] value;
    boolean tombstone;

    public Entry(int key, byte[] value, boolean tombstone) {
        this.key = key;
        this.value = value;
        this.tombstone = tombstone;
    }
}
