package org.monke.lsm.wal;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of a Write-Ahead Log (WAL) that persists entries to files on disk.
 *
 * <p> Files have normalized names : wal-xxxx.log.
 *
 * <p> Data is appended using a {@link RandomAccessFile} that behaves like a large byte array with index and pointer.
 */
@Slf4j
public class FileBasedWAL implements WriteAheadLog {

    // File constants :
    private static final String FILE_PREFIX = "wal-";
    private static final String FILE_SUFFIX = ".log";
    private static final int DEFAULT_MAX_FILE_SIZE = 64 * 1024 * 1024; // 64MB files max.

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // In-memory index to find files when reading from ranges.
    private final Map<Integer, Path> segmentIndex = new TreeMap<>();

    private final Path walDirectory;

    private long currentSequenceNumber = -1;
    private int currentSegmentIndex = 0;
    private RandomAccessFile currentSegment;


    public FileBasedWAL(Path walDirectory) {
        this.walDirectory = walDirectory;
    }


    @Override
    public WALEntry write(byte[] data) {
        lock.writeLock().lock();

        try {
            long nextSequenceNumber = getNextSequenceNumber();
            WALEntry entry = WALEntry.create(nextSequenceNumber, data);

            writeEntry(entry);
            currentSequenceNumber = nextSequenceNumber;

            return entry;

        } catch (IOException e) {
            log.error("Failed to append entry wth sequence {}: {}", getCurrentSequenceNumber(), e.getMessage());
            throw new RuntimeException("Failed to append entry", e);

        } finally {
            lock.writeLock().unlock();
        }
    }

    private void writeEntry(WALEntry entry) throws IOException {
        // TODO: Serialize the entry and write it to the current segment, handling segment rotation if needed.
    }

    @Override
    public List<WALEntry> writeAll(List<byte[]> dataList) {
        return List.of();
    }

    @Override
    public List<WALEntry> readFrom(long fromSequenceNumber) {
        return List.of();
    }

    @Override
    public List<WALEntry> readRange(long fromSequenceNumber, long toSequenceNumber) {
        return List.of();
    }

    @Override
    public List<WALEntry> readFrom(Instant fromTimestamp) {
        return List.of();
    }

    @Override
    public List<WALEntry> readRange(Instant fromTimestamp, Instant toTimestamp) {
        return List.of();
    }

    @Override
    public long getCurrentSequenceNumber() {
        return currentSequenceNumber;
    }

    @Override
    public long getNextSequenceNumber() {
        return currentSequenceNumber + 1; // Indexed to 0.
    }

    @Override
    public long size() {
        return currentSequenceNumber + 1; // Indexed to 0.
    }

    @Override
    public boolean isEmpty() {
        return currentSequenceNumber == -1;
    }

    @Override
    public void close() throws Exception {
        log.info("Closing file based WAL: currentSequence={}, totalFiles={}",
            currentSequenceNumber, segmentIndex.size());

        lock.writeLock().lock();

        try {
            // TODO: Closes all open file handles and flushes any pending writes to disk.

            log.info("file based WAL closed successfully");

        } catch (Exception e) {
            log.error("Error while closing file based WAL", e);
            throw e;

        } finally {
            lock.writeLock().unlock();
        }
    }
}
