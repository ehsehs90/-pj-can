
package cantest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import java.util.Enumeration;

public class SerialTest implements SerialPortEventListener {
	
	//String[] str = null;
	//int i = 0;
	

	SerialPort serialPort;
	CarLattePanda car;

	private static final String PORT_NAMES[] = { "COM8", // Windows
	};
	private InputStream input;
	private OutputStream output;
	private static final int TIME_OUT = 2000;
	private static final int DATA_RATE = 9600;

	public void initialize(CarLattePanda carr) {

		car = carr;

		CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			for (String portName : PORT_NAMES) {
				if (currPortId.getName().equals(portName)) {
					portId = currPortId;
					break;
				}
			}
		}
		if (portId == null) {
			System.out.println("Could not find COM port.");
			return;
		}

		try {
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);
			serialPort.setSerialPortParams(DATA_RATE, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			input = serialPort.getInputStream();
			output = serialPort.getOutputStream();
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);

		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

//	public synchronized void serialEvent(SerialPortEvent oEvent) {
//		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
//			String sCommand = "";
//			int cTemp;
//			try {
//				String[] str = null;
//				int i = 0;
//				while (input.available() > 0) {
//					try {
//						int available = input.available();
//						byte chunk[] = new byte[available];
//						input.read(chunk, 0, available);
//						//System.out.print(new String(chunk));
//						// CarLattePanda car = new CarLattePanda();
//						str[i] = new String(chunk);
//						i++;
//						
//						
//						//car.sendData("rfid");
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//
//				System.out.println(str[0].toString());
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//	}
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
			byte chunk[] = new byte[1];
			int len = -1;
			String str = "";
			StringBuilder sb = new StringBuilder();
			int idx = 0;
			
			try {
				while (idx < 110) {
					len = this.input.read(chunk);
					String ss = new String(chunk,0,len);
					ss = ss.replaceAll("(\r\n|\r|\n|\n\r)", " ");
					String t = "";
					for(int i=0; i<ss.length(); i++) {
						t += ss.charAt(i);
					}
					sb.append(t);
					//System.out.println(sb.toString());
					idx += 1;
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			System.out.println(sb.toString());
			String sooy = sb.toString();
			if(sooy.contains("D6 2C 4B 54")) {
				car.sendData("start");
			}
			//System.out.println(sb.toString());
			
			
		}
	}
}
