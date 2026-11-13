package com.javabank.util;

import java.math.BigDecimal;
import java.util.Scanner;

public class InputHelper {
    private static final Scanner scanner = new Scanner(System.in);

    public static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                int val = Integer.parseInt(line);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println(">>> [!] Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println(">>> [!] Invalid integer input. Please try again.");
            }
        }
    }

    public static BigDecimal readBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim().replace("₹", "").replace(",", "");
            try {
                BigDecimal val = new BigDecimal(line);
                if (val.compareTo(BigDecimal.ZERO) > 0) {
                    return val;
                }
                System.out.println(">>> [!] Amount must be strictly greater than zero.");
            } catch (Exception e) {
                System.out.println(">>> [!] Invalid monetary amount. Example: 2500.00");
            }
        }
    }

    public static String readPin(String prompt) {
        while (true) {
            System.out.print(prompt);
            String pin = scanner.nextLine().trim();
            if (pin.matches("^\\d{4}$")) {
                return pin;
            }
            System.out.println(">>> [!] Security PIN must be exactly 4 digits (e.g. 1234).");
        }
    }
}
