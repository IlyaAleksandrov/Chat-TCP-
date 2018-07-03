package aleksandrov.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An additional class for reading or writing to the console
 */

public class ConsoleHelper {
    /**
     * BufferedReader for console
     */
    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));


    /**
     * method to write output to the console
     * @param message - message to print into the console
     */
    public static void writeMessage(String message){
        System.out.println(message);
    }

    /**
     * method which tries to read the input from console
     * @return returning the string from input or tries to read again
     */
    public static String readString() {
        try{
            return br.readLine();
        }
        catch (IOException e){
            System.out.println("Error in entering the text. Try again.");
            return readString();
        }
    }

    /**
     * method which tries to read the integer input from console
     * it used for typing port numbers
     * @return the number of port
     */
    public static int readInt(){
        try {
            return Integer.parseInt(readString());
        }
        catch(NumberFormatException e){
            System.out.println("Error in entering a digit. Try again.");
            return readInt();
        }
    }
}
