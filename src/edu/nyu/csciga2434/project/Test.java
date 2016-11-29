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
        try {
            FileReader fileReader = new FileReader(input);
            Scanner scanner = new Scanner(fileReader);
            while (scanner.hasNext()) {
                manager.readCommand(scanner.nextLine());
            }
            System.out.println(manager.waitList.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
