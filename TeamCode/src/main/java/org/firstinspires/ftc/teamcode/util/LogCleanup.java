package org.firstinspires.ftc.teamcode.util;

import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;

import java.io.File;

/**
 * LogCleanup — reclaims hub storage by deleting log/CSV files older than 14 days.
 *
 * Why: FTC matchlogs (one per OpMode run) and any Datalogger CSVs accumulate forever unless
 * something cleans them up. Over a season this eats hub storage. Persistence files (snapshot,
 * tuning) are safe — they overwrite in place, they never accumulate — so they're skipped here.
 *
 * How: {@link #maybeRun} is called at OpMode init but only does work when 14 days have passed
 * since the last cleanup (tracked via a small stamp file). The 14-day gate means running it at
 * every init has essentially zero cost on normal days.
 *
 * SAFETY:
 *   - Only deletes files older than 14 days.
 *   - Matchlogs directory: only touches .log and .txt files (matches FTC's naming).
 *   - Settings directory:  only touches .csv files (protects our JSONs and FTC configs).
 *   - Never recurses into subdirectories.
 *
 * Rules (CLAUDE.md §7): file I/O NEVER in the loop — this is init-only.
 */
public final class LogCleanup {

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long CLEANUP_INTERVAL_MS = 14L * DAY_MS;
    private static final long AGE_THRESHOLD_MS    = 14L * DAY_MS;
    private static final String STAMP_FILE = "last_log_cleanup.txt";

    /**
     * Call once at OpMode init. Runs cleanup only if 14+ days have passed since the last run.
     * Telemeters loudly when it deletes anything; silent on the fast path (no work needed).
     */
    public static void maybeRun(Telemetry telemetry) {
        try {
            long now = System.currentTimeMillis();
            File stamp = AppUtil.getInstance().getSettingsFile(STAMP_FILE);
            long lastRun = stamp.exists() ? stamp.lastModified() : 0L;
            if (now - lastRun < CLEANUP_INTERVAL_MS) return;

            long cutoff = now - AGE_THRESHOLD_MS;
            File settingsDir = stamp.getParentFile();           // /sdcard/FIRST/settings/
            File matchlogsDir = new File(AppUtil.FIRST_FOLDER, "matchlogs"); // /sdcard/FIRST/matchlogs/

            int deleted = 0;
            deleted += purgeOlderThan(matchlogsDir, cutoff, ".log", ".txt");
            deleted += purgeOlderThan(settingsDir,  cutoff, ".csv");

            // Touch the stamp file so we don't run again for 14 more days.
            //noinspection ResultOfMethodCallIgnored
            stamp.getParentFile().mkdirs();
            //noinspection ResultOfMethodCallIgnored
            stamp.createNewFile();
            //noinspection ResultOfMethodCallIgnored
            stamp.setLastModified(now);

            String msg = "LogCleanup: deleted " + deleted + " files older than 14 days";
            RobotLog.i("Persistence: %s", msg);
            if (telemetry != null) telemetry.addLine(msg);

        } catch (Throwable t) {
            RobotLog.e("LogCleanup: failed: %s", t.getMessage());
        }
    }

    /**
     * Deletes files directly inside {@code dir} whose extension is in {@code extensions} and whose
     * mtime is older than {@code cutoffMillis}. Never recurses; never touches subdirectories.
     */
    private static int purgeOlderThan(File dir, long cutoffMillis, String... extensions) {
        if (dir == null || !dir.isDirectory()) return 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        int deleted = 0;
        for (File f : files) {
            if (!f.isFile()) continue;
            if (f.lastModified() >= cutoffMillis) continue;
            String name = f.getName().toLowerCase();
            if (!hasExtension(name, extensions)) continue;
            if (f.delete()) deleted++;
        }
        return deleted;
    }

    private static boolean hasExtension(String lowerName, String[] extensions) {
        for (String ext : extensions) {
            if (lowerName.endsWith(ext)) return true;
        }
        return false;
    }

    private LogCleanup() { }
}
