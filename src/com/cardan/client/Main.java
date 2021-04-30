package com.cardan.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/*
    Entities:
    * w: any web server (ex: www.google.com)
    * s: the local server (the java server program that we will create)
    * c: this program

    This program (eventually) will:
        1. Ask user for input (w x y)
            w: web server ("s") (www.google.com)
            x: integer, where "z" = min(x, 1460) is the # of bytes C will send to "s" in every packet containing "w"'s page
            y: timeout period in milliseconds
        2. Send HTTP GET to "w" using HTTPURLConnection
        3. When a line of the page is received use two threads to:
            a: Send packets w/ the line to "s" at UDP port 22333 ("z" bytes in each packet)
            b: Print page contents to terminal
        4. If "c" does not get ACK before timeout period "y" expires; resend the last "z" bytes. If after this resend no
                ACK is received send "FAIL" to "s" and quit.
        5. Whenever an ACK is received from "s" print "ACK".
        6. When all bytes have been sent, print "DONE" and total time taken from beginning of page transfer until last
                ACK has been received, and total number of bytes in the page sent
 */

public class Main {

    public static void main(String[] args){
	    int destPort = 22333;
	    String strUrl = "";
	    int desiredBytes = 1460;
	    int desiredTimeout = 0;
	    String userInput = "";
	    Scanner scanner = new Scanner(System.in);

	    System.out.println("Welcome to the cardanClient group project");
	    System.out.println("Please input desired web server \"w\", max number of bytes to send (less than 1460) \"x\", and desired " +
                "timeout in seconds \"y\". Use the format: w x y ");
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
        if(1460 < desiredBytes){
            desiredBytes = 1460;
        }

        System.out.printf("Website: %s\r\n", strUrl);
        System.out.printf("DesiredBytes: %d\r\n", desiredBytes);
        System.out.printf("Timeout: %d seconds\r\n", desiredTimeout);

        try {
            //URL url = new URL("http://www.google.com");
            URL url = new URL(strUrl);
            get_web_page(url);
        }
        catch (MalformedURLException e){
            System.out.println("Invalid URL. Exiting...");
            System.exit(-1);
        }

    }

    public static void get_web_page(URL url) {
        BufferedReader reader;
        String line;
        try {
            // Create a connection object
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            // Set the properties of the connection
            con.setRequestMethod("GET");
            // send the GET request
            con.connect();
            // Receive the response into a Buffered Reader object
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            // Loop through the response and print it to the terminal
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("Page downloaded.");
        }
        catch(IOException e){
            System.out.print("Error: ");
            System.out.println(e.getMessage());
            System.exit(-1);
        }
    }

    private static void clearScreen(){
        System.out.println("\033[2J\033[0;0H");
    }
}
