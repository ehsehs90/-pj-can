package cantest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Random;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

public class CarLattePanda extends Application {

	// 메시지 창 (받은 메시지를 보여주는 역할)
	TextArea textarea;

	// CarLattePanda 포트번호 지정
	String portName = "COM10";

	// 연결 버튼 (COM 포트 연결 버튼) -> 메시지가 들어오는지 기다렸다가 데이터가 들어오면 받는다.
	// Button connBtn, sendBtn;
	Button connBtn, rfidBtn, stopBtn;

	// 사용할 COM 포트를 지정하기 위해서 필요
	private CommPortIdentifier portIdentifier;

	// 만약 COM 포트를 사용할 수 있고, 해당 포트를 open 하면 COM 포트 객체를 획득
	private CommPort commPort;

	private SerialPort serialPort;

	// Byte 계열로 입출력을 한다.
	private BufferedInputStream bis;
	private OutputStream out;

	// 차량 주행 시작시
	Thread driveThread;
	String[] carsData = new String[2];
	double temp;
	double humidity;

	public static void main(String[] args) {
		launch();
	}

	public String carData(double u) {
		int it;
		int ri;
		int itIndex = 12;
		int riIndex = 4;
		int zeroFill;
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();

		String[] str = Double.toString(u).split("\\.");
		if (str[1].length() > 1) {
			str[1] = str[1].substring(0, 2);
		}
//      System.out.println(Arrays.toString(str));

		it = Integer.parseInt(str[0]);
		ri = Integer.parseInt(str[1]);
		String rt = Integer.toHexString(it);
		String rt2 = Integer.toHexString(ri);
//      System.out.println(rt);
//      System.out.println(rt2);
		sb.append(rt);
		zeroFill = itIndex - rt.length();
		for (int i = 0; i < zeroFill; i++) {
			sb.insert(0, "0");
		}
		sb.append(rt2);
		zeroFill = riIndex - rt2.length();
		for (int i = 0; i < zeroFill; i++) {
			sb.insert(12, "0");
		}
		return sb.toString();
	}

	public String carStringTOHex(String str, String str2) {
		int checkSum = 0;
		String msg = str + str2;
		for (int i = 0; i < msg.length(); i++) {
			checkSum += msg.toUpperCase().charAt(i);
		}
		checkSum = checkSum & 0xff;
		String myReturn = Integer.toHexString(checkSum);
		if (myReturn.length() == 1) {
			myReturn = "0" + myReturn;
		}
		return myReturn.toUpperCase();
	}

