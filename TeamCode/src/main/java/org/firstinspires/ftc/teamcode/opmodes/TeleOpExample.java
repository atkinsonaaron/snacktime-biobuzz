package org.firstinspires.ftc.teamcode.opmodes;

import com.pedropathing.follower.Follower;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;
import com.seattlesolvers.solverslib.gamepad.GamepadEx;
import com.seattlesolvers.solverslib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.config.TuningConfig;
import org.firstinspires.ftc.teamcode.pedroPathing.Constants;
import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.util.BulkReads;
import org.firstinspires.ftc.teamcode.util.LoopTimer;
import org.firstinspires.ftc.teamcode.util.Persistence;

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

    @Override
    public void initialize() {
        // MANUAL bulk caching — the biggest lever on loop time (section 0, section 4 rule 1).
        bulkReads = new BulkReads(hardwareMap);

        drivetrain = new Drivetrain(hardwareMap);
        driver = new GamepadEx(gamepad1);

        // Pedro drives the wheels; startTeleopDrive() sets it to open-loop mode (§10).
        follower = Constants.createFollower(hardwareMap);
        follower.startTeleopDrive();

        Persistence.writeSnapshot(new Persistence.Snapshot()); // safe: init, not the loop (section 7)
        loopTimer.reset();
    }

    @Override
    public void run() {
        // RULE 1, NON-NEGOTIABLE: clear the bulk cache FIRST, every loop, always (section 4).
        bulkReads.clear();

        // Read -> process -> write (section 4, rule 2).
        double cap = driver.getButton(GamepadKeys.Button.LEFT_BUMPER)
                ? TuningConfig.driveSlowModeCap
                : TuningConfig.driveSpeedCap;

        // Field-centric: Pedro rotates strafe/forward by the Pinpoint heading before applying power.
        // Sign convention from PedroTeleOpSample: (-leftY, -leftX, -rightX).
        // TODO: if a direction is backwards on the robot, flip that sign.
        follower.setTeleOpDrive(
                -driver.getLeftY() * cap,
                -driver.getLeftX() * cap,
                -driver.getRightX() * cap,
                false);
        follower.update();

        // Runs the command scheduler + every subsystem's periodic().
        super.run();

        // Loop-time readout is REQUIRED (section 0 prime directive, section 4 rule 7).
        // Pass numbers, not hand-built strings (rule 8). Watch Loop Hz for regressions.
        loopTimer.update();
        telemetry.addData("Loop Hz", loopTimer.getHz());
        telemetry.addData("Worst ms", loopTimer.getMaxLoopMs());
        telemetry.addData("Heading °", Math.toDegrees(follower.getPose().getHeading()));
        telemetry.update();
    }

    @Override
    public void reset() {
        follower.breakFollowing();
        drivetrain.stop();
        Persistence.writeSnapshot(new Persistence.Snapshot()); // post-match record (section 7)
        CommandScheduler.getInstance().reset();
    }
}
