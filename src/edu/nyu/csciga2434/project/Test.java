package edu.nyu.csciga2434.project;

import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

/**
 * User: Minda Fang
 * Date: 11/29/16
 * Time: 10:00 AM
 *
 */
public class Test {

    private static TransactionManager manager = new TransactionManager();

    public static void main(String[] args) {
        String input = args[0];
        parseInput(input);
    }

    private static void parseInput(String input) {
        System.out.println("[New Test] Advanced Database Project New round of testing starts! By Minda Fang and Kim Tae Young.");
        try {
            FileReader fileReader = new FileReader(input);
            Scanner scanner = new Scanner(fileReader);
            while (scanner.hasNext()) {
                manager.readCommand(scanner.nextLine());
            }
            if (manager.getBufferedWaitList() != null) {
                System.out.println("Buffered WaitList Size:" + manager.getBufferedWaitList().size());
                System.out.println("[EOF] The testing is ended!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