	// SerialPort에서 발생하는 이벤트를 처리하기 위한 클래스
	class MyPortListener implements SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent event) {
			// Serial Port에서 event가 발생하면 호출!
			if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				// Port를 통해서 데이터가 들어왔다는 의미
				byte[] readBuffer = new byte[128];

				try {

					// Stream 안의 데이터를 반복해서 읽어온다.
					while (bis.available() > 0) {
						bis.read(readBuffer);
					}

					String result = new String(readBuffer);
					// 받은 result 데이터를 다시 변환시켜줘야함

					printMsg("받은 메시지는 ___ " + result + "___");
					String[] code = result.split(":");
					if (code[1].contains("U280")) {
						for (int i = 1; i <= code.length - 1; i++) {
							// ServerLattePanda에서 차량출발하라는 신호보낼때(start)
							if (code[i].contains("U2800000002737461727453333789")) {
								printMsg("차량을 출발하겠습니다");
								// 차량 출발(위치정보, 온도, 습도 변화 시작)
								driveThread = new Thread() {
									public void run() {
										printMsg("주행 시작");
										printMsg("온도 : " + temp);
										printMsg("습도 : " + humidity);

										Random generator = new Random();
										temp = generator.nextInt(5) + 20; // 20~25도 사이
										humidity = generator.nextInt(10) + 40; // 40~50% 사이

										int flag = 0;
										while (!isInterrupted()) {
											flag++;
											temp = generator.nextInt(5) + 20; // 20~25도 사이
											humidity = generator.nextInt(10) + 40; // 40~50% 사이
											String tempMsg = ":W2800000005" + carData(temp).toUpperCase()
													+ carStringTOHex("W2800000005", carData(temp)) + "\r";
											carsData[0] = tempMsg;
											String humidityMsg = ":W2800000006" + carData(humidity).toUpperCase()
													+ carStringTOHex("W2800000006", carData(humidity)) + "\r";
											carsData[1] = humidityMsg;
//                                 String oilMsg = ":W2800000003"+carData(engineOil).toUpperCase()+stringTOHex("W2800000003",carData(engineOil))+"\r";
//                                 carsData[2] = oilMsg;

											for (int i = 0; i < carsData.length; i++) {
												// for(int i = 0; i < 2; i++) {
												try {
													byte[] inputData = carsData[i].getBytes();
													out.write(inputData);
													inputData = null;
												} catch (IOException e) {
													e.printStackTrace();
												}
											}
											try {
												Thread.sleep(1);
											} catch (InterruptedException e) {
												break;
											}
										}
										printMsg("Thread중지");
									}
								};
								driveThread.start();
							} /*
								 * else if(code[i].contains("U2800000005")){ temp =
								 * Double.parseDouble(hexstringtoInt(code[i])); }else
								 * if(code[i].contains("U2800000006")) { humidity =
								 * Double.parseDouble(hexstringtoInt(code[i])); }else {
								 * printMsg("알수없는 protocol입니다."); }
								 */
						}
					}

				} catch (Exception e) {
					// TODO: handle exception
					System.out.println(e);
				}

			}

		}

	}

	public String hexstringtoInt(String str) {
		String natureHexString = str.substring(11, 23);
		String primeHexString = str.substring(23, 27);

		int n = Integer.parseUnsignedInt(natureHexString, 16);
		int p = Integer.parseUnsignedInt(primeHexString, 16);

		String number = n + "." + p;

		return number;
	}

	public static String stringToHex(String s) {
		String result = "";

		for (int i = 0; i < s.length(); i++) {
			result += String.format("%02X", (int) s.charAt(i));
		}

		return result;
	}

	private void printMsg(String msg) {
		Platform.runLater(() -> {
			textarea.appendText(msg + "\n");
		});
	}

	public void sendData(String sendMsg) {

		// 데이터 보내기
		try {
			out = serialPort.getOutputStream();
			// CAN 데이터 수신 허용 설정
			// String ckSum = getCheckSum();
//         String cksum = getCheckSum();
			String msg = getCheckSum(sendMsg);
			// String msg = ":"+cksum+"\r";
			System.out.println(msg);

			// 문자열을 outputStream으로 바로 쏠수 없어서 try-catch
			try {
				byte[] inputData = msg.getBytes();
				out.write(inputData);
				printMsg(portName + "가 송신을 시작합니다.");
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println(e);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private String getCheckSum(String sendMsg) {
		// String data = stringToHex(tf.getText());
		String data = stringToHex(sendMsg);
		String id = "00000001";
		String msg = "W28" + id + data;
		System.out.println(msg);

		int a = 0;
		for (char item : msg.toCharArray()) {
			a += item;
		}
		int result = a & 0xff;
		String b = Integer.toHexString(result).toUpperCase();
		System.out.println(result);
		System.out.println(Integer.toHexString(result).toUpperCase());

		return ":" + msg + b + "\r";
	}

	private void connectPort(String portName) {
		// portName을 이용해 Port에 접근해서 객체를 생성해요.
		try {
			// portName을 이용해서 CommPortIdentifier 객체를 만든다.
			portIdentifier = CommPortIdentifier.getPortIdentifier(portName);

			printMsg(portName + "에 연결을 시도합니다.");

			// portIdentifier를 이용해서 현재 사용되고 있는지 확인한다.
			if (portIdentifier.isCurrentlyOwned()) {
				printMsg(portName + "가 다른 프로그램에 의해서 사용되고 있어요 ㅠㅠ");
			} else {
				// Port를 열고 Port 객체를 획득
				commPort = portIdentifier.open("MyApp", 5000);

				// Port 객체(commPort)를 얻은 후 Serial인지 Parallel인지를 확인한 후 적절하게 Type Casting
				if (commPort instanceof SerialPort) {
					// SerialPort이면 True
					serialPort = (SerialPort) commPort;

					// SerialPort에 대한 설정
					serialPort.setSerialPortParams(921600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
							SerialPort.PARITY_NONE);

					// 포트에 이벤트를 감지하는 리스너를 붙여 메시지가 들어왔을 때 알림을 준다.
					serialPort.addEventListener(new MyPortListener());

					// Serial Port의 데이터가 유효할 때 (데이터가 들어옴) 알려주는 기능을 활성화
					serialPort.notifyOnDataAvailable(true);
					printMsg(portName + "에 리스너가 등록되었어요~");

					// 데이터 받기
					bis = new BufferedInputStream(serialPort.getInputStream());
					// 데이터 보내기
					out = serialPort.getOutputStream();

					// CAN 데이터 수신 허용 설정
					// 프로토콜을 이용해서 정해진 형식대로 문자열을 만들어서 out stream을 통해서 출력
					String msg = ":G11A9\r";

					// 문자열을 outputStream으로 바로 쏠수 없어서 try-catch
					try {
						byte[] inputData = msg.getBytes();
						out.write(inputData);
						printMsg(portName + "가 수신을 시작합니다.");
					} catch (Exception e) {
						// TODO: handle exception
						System.out.println(e);
					}

				}
			}

		} catch (Exception e) {
			// TODO: handle exception
			System.out.println(e);
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		BorderPane root = new BorderPane();

		root.setPrefSize(700, 500);

		textarea = new TextArea();
		root.setCenter(textarea);

		connBtn = new Button("COM 포트 연결!!");
		connBtn.setPrefSize(200, 100);

		// 람다식을 사용해서 이벤트를 핸들링한다.
		connBtn.setOnAction(t -> {
			// String portName = "COM11";

			// portName 연결
			connectPort(portName);
			// rfid 포트 연결
			SerialTest main = new SerialTest();
			main.initialize(CarLattePanda.this);
			System.out.println("Started");
		});

//      rfidBtn = new Button("RFID 신호 전송");
//      rfidBtn.setPrefSize(200, 100);
//      rfidBtn.setOnAction(t->{
//         //String portName = "COM11";
//         
//         sendData("start");
//      });

		stopBtn = new Button("주행 중지");
		stopBtn.setPrefSize(200, 100);
		stopBtn.setOnAction(t -> {

			driveThread.interrupt();
			printMsg("온도 : " + temp);
			printMsg("습도 : " + humidity);

		});
		// 긴 Panel 하나를 생성
		FlowPane flowpane = new FlowPane();
		flowpane.setPrefSize(700, 50);
		flowpane.getChildren().add(connBtn);
		// flowpane.getChildren().add(rfidBtn);
		flowpane.getChildren().add(stopBtn);
		root.setBottom(flowpane);

		// 화면에 띄우기
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.setTitle("CarLattePanda");
		primaryStage.show();

	}

}