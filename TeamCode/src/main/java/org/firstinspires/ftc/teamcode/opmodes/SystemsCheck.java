package org.firstinspires.ftc.teamcode.opmodes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.teamcode.util.LogCleanup;
import org.firstinspires.ftc.teamcode.util.Persistence;
import org.firstinspires.ftc.teamcode.util.RobotIdentity;

import java.util.ArrayList;
import java.util.List;

/**
 * SystemsCheck — the pre-match diagnostic OpMode (CLAUDE.md section 5).
 *
 * Run this BEFORE every match. It confirms each motor is present and configured, then pulses each
 * one so a human can verify the RIGHT mechanism moves. A wiring or config fault (a dead motor, a
 * swapped port — the kind of thing the back-left slip was) shows up on the bench instead of
 * mid-match. The result is stamped into a snapshot (section 7).
 *
 * Written as a plain LinearOpMode ON PURPOSE: it must still work even if the framework layer is
 * broken. That's the whole point of a diagnostic.
 */
@TeleOp(name = "34672 Systems Check", group = "diagnostics")
public class SystemsCheck extends LinearOpMode {

    // Names must match the Robot Controller configuration exactly (CLAUDE.md §10).
    // Add game-mechanism motors here when they are wired and configured.
    private static final String[] MOTOR_NAMES = {
            "LF_Motor", "LR_Motor", "RF_Motor", "RR_Motor"
    };

    @Override
    public void runOpMode() {
        // Which robot is this? Resolve FIRST — tuning load is per-robot (see RobotIdentity).
        RobotIdentity robotId = RobotIdentity.resolve();
        Persistence.loadAndApplyTuning(robotId, telemetry);
        LogCleanup.maybeRun(telemetry); // fires once every 14 days, silent otherwise

        List<String> notes = new ArrayList<>();
        boolean passed = true;

        // --- which robot is this? (from the hub network name) ---
        notes.add(robotId.banner());
        if (!robotId.isKnown()) {
            // Not a hard fail — SystemsCheck runs on both robots, and an unnamed bench hub is a
            // legitimate state. But it means tuning selection failed closed (loaded nothing), so
            // say so loudly here rather than let it surprise anyone later.
            notes.add("WARN robot identity UNKNOWN — set the hub name to ...-C-RC or ...-T-RC");
        }

        // --- check each motor is present and configured ---
        for (String name : MOTOR_NAMES) {
            boolean ok;
            try {
                hardwareMap.get(DcMotorEx.class, name);
                ok = true;
            } catch (Throwable t) {
                ok = false;
            }
            notes.add(ok ? ("OK   motor '" + name + "' found")
                         : ("FAIL motor '" + name + "' MISSING"));
            if (!ok) passed = false;
        }

        // ─────────────────────────────────────────────────────────────────────────────────────
        // TODO: SystemsCheck build-out — planned checks, not yet implemented.
        //
        //  (a) Status-LED indicator (see CLAUDE.md §5 "deterministic init, fail loud").
        //      Drive an indicator from the pass/warn/fail state so the answer is glanceable at
        //      the bench BEFORE START, not buried in telemetry:
        //          RED    = a check FAILED (missing motor, low battery, identity UNKNOWN)
        //          YELLOW = passed-with-warnings
        //          GREEN  = all clear, cleared to play
        //      Primary target: the Control Hub's onboard LED — it's a LynxModule that implements
        //      Blinker, reachable via hardwareMap.getAll(LynxModule.class) (same handle BulkReads
        //      grabs): hub.setConstant(Color.RED). Zero extra hardware; visible at the bench only.
        //      Optional: also drive a REV Blinkin + LED strip (runs as a servo — see the
        //      SampleRevBlinkinLedDriver in FtcRobotController samples) so the DRIVE TEAM can see
        //      it across the field. Wrap both behind a util/StatusLED helper that degrades
        //      gracefully if the Blinkin isn't wired. LOOP-COST NOTE (§0/§4): set the LED only on
        //      state CHANGE — it's a bus write — never every loop.
        //      NOTE: the Driver Hub's open USB port canNOT drive an LED (no SDK path); all
        //      indicator logic lives on the Control Hub side.
        //
        //  (b) Stick-at-rest / drift check. Before START, confirm every gamepad axis reads within
        //      the deadzone — i.e. |axis| < JoystickCurve.deadzone (the "zero point", currently
        //      0.05). A stick that rests outside the deadzone will command drive the instant the
        //      match starts. Check driver + operator left/right X/Y (and triggers) and FAIL loud
        //      if any is drifting, naming the offending stick.
        //
        //  (c) Sensor checks — Limelight reachable, Pinpoint (I2C) responding, rangefinder reading.
        // ─────────────────────────────────────────────────────────────────────────────────────

        // --- battery voltage is a cheap, high-value pre-match check ---
        double volts = 0.0;
        try {
            volts = hardwareMap.voltageSensor.iterator().next().getVoltage();
            notes.add(String.format("Battery: %.2f V", volts));
            if (volts < 12.0) {
                notes.add("WARN battery is low — charge before the match");
            }
        } catch (Throwable t) {
            notes.add("WARN could not read battery voltage");
        }

        telemetry.addLine(passed ? "SYSTEMS CHECK: PASS" : "SYSTEMS CHECK: *** FAIL ***");
        for (String note : notes) telemetry.addLine(note);
        telemetry.addLine("Press START to pulse each motor, or STOP to exit.");
        telemetry.update();

        // Save the result before we even start (section 7).
        Persistence.Snapshot snap = new Persistence.Snapshot();
        snap.systemsCheckPassed = passed;
        snap.systemsCheckNotes = notes;
        snap.startingBatteryVolts = volts;
        snap.robot = robotId.robot.name();
        snap.networkName = robotId.networkName;
        Persistence.writeSnapshot(snap, hardwareMap);

        waitForStart();
        if (!opModeIsActive()) return;

        // Active check: pulse each motor so a human confirms the RIGHT mechanism moves.
        for (String name : MOTOR_NAMES) {
            if (!opModeIsActive()) break;
            DcMotorEx motor;
            try {
                motor = hardwareMap.get(DcMotorEx.class, name);
            } catch (Throwable t) {
                continue;
            }
            telemetry.addLine("Pulsing '" + name + "' — watch that the correct mechanism moves.");
            telemetry.update();
            motor.setPower(0.25);
            sleep(400);
            motor.setPower(0.0);
            sleep(300);
        }

        telemetry.addLine("Systems check complete.");
        telemetry.update();
        Persistence.saveTuning(robotId);
    }
}
