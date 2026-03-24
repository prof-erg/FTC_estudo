// Define em qual parte do projeto este arquivo se encontra.
// Isso é obrigatório para o Android Studio entender onde ele está.
package org.firstinspires.ftc.teamcode;

// Importa os tipos necessários para criar um OpMode Linear.
// Um LinearOpMode roda sequencialmente, linha por linha.
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

// Indica que este programa vai aparecer na aba AUTONOMOUS do Driver Station.
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

// Importações para motores, servos e recursos do sistema FTC.
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.ElapsedTime;

// Essa anotação faz o Driver Station exibir este OpMode na aba de AUTÔNOMO.
@Autonomous
// Aqui começa a classe principal. O nome do arquivo deve ser igual ao nome da classe.
public class JBTechTESTES extends LinearOpMode {

    // Motores e servos do robô, declarados como variáveis da classe
    // para serem usados dentro de qualquer método.
    private DcMotor flywheel;     // Motor responsável por lançar bolas
    private DcMotor coreHex;      // Motor que empurra a bola para o lançador
    private DcMotor leftDrive;    // Motor da roda esquerda
    private CRServo servo;        // Servo contínuo que agita as bolas
    private DcMotor rightDrive;   // Motor da roda direita

    // Valores fixos de velocidade do flywheel (lançador)
    private static final int bankVelocity = 1300;  // Velocidade para arremesso perto
    private static final int farVelocity = 1900;   // Velocidade para arremesso distante
    private static final int maxVelocity = 2200;   // Velocidade máxima de giro

    // Strings usadas para selecionar o tipo de modo de operação antes do Start
    private static final String TELEOP = "TELEOP";
    private static final String AUTO_BLUE = "AUTO BLUE";
    private static final String AUTO_RED = " AUTO RED";

    // Guarda qual modo o operador selecionou
    private String operationSelected = TELEOP;

    // Conversão de polegadas para "ticks" do encoder dos motores das rodas
    private double WHEELS_INCHES_TO_TICKS = (28 * 5 * 3) / (3 * Math.PI);

    // Timers usados no autônomo
    private ElapsedTime autoLaunchTimer = new ElapsedTime();
    private ElapsedTime autoDriveTimer = new ElapsedTime();


    // -------------------------------------------------------------------------
    // runOpMode() — PONTO DE INÍCIO DO PROGRAMA DO ROBÔ
    // -------------------------------------------------------------------------
    @Override
    public void runOpMode() {

        // Realiza o mapeamento das peças conectadas na REV Hub
        flywheel = hardwareMap.get(DcMotor.class, "flywheel");
        coreHex = hardwareMap.get(DcMotor.class, "coreHex");
        leftDrive = hardwareMap.get(DcMotor.class, "leftDrive");
        servo = hardwareMap.get(CRServo.class, "servo");
        rightDrive = hardwareMap.get(DcMotor.class, "rightDrive");

        // Configura o flywheel para rodar usando o encoder
        flywheel.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        // Inverte a direção de alguns motores
        flywheel.setDirection(DcMotor.Direction.REVERSE);
        coreHex.setDirection(DcMotor.Direction.REVERSE);
        leftDrive.setDirection(DcMotor.Direction.REVERSE);

        // Garante que o servo começa parado
        servo.setPower(0);

        // Antes de apertar Start, o operador escolhe o modo (TeleOp / Blue / Red)
        while (opModeInInit()) {
            operationSelected = selectOperation(operationSelected, gamepad1.psWasPressed());
            telemetry.update();
        }

        // Aguarda o operador apertar Start
        waitForStart();

        // Depois do Start, executa o modo escolhido
        if (operationSelected.equals(AUTO_BLUE)) {
            doAutoBlue();
        } else if (operationSelected.equals(AUTO_RED)) {
            doAutoRed();
        } else {
            doTeleOp();
        }
    }


    // -------------------------------------------------------------------------
    // Função que alterna entre TELEOP ⇄ AUTO BLUE ⇄ AUTO RED
    // -------------------------------------------------------------------------
    private String selectOperation(String state, boolean cycleNext) {
        if (cycleNext) {
            if (state.equals(TELEOP)) state = AUTO_BLUE;
            else if (state.equals(AUTO_BLUE)) state = AUTO_RED;
            else if (state.equals(AUTO_RED)) state = TELEOP;
            else telemetry.addData("WARNING", "Unknown Operation State");
        }

        telemetry.addLine("Press Home Button to cycle options");
        telemetry.addData("CURRENT SELECTION", state);
        if (state.equals(AUTO_BLUE) || state.equals(AUTO_RED))
            telemetry.addLine("Please remember to enable the AUTO timer!");
        telemetry.addLine("Press START to start your program");
        return state;
    }


    // -------------------------------------------------------------------------
    // TELEOP — Código usado no controle manual do robô
    // -------------------------------------------------------------------------
    private void doTeleOp() {
        while (opModeIsActive()) {

            splitStickArcadeDrive();           // controle das rodas
            setFlywheelVelocity();             // controle do lançador
            manualCoreHexAndServoControl();    // alimentador e agitador

            telemetry.addData("Flywheel Velocity", ((DcMotorEx) flywheel).getVelocity());
            telemetry.addData("Flywheel Power", flywheel.getPower());
            telemetry.update();
        }
    }


