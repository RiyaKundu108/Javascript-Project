import java.util.ArrayList;
import java.util.Scanner;

public class LuckyDraw {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Input names as an array
        String[] names = scanner.nextLine().split(" ");
        int n = scanner.nextInt();

        // Process the names
        ArrayList<String> remainingNames = eliminatePalindromes(names);

        // Apply the second criteria
        String winner = findWinner(remainingNames, n);

        // Output the winner
        System.out.println(winner);
    }

    // Function to eliminate palindromes from the names
    private static ArrayList<String> eliminatePalindromes(String[] names) {
        ArrayList<String> remainingNames = new ArrayList<>();

        for (String name : names) {
            // Check if the name forms a palindrome
            if (!isPalindrome(name.charAt(0), name.substring(1))) {
                remainingNames.add(name);
            }
        }

        return remainingNames;
    }

    // Function to check if a string is a palindrome
    private static boolean isPalindrome(char firstChar, String remaining) {
        StringBuilder str = new StringBuilder(remaining);
        return str.reverse().toString().equalsIgnoreCase(firstChar + remaining);
    }

    // Function to find the winner based on the second criteria
    private static String findWinner(ArrayList<String> names, int n) {
        int index = 0;

        while (names.size() > 1) {
            index = (index + n - 1) % names.size();
            names.remove(index);
        }

        return names.get(0);
    }
}