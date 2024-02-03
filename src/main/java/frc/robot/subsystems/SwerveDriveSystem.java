// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.ctre.phoenix.ErrorCode;
import com.ctre.phoenix.sensors.Pigeon2;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.GenericEntry;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.SwerveSystemConstants;
import frc.robot.Constants.SwerveSystemConstants.SwerveSystemDeviceConstants;
import frc.robot.commands.DriveSwerveCommand;
import frc.robot.util.AppliedController;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * SwerveDriveSystem.
 */
public class SwerveDriveSystem extends SubsystemBase {
    GenericEntry m_getPidDriveP;
    GenericEntry m_getPidDriveD;

    GenericEntry m_getPidTurnP;
    GenericEntry m_getPidTurnD;

    public static final boolean isPIDTuning = SwerveSystemConstants.isPIDTuning;

    private final double m_maxSpeed = SwerveSystemConstants.maxSpeedMetersPerSecond;
    private final double m_maxAngularSpeed = SwerveSystemConstants.maxAngularSpeed;
    private boolean m_fieldRelative = true;

    private final Translation2d m_frontLeftLocation = new Translation2d(
            SwerveSystemConstants.frameDistanceToModulesMeters,
            SwerveSystemConstants.frameDistanceToModulesMeters);
    private final Translation2d m_frontRightLocation = new Translation2d(
            SwerveSystemConstants.frameDistanceToModulesMeters,
            -SwerveSystemConstants.frameDistanceToModulesMeters);
    private final Translation2d m_backLeftLocation = new Translation2d(
            -SwerveSystemConstants.frameDistanceToModulesMeters,
            SwerveSystemConstants.frameDistanceToModulesMeters);
    private final Translation2d m_backRightLocation = new Translation2d(
            -SwerveSystemConstants.frameDistanceToModulesMeters,
            -SwerveSystemConstants.frameDistanceToModulesMeters);

    private final SwerveModule m_frontLeft = new SwerveModule(
            SwerveSystemDeviceConstants.frontLeftDriveMotorID,
            SwerveSystemDeviceConstants.frontLeftTurnMotorID,
            SwerveSystemDeviceConstants.frontLeftTurnEncoderChannel,
            SwerveSystemDeviceConstants.frontLeftOffset);

    private final SwerveModule m_frontRight = new SwerveModule(
            SwerveSystemDeviceConstants.frontRightDriveMotorID,
            SwerveSystemDeviceConstants.frontRightTurnMotorID,
            SwerveSystemDeviceConstants.frontRightTurnEncoderChannel,
            SwerveSystemDeviceConstants.frontRightOffset);

    private final SwerveModule m_backLeft = new SwerveModule(
            SwerveSystemDeviceConstants.backLeftDriveMotorID,
            SwerveSystemDeviceConstants.backLeftTurnMotorID,
            SwerveSystemDeviceConstants.backLeftTurnEncoderChannel,
            SwerveSystemDeviceConstants.backLeftOffset);

    private final SwerveModule m_backRight = new SwerveModule(
            SwerveSystemDeviceConstants.backRightDriveMotorID,
            SwerveSystemDeviceConstants.backRightTurnMotorID,
            SwerveSystemDeviceConstants.backRightTurnEncoderChannel,
            SwerveSystemDeviceConstants.backRightOffset);

    private final Pigeon2 m_gyro = new Pigeon2(SwerveSystemConstants.gyroCanID);

    private final SwerveDriveKinematics m_kinematics = new SwerveDriveKinematics(
            m_frontLeftLocation, m_frontRightLocation, m_backLeftLocation, m_backRightLocation);

    private final SwerveDriveOdometry m_odometry = new SwerveDriveOdometry(m_kinematics,
            Rotation2d.fromDegrees(-getAnglePosition()), new SwerveModulePosition[] {
                    m_frontLeft.getPosition(),
                    m_frontRight.getPosition(),
                    m_backLeft.getPosition(),
                    m_backRight.getPosition()
            });

    private AppliedController m_controller;

    public SwerveDriveSystem(AppliedController controller) {
        m_controller = controller;
        initShuffleBoard();
        setDefaultCommand(new DriveSwerveCommand(this, m_controller));
    }