    // -------------------------------------------------------------------------
    // Controle das rodas — tipo Arcade Drive
    // -------------------------------------------------------------------------
    private void splitStickArcadeDrive() {
        float X = gamepad1.right_stick_x;   // vira
        float Y = -gamepad1.left_stick_y;   // frente/trás

        leftDrive.setPower(Y - X);
        rightDrive.setPower(Y + X);
    }


    // -------------------------------------------------------------------------
    // Controle manual do CoreHex e do Servo
    // -------------------------------------------------------------------------
    private void manualCoreHexAndServoControl() {

        if (gamepad1.cross) coreHex.setPower(0.5);
        else if (gamepad1.triangle) coreHex.setPower(-0.5);

        if (gamepad1.dpad_left) servo.setPower(1);
        else if (gamepad1.dpad_right) servo.setPower(-1);
    }


    // -------------------------------------------------------------------------
    // Controle do Flywheel (lançador)
    // -------------------------------------------------------------------------
    private void setFlywheelVelocity() {

        if (gamepad1.options) {
            flywheel.setPower(-0.5);
        }
        else if (gamepad1.left_bumper) {
            FAR_POWER_AUTO();
        }
        else if (gamepad1.right_bumper) {
            BANK_SHOT_AUTO();
        }
        else if (gamepad1.circle) {
            ((DcMotorEx) flywheel).setVelocity(bankVelocity);
        }
        else if (gamepad1.square) {
            ((DcMotorEx) flywheel).setVelocity(maxVelocity);
        }
        else {
            ((DcMotorEx) flywheel).setVelocity(0);
            coreHex.setPower(0);

            if (!gamepad1.dpad_right && !gamepad1.dpad_left)
                servo.setPower(0);
        }
    }


    // -------------------------------------------------------------------------
    // Funções AUTOMÁTICAS de disparo
    // -------------------------------------------------------------------------
    private void BANK_SHOT_AUTO() {
        ((DcMotorEx) flywheel).setVelocity(bankVelocity);
        servo.setPower(-1);

        if (((DcMotorEx) flywheel).getVelocity() >= bankVelocity - 100)
            coreHex.setPower(1);
        else
            coreHex.setPower(0);
    }

    private void FAR_POWER_AUTO() {
        ((DcMotorEx) flywheel).setVelocity(farVelocity);
        servo.setPower(-1);

        if (((DcMotorEx) flywheel).getVelocity() >= farVelocity - 100)
            coreHex.setPower(1);
        else
            coreHex.setPower(0);
    }


    // -------------------------------------------------------------------------
    // Função de movimento automático usando encoders
    // -------------------------------------------------------------------------
    private void autoDrive(double speed, int leftDistanceInch, int rightDistanceInch, int timeout_ms) {

        autoDriveTimer.reset();

        leftDrive.setTargetPosition((int) (leftDrive.getCurrentPosition() + leftDistanceInch * WHEELS_INCHES_TO_TICKS));
        rightDrive.setTargetPosition((int) (rightDrive.getCurrentPosition() + rightDistanceInch * WHEELS_INCHES_TO_TICKS));

        leftDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightDrive.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        leftDrive.setPower(Math.abs(speed));
        rightDrive.setPower(Math.abs(speed));

        while (opModeIsActive() &&
            (leftDrive.isBusy() || rightDrive.isBusy()) &&
            autoDriveTimer.milliseconds() < timeout_ms) {
            idle();
        }

        leftDrive.setPower(0);
        rightDrive.setPower(0);

        leftDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rightDrive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }


    // -------------------------------------------------------------------------
    // AUTÔNOMO — Aliança Azul
    // -------------------------------------------------------------------------
    private void doAutoBlue() {
        if (opModeIsActive()) {

            telemetry.addData("RUNNING OPMODE", operationSelected);
            telemetry.update();

            autoLaunchTimer.reset();

            while (opModeIsActive() && autoLaunchTimer.milliseconds() < 10000) {
                BANK_SHOT_AUTO();
                telemetry.addData("Launcher Countdown", autoLaunchTimer.seconds());
                telemetry.update();
            }

            ((DcMotorEx) flywheel).setVelocity(0);
            coreHex.setPower(0);
            servo.setPower(0);

            autoDrive(0.5, -12, -12, 5000);
            autoDrive(0.5, -8, 8, 5000);
            autoDrive(1, -50, -50, 5000);
        }
    }


    // -------------------------------------------------------------------------
    // AUTÔNOMO — Aliança Vermelha
    // -------------------------------------------------------------------------
    private void doAutoRed() {
        if (opModeIsActive()) {

            telemetry.addData("RUNNING OPMODE", operationSelected);
            telemetry.update();

            autoLaunchTimer.reset();

            while (opModeIsActive() && autoLaunchTimer.milliseconds() < 10000) {
                BANK_SHOT_AUTO();
                telemetry.addData("Launcher Countdown", autoLaunchTimer.seconds());
                telemetry.update();
            }

            ((DcMotorEx) flywheel).setVelocity(0);
            coreHex.setPower(0);
            servo.setPower(0);

            autoDrive(0.5, -12, -12, 5000);
            autoDrive(0.5, 8, -8, 5000);
            autoDrive(1, -50, -50, 5000);
        }
    }
}