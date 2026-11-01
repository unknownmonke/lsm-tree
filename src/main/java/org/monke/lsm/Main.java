package org.monke.lsm;

import org.monke.lsm.skiplist.Entry;
import org.monke.lsm.skiplist.SkipList;

public class Main {

    static void main() {
        generateSkipList();
    }

    static void generateSkipList() {
        SkipList skipList = new SkipList(3, 0.5);

        int numEntries = 10;

        for (int i = 0; i < numEntries; i++) {
            int key = (int) (Math.random() * 100); // Random key between 0 and 99.
            skipList.insert(new Entry(key, ("value" + i).getBytes(), false));
        }
        skipList.print();
    }
}
