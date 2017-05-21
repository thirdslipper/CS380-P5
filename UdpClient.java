import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Random;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class UdpClient {
	public static void main(String[] args) {
		try (Socket socket = new Socket("codebank.xyz", 38005)){
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			PrintStream ps = new PrintStream(os);

			byte[] deadbeef = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
			ps.write(getIPV4Header(4, deadbeef));
			handshake(is);
			byte[] port = getPort(is);
			byte[] data;
			byte[] udp;
			int size = 1;
			
//			for (int i = 0; i < 12; ++i){
			size <<= 1;
				data = getData(size);		//(size) bytes of random data
				System.out.println("Data length: " + size);
				udp = UdpHeader(port, data);
				ps.write(getIPV4Header(8+size, udp));//, UdpHeader(size, port, data)));	//getData(size))));
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
		return arr;
	}
	public static byte[] UdpHeader(byte[] port, byte[] data){
		//minimum size is 8, + data size
		int length = 8 + data.length;	
		byte[] UDP = new byte[length];
		
		UDP[0] = (byte) 0xFF;// src, 2 byte
		UDP[1] = (byte) 0xFF;	
		
		UDP[2] = port[0]; // dest, 2 byte
		UDP[3] = port[1];
		
		UDP[4] = (byte) length;
		UDP[4] <<= 8; 
		UDP[5] = (byte) length;
 		
		UDP[6] = 0; //checksum to do
		UDP[7] = 0;
		
		for (int i = 8; i < length; ++i){
			UDP[i] = data[i-8];
		}
		
		short checksum = pseudoheader(length, port, data);
		UDP[6] = (byte) ((checksum >> 8) & 0xFF);
		UDP[7] = (byte) (checksum & 0xFF);
		
		return UDP;
	}
	public static short pseudoheader(int UdpLength, byte[] port, byte[] data){
		int length = 20 + UdpLength;
		byte[] arr = new byte[length];
		arr[0] = (byte) 76; 	//Src public IP Address
		arr[1] = (byte) 175;	
		arr[2] = (byte) 85;	
		arr[3] = (byte) 174;
		
		arr[4] = (byte) 52;	//Dest socket inet address : 52.37.88.154
		arr[5] = (byte) 37;
		arr[6] = (byte) 88;
		arr[7] = (byte) 154;
		
		arr[8] = 0x0; //padded
		arr[9] = 0x11;		//protocol
		arr[10] = (byte) (UdpLength >> 8);	//udp length
		arr[11] = (byte) (UdpLength & 0xFF);	//udp length 2
		
		arr[12] = (byte) 0xff;	//src port
		arr[13] = (byte) 0xff;
		
		arr[14] = port[0];	//dest port
		arr[15] = port[1];
		
		arr[16] = (byte) (length >> 8); 	//length
		arr[17] = (byte) (length & 0xFF);
		
		for (int i = 0; i < data.length; ++i){
			arr[i+20] = data[i];
		}
		return checksum(arr);
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
		System.out.println();
	}
	public static byte[] getData(int size){
		Random rng = new Random();
		byte[] data = new byte[size];
		for (int i = 0; i < size; ++i){
			data[i] = (byte) rng.nextInt(256);
		}
		return data;
	}
	public static byte[] getPort(InputStream is){
		byte[] port = new byte[2];
		try {
			for (int i = 0; i < 2; ++i){
				port[i] = (byte) is.read();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
//		System.out.println(Integer.toBinaryString(port & 0xFFFF));
		return port;
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