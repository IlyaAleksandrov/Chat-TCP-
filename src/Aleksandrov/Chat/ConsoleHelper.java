package Aleksandrov.Chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleHelper {
    private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    public static void writeMessage(String message){
        System.out.println(message);
    }

    public static String readString() {
        try{
            return br.readLine();
        }
        catch (IOException e){
            System.out.println("Error in entering the text. Try again.");
            return readString();
        }
    }

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
