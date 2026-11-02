package org.monke.lsm.wal;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.zip.CRC32;

/**
 * Represents a single entry in the Write-Ahead Log.
 */
public class WALEntry {

    @Getter
    private final long sequenceNumber;

    @Getter
    private final Instant timestamp;

    /**
     * Contains the data payload, e.g. serialized key-value pairs and tombstones.
     * This data is what will be written to the WAL file and later used to reconstruct the Memtable state.
     */
    private final byte[] data;

    /**
     * Checksum for the entry and sequence number, used to verify data integrity when reading from the WAL.
     */
    private final byte[] checksum;


    private WALEntry(long sequenceNumber, Instant timestamp, byte[] data, byte[] checksum) {
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.data = data.clone(); // Defensive copy.
        this.checksum = checksum.clone(); // Defensive copy.
    }


    public static WALEntry create(long sequenceNumber, byte[] data) {
        byte[] checksum = calculateChecksum(sequenceNumber, data);
        return new WALEntry(sequenceNumber, Instant.now(), data, checksum);
    }

    /**
     * Calculates checksum for the given sequence number and data.
     * Uses CRC32 validation to detect accidental changes and errors in raw data.
     */
    private static byte[] calculateChecksum(long sequenceNumber, byte[] data) {

        ByteBuffer buffer = ByteBuffer.allocate(8 + data.length);
        buffer.putLong(sequenceNumber);
        buffer.put(data);

        CRC32 crc32 = new CRC32();
        crc32.update(buffer.array());
        return ByteBuffer.allocate(8).putLong(crc32.getValue()).array();
    }

    // Getter with a defensive copy.
    public byte[] getData() {
        return data.clone();
    }

    // Getter with a defensive copy.
    public byte[] getChecksum() {
        return checksum.clone();
    }
}
