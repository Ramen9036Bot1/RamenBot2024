package frc.robot.subsystems;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.VisionConstants;
import frc.robot.LimelightHelpers.LimelightResults;
import frc.robot.LimelightHelpers;

import java.util.Map;

public class VisionSystem extends SubsystemBase {
    private final double limelightMountAngleRadiansY = VisionConstants.limelightMountAngleRadiansY;
    private final double limelightMountAngleRadiansX = VisionConstants.limelightMountAngleRadiansX;

    private double m_targetY;

    private final double limelightLensHeightMeters = VisionConstants.limelightLensHeightMeters;
    private final double aprilTagHeightMeters = VisionConstants.aprilTagHeightMeters;

    private NetworkTable limelightTable = NetworkTableInstance.getDefault()
            .getTable(VisionConstants.limelightName);
    private NetworkTableEntry tableX = limelightTable.getEntry("tx");
    private NetworkTableEntry tableY = limelightTable.getEntry("ty");
    private NetworkTableEntry tableArea = limelightTable.getEntry("ta");
    private NetworkTableEntry tableID = limelightTable.getEntry("tid");

    private final Field2d m_fieldSim = new Field2d();
    private int[] m_numTags = {
            0
    };

    private final double EPSILON = 0.0000001;
    private Pose2d m_fieldPose = new Pose2d();

    public VisionSystem() {
        m_targetY = 0;
        displayToShuffleBoard();
        LimelightHelpers
                .setCameraPose_RobotSpace(
                        VisionConstants.limelightName,
                        0.25,
                        0.25,
                        0.46,
                        0,
                        18.5,
                        0);
    }

    // $IDO - This is where the vision shuffleboard is done
    private void displayToShuffleBoard() {
        ShuffleboardTab tab = Shuffleboard.getTab("Vision");

        // $IDO - Good doc on limelight fields
        // https://docs.limelightvision.io/docs/docs-limelight/apis/complete-networktables-api
        // https://docs.limelightvision.io/docs/docs-limelight/apis/limelight-lib
        // Super simple example:
        // https://github.com/Lambda-Corps/2020InfiniteRecharge/blob/master/src/main/java/frc/robot/subsystems/Vision.java

        // $IDO - Limelighthelpers
        // LimelightHelpers library: https://github.com/LimelightVision/limelightlib-wpijava
        // **** LimelightHelpers docs:
        // https://www.chiefdelphi.com/t/introducing-limelight-lib/425660?page=2
        // https://docs.limelightvision.io/docs/docs-limelight/apis/limelight-lib

        // Uses limelighthelpers:
        // https://github.com/6391-Ursuline-Bearbotics/2023-Swerverybot/blob/main/src/main/java/frc/robot/subsystems/Vision/Limelight.java

        // Limelight port forwarding for laptop use:
        // https://docs.limelightvision.io/docs/docs-limelight/getting-started/best-practices
        // Crosshair calibration:
        // https://docs.limelightvision.io/docs/docs-limelight/getting-started/crosshair

        tab.addDouble("Y Robot Pose", () -> getFieldPose().getY());
        tab.addDouble("Y Speaker Pose", () -> m_targetY);
        tab.addDouble("Y Distance Pose", () -> getSpeakerYDistance());

        // tab.addBoolean("Is Detecting", () -> isDetected())
        // .withPosition(0, 0);

        // tab.addInteger("Num tags", () -> m_numTags[0])
        // .withPosition(1, 0);

        // tab.addDouble("ID", () -> getID())
        // .withPosition(2, 0);

        // tab.addDouble("X Degrees", () -> getX())
        // .withWidget(BuiltInWidgets.kGyro)
        // .withPosition(1, 2)
        // .withSize(2, 2);

        // tab.addDouble("Y Degrees", () -> getY())
        // .withWidget(BuiltInWidgets.kGyro)
        // .withPosition(3, 2)
        // .withSize(2, 2)
        // .withProperties(Map.of("Starting angle", 270.0));

        // tab.addDouble("Distance Meters X", () -> getDistanceMetersX())
        // .withWidget(BuiltInWidgets.kNumberBar)
        // .withPosition(1, 4)
        // .withSize(2, 1)
        // .withProperties(Map.of("min", 0, "max", 10));

        // tab.addDouble("Distance Meters Y", () -> getDistanceMetersY())
        // .withWidget(BuiltInWidgets.kNumberBar)
        // .withPosition(3, 4)
        // .withSize(2, 1)
        // .withProperties(Map.of("min", 0, "max", 10));

        // tab.add("Field", m_fieldSim)
        // .withWidget(BuiltInWidgets.kField)
        // .withPosition(5, 2)
        // .withSize(5, 3);
    }

