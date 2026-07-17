package org.firstinspires.ftc.teamcode.util;

/**
 * ServoUtil — pure math helpers for servo control.
 *
 * FTC servos accept positions from 0.0 to 1.0, which maps across the servo's physical
 * rotation range. Each mechanism subsystem supplies its own range and soft-limit constants;
 * this class just does the math so the logic lives in one testable place (CLAUDE.md §9).
 *
 * How to tell if it's working: set a target angle, watch the servo move to that position.
 * If it overshoots the mechanism's physical limits, tighten MIN_DEG / MAX_DEG in the subsystem.
 *
 * Usage in a subsystem:
 *   private static final double RANGE_DEG = 300.0; // check servo spec sheet
 *   private static final double MIN_DEG   = 10.0;  // soft lower limit
 *   private static final double MAX_DEG   = 170.0; // soft upper limit
 *
 *   servo.setPosition(ServoUtil.degreesToPositionClamped(targetDeg, MIN_DEG, MAX_DEG, RANGE_DEG));
 */
public class ServoUtil {

    /**
     * Converts degrees to a servo position in [0.0, 1.0].
     *
     * @param degrees      target angle in degrees
     * @param rangeDegrees the servo's full physical rotation range (e.g. 300 for a goBILDA Torque)
     * @return servo position 0.0–1.0
     */
    public static double degreesToPosition(double degrees, double rangeDegrees) {
        return degrees / rangeDegrees;
    }

    /**
     * Clamps degrees to [minDeg, maxDeg] then converts to a servo position in [0.0, 1.0].
     *
     * Always prefer this over degreesToPosition — every actuator needs soft limits to prevent
     * a bad command from driving a mechanism into a hard stop (CLAUDE.md §5).
     *
     * @param degrees      target angle in degrees
     * @param minDeg       soft lower limit in degrees
     * @param maxDeg       soft upper limit in degrees
     * @param rangeDegrees the servo's full physical rotation range (e.g. 300 for a goBILDA Torque)
     * @return servo position 0.0–1.0
     */
    public static double degreesToPositionClamped(double degrees, double minDeg, double maxDeg, double rangeDegrees) {
        double clamped = Math.max(minDeg, Math.min(maxDeg, degrees));
        return clamped / rangeDegrees;
    }

    /**
     * Converts a servo position (0.0–1.0) back to degrees — useful for telemetry.
     *
     * @param position     servo position 0.0–1.0
     * @param rangeDegrees the servo's full physical rotation range
     * @return angle in degrees
     */
    public static double positionToDegrees(double position, double rangeDegrees) {
        return position * rangeDegrees;
    }

    private ServoUtil() { } // static holder; never instantiated
}