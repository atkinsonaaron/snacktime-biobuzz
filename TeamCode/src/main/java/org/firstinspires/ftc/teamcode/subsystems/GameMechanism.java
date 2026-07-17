package org.firstinspires.ftc.teamcode.subsystems;

import com.bylazar.telemetry.PanelsTelemetry;
import com.seattlesolvers.solverslib.command.InstantCommand;
import com.seattlesolvers.solverslib.command.SubsystemBase;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.config.TuningConfig;

/**
 * GameMechanism — template for a game-specific mechanism (add at kickoff).
 *
 * Pattern:
 *   - Declare hardware objects here (motors, servos, sensors for this mechanism).
 *   - Expose intent-level methods (e.g. collect(), eject(), stop()).
 *   - Wrap each in an InstantCommand so TeleOp can bind it to a button
 *     and Auto can compose it into a command tree.
 *   - periodic() publishes health telemetry gated on verboseTelemetry (CLAUDE.md §4 rule 8).
 *   - Extract any math into a pure function in logic/ so it can be unit-tested off-robot (§9).
 *   - Every tunable number belongs in TuningConfig, not hardcoded here (§6 Tier 1).
 *
 * Add config names to CLAUDE.md §10 hardware map once locked in.
 */
public class GameMechanism extends SubsystemBase {

    // TODO: declare hardware objects
    // e.g. private final MotorEx motor;
    //      private final Servo servo;

    public GameMechanism(HardwareMap hardwareMap) {
        // TODO: init hardware from hardwareMap
        // e.g. motor = new MotorEx(hardwareMap, "mechanism_motor");
    }

    // TODO: add intent-level methods for this mechanism
    // public void collect() { ... }
    // public void eject()   { ... }

    public void stop() {
        // TODO: set all outputs to safe/stopped state
    }

    // Command wrappers for button bindings and command-tree composition (CLAUDE.md §3).
    // public InstantCommand collectCommand() { return new InstantCommand(this::collect, this); }
    // public InstantCommand ejectCommand()   { return new InstantCommand(this::eject,   this); }
    public InstantCommand stopCommand()    { return new InstantCommand(this::stop,    this); }

    @Override
    public void periodic() {
        if (TuningConfig.verboseTelemetry) {
            // TODO: publish mechanism health (current, position, target-vs-actual)
            // PanelsTelemetry.INSTANCE.getTelemetry().debug("mechanism state: " + ...);
        }
    }
}