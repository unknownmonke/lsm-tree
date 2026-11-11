package org.monke.lsm.wal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WALEntryTest {

    @Test
    void serialize_should_serialize_with_expected_length() {
        byte[] data = "test data".getBytes();
        long sequenceNumber = 1L;

        WALEntry entry = WALEntry.create(sequenceNumber, data);
        byte[] serialized = entry.serialize();

        // [entryLength(4)][sequenceNumber(8)][timestamp(8)][dataLength(4)][data(N)][checksum(8)]
        int expectedLength = 4 + 8 + 8 + 4 + entry.getData().length + 8; // 4 bytes for entryLength

        assertThat(serialized.length).isEqualTo(expectedLength);
    }

    @Test
    void serialization_should_work_correctly() {
        byte[] data = "test data".getBytes();
        long sequenceNumber = 1L;

        WALEntry entry = WALEntry.create(sequenceNumber, data);
        byte[] serialized = entry.serialize();

        WALEntry deserializedEntry = WALEntry.deserialize(serialized);

        assertThat(deserializedEntry.getSequenceNumber()).isEqualTo(entry.getSequenceNumber());
        assertThat(deserializedEntry.getTimestamp().toEpochMilli()).isEqualTo(entry.getTimestamp().toEpochMilli());
        assertThat(deserializedEntry.getData()).isEqualTo(entry.getData());
        assertThat(deserializedEntry.getChecksum()).isEqualTo(entry.getChecksum());
    }

    @Test
    void deserialize_should_throw_for_invalid_length() {
        byte[] data = "test data".getBytes();
        long sequenceNumber = 1L;

        WALEntry entry = WALEntry.create(sequenceNumber, data);
        byte[] serialized = entry.serialize();

        // Corrupts serialized data by adding bytes to the serialized value.
        byte[] largerSerialized = new byte[serialized.length + 1];

        assertThrows(IllegalArgumentException.class, () -> WALEntry.deserialize(largerSerialized));
    }

    @Test
    void deserialize_should_throw_for_invalid_checksum() {
        byte[] data = "test data".getBytes();
        long sequenceNumber = 1L;

        WALEntry entry = WALEntry.create(sequenceNumber, data);
        byte[] serialized = entry.serialize();

        // Corrupts serialized data by changing the checksum to an incorrect value.
        serialized[serialized.length - 1] ^= (byte) 0xFF; // Flips last byte of checksum.

        assertThrows(IllegalArgumentException.class, () -> WALEntry.deserialize(serialized));
    }
}
