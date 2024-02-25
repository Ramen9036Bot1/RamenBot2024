package frc.robot;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.commands.PathPlannerAuto;
import com.pathplanner.lib.util.HolonomicPathFollowerConfig;
import com.pathplanner.lib.util.PIDConstants;
import com.pathplanner.lib.util.ReplanningConfig;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ParallelCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.OperatorConstants;
import frc.robot.Constants.PresetConstants;
import frc.robot.Constants.ShooterConstants;
import frc.robot.Constants.CommandsConstants.SetArmConstants;
import frc.robot.subsystems.ArmSystem;
import frc.robot.subsystems.HookSystem;
import frc.robot.subsystems.IntakeSystem;
import frc.robot.subsystems.ShooterSystem;
import frc.robot.commands.IntakeRevCommand;
import frc.robot.commands.SetArmToAngleCommand;
import frc.robot.commands.SetIntakeSpeedCommand;
import frc.robot.commands.SetShooterSpeedCommand;
import frc.robot.commands.VisionAutoAlignCommand;
import frc.robot.subsystems.SwerveDriveSystem;
import frc.robot.subsystems.VisionSystem;
import frc.robot.util.AppliedController;

/**
 * RobotContainer.
 */
public class RobotContainer {
    private final AppliedController m_driveController = new AppliedController(
            OperatorConstants.driveControllerPort);
    private final AppliedController m_armController = new AppliedController(
            OperatorConstants.armControllerPort);

    private SwerveDriveSystem m_swerveDrive = new SwerveDriveSystem(m_driveController);
    private VisionSystem m_visionSystem = new VisionSystem();

    private ShooterSystem m_shooterSystem = new ShooterSystem();
    private ArmSystem m_armSystem = new ArmSystem(m_armController);
    private IntakeSystem m_intakeSystem = new IntakeSystem();
    private HookSystem m_hookSystem = new HookSystem(m_armController);

    public SendableChooser<Command> AutoChooser;

    public RobotContainer() {
        double pullBackNoteTime = 0;
        double pullBackNoteSpeed = 0.2;
        initShuffleBoard();
        NamedCommands.registerCommand(
                "Set Arm To Ground",
                new SetArmToAngleCommand(m_armSystem, SetArmConstants.armMin));
        NamedCommands.registerCommand(
                "Set Arm To Shoot",
                new SetArmToAngleCommand(m_armSystem, PresetConstants.speakerPresetAngleRadians));
        NamedCommands.registerCommand(
                "Shoot Notes",
                new ParallelCommandGroup(
                        new SetShooterSpeedCommand(m_shooterSystem,
                                pullBackNoteTime,
                                -pullBackNoteSpeed),
                        new SetIntakeSpeedCommand(m_intakeSystem,
                                pullBackNoteTime,
                                pullBackNoteSpeed))
                        .andThen(
                                new IntakeRevCommand(m_intakeSystem,
                                        m_shooterSystem,
                                        m_armController)));
    }

    public void scheduleAutonomousCommand() {
        m_swerveDrive.resetGyroFieldRelativeAuto();
        m_swerveDrive.resetPose(
                new Pose2d(new Translation2d(),
                        Rotation2d.fromRadians(m_swerveDrive.getAnglePosition())));

        AutoBuilder.configureHolonomic(
                m_swerveDrive::getPoseMeters,
                m_swerveDrive::resetPose,
                m_swerveDrive::getSpeeds,
                m_swerveDrive::driveFromChassisSpeeds,
                new HolonomicPathFollowerConfig(
                        new PIDConstants(4),
                        new PIDConstants(0.1),
                        2,
                        m_swerveDrive.getDriveBaseRadius(),
                        new ReplanningConfig()),
                () -> {
                    var alliance = DriverStation.getAlliance();
                    if (alliance.isPresent()) {
                        return alliance.get() != DriverStation.Alliance.Red;
                    }
                    return true;
                },
                m_swerveDrive);
        Command auto = new PathPlannerAuto("Bottom to Speaker");
        auto.schedule();
    }

    private void initShuffleBoard() {
        Shuffleboard.getTab("Arm").addDouble(
                "Angle to Shoot",
                () -> m_armSystem.getShootingAngle(m_visionSystem.getDistanceMetersY())
                        + ShooterConstants.shootOffsetLimeLight);
    }

    /**
     * This is the single place that joystick triggers/buttons are bound to specific commands.
     */
    public void bindCommands() {
        // Push note piece back on start up. May not need to happen when reflectometer is used.
        double pullBackNoteTime = 0.1;
        double pullBackNoteSpeed = 0.2;
        double waitTime = 0.2;
        new Trigger(() -> m_armController.getAButton()).onTrue(
                new ParallelCommandGroup(
                        new SetShooterSpeedCommand(m_shooterSystem, pullBackNoteTime,
                                -pullBackNoteSpeed),
                        new SetIntakeSpeedCommand(m_intakeSystem, pullBackNoteTime,
                                pullBackNoteSpeed))
                        .andThen(new WaitCommand(waitTime))
                        .andThen(
                                new IntakeRevCommand(m_intakeSystem, m_shooterSystem,
                                        m_armController)));

        new Trigger(() -> m_armController.getBButton()).onTrue(
                new SetArmToAngleCommand(m_armSystem, m_armSystem.getShootingAngle(
                        m_visionSystem.getDistanceMetersY())
                        + ShooterConstants.shootOffsetLimeLight).andThen(
                                new WaitCommand(waitTime).andThen(
                                        new ParallelCommandGroup(
                                                new SetShooterSpeedCommand(m_shooterSystem,
                                                        pullBackNoteTime,
                                                        -pullBackNoteSpeed),
                                                new SetIntakeSpeedCommand(m_intakeSystem,
                                                        pullBackNoteTime,
                                                        pullBackNoteSpeed))
                                                .andThen(
                                                        new IntakeRevCommand(m_intakeSystem,
                                                                m_shooterSystem,
                                                                m_armController)))));

        // Amp Preset
        new Trigger(() -> m_armController.getXButton()).onTrue(
                new SetArmToAngleCommand(m_armSystem, PresetConstants.ampPresetAngleRadians));

        // Sub-woofer Preset
        new Trigger(() -> m_armController.getYButton()).onTrue(
                new SetArmToAngleCommand(m_armSystem, PresetConstants.speakerPresetAngleRadians));

        // Auto-align
        new Trigger(() -> m_driveController.getAButton()).onTrue(
                new VisionAutoAlignCommand(m_swerveDrive, m_visionSystem));
    }

    public void stopRobot() {
        m_swerveDrive.stopSystem();
        m_visionSystem.stopSystem();

        m_shooterSystem.stopSystem();
        m_armSystem.stopSystem();
        m_intakeSystem.stopSystem();
        m_hookSystem.stopSystem();
    }
}
