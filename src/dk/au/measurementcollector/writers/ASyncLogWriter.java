package dk.au.measurementcollector.writers;

import android.os.SystemClock;
import android.util.Log;
import dk.au.measurementcollector.Toaster;
import dk.au.perpos.sensing.measurements.Measurement;
import dk.au.perpos.sensing.webservice.PerPosSerializer;
import dk.au.perpos.sensing.webservice.SerializationException;

import java.io.BufferedWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ASyncLogWriter implements LogWriter {

    private String filePrefix;
    private File folder;
    private BufferedWriter logFile;
    private boolean showTag = false;

    private static final ExecutorService executor = Executors
            .newSingleThreadExecutor();
    private static final int FLUSH_SIZE_LIMIT = 2000;
    private static final long FLUSH_TIME_LIMIT = TimeUnit.SECONDS
            .toMillis(20000);

    private final LinkedBlockingQueue<String> measurements = new LinkedBlockingQueue<String>();
    private long timeOffset;
    private long lastAttemptedFlushTime;

    public ASyncLogWriter(String filePrefix, File folder, boolean showTag,
                          long timeOffset) {
        this.filePrefix = filePrefix;
        this.showTag = showTag;
        this.folder = folder;
        this.timeOffset = timeOffset;
    }

    public void writeSample(String tag, Object data) {
        writeSample(tag, data, System.currentTimeMillis());
    }


    public void writeSample(String tag, Object data, long timeStamp){
        if (logFile == null) {
            Toaster.showToast("No open log file for: " + tag);
            System.exit(1);
        }

        String dataRepresentation = "";

        if (data == null) {
            dataRepresentation = "null";

        } else if (data instanceof String) {
            dataRepresentation = (String) data;

        } else if (data instanceof Collection<?>) {
            Collection<?> list = (Collection<?>) data;
            StringBuilder sb = new StringBuilder("[ ");
            for (Object o : list) {
                try {
                    sb.append(PerPosSerializer.serialize(o) + ",");
                } catch (SerializationException e) {
                    Log.e("StandardLogWriter", "Could not serialize: " + o);
                    Toaster.showToast("Could not serialize: "
                            + o.toString());
                }
            }
            sb.deleteCharAt(sb.length() - 1); // Delete last ","
            sb.append(" ]");
            dataRepresentation = sb.toString();

        } else if (data instanceof Measurement) {
            // Serialize all PerPos measurement objects
            try {
                dataRepresentation = PerPosSerializer.serialize(data);
            } catch (SerializationException e) {
                e.printStackTrace();
            }

        } else {
            // Just save string representation of everything else
            dataRepresentation = data.toString();
        }
        final String sample = (showTag ? tag + "::" : "")
                + dataRepresentation + "\n";
        measurements.offer(sample);
        if (measurements.size() >= FLUSH_SIZE_LIMIT
                || SystemClock.elapsedRealtime() - lastAttemptedFlushTime >= FLUSH_TIME_LIMIT) {
            lastAttemptedFlushTime = SystemClock.elapsedRealtime();
            writeMeasurementsAsync();
        }
    }

    private Future<?> writeMeasurementsAsync() {
        if (logFile != null)
            return executor.submit(new Runnable() {

                @Override
                public void run() {
                    List<String> s = new ArrayList<String>(measurements.size());
                    measurements.drainTo(s);
                    FileUtil.writeToLog(s, logFile);
                }
            });
        return null;
    }

    /**
     * Create a new log file. This will only work if the file isn't open
     * already.
     */
    public void createNewLog() {
        if (logFile == null) {
            logFile = FileUtil.open(
                    filePrefix + "_" + (System.currentTimeMillis()) + ".log",
                    folder);
        }
    }

    /**
     * Close any currently open log file.
     */
    public void closeLog() {
        if (logFile != null) {
            writeMeasurementsAsync();
            executor.submit(new Runnable() {

                @Override
                public void run() {
                    FileUtil.close(logFile);
                }
            });
            logFile = null;
        }
    }
}