    /**
     * X angle, left-right, from April tag. X cross-hair angle.
     */
    public double getX() {
        return tableX.getDouble(0);
    }

    /**
     * Y angle, up-down, to April tag. Y cross-hair angle.
     */
    public double getY() {
        return tableY.getDouble(0);
    }

    public double getXRadians() {
        return Math.toRadians(getX());
    }

    public double getYRadians() {
        return Math.toRadians(getY());
    }

    /**
     * Area of April tag in view.
     */
    public double getArea() {
        return tableArea.getDouble(0);
    }

    // $IDO - This seems like a strange way to see if any ID is detected
    public boolean isDetected() {
        return getX() + getY() + getArea() != 0;
    }

    public double getID() {
        return tableID.getDouble(0);
    }

    // $IDO - This isn't even used!
    public boolean isDetectedIDValid() {
        double myID = getID();
        if (Constants.VisionConstants.targetedIDList.contains(myID)) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Distance to April tag in meters Y.
     */
    // $IDO - This is where the Y height is being calculated
    public double getDistanceMetersY() {
        double angleToGoalRadians = limelightMountAngleRadiansY + getYRadians();
        double distanceFromLimelightToGoalMeters = (aprilTagHeightMeters
                - limelightLensHeightMeters) / (Math.tan(angleToGoalRadians) + EPSILON);
        return distanceFromLimelightToGoalMeters;
    }

    /**
     * Distance to April tag in meters X.
     */
    // $IDO - Is this right? It's calculating X meters from getDistanceMetersY()
    public double getDistanceMetersX() {
        double angleToGoalRadians = limelightMountAngleRadiansX + getXRadians();
        double distanceFromLimelightToGoalMeters = getDistanceMetersY()
                * Math.tan(angleToGoalRadians);
        return distanceFromLimelightToGoalMeters;
    }

    @Override
    public void periodic() {
        LimelightResults llresults = LimelightHelpers.getLatestResults("limelight-ramen");

        int numAprilTags = llresults.targetingResults.targets_Fiducials.length;
        m_numTags[0] = numAprilTags;
        m_fieldPose = new Pose2d();

        try {
            m_targetY = llresults.targetingResults.targets_Fiducials[0].getTargetPose_RobotSpace2D()
                    .getY();
        }
        catch (Exception e) {
            m_targetY = 0;
        }

        if (numAprilTags > 0) {

            // Pose2d pose = llresults.targetingResults.targets_Fiducials[0]
            // .getRobotPose_FieldSpace2D();

            m_fieldPose = llresults.targetingResults.getBotPose2d_wpiBlue();

            // double x = pose.getTranslation().getX();
            // double y = pose.getTranslation().getY();
            // double rotation = pose.getRotation().getDegrees();
            // System.out.println("x=" + x + ", y=" + y + ", rot=" + rotation);

            m_fieldSim.setRobotPose(m_fieldPose);
        }
    }

    public double getSpeakerYDistance() {
        return -m_targetY;
    }

    public Pose2d getFieldPose() {
        return m_fieldPose;
    }

    public void stopSystem() {
    }
}
