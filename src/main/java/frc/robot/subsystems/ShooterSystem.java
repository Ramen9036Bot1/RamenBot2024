// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ShooterConstants;

/**
 * SwerveDriveSystem.
 */
public class ShooterSystem extends SubsystemBase {

    public final CANSparkMax m_shooterMotor = new CANSparkMax(ShooterConstants.shooterMotorID,
            MotorType.kBrushless);
    public double speed;

    public ShooterSystem() {

    }

    public void setShootSpeed(double SPEED) {
        speed = SPEED;
        m_shooterMotor.set(speed);

    }

    public double getShootSpeed() {
        return m_shooterMotor.get();
    }

    @Override
    public void periodic() {
        // Shuffleboard.getTab("Sensor").addBoolean(getName(), null);
        // Shuffleboard.getTab("Swerve").add("X Pose Meters", m_odometry.getPoseMeters().getX())

    }

    /**
     * Stop the swerve drive system.
     */
    public void stopSystem() {

        m_shooterMotor.stopMotor();
    }
}
