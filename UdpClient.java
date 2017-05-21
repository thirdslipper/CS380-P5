/**
 * Author: Colin Koo
 * Professor: Davarpanah
 * Program: Implement UDP packet.
 * Source: http://www.roman10.net/2011/11/27/how-to-calculate-iptcpudp-checksumpart-1-theory/
 */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Random;

public class UdpClient {
	/**
	 * The main method connects to a designated server through a socket and writes IPv4 and UDP
	 * packets with the correct header to the server, receiving a response in return and calculating the RTT. 
	 * @param args
	 */
	public static void main(String[] args) {
		try (Socket socket = new Socket("codebank.xyz", 38005)){
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			PrintStream ps = new PrintStream(os);

			byte[] deadbeef = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
			ps.write(getIPV4Header(4, deadbeef));	//Hardcoded 4 bytes for server handshake.
			System.out.print("Handshake response: ");
			serverResponse(is);
			
			byte[] port = getPort(is), data;
			double sent = 0, arrival = 0, avg = 0, elapsed = 0;
			long portNum = (short) (((((short) port[0]) << 8) & 0xFF00) | port[1]);
			System.out.println("Port number received: " + portNum);
			int size = 1;
			
			for (int i = 0; i < 12; ++i){
			size <<= 1;
				data = getData(size);		//Random data in an int (size) byte array
				System.out.println("Data length: " + size);
				
				ps.write(getIPV4Header(size+8, UdpHeader(port, data)));
				sent = System.currentTimeMillis();
				System.out.print("Response: ");
				serverResponse(is);
				
				arrival = System.currentTimeMillis();
				elapsed = arrival - sent;
				System.out.println("RTT: " + elapsed + "ms\n");
				avg += elapsed;
			}
			System.out.println("Average RTT: " + (avg/12) + "ms");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	/**
	 * This method is the same method as from the IPv4 program, with differences in the protocol and data fields, which are based off
	 * the UDP header.
	 * @param size
	 * @param data
	 * @return
	 */
	public static byte[] getIPV4Header(int size, byte[] data){
		short length = (short) (20+size);//20 bytes from IPv4 header, 8 bytes from UDP header, and int (size) from the desired data size.
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

		short cksum = checksum(arr);	//Checksum doesnt include data.
		arr[10] = (byte) ((cksum >> 8) & 0xFF);
		arr[11] = (byte) (cksum & 0xFF);
		
		for (int i = 20; i < 20+data.length; ++i){
			arr[i] = data[i-20];
		}
		return arr;
	}
	/**
	 * The UdpHeader method takes in a parameter port and data, which are variables to construct
	 * a UDP header.  It utilizes the psuedoheader method which uses data from the IPv4 and UDP
	 * headers to return a checksum for the UDP header.  
	 * @param port, port number from server
	 * @param data, randomized data
	 * @return byte array representing the UDP packet.
	 */
	public static byte[] UdpHeader(byte[] port, byte[] data){
		//minimum size is 8, + data size
		int length = 8 + data.length;	
		byte[] UDP = new byte[length];
		
		UDP[0] = (byte) 0xFF;// src, 2 byte
		UDP[1] = (byte) 0xFF;	
		
		UDP[2] = port[0]; // dest, 2 byte
		UDP[3] = port[1];
		
		UDP[4] = (byte) (length >> 8);	//UDP Length
		UDP[5] = (byte) length;
 		
		UDP[6] = 0; //checksum to do, includes data
		UDP[7] = 0;
		
		for (int i = 8; i < length; ++i){	
			UDP[i] = data[i-8];
		}
		
		short checksum = pseudoHeader(length, port, data);
		UDP[6] = (byte) ((checksum >> 8) & 0xFF);
		UDP[7] = (byte) (checksum & 0xFF);
		
		return UDP;
	}
	/**
	 * The pseudoHeader method constructs a checksum for the UDP header, using the IPv4's source address,
	 * destination address, protocol, and one byte of padding.  It also uses all of the UDP header's fields, 
	 * including the source port, destination port, UDP header length, and data.
	 * @param UdpLength
	 * @param port
	 * @param data
	 * @return checksum.
	 */
	public static short pseudoHeader(int UdpLength, byte[] port, byte[] data){
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
		
		arr[12] = (byte) 0xFF;	//src port
		arr[13] = (byte) 0xFF;
		
		arr[14] = port[0];	//dest port
		arr[15] = port[1];
		
		arr[16] = (byte) (UdpLength >> 8); 	//length
		arr[17] = (byte) (UdpLength & 0xFF);
		
		for (int i = 0; i < data.length; ++i){
			arr[i+18] = data[i];
		}
		return checksum(arr);
	}
	/**
	 * Reads 4 bytes from the server and prints it in hex.
	 * @param is
	 */
	public static void serverResponse(InputStream is){
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
	/**
	 * Randomizes all the slots in a byte array.  
	 * @param size of the data to be used in the UDP header
	 * @return byte array of random data
	 */
	public static byte[] getData(int size){
		Random rng = new Random();
		byte[] data = new byte[size];
		for (int i = 0; i < size; ++i){
			data[i] = (byte) rng.nextInt(256);
		}
		return data;
	}
	/**
	 * Returns the port number
	 * @param is
	 * @return byte array where first slot represents the first half of the port number and 
	 * 		the second slot represents the second half.
	 */
	public static byte[] getPort(InputStream is){
		byte[] port = new byte[2];
		try {
			for (int i = 0; i < 2; ++i){
				port[i] = (byte) is.read();
			}
		} catch (IOException e){
			e.printStackTrace();
		}
		return port;
	}
	/**
	 * This method returns a checksum for the parameter array, the same method used in a previous exercise. 
	 * @param b- byte array.
	 * @return checksum
	 */
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
//		short checksum = (short) (~sum);
//		System.out.println("Checksum calculated: 0x" + Integer.toHexString(checksum & 0xFFFF).toUpperCase());
		return (short) (~sum);
	}
}