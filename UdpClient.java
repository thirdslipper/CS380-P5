import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class UdpClient {
	public static void main(String[] args) {
		try (Socket socket = new Socket("codebank.xyz", 38005)){
			InputStream is = socket.getInputStream();
/*			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader br = new BufferedReader(isr);*/
			
			OutputStream os = socket.getOutputStream();
			PrintStream ps = new PrintStream(os);
			
			int size = 1;
//			for (int i = 0; i < 12; ++i){
//			size <<= 1;
			System.out.println("Data length: " + size);

			byte[] deadbeef = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
			ps.write(getIPV4Header(4, deadbeef));
			handshake(is);
//			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	public static byte[] getIPV4Header(int size, byte[] data){
		short length = (short) (20+size);
		byte[] arr = new byte[length];
		
		arr[0] = 0x45; 	//Version, HLen
		arr[1] = 0x0;	//TOS
		arr[2] = (byte) ((length >> 8) & 0xFF);	//Header + Data(0)?
		arr[3] = (byte) (length & 0xFF); 		//20+2*
		
		arr[4] = 0x0;	//ident
		arr[5] = 0x0;
		arr[6] = (0x1 << 6); 	// flag
		arr[7] = 0x0;	// offset
		
		arr[8] = 0x32; 	// 50 TTL
		arr[9] = 0x11; 	// TCP = 6, UDP = 11
		arr[10] = 0x0; 	// todo Checksum
		arr[11] = 0x0;
		
		arr[12] = (byte) 76; 	//Src public IP Address
		arr[13] = (byte) 175;	
		arr[14] = (byte) 85;	
		arr[15] = (byte) 174;		
		
		arr[16] = (byte) 52;	//Dest socket inet address : 52.37.88.154
		arr[17] = (byte) 37;
		arr[18] = (byte) 88;
		arr[19] = (byte) 154;

		short cksum = checksum(arr);
		arr[10] = (byte) ((cksum >> 8) & 0xFF);
		arr[11] = (byte) (cksum & 0xFF);
		
		// Data fields are defaulted to 0 in the instantiation of the array.
		for (int i = 20; i < 20+data.length; ++i){
			arr[i] = data[i-20];
		}
/*		arr[20] = (byte) 0xDE;
		arr[21] = (byte) 0xAD;
		arr[22] = (byte) 0xBE;
		arr[23] = (byte) 0xEF;*/
		
		return arr;
	}
		
	public static void handshake(InputStream is){
		try {
			System.out.print("0x");
			byte serverResponse = 0;
			for (int i = 0; i < 4; ++i){
				serverResponse = (byte) is.read();
				System.out.print(Integer.toHexString(serverResponse & 0xFF).toUpperCase());
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void getPort(InputStream is){
		
	}
	
	public static short checksum(byte[] b){
		long concat = 0x0;
		long sum = 0x0;
		for (int i = 0; i < b.length; i+=2){
			concat = (long) (b[i] & 0xFF);
			concat <<= 8;

			if ((i+1) < b.length){
				concat |= (b[i+1] & 0xFF);
			}
			sum = sum + concat;
			if (sum > 0xFFFF){
				sum &= 0xFFFF;
				sum ++;
			}
		}
		short checksum = (short) (~sum);
//		System.out.println("Checksum calculated: 0x" + Integer.toHexString(checksum & 0xFFFF).toUpperCase());
		return (short) (~sum);
	}
}