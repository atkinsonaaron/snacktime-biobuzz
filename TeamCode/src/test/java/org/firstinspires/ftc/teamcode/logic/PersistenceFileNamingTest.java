package org.firstinspires.ftc.teamcode.logic;

import org.firstinspires.ftc.teamcode.util.Persistence;
import org.firstinspires.ftc.teamcode.util.RobotIdentity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;

/**
 * Off-robot unit tests for the robot-aware file selection in Persistence — the pure logic that
 * decides which tuning/snapshot file belongs to which robot. Run with ./gradlew :TeamCode:test.
 *
 * The safety-critical case is UNKNOWN -> null (fail closed): an unidentified hub must NEVER be
 * handed the competition robot's tuning file, so tuningFileFor must return null and callers skip
 * loading/saving. These tests lock that behavior in.
 */
public class PersistenceFileNamingTest {

    // ---- tuningFileFor -----------------------------------------------------------

    @Test
    public void competition_getsCompFile() {
        assertEquals("comp_tuning.json",
                Persistence.tuningFileFor(RobotIdentity.Robot.COMPETITION));
    }

    @Test
    public void testbot_getsItsOwnFile() {
        assertEquals("testbot_tuning.json",
                Persistence.tuningFileFor(RobotIdentity.Robot.TESTBOT));
    }

    @Test
    public void unknown_getsNoFile_failClosed() {
        // The whole safety guarantee: UNKNOWN never resolves to a real tuning file.
        assertNull(Persistence.tuningFileFor(RobotIdentity.Robot.UNKNOWN));
    }

    @Test
    public void compAndTest_neverShareAFile() {
        assertNotEquals(Persistence.tuningFileFor(RobotIdentity.Robot.COMPETITION),
                Persistence.tuningFileFor(RobotIdentity.Robot.TESTBOT));
    }

    // ---- snapshotFileFor ---------------------------------------------------------

    @Test
    public void snapshot_isPerRobot() {
        assertEquals("snacktime_snapshot_COMPETITION.json", Persistence.snapshotFileFor("COMPETITION"));
        assertEquals("snacktime_snapshot_TESTBOT.json",     Persistence.snapshotFileFor("TESTBOT"));
    }

    @Test
    public void snapshot_nullOrBlank_fallsBackToUnknown() {
        assertEquals("snacktime_snapshot_UNKNOWN.json", Persistence.snapshotFileFor(null));
        assertEquals("snacktime_snapshot_UNKNOWN.json", Persistence.snapshotFileFor("   "));
    }
}