    /**
     * Initializes the Shuffleboard.
     */
    public void initShuffleBoard() {

        Shuffleboard.getTab("Position").addDouble("X Pose Meters: ", () -> getxPosition());
        Shuffleboard.getTab("Position").addDouble("Y Pose Meters: ", () -> getyPosition());
        Shuffleboard.getTab("Position").addDouble("Rotation: ", () -> getAnglePosition());

        // m_frontLeft.displayDesiredStateToDashBoard("Front Left");
        // m_backLeft.displayDesiredStateToDashBoard("Back Left");
        // m_frontRight.displayDesiredStateToDashBoard("Front Right");
        // m_backRight.displayDesiredStateToDashBoard("Back Right");

        // Also display all Swerve values on a SINGLE dashboard using a Grid layout
        displayModuleToSingleSwerveDashV2("Front Left", m_frontLeft);
        displayModuleToSingleSwerveDashV2("Back Left", m_backLeft);
        displayModuleToSingleSwerveDashV2("Front Right", m_frontRight);
        displayModuleToSingleSwerveDashV2("Back Right", m_backRight);

        if (isPIDTuning) {
            m_getPidDriveP = Shuffleboard.getTab("Swerve Tuning")
                    .getLayout("PID Tuning Drive Values", BuiltInLayouts.kList)
                    .add("Drive P", SwerveModule.pidDriveP).withWidget(BuiltInWidgets.kNumberSlider)
                    .withProperties(Map.of("min", 0, "max", 5)).getEntry();
            m_getPidDriveD = Shuffleboard.getTab("Swerve Tuning")
                    .getLayout("PID Tuning Drive Values", BuiltInLayouts.kList)
                    .add("Drive D", SwerveModule.pidDriveD).withWidget(BuiltInWidgets.kNumberSlider)
                    .withProperties(Map.of("min", 0, "max", 5)).getEntry();

            m_getPidTurnP = Shuffleboard.getTab("Swerve Tuning")
                    .getLayout("PID Tuning Turn Values", BuiltInLayouts.kList)
                    .add("Turn P", SwerveModule.pidTurnP).withWidget(BuiltInWidgets.kNumberSlider)
                    .withProperties(Map.of("min", 0, "max", 5)).getEntry();
            m_getPidTurnD = Shuffleboard.getTab("Swerve Tuning")
                    .getLayout("PID Tuning Turn Values", BuiltInLayouts.kList)
                    .add("Turn D", SwerveModule.pidTurnD).withWidget(BuiltInWidgets.kNumberSlider)
                    .withProperties(Map.of("min", 0, "max", 5)).getEntry();

        }
    }

    public void setFieldRelative(boolean fieldRelative) {
        // Sets field relative to true or false dependent on right bumper being pressed
        m_fieldRelative = fieldRelative;
    }

    public void drive(double xspeed, double yspeed, double rot) {
        drive(xspeed, yspeed, rot, m_fieldRelative);
    }

    /**
     * Drive the robot using the given x, y and rotation speeds. Speeds range from [-1, 1].
     */
    public void drive(double xspeed, double yspeed, double rot, boolean fieldRelative) {
        // System.out.println("xSpeed: " + xSpeed + ", ySpeed: " + ySpeed + ", rot: " + rot);

        var swerveModuleStates = m_kinematics.toSwerveModuleStates(fieldRelative ? ChassisSpeeds
                .fromFieldRelativeSpeeds(xspeed, yspeed, rot * m_maxAngularSpeed, getRotation2d())
                : new ChassisSpeeds(xspeed, yspeed, rot * m_maxAngularSpeed));

        SwerveDriveKinematics.desaturateWheelSpeeds(swerveModuleStates, m_maxSpeed);
        m_frontLeft.setDesiredState(swerveModuleStates[0]);
        m_frontRight.setDesiredState(swerveModuleStates[1]);
        m_backLeft.setDesiredState(swerveModuleStates[2]);
        m_backRight.setDesiredState(swerveModuleStates[3]);
    }

    /**
     * Display the state of the swerve module to the dashboard.
     */
    public void displaySwerveStateToDashBoard(String name, SwerveModuleState state) {
        ShuffleboardTab tab = Shuffleboard.getTab(name);
        tab.add("Angle", state.angle);
        tab.add("Speed Meters", state.speedMetersPerSecond);
    }

