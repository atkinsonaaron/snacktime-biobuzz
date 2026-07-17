package org.firstinspires.ftc.teamcode.pedroPathing;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.ftc.FollowerBuilder;
import com.pedropathing.ftc.drivetrains.MecanumConstants;
import com.pedropathing.ftc.localization.constants.PinpointConstants;
import com.pedropathing.paths.PathConstraints;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class Constants {
    public static FollowerConstants followerConstants = new FollowerConstants();

    public static PathConstraints pathConstraints = new PathConstraints(0.99, 100, 1, 1);

    // Motor names must match Robot Controller configuration exactly (CLAUDE.md §10).
    // TODO: verify directions on the robot — push forward, all four wheels should drive forward.
    // If one wheel runs backwards, flip its Direction here.
    public static MecanumConstants mecanumConstants = new MecanumConstants()
            .leftFrontMotorName("LF_Motor")
            .leftRearMotorName("LR_Motor")
            .rightFrontMotorName("RF_Motor")
            .rightRearMotorName("RR_Motor")
            .leftFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
            .leftRearMotorDirection(DcMotorSimple.Direction.REVERSE)
            .rightFrontMotorDirection(DcMotorSimple.Direction.FORWARD)
            .rightRearMotorDirection(DcMotorSimple.Direction.FORWARD);

    // Pinpoint localizer (CLAUDE.md §10). Config name matches the RC I2C port name.
    // TODO: measure the pod offsets from the physical robot center and set forwardPodY +
    // strafePodX in inches. Pedro defaults (forwardPodY=1, strafePodX=-2.5) are placeholders
    // — odometry will be inaccurate until these are set to real values.
    public static PinpointConstants pinpointConstants = new PinpointConstants()
            .hardwareMapName("pinpoint")
            .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);

    public static Follower createFollower(HardwareMap hardwareMap) {
        return new FollowerBuilder(followerConstants, hardwareMap)
                .mecanumDrivetrain(mecanumConstants)
                .pinpointLocalizer(pinpointConstants)
                .pathConstraints(pathConstraints)
                .build();
    }
}
