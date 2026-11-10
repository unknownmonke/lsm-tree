package org.monke.lsm.skiplist;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SkipListTest {

    private static final SkipList SKIPLIST = new SkipList(3, 0.5);

    private static final List<Entry> ENTRIES = List.of(
        new Entry(3, "value3".getBytes(), false),
        new Entry(6, "value6".getBytes(), false),
        new Entry(9, "value9".getBytes(), false)
    );


    @BeforeAll
    static void setup() {
        ENTRIES.forEach(SKIPLIST::insert);
    }

    @Test
    void insert_should_work_correctly() {
        // Verifies that nodes have been inserted in the correct order at bottom level only,since other levels are probabilistic.
        Node node = SKIPLIST.head.next[0];
        assertThat(node.entry.key).isEqualTo(3);
        node = node.next[0];
        assertThat(node.entry.key).isEqualTo(6);
        node = node.next[0];
        assertThat(node.entry.key).isEqualTo(9);
    }

    @Test
    void find_should_work_correctly() {
        Entry entry = SKIPLIST.find(9);

        assertThat(entry).isNotNull();
        assertThat(new String(entry.value)).isEqualTo("value9");
    }

    @Test
    void all_should_return_all_entries() {
        assertThat(SKIPLIST.all()).isEqualTo(ENTRIES);
    }
}