    private double roundTo3Digits(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private void addItemToGrid(ShuffleboardLayout grid,
            String name,
            DoubleSupplier valueSupplier,
            int row) {
        grid.addString("Label" + Integer.toString(row), () -> name).withPosition(0, row);
        grid.addDouble(name, valueSupplier).withPosition(1, row);
    }

    // Class to hold two integers, x,y
    private class IntPos {
        @SuppressWarnings("MemberName")
        public int x;

        @SuppressWarnings("MemberName")
        public int y;

        private IntPos(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private void displayModuleToSingleSwerveDashV2(String name, SwerveModule module) {
        ShuffleboardTab tab = Shuffleboard.getTab("Swerve");
        final Map<String, IntPos> gridPositions = Map.of("Front Left",
                new IntPos(2, 2),
                "Front Right",
                new IntPos(6, 2),
                "Back Left",
                new IntPos(2, 4),
                "Back Right",
                new IntPos(6, 4));

        // Get the desired position of the grid widget, based on the name
        // of the module. If the name is not found, use 0,0.
        IntPos pos = gridPositions.get(name);
        if (pos == null) {
            pos = new IntPos(0, 0);
        }

        // Create a List widget to hold the formatted strings
        int numGridItems = 4;
        ShuffleboardLayout grid = tab.getLayout(name, BuiltInLayouts.kGrid)
                .withPosition(pos.x, pos.y).withSize(4, 2)
                .withProperties(Map.of("Label position",
                        "HIDDEN",
                        "Number of columns",
                        2,
                        "Number of rows",
                        numGridItems));

        addItemToGrid(grid,
                "Turn Relative Encoder Radians",
                () -> roundTo3Digits(module.getTurnEncoderRadians()),
                0);
        addItemToGrid(grid,
                "Turn Absolute Encoder Radians",
                () -> roundTo3Digits(module.getRawTurnEncoderRadians()),
                1);
        addItemToGrid(grid,
                "Drive Velocity",
                () -> roundTo3Digits(module.getDriveEncoderVelocity()),
                2);
        // addItemToGrid(grid, "Unoptimized setpoint",
        // () -> roundTo2Digits(module.getUnoptimizedTurningSetpointRotations()), 3);
        // addItemToGrid(grid, "Turn setpoint", () ->
        // roundTo2Digits(module.getTurningSetpointRotations()), 4);
        addItemToGrid(grid, "Turn offset", () -> roundTo3Digits(module.getOffset()), 3);
    }

    /**
     * Update the field relative position of the robot.
     */
    public void updateOdometry() {
        m_odometry.update(getRotation2d(), new SwerveModulePosition[] {
                m_frontLeft.getPosition(),
                m_frontRight.getPosition(),
                m_backLeft.getPosition(),
                m_backRight.getPosition()
        });
    }

    public double getxPosition() {

        return m_odometry.getPoseMeters().getX();
    }

    public double getyPosition() {
        return m_odometry.getPoseMeters().getY();
    }

    public boolean resetGyroFieldRelative() {
        return ErrorCode.OK == m_gyro.setYaw(270.0);
    }

    public double getAnglePosition() {
        return m_gyro.getYaw(); // rotation in horizontal plane
    }

    public Rotation2d getRotation2d() {
        return Rotation2d.fromDegrees(getAnglePosition()); // converts from degrees
    }

    public double getFrontLeftTurnEncoder() {
        return m_frontLeft.getTurnEncoderRadians();
    }

    public double getBackLeftTurnEncoder() {
        return m_backLeft.getTurnEncoderRadians();
    }

    public double getFrontRightTurnEncoder() {
        return m_frontRight.getTurnEncoderRadians();
    }

    public double getBackRightTurnEncoder() {
        return m_backLeft.getTurnEncoderRadians();
    }

    public double getFrontLeftDriveEncoder() {
        return m_frontLeft.getDriveEncoderPosition();
    }

    public double getBackLeftDriveEncoder() {
        return m_frontLeft.getDriveEncoderPosition();
    }

    public double getFrontRightDriveEncoder() {
        return m_frontLeft.getDriveEncoderPosition();
    }

    public double getBackRightDriveEncoder() {
        return m_frontLeft.getDriveEncoderPosition();
    }

    public double getFrontLeftDriveVelocity() {
        return m_frontLeft.getDriveEncoderVelocity();
    }

    public double getBackLeftDriveVelocity() {
        return m_backLeft.getDriveEncoderVelocity();
    }

    public double getFrontRightDriveVelocity() {
        return m_frontRight.getDriveEncoderVelocity();
    }

    public double getBackRightDriveVelocity() {
        return m_backRight.getDriveEncoderVelocity();
    }

    /**
     * Read the PID values from the Shuffleboard.
     */
    public void updatePidFromShuffleBoard() {
        if (isPIDTuning) {
            double pidDriveP = m_getPidDriveP.getDouble(SwerveModule.pidDriveP);
            double pidDriveD = m_getPidDriveD.getDouble(SwerveModule.pidDriveD);

            @SuppressWarnings("VariableDeclarationUsageDistance")
            double pidTurnP = m_getPidTurnP.getDouble(SwerveModule.pidTurnP);

            @SuppressWarnings("VariableDeclarationUsageDistance")
            double pidTurnD = m_getPidTurnD.getDouble(SwerveModule.pidTurnD);

            m_frontLeft.updateDrivePid(pidDriveP, pidDriveD);
            m_frontRight.updateDrivePid(pidDriveP, pidDriveD);
            m_backLeft.updateDrivePid(pidDriveP, pidDriveD);
            m_backRight.updateDrivePid(pidDriveP, pidDriveD);

            m_frontLeft.updateTurnPid(pidTurnP, pidTurnD);
            m_frontRight.updateTurnPid(pidTurnP, pidTurnD);
            m_backLeft.updateTurnPid(pidTurnP, pidTurnD);
            m_backRight.updateTurnPid(pidTurnP, pidTurnD);
        }
    }

    @Override
    public void periodic() {
        updatePidFromShuffleBoard();
        updateOdometry();
        // Shuffleboard.getTab("Swerve").add("X Pose Meters", m_odometry.getPoseMeters().getX());

    }

    /**
     * Stop the swerve drive system.
     */
    public void stopSystem() {
        m_frontLeft.stopSystem();
        m_frontRight.stopSystem();
        m_backLeft.stopSystem();
        m_backRight.stopSystem();
    }
}
