package dk.au.measurementcollector.utils;

import java.io.IOException;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class NTPSync {
	
	private static int iterations = 5;
	
	/**
	 * Returns the NTP-given time offset in ms.
	 * 
	 * @return the time offset in milliseconds
	 * @throws UnknownHostException
	 * @throws SocketException
	 * @throws IOException
	 */
	public static double getTimeOffset() throws UnknownHostException, SocketException, IOException {
		
		double accumulatedTime = 0.0;
		for(int i = 0; i < iterations; i++) {
			// Send request to NTP-server
			DatagramSocket socket = new DatagramSocket();
			InetAddress address = InetAddress.getByName("ntp.inet.tele.dk");
			byte[] buf = new NtpMessage().toByteArray();
			DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 123);
			
			// Set the transmit timestamp *just* before sending the packet
			NtpMessage.encodeTimestamp(packet.getData(), 40,
				(System.currentTimeMillis()/1000.0) + 2208988800.0);
			
			// Send request to NTP-server
			socket.send(packet);
			
			// Get response
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			
			// Record destination timestamp
			double destinationTimestamp =
				(System.currentTimeMillis()/1000.0) + 2208988800.0;
			
			// Process response
			NtpMessage msg = new NtpMessage(packet.getData());
			
			// Calculate local clock-offset
			double localClockOffset =
				((msg.receiveTimestamp - msg.originateTimestamp) +
				(msg.transmitTimestamp - destinationTimestamp)) / 2;
			
			accumulatedTime += localClockOffset;
		}
		
		return (accumulatedTime/iterations)*1000;
	}

}
