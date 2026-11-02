package org.monke.lsm.wal;

import java.time.Instant;
import java.util.List;

/**
 * Data structure to record operations to a stable storage before applying them to a main data structure.
 *
 * <p> Ensures recovery and consistency of state by replaying operations from the WAL.
 *
 * <p> Implemented as an append-only log :
 * <ul>
 *     <li> Entries are written sequentially to a segment (file...), until it reaches a maximum size.
 *     <li> When full, a new segment is created.
 *     <li> An entry may span multiple segments if its size exceeds the maximum segment size, and the WAL will handle reading and writing such entries across segments.
 *     <li> Reading can be done from specific ranges, in that case a segment index allows to find correct segments.
 * </ul>
 */
public interface WriteAheadLog extends AutoCloseable {

    /**
     * Writes a single entry to the WAL, atomically with auto-generated sequence number.
     * Handles file rotation and page management internally.
     */
    WALEntry write(byte[] data);

    /**
     * Writes entries in batch.
     */
    List<WALEntry> writeAll(List<byte[]> dataList);;

    /**
     * Reads entries from a given sequence number (inclusive) to latest entry.
     */
    List<WALEntry> readFrom(long fromSequenceNumber);

    /**
     * Reads entries between 2 inclusive sequence numbers.
     */
    List<WALEntry> readRange(long fromSequenceNumber, long toSequenceNumber);

    /**
     * Reads entries from a given timestamp (inclusive) to latest entry.
     */
    List<WALEntry> readFrom(Instant fromTimestamp);

    /**
     * Reads entries between 2 inclusive timestamps.
     */
    List<WALEntry> readRange(Instant fromTimestamp, Instant toTimestamp);

    /**
     * Gets current sequence number of last written entry or -1 if no entries exist.
     */
    long getCurrentSequenceNumber();

    /**
     * Gets next sequence number that should be used for new entries.
     *
     * <p> May differs from size if sequence is not naturally incremental or if entries are deleted.
     */
    long getNextSequenceNumber();

    /**
     * Gets total number of entries in the WAL.
     */
    long size();

    /**
     * Checks if the WAL is empty.
     */
    boolean isEmpty();
}
