package org.firstinspires.ftc.teamcode.opmodes;

import com.bylazar.telemetry.PanelsTelemetry;
import com.pedropathing.follower.Follower;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.util.JoystickCurve;
import org.firstinspires.ftc.teamcode.util.BulkReads;
import org.firstinspires.ftc.teamcode.util.LogCleanup;
import org.firstinspires.ftc.teamcode.util.LoopTimer;
import org.firstinspires.ftc.teamcode.util.Persistence;
import org.firstinspires.ftc.teamcode.util.RobotIdentity;

/**
 * TeleOpExample — field-centric mecanum drive. LEFT_BUMPER = slow mode.
 *
 * Pedro reads the Pinpoint heading and rotates stick inputs to field coordinates each loop.
 * Driver Hub telemetry is minimal and glanceable (CLAUDE.md sections 4, 8).
 */
@TeleOp(name = "34672 TeleOp (example)")
public class TeleOpExample extends CommandOpMode {

    private final LoopTimer loopTimer = new LoopTimer();
    private BulkReads bulkReads;
    private Drivetrain drivetrain;
    private GamepadEx driver;
    private Follower follower;
    private double startBatteryVolts = 0.0;
    private RobotIdentity robotId;
    // Built once at init; reused each loop (§4 rule 8, no per-loop alloc). idBanner (plain) goes to
    // Panels, which has no HTML display-format concept; idBannerHtml (larger/bold/colored) goes to
    // the Driver Station, which does.
    private String idBanner;
    private String idBannerHtml;

    @Override
    public void initialize() {
        // MANUAL bulk caching — the biggest lever on loop time (section 0, section 4 rule 1).
        bulkReads = new BulkReads(hardwareMap);

        // Which robot is this? Read once, from the hub network name (see RobotIdentity).
        robotId = RobotIdentity.resolve();
        idBanner = robotId.banner();
        idBannerHtml = robotId.bannerHtml();
        // Enables the "subset of HTML tags" idBannerHtml relies on for larger/colored text. Affects
        // the whole Driver Station panel, not just this line — other lines have no tags, so they
        // render unchanged.
        telemetry.setDisplayFormat(Telemetry.DisplayFormat.HTML);

        Persistence.loadAndApplyTuning(robotId, telemetry);
        LogCleanup.maybeRun(telemetry); // fires once every 14 days, silent otherwise

        drivetrain = new Drivetrain(hardwareMap);
        driver = new GamepadEx(gamepad1);

        // Pedro drives the wheels; startTeleopDrive() sets it to open-loop mode (§10).
        follower = Constants.createFollower(hardwareMap);
        follower.startTeleopDrive();

        Persistence.Snapshot initSnap = new Persistence.Snapshot();
        initSnap.robot = robotId.robot.name();
        initSnap.networkName = robotId.networkName;
        Persistence.writeSnapshot(initSnap, hardwareMap); // safe: init, not the loop (section 7)
    }

    @Override
    public void run() {
        // RULE 1, NON-NEGOTIABLE: clear the bulk cache FIRST, every loop, always (section 4).
        bulkReads.clear();
        if (startBatteryVolts == 0.0) {
            startBatteryVolts = Persistence.readBatteryVolts(hardwareMap);
            // Deferred here (not init) because the voltage sensor reads 0.0 too early during init.
            // It's an uncached hardware round-trip (voltage reads aren't covered by BulkReads), so
            // reset the timer right after paying that one-time cost — otherwise it wrongly counts
            // toward every session's maxLoopMs, matching LoopTimer.reset()'s own documented intent.
            loopTimer.reset();
        }

        // Read -> process -> write (section 4, rule 2).
        double cap = driver.getButton(GamepadKeys.Button.LEFT_BUMPER)
                ? Drivetrain.driveSlowModeCap
                : Drivetrain.driveSpeedCap;

        // Field-centric: Pedro rotates strafe/forward by the Pinpoint heading before applying power.
        // Sign convention verified on-robot 2026-07-18: forward was inverted vs. PedroTeleOpSample's
        // -leftY (strafe/turn matched as-is). Pedro's Line test drove the correct physical direction
        // autonomously, so this is a TeleOp-mapping-only flip, not a motor-wiring issue.
        double dz = JoystickCurve.deadzone;
        double forward = applyDeadzone(driver.getLeftY(), dz);
        double strafe  = applyDeadzone(-driver.getLeftX(), dz);
        double turn    = applyDeadzone(-driver.getRightX(), dz);
        follower.setTeleOpDrive(forward * cap, strafe * cap, turn * cap, false);
        follower.update();

        // Runs the command scheduler + every subsystem's periodic().
        super.run();

        // Loop-time readout is REQUIRED (section 0 prime directive, section 4 rule 7).
        // Pass numbers, not hand-built strings (rule 8). Watch Loop Hz for regressions.
        loopTimer.update();
        // Robot identity banner FIRST, so "which robot am I on?" is always the top line — larger/
        // colored on the Driver Hub (HTML), plain text mirrored to Panels. Pre-built strings, so no
        // per-loop allocation (§4 rule 8).
        telemetry.addLine(idBannerHtml);
        PanelsTelemetry.INSTANCE.getTelemetry().debug(idBanner);
        telemetry.addData("Loop Hz", loopTimer.getHz());
        telemetry.addData("Worst ms", loopTimer.getMaxLoopMs());
        telemetry.addData("X in", follower.getPose().getX());
        telemetry.addData("Y in", follower.getPose().getY());
        telemetry.addData("Heading °", Math.toDegrees(follower.getPose().getHeading()));
        telemetry.update();
    }

    /** Returns 0 if |value| is within the deadzone, otherwise passes value through unchanged. */
    private static double applyDeadzone(double value, double deadzone) {
        return Math.abs(value) < deadzone ? 0.0 : value;
    }

    @Override
    public void reset() {
        follower.breakFollowing();
        drivetrain.stop();
        Persistence.saveTuning(robotId);
        Persistence.Snapshot stopSnap = new Persistence.Snapshot();
        stopSnap.robot = robotId.robot.name();
        stopSnap.networkName = robotId.networkName;
        stopSnap.startingBatteryVolts = startBatteryVolts;
        stopSnap.captureLoop(loopTimer); // loop-time trend data (§0)
        Persistence.writeSnapshot(stopSnap, hardwareMap); // post-match record (section 7)
        CommandScheduler.getInstance().reset();
    }
}
