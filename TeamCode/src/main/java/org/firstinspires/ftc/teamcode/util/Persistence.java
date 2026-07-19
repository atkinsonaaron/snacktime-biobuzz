package org.firstinspires.ftc.teamcode.util;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import com.qualcomm.robotcore.util.ReadWriteFile;
import org.firstinspires.ftc.teamcode.config.TuningConfig;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.util.JoystickCurve;
import org.firstinspires.ftc.teamcode.hardware.BuildInfo;

import com.qualcomm.robotcore.hardware.VoltageSensor;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Persistence — two jobs (CLAUDE.md §7), both now ROBOT-AWARE (see {@link RobotIdentity}):
 *
 *   1. TUNING BACKUP (session persistence), PER ROBOT:
 *      {@link #saveTuning(RobotIdentity)} writes every registered tunable to that robot's own file.
 *      {@link #loadAndApplyTuning(RobotIdentity, Telemetry)} reads it back and applies the values
 *      to the live static fields on each class in TUNING_CLASSES. Call save on every OpMode stop;
 *      call load on every OpMode init.
 *
 *   2. SNAPSHOT (traceability record), PER ROBOT:
 *      {@link #writeSnapshot(Snapshot, HardwareMap)} writes a full JSON record (git hash, hardware,
 *      loop-time stats, every tunable) to a per-robot file, and also logs it via RobotLog so it
 *      appears in robotControllerLog.txt — grep "SNAPSHOT:" after any session.
 *
 * THE TWO-ROBOT MODEL (why the files are per-robot):
 *   The SAME commit runs on both the Competition robot and the Test bot (one codebase, never
 *   forked). They differ mainly in drivetrain/Pedro tuning (mass & CG differ). So:
 *     - Competition robot -> {@code comp_tuning.json}  (its session tuning)
 *     - Test bot          -> {@code TESTBOT_SCRATCH_do_not_promote.json}  (scratch; loud on purpose)
 *     - UNKNOWN identity   -> NO tuning file loaded or saved (fail closed). An unidentified hub is
 *       NEVER assumed to be the comp robot; it runs on the in-code defaults and says so loudly.
 *   CANONICAL tuning = the in-code static defaults, which represent the COMPETITION robot and are
 *   the git backup. Promote dialed-in values back to source FROM THE COMPETITION ROBOT only; the
 *   test bot's scratch file stays on its hub and is never committed (it's gitignored). This is what
 *   lets the kids tune the test bot freely without ever endangering the competition tuning.
 *
 * HARD RULES (CLAUDE.md §7):
 *   - File I/O NEVER happens in the main loop — only on init, stop, or explicit button press.
 *   - loadAndApplyTuning MUST telemeter loudly when it finds and applies a file.
 *   - git is the real backup. A hub re-flash wipes hub files; git doesn't.
 */
public final class Persistence {

    private static final String TAG = "Persistence";

    // Tuning file names, per robot. UNKNOWN deliberately has none — fail closed.
    static final String COMP_TUNING_FILE    = "comp_tuning.json";
    static final String TESTBOT_TUNING_FILE = "TESTBOT_SCRATCH_do_not_promote.json";

    // All @Configurable classes whose public static fields are included in session persistence.
    // TuningConfig holds cross-cutting/drivetrain values; each mechanism subsystem holds its own.
    // KICKOFF: add each new @Configurable subsystem class here, e.g. GameMechanism.class.
    // Keys in the JSON are namespaced "ClassName.fieldName" to avoid collisions.
    private static final List<Class<?>> TUNING_CLASSES = Arrays.asList(
            TuningConfig.class,
            Drivetrain.class,
            JoystickCurve.class
            // KICKOFF: add each new @Configurable subsystem class here, e.g. GameMechanism.class.
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // -------------------------------------------------------------------------
    // Snapshot
    // -------------------------------------------------------------------------

    /** Everything worth persisting in a full record. Keep it a plain data holder. */
    public static class Snapshot {
        public String gitHash   = BuildInfo.GIT_HASH;
        public String buildTime = BuildInfo.BUILD_TIME;
        public long   savedAtSeconds = System.currentTimeMillis() / 1000L;

        // Which physical robot produced this snapshot, and the hub network name it was read from
        // (see RobotIdentity). Set by the OpMode; defaults are the fail-closed values.
        public String robot       = "UNKNOWN";
        public String networkName = "(unavailable)";

        public String alliance           = "UNKNOWN";
        public String startPose          = "UNKNOWN";
        public String lastKnownGoodPose  = "UNKNOWN";

        public double  startingBatteryVolts = 0.0;
        public boolean systemsCheckPassed   = false;
        public List<String> systemsCheckNotes = new ArrayList<>();

        // All devices from the RC hardware config: name → connection info (port/bus).
        // getConnectionInfo() is standard FTC SDK API on every HardwareDevice.
        public Map<String, String> hardware = new LinkedHashMap<>();

        // Loop-time stats from the OpMode's LoopTimer at write time — populated via captureLoop().
        // Watch these across sessions to spot regressions caused by code changes (§0 prime directive).
        public double avgLoopHz  = 0.0;  // smoothed average loop rate
        public double avgLoopMs  = 0.0;  // smoothed average cycle time
        public double maxLoopMs  = 0.0;  // worst single-cycle time since reset (tail latency)

        // Every registered tunable at the time of the write — auto-captured via reflection.
        // Keys are namespaced "ClassName.fieldName" so multiple @Configurable classes don't collide.
        public Map<String, Object> tuning = new LinkedHashMap<>();

        /** Populate loop stats from the OpMode's LoopTimer. Call once at stop, never in the loop. */
        public void captureLoop(LoopTimer timer) {
            double rawAvgMs = timer.getAverageMs();
            double rawAvgHz = rawAvgMs > 0.0 ? 1000.0 / rawAvgMs : 0.0;
            avgLoopMs = roundTo1Decimal(rawAvgMs);
            avgLoopHz = roundTo1Decimal(rawAvgHz);
            maxLoopMs = roundTo1Decimal(timer.getMaxLoopMs());
        }

        private static double roundTo1Decimal(double value) {
            return Math.round(value * 10.0) / 10.0;
        }
    }

    /** AUTO-EXPORT with hardware capture. Safe on init and stop. NEVER in the loop. */
    public static void writeSnapshot(Snapshot snapshot, HardwareMap hardwareMap) {
        captureHardware(snapshot, hardwareMap);
        writeSnapshot(snapshot);
    }

    /** AUTO-EXPORT without hardware capture. Prefer the two-arg overload when hardwareMap is available. */
    public static void writeSnapshot(Snapshot snapshot) {
        captureTuningInto(snapshot.tuning);
        try {
            // Per-robot filename so pulling snapshots from both robots into one folder never clobbers,
            // and each file is self-describing (see snapshotFileFor).
            File file = AppUtil.getInstance().getSettingsFile(snapshotFileFor(snapshot.robot));
            file.getParentFile().mkdirs();
            String json = GSON.toJson(snapshot);
            ReadWriteFile.writeFile(file, json);
            RobotLog.i("Persistence: snapshot OK → %s", file.getAbsolutePath());
            RobotLog.i("SNAPSHOT:%s", json);
        } catch (Throwable t) {
            RobotLog.e("Persistence: snapshot FAILED: %s", t.getMessage());
            Log.e(TAG, "Failed to write snapshot", t);
        }
    }

    // -------------------------------------------------------------------------
    // Tuning backup — session persistence
    // -------------------------------------------------------------------------

    /**
     * The tuning file name for a robot, or null if identity is UNKNOWN. Pure + package-private so it
     * can be unit-tested off-robot (§9). UNKNOWN returns null on purpose: fail closed — never
     * load/save tuning for a hub we can't identify.
     */
    public static String tuningFileFor(RobotIdentity.Robot robot) {
        switch (robot) {
            case COMPETITION: return COMP_TUNING_FILE;
            case TESTBOT:     return TESTBOT_TUNING_FILE;
            default:          return null; // UNKNOWN
        }
    }

    /** Per-robot snapshot file name. {@code robot} is Snapshot.robot (enum name, or "UNKNOWN"). Pure. */
    public static String snapshotFileFor(String robot) {
        String tag = (robot == null || robot.trim().isEmpty()) ? "UNKNOWN" : robot.trim();
        return "snacktime_snapshot_" + tag + ".json";
    }

    /**
     * Saves every registered tunable to THIS robot's tuning file, using namespaced
     * "ClassName.fieldName" keys. Call on every OpMode stop/reset. NEVER in the loop.
     * UNKNOWN identity saves nothing (fail closed) — we never want to persist tuning we can't
     * attribute to a specific robot.
     */
    public static void saveTuning(RobotIdentity id) {
        String fileName = tuningFileFor(id.robot);
        if (fileName == null) {
            RobotLog.i("Persistence: robot UNKNOWN — tuning NOT saved (fail closed)");
            return;
        }
        try {
            File file = AppUtil.getInstance().getSettingsFile(fileName);
            file.getParentFile().mkdirs();
            Map<String, Object> values = new LinkedHashMap<>();
            captureTuningInto(values);
            ReadWriteFile.writeFile(file, GSON.toJson(values));
            RobotLog.i("Persistence: %s tuning saved → %s", id.robot, file.getAbsolutePath());
        } catch (Throwable t) {
            RobotLog.e("Persistence: tuning save FAILED: %s", t.getMessage());
            Log.e(TAG, "Failed to save tuning", t);
        }
    }

    /**
     * Loads THIS robot's tuning file and applies every value back to the live tunable statics on each
     * class in TUNING_CLASSES. Telemeters loudly — required by CLAUDE.md §7.
     * Call on every OpMode init. NEVER in the loop.
     *
     * FAIL CLOSED: if identity is UNKNOWN, loads NOTHING and says so loudly — the robot runs on the
     * in-code defaults (which are the COMPETITION values). An unidentified hub is never given the
     * comp robot's saved tuning by accident, nor the test bot's.
     *
     * @return true if a tuning file was found and applied; false if running from code defaults.
     */
    public static boolean loadAndApplyTuning(RobotIdentity id, Telemetry telemetry) {
        String fileName = tuningFileFor(id.robot);
        if (fileName == null) {
            String msg = "ROBOT UNKNOWN [" + id.networkName + "] — NO tuning file loaded; running on "
                    + "code defaults (= competition values). Name the hub ...-C-RC or ...-T-RC.";
            if (telemetry != null) telemetry.addLine(msg);
            RobotLog.i("Persistence: %s", msg);
            return false;
        }
        try {
            File file = AppUtil.getInstance().getSettingsFile(fileName);
            if (!file.exists()) {
                // No session file yet for this robot — code defaults apply. On the test bot those are
                // the competition values until it's tuned; be honest about it rather than silent.
                String msg = String.format(Locale.US,
                        "%s: no tuning file yet (%s) — running on code defaults", id.robot, fileName);
                if (telemetry != null) telemetry.addLine(msg);
                RobotLog.i("Persistence: %s", msg);
                return false;
            }

            Map<String, Object> values = GSON.fromJson(
                    ReadWriteFile.readFile(file),
                    new TypeToken<Map<String, Object>>() {}.getType());
            if (values == null || values.isEmpty()) return false;

            int applied = 0;
            for (Class<?> cls : TUNING_CLASSES) {
                String prefix = cls.getSimpleName() + ".";
                for (Field f : cls.getDeclaredFields()) {
                    int mods = f.getModifiers();
                    if (!Modifier.isPublic(mods) || !Modifier.isStatic(mods)) continue;
                    Object val = values.get(prefix + f.getName());
                    if (val == null) continue;
                    try {
                        applyToField(f, val);
                        applied++;
                    } catch (Exception ignored) { }
                }
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new Date(file.lastModified()));
            String msg = String.format(Locale.US,
                    "LOADED %s TUNING (%s, %s) — %d values", id.robot, fileName, timestamp, applied);
            telemetry.addLine(msg);
            RobotLog.i("Persistence: %s", msg);
            return true;

        } catch (Throwable t) {
            RobotLog.e("Persistence: tuning load FAILED: %s", t.getMessage());
            Log.e(TAG, "Failed to load tuning", t);
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void captureHardware(Snapshot snap, HardwareMap hwMap) {
        snap.hardware.clear();
        try {
            List<String> names = new ArrayList<>(hwMap.getAllNames(HardwareDevice.class));
            Collections.sort(names);
            for (String name : names) {
                String info;
                try {
                    info = parseConnectionInfo(hwMap.get(HardwareDevice.class, name).getConnectionInfo());
                } catch (Throwable t) {
                    info = "unknown";
                }
                snap.hardware.put(name, info);
            }
        } catch (Throwable t) {
            snap.hardware.put("ERROR", "failed to enumerate: " + t.getMessage());
        }
    }

    private static void captureTuningInto(Map<String, Object> map) {
        map.clear();
        for (Class<?> cls : TUNING_CLASSES) {
            for (Field f : cls.getDeclaredFields()) {
                int mods = f.getModifiers();
                if (!Modifier.isPublic(mods) || !Modifier.isStatic(mods)) continue;
                try { map.put(cls.getSimpleName() + "." + f.getName(), f.get(null)); }
                catch (Exception ignored) { }
            }
        }
    }

    /**
     * Applies a GSON-deserialized value to a tunable static field.
     * GSON always deserializes JSON numbers as Double, so we convert to the field's actual type.
     */
    private static void applyToField(Field f, Object val) throws IllegalAccessException {
        Class<?> type = f.getType();
        if (type == double.class || type == Double.class) {
            f.set(null, ((Number) val).doubleValue());
        } else if (type == boolean.class || type == Boolean.class) {
            f.set(null, (Boolean) val);
        } else if (type == long.class || type == Long.class) {
            f.set(null, ((Number) val).longValue());
        } else if (type == int.class || type == Integer.class) {
            f.set(null, ((Number) val).intValue());
        }
    }

    /** Extracts "port X" from raw SDK connection strings like "USB (embedded); module 173; port 0". */
    static String parseConnectionInfo(String raw) {
        if (raw == null) return "unknown";
        for (String part : raw.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("port ")) return trimmed;
        }
        return raw.trim(); // fallback: return as-is if no port segment found
    }

    /** Reads the first voltage sensor safely; returns 0.0 if none found or read fails. */
    public static double readBatteryVolts(HardwareMap hardwareMap) {
        try {
            java.util.Iterator<VoltageSensor> it = hardwareMap.voltageSensor.iterator();
            if (it.hasNext()) return it.next().getVoltage();
        } catch (Throwable ignored) {}
        return 0.0;
    }

    private Persistence() { }
}
