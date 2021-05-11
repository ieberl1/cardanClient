package com.cardan.client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import static java.lang.Math.min;

/*
    Entities:
    * w: any web server (ex: www.google.com)
    * s: the local server (the java server program that we will create)
    * c: this program

    This program (eventually) will:
(DONE)  1.Ask user for input (w x y)
            w: web server (www.google.com)
            x: integer, where "z" = min(x, 1460) is the # of bytes C will send to "s" in every packet containing "w"'s page
            y: timeout period in milliseconds
(DONE)  2. Send HTTP GET to "w" using HTTPURLConnection
(DONE)  3. When a line of the page is received use two threads to:
  (DONE)    a: Send packets w/ the line to localhost:22333 (UDP) ("z" bytes in each packet)
  (DONE)    b: Print page contents to terminal
(DONE)  4. If "c" does not get ACK before timeout period "y" expires; resend the last "z" bytes. If after this resend no
                ACK is received send "FAIL" to "s" and quit.
(DONE)  5. Whenever an ACK is received from "s" print "ACK".
(DONE)  6. When all bytes have been sent, print "DONE" and total time taken from beginning of page transfer until last
                ACK has been received, and total number of bytes in the page sent
 */

public class cardanClient
{

    public static void main(String[] args) {
        InetAddress destIP;
        int destPort = 22333;
	    String strUrl = "";
	    int desiredBytes = 1460;
	    int desiredTimeout = 0;
	    int total_bytes_successfully_sent = 0;
	    Scanner scanner = new Scanner(System.in);
	    java.util.ArrayList<UDPThread> threadArray = new ArrayList<>();

	    System.out.println("Welcome to the cardanClient group project");
	    System.out.println("Please input desired web server \"w\", max number of bytes to send (less than 1460) " +
                "\"x\", and desired timeout in seconds \"y\". Use the format: w x y ");
	    System.out.println("Example: http://www.google.com 123 5");
	    System.out.print("> ");

        try{
            strUrl = scanner.next();
            desiredBytes = scanner.nextInt();
            desiredTimeout = scanner.nextInt();
        }
        catch(Exception e){
            System.out.println("Invalid input. Exiting...");
            System.exit(-1);
        }
        // Verify desiredBytes and desiredTimeout are in a valid range
        if(0 > desiredBytes || 0 > desiredTimeout){
            System.out.println("Invalid input. Exiting...");
            System.exit(-1);
        }
        // Get min(x, 1460)
        desiredBytes = min(desiredBytes, 1460);

        // System.out.printf("Website: %s%n", strUrl);
        // System.out.printf("DesiredBytes: %d%n", desiredBytes);
        // System.out.printf("Timeout: %d seconds%n", desiredTimeout);

        try {
            destIP = InetAddress.getLocalHost();
            //URL url = new URL("http://www.google.com");
            URL url = new URL(strUrl);
            BufferedReader reader;
            String line;
            try {
                // Create a connection object
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                // Set the properties of the connection
                con.setRequestMethod("GET");
                // Get start time
                long startTime = System.nanoTime();
                // send the GET request
                con.connect();
                // Receive the response into a Buffered Reader object
                reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                // Loop through the response and print it to the terminal
                while ((line = reader.readLine()) != null) {
                    // todo: Use threading to do the next lines at the same time
                    if(0 >= line.length()) {
                        continue;
                    }
                    System.out.printf("Line: %s%n", line);
                    // Send packets w/ the line to "s" at UDP port 22333 ("z" bytes in each packet)
                    UDPThread udpThread = new UDPThread();
                    threadArray.add(udpThread);
                    udpThread.setup_socket(desiredTimeout);
                    udpThread.setDestIp(destIP);
                    udpThread.setDestPort(destPort);
                    udpThread.setMaxSendBytes(desiredBytes);
                    udpThread.setLine(line);
                    udpThread.start();

                }
                // Set the ending time
                long endTime = System.nanoTime();
                double total_time_sec = (endTime - startTime) / 1000000000.0;
                // Get total bytes successfully send and ACKED
                for(UDPThread thread : threadArray){
                    thread.join();  // Makes sure that the thread has finished
                    total_bytes_successfully_sent += thread.get_total_bytes();
                }
                System.out.println("DONE!");
                System.out.printf("Total time: %.2f seconds%n", total_time_sec);
                System.out.printf("Total bytes successfully sent: %d%n", total_bytes_successfully_sent);
            }
            catch(IOException e){
                System.out.print("Error: ");
                System.out.println(e.getMessage());
                System.exit(-1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        catch (MalformedURLException e) {
            System.out.println("Invalid URL. Exiting...");
            System.exit(-1);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

class UDPThread extends Thread{
    DatagramSocket sock = null;
    InetAddress dest_ip = null;
    int dest_port = 0;
    int max_send_bytes = 0;
    String line = null;
    int total_bytes_received = 0;


    /**
     * <p>Sets the destination IP address for the class
     * </p>
     * @param dest_ip The InetAddress object
     */
    void setDestIp(InetAddress dest_ip){
        this.dest_ip = dest_ip;
    }


    /**
     * <p>Sets the destination port for the class
     * </p>
     * @param dest_port The int port to send packets to
     */
    void setDestPort(int dest_port){
        this.dest_port = dest_port;
    }


    /**
     * <p>The sets class variable for the maxinum number of bytes to send in one packet
     * </p>
     * @param max_send_bytes The int maximum number of bytes to send in one packet
     */
    void setMaxSendBytes(int max_send_bytes){
        this.max_send_bytes = max_send_bytes;
    }


    /**
     * <p>Sets the class variable for the string to send to the destination
     * </p>
     * @param line The string to send to the destination
     */
    void setLine(String line){
        this.line = line;
    }


    /**
     * <p>Returns the total number of bytes successfully sent (and ACKed)
     * </p>
     * @return  the int number of bytes successfully sent (and ACKed)
     */
    public int get_total_bytes(){ return total_bytes_received; }


    /**
     * <p>The code to execute when the thread is started. It will verify all needed class variables have been assigned,
     * and then send the data over udp, wait for the response, and record the number of bytes successfully sent and
     * ACKed.
     * </p>
     */
    public void run(){
        try{
            // Make sure we're ready to run before attempting to run
            if (sock == null ||
                    dest_ip == null ||
                    dest_port == 0 ||
                    max_send_bytes == 0 ||
                    line == null){
                System.out.println("Not ready to start thread!");
                System.out.println("One of the arguments have not been set");
                return;
            }

            int bytes_received = udp_send_receive(sock, dest_ip, dest_port, max_send_bytes, line);
            if (bytes_received >= 0) {
                total_bytes_received += bytes_received;
            } else {
                System.out.println("Failed to receive data!");
                System.out.println("Sending 'FAIL' message.");
                udp_send(sock, dest_ip, dest_port, "FAIL".getBytes());
                sock.close();
                System.out.println("Exiting...");
                System.exit(-1);
            }
        }
        catch (Exception e){
            System.out.println("Caught the exception!");
            e.printStackTrace();
        }
        sock.close();
    }


    /**
     * <p>The setup_socket method will create a DatagramSocket object for sending and receiving data. It sets the
     * timeout of the socket to the timeout specified
     * </p>
     * @param timeout   The int timeout in milliseconds to wait for a response
     */
    void setup_socket(int timeout){
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            sock.setSoTimeout(timeout);
        }
        catch (SocketException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        this.sock = sock;
    }


    /**
     * <p>The udp_send method will send an array of bytes to the defined ip:port combo using the given socket.
     * </p>
     * @param sock  The DatagramSocket object used to send the data
     * @param ip    The InetAddress ip address to send the packet to
     * @param port  The int port to send the packet to
     * @param send_data The byte array of data to send
     */
    private static void udp_send(DatagramSocket sock, InetAddress ip, int port, byte[] send_data) {
        try{
            DatagramPacket udp_packet = new DatagramPacket(send_data, send_data.length, ip, port);
            sock.send(udp_packet);
        }
        catch(SocketException e) {
            e.printStackTrace();  // Just print the exception to the terminal
            System.exit(-1);  // Bail
        } catch (IOException e) {
            System.out.println("Socket send error occurred");
            e.printStackTrace();
            System.exit(-1);  // Bail
        }
    }

    /**
     * <p>The udp_receive method will attempt to receive data from the given socket. Assumes socket timeout has already
     * been set and that the data received is always a string that can be converted to an int.
     * </p>
     * @param recv_socket   The DatagramSocket object to receive the data from
     * @return  The data recieved parsed as an int
     */
    private static int udp_receive(DatagramSocket recv_socket) {
        int ret = 0;
        byte[] receive_buf = new byte[1460];
        DatagramPacket data_in = new DatagramPacket(receive_buf, receive_buf.length);  // 1460 is max amount to expect
        try {
            // Get and store info about received packet
            recv_socket.receive(data_in);
            String data_in_str = new String(data_in.getData()).trim();

            // Take actions on received packet
            System.out.println("ACK");
            ret = Integer.parseInt(data_in_str);
        }
        catch (SocketTimeoutException e) {
            System.out.println("Socket timed out");
            ret = -1;
        }
        catch (SocketException e) {
            System.out.println("A socket error has occurred.");
            System.out.println("Error: " + e.getMessage());
            System.exit(-1);
        }
        catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        return ret;
    }

    /**
     * <p>The udp_send_receive method will send a message to a remote host, get the response, resend once (if required),
     * and return the number of bytes successfully sent and ACKed.
     * </p>
     * @param sock  The DatagramSocket object that will be used for sending and receiving
     * @param dest_ip   The InetAddress object that has the ip address to send data to
     * @param dest_port The int port to send data to
     * @param max_send_bytes    The maximum number of bytes that can be sent in one packet
     * @param line  The String data to send
     * @return  The int number of bytes successfully send and ACKed, or -1 if timeout
     */
    private static int udp_send_receive(DatagramSocket sock, InetAddress dest_ip, int dest_port, int max_send_bytes, String line) {
        int bytes_sent_n_acked = 0;
        int data_len = line.length();
        int bytes_left = line.length();
        byte[] data_to_send = line.getBytes();
        // System.out.printf("Data len is '%s'%n", data_len);
        while(bytes_sent_n_acked < data_len) {
            int tmp_bytes_received = 0;
            int bytes_send_len = min(max_send_bytes, bytes_left);
            byte[] curr_data = Arrays.copyOfRange(data_to_send, bytes_sent_n_acked, (bytes_sent_n_acked + bytes_send_len));
            // System.out.printf("Sending byte numbers %d to %d%n", bytes_sent_n_acked, (bytes_sent_n_acked + bytes_send_len));
            // System.out.printf("Sending: %s%n", Arrays.toString(curr_data));
            for (int i = 0; i < 2; i++) {
                udp_send(sock, dest_ip, dest_port, curr_data);
                tmp_bytes_received = udp_receive(sock);
                if (0 < tmp_bytes_received) {
                    break;
                }
                // timeout occurred so just continue loop
            }
            // Check for timeout
            if(tmp_bytes_received == -1) {
                bytes_sent_n_acked = -1;  // timeout has occurred.
                break;
            }
            else {
                bytes_sent_n_acked += bytes_send_len;
                bytes_left -= bytes_send_len;
            }
        }
        return bytes_sent_n_acked;
    }

}
