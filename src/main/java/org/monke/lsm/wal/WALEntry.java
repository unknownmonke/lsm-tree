package org.monke.lsm.wal;

import lombok.Getter;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Arrays;
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
        Instant timestamp = Instant.now();
        byte[] checksum = checksum(sequenceNumber, timestamp, data);
        return new WALEntry(sequenceNumber, timestamp, data, checksum);
    }

    /**
     * Serializes entry into a byte array for storage.
     *
     * <p> Format :
     * <pre>
     *     [entryLength(4)][sequenceNumber(8)][timestamp(8)][dataLength(4)][data(N)][checksum(8)]
     * </pre>
     */
    public byte[] serialize() {
        int entryLength = 8 + 8 + 4 + data.length + checksum.length;

        ByteBuffer buffer = ByteBuffer.allocate(4 + entryLength); // 4 bytes for entry length value + entry length.

        // Puts entry fields in order, as defined in the format above.
        buffer.putInt(entryLength);
        buffer.putLong(sequenceNumber);
        buffer.putLong(timestamp.toEpochMilli());
        buffer.putInt(data.length);
        buffer.put(data);
        buffer.put(checksum);

        return buffer.array();
    }

    /**
     * Deserializes a byte entry into its object representation, with length and checksum validation.
     */
    public static WALEntry deserialize(byte[] serializedEntry) {
        ByteBuffer buffer = ByteBuffer.wrap(serializedEntry);

        int entryLength = buffer.getInt();
        int length = serializedEntry.length - 4; // Excludes the 4 bytes of entryLength itself.

        if (length != entryLength) {
            throw new IllegalArgumentException("Entry length mismatch: expected " + entryLength + ", but got " + length);
        }

        long sequenceNumber = buffer.getLong();
        Instant timestamp = Instant.ofEpochMilli(buffer.getLong());
        int dataLength = buffer.getInt();

        byte[] data = new byte[dataLength];
        buffer.get(data);
        byte[] checksum = new byte[8];
        buffer.get(checksum);

        byte[] calculatedChecksum = checksum(sequenceNumber, timestamp, data);

        if (!Arrays.equals(checksum, calculatedChecksum)) {
            throw new IllegalArgumentException("Checksum validation failed");
        }

        return new WALEntry(sequenceNumber, timestamp, data, checksum);
    }

    /**
     * Calculates checksum for a given entry (sequence number, timestamp, and data).
     * Uses CRC32 validation to detect changes and errors in raw data.
     */
    private static byte[] checksum(long sequenceNumber, Instant timestamp, byte[] data) {

        // [sequenceNumber(8)][timestamp(8)][dataLength(4)][data(N)].
        ByteBuffer buffer = ByteBuffer.allocate(8 + 8 + 4 + data.length);
        buffer.putLong(sequenceNumber);
        buffer.putLong(timestamp.toEpochMilli());
        buffer.putInt(data.length);
        buffer.put(data);

        CRC32 crc32 = new CRC32();
        crc32.update(buffer);
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
