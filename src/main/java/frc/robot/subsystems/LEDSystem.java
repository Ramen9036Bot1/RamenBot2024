package frc.robot.subsystems;

import edu.wpi.first.wpilibj.AddressableLED;
import edu.wpi.first.wpilibj.AddressableLEDBuffer;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.OperatorConstants;

public class LEDSystem extends SubsystemBase {

    // I WANT TO USE THE TOP INTAKE MOTOR BECAUSE THAT WILL ONLY CHANGE IF IT ACTUALLY GRABS A PIECE

    // THE BOTTOM NOTE MIGHT SLOW DOWN BECAUSE IT HITS THE NOTE, BUT IT MAY NOT ACTUALLY INTAKE IT
    // private final CANSparkMax intakeMotor = new CANSparkM
    // IntakeConstants.intakeMotorRight
    // MotorType.kBrushless);
    // private RelativeEncoder m_encoder = intakeMotor.getEncoder();

    private AddressableLED m_LEDLight = new AddressableLED(
            Constants.OperatorConstants.kLEDLightsChannel);
    private AddressableLEDBuffer m_LEDBuffer = new AddressableLEDBuffer(
            Constants.OperatorConstants.kLEDLightsLength);

    private int m_ledLoop;
    private int m_ledR;
    private int m_ledG;
    private int m_ledB;
    private int m_ledHue;

    private DigitalInput beamBreak = new DigitalInput(1);

    private boolean noteInIntake = false;

    private IntakeSystem m_intakeSystem;

    public LEDSystem(IntakeSystem intakeSystem) {

        // intakeMotor.restoreFactoryDefaults();
        // intakeMotor.setSmartCurrentLimit(IntakeConstants.smartCurrentLimit);
        // initShuffleBoard();

        m_ledLoop = 0;
        m_ledR = 0;
        m_ledG = 255;
        m_ledB = 255;
        m_ledHue = 0;
        m_LEDLight.setLength(m_LEDBuffer.getLength());

        m_intakeSystem = intakeSystem;
        for (int i = 0; i < OperatorConstants.kLEDLightsLength; i++) {
            m_LEDBuffer.setRGB(i, 255, 0, 255);
        }
        m_LEDLight.setData(m_LEDBuffer);

        m_LEDLight.start();

    }

    /**
     * Sets the LED lights to yellow.
     */
    public void setLedsYellow() {
        m_ledR = 255;
        m_ledG = 255;
        m_ledB = 0;
    }

    public void resetLED() {
        m_ledR = 255;
        m_ledG = 0;
        m_ledB = 0;
    }

    public void initShuffleBoard() {
        Shuffleboard.getTab("Intake")
                .add("Current Output: ", m_intakeSystem.getOutputCurrent());
    }

    // IF THIS DOES NOT WORK, ADD A VELOCITY METHOD (Look at encoder values in Intake System to see
    // if it works)

    @Override
    public void periodic() {
        if (beamBreak.get()) {
            if (noteInIntake) { // if note was just in intake, but shot it out, then it will run
                // LED to Red
                for (int i = 0; i < OperatorConstants.kLEDLightsLength; i++) {
                    m_LEDBuffer.setRGB(i, 255, 0, 255);
                }
                m_LEDLight.setData(m_LEDBuffer);
                noteInIntake = false;

            }
            // otherwise, it wont do anything, so that it is not constantly running the for loop

            // m_armController.setRumble(RumbleType.kBothRumble, 0);
        }
        // // Beam Break does not see the light (has the note)
        else {
            if (!noteInIntake) { // if note was just intaked, but changed, then it will run the
                // LED to Green
                for (int i = 0; i < OperatorConstants.kLEDLightsLength; i++) {
                    m_LEDBuffer.setRGB(i, 255, 255, 0);
                }
                m_LEDLight.setData(m_LEDBuffer);
                noteInIntake = true;
            }
        }
    }

    /**
     * Stop the intake system.
     */
    public void stopSystem() {
        m_LEDBuffer.setRGB(0, 0, 0, 0);
        m_LEDLight.setData(m_LEDBuffer);
        m_LEDLight.start();
    }
}
