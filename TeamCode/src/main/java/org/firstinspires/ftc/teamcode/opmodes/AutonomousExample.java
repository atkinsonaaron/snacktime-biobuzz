package org.firstinspires.ftc.teamcode.opmodes;

import com.seattlesolvers.solverslib.command.Command;
import com.seattlesolvers.solverslib.command.CommandOpMode;
import com.seattlesolvers.solverslib.command.CommandScheduler;
import com.seattlesolvers.solverslib.command.SequentialCommandGroup;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

import org.firstinspires.ftc.teamcode.subsystems.Drivetrain;
import org.firstinspires.ftc.teamcode.util.BulkReads;
import org.firstinspires.ftc.teamcode.util.LoopTimer;
import org.firstinspires.ftc.teamcode.util.Persistence;

/**
 * AutonomousExample — shows the command-tree structure (CLAUDE.md section 3) that REPLACES a
 * hand-rolled state machine. The routine reads top-to-bottom like a plan.
 *
 * ALLIANCE / START POSE (section 9): chosen in ONE place (below), so the robot never runs from the
 * wrong pose. Wire these to a pre-match selector or a Panels configurable.
 *
 * PEDRO: add the follower here via SolversLib's pedroPathing module, and compose FollowPath-style
 * commands into the tree below. Remember (section 2): we install Pedro OURSELVES at the latest
 * version — SolversLib's module does not bundle it.
 */
@Autonomous(name = "34672 Auto (example)")
public class AutonomousExample extends CommandOpMode {

    // -- the single source of match context ------------------------------------------
    private static final String ALLIANCE = "RED";          // TODO: select via gamepad/Panels
    private static final String START_POSE_LABEL = "RED_LEFT";

    private final LoopTimer loopTimer = new LoopTimer();
    private BulkReads bulkReads;
    private Drivetrain drivetrain;

    @Override
    public void initialize() {
        bulkReads = new BulkReads(hardwareMap);
        drivetrain = new Drivetrain(hardwareMap);

        // TODO: create the Pedro follower and set the starting pose from START_POSE_LABEL.

        Persistence.writeSnapshot(snapshot());  // AUTO-EXPORT on init is safe (section 7)

        // The whole autonomous, as a composed command tree. No switch statement.
        schedule(routine());
        loopTimer.reset();
    }

    private Command routine() {
        return new SequentialCommandGroup(
                // TODO: add Pedro path-follow commands and game mechanism commands here.
                // e.g. new ParallelCommandGroup(followPath, mechanism.collectCommand())
        );
    }

    @Override
    public void run() {
        // RULE 1, NON-NEGOTIABLE: clear the bulk cache FIRST, every loop, always (section 4).
        bulkReads.clear();

        super.run();   // command scheduler + subsystem periodics

        // Loop-time readout is REQUIRED (section 0, section 4 rule 7).
        loopTimer.update();
        telemetry.addData("Loop Hz", loopTimer.getHz());
        telemetry.addData("Worst ms", loopTimer.getMaxLoopMs());
        telemetry.update();
    }

    @Override
    public void reset() {
        drivetrain.stop();
        Persistence.writeSnapshot(snapshot());   // post-match record (section 7)
        CommandScheduler.getInstance().reset();
    }

    private Persistence.Snapshot snapshot() {
        Persistence.Snapshot s = new Persistence.Snapshot();
        s.alliance = ALLIANCE;
        s.startPose = START_POSE_LABEL;
        // TODO: add tuning values here as you dial them in (CLAUDE.md §7)
        return s;
    }
}
