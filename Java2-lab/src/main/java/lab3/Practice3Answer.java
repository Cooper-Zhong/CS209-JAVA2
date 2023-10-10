package lab3;

import org.apache.commons.math3.primes.Primes;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

import static org.apache.commons.math3.primes.Primes.isPrime;

public class Practice3Answer {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("Please input the function No:");
            System.out.println("1 - Get even numbers");
            System.out.println("2 - Get odd numbers");
            System.out.println("3 - Get prime numbers");
            System.out.println("4 - Get prime numbers that are bigger than 5");
            System.out.println("0 - Quit");
            int func_no = in.nextInt();
            if (func_no == 0) {
                return;
            }
            //functional programming
            Predicate<Integer> evenFilter = num -> num%2 == 0;
            Predicate<Integer> oddFilter = num -> num%2 != 0;
            Predicate<Integer> primeFilter = Primes::isPrime;//method reference
            Predicate<Integer> primeGreaterThan5Filter = num -> (isPrime(num) && num>5);
            Predicate<Integer> filter;
            switch (func_no) {
                case 1:
                    filter = evenFilter;
                    break;
                case 2:
                    filter = oddFilter;
                    break;
                case 3:
                    filter = primeFilter;
                    break;
                case 4:
                    filter = primeGreaterThan5Filter;
                    break;
                case 0:
                    System.exit(0);
                default:
                    System.out.println("Invalid choice.");
                    return;
            }

            System.out.println("Input size of the list:");
            int size = in.nextInt();
            int[] arr = new int[size];
            System.out.println("Input elements of the list:");
            for (int i = 0; i<size; i++) {
                arr[i] = in.nextInt();
            }
            //apply to the array
            List<Integer> res = filterArray(arr, filter);
            System.out.println("Filter results:");
            System.out.println(res);
        }
    }

    public static List<Integer> filterArray(int[] array, Predicate<Integer> filter) {
        List<Integer> result = new ArrayList<>();
        for (int num : array) {
            if (filter.test(num)) {
                result.add(num);
            }
        }
        return result;
    }
}
