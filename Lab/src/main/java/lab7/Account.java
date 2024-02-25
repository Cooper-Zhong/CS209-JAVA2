package lab7;

import java.util.*;
import java.util.concurrent.locks.*;

public class Account {
    private double balance;
    private Lock accountLock;

    public Account() {
        accountLock = new ReentrantLock();
    }

    /**
     * @param money
     */
    public void deposit(double money) {
        accountLock.lock();
        try {
            double newBalance = balance + money;
            try {
                Thread.sleep(10);   // Simulating this service takes some processing time
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            balance = newBalance;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            accountLock.unlock();
        }

    }


    public double getBalance() {
        return balance;
    }
}