package org.firstinspires.ftc.teamcode.logic;

import org.firstinspires.ftc.teamcode.util.ServoUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Off-robot unit tests for ServoUtil — run with ./gradlew :TeamCode:test (no robot needed).
 */
public class ServoUtilTest {

    private static final double DELTA = 0.0001; // tolerance for floating-point comparisons
    private static final double RANGE = 300.0;

    // ---- degreesToPosition -------------------------------------------------------

    @Test
    public void degreesToPosition_zero_returnsZero() {
        assertEquals(0.0, ServoUtil.degreesToPosition(0, RANGE), DELTA);
    }

    @Test
    public void degreesToPosition_fullRange_returnsOne() {
        assertEquals(1.0, ServoUtil.degreesToPosition(300, RANGE), DELTA);
    }

    @Test
    public void degreesToPosition_midpoint_returnsHalf() {
        assertEquals(0.5, ServoUtil.degreesToPosition(150, RANGE), DELTA);
    }

    // ---- degreesToPositionClamped ------------------------------------------------

    @Test
    public void clamped_withinLimits_convertsNormally() {
        // 90° with 300° range = 0.3, no clamping needed
        assertEquals(0.3, ServoUtil.degreesToPositionClamped(90, 10, 170, RANGE), DELTA);
    }

    @Test
    public void clamped_belowMin_clampsToMin() {
        // 0° is below min of 10° — should clamp to 10°
        assertEquals(ServoUtil.degreesToPosition(10, RANGE),
                ServoUtil.degreesToPositionClamped(0, 10, 170, RANGE), DELTA);
    }

    @Test
    public void clamped_aboveMax_clampsToMax() {
        // 200° is above max of 170° — should clamp to 170°
        assertEquals(ServoUtil.degreesToPosition(170, RANGE),
                ServoUtil.degreesToPositionClamped(200, 10, 170, RANGE), DELTA);
    }

    @Test
    public void clamped_exactlyAtMin_passes() {
        assertEquals(ServoUtil.degreesToPosition(10, RANGE),
                ServoUtil.degreesToPositionClamped(10, 10, 170, RANGE), DELTA);
    }

    @Test
    public void clamped_exactlyAtMax_passes() {
        assertEquals(ServoUtil.degreesToPosition(170, RANGE),
                ServoUtil.degreesToPositionClamped(170, 10, 170, RANGE), DELTA);
    }

    // ---- positionToDegrees (round-trip) -----------------------------------------

    @Test
    public void positionToDegrees_roundTrip() {
        double original = 120.0;
        double position = ServoUtil.degreesToPosition(original, RANGE);
        assertEquals(original, ServoUtil.positionToDegrees(position, RANGE), DELTA);
    }
}