package lab7;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyBlockingQueue<E> {
    private Queue<E> queue;
    private int capacity;

    private ReentrantLock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();
    private Condition notFull = lock.newCondition();

    public MyBlockingQueue(int capacity) {
        //TODO: Constructor with input capacity
        this.capacity = capacity;
        queue = new LinkedList<>();
    }

    public void put(E e, String name) {
        //TODO: When the queue is full,  wait until the consumer takes data and the queue has some empty buffer
        lock.lock();
        try {
            while (queue.size() == capacity) {
                notFull.await();
            }
            queue.add(e);
            System.out.println("Thread:" + name + ", Produced: " + e + ", Queue:" + queue);
            notEmpty.signalAll();
        } catch (InterruptedException ex) {
            ex.printStackTrace();

        } finally {
            lock.unlock();
        }

    }

    public E take(String name) {
        //TODO: When queue empty,   wait until the producer puts new data into the queue
        lock.lock();
        try {
            while (queue.isEmpty()) {
                notEmpty.await();
            }
            E e = queue.remove();
            System.out.println("Thread:" + name + ", Consumed: " + e + ", Queue:" + queue);
            notFull.signalAll();
            return e;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            lock.unlock();
        }
    }

    public static void main(String[] args) throws InterruptedException {

        int CAPACITY = 200;
        int PRODUCER_WORK = 21;
        int PRODUCER_CNT = 100;
        int PRODUCER_OFF = 1000; // sleep
        int CONSUMER_WORK = 20; // 21>20
        int CONSUMER_CNT = 100;
        int CONSUMER_OFF = 10; // sleep

        MyBlockingQueue<Integer> queue = new MyBlockingQueue<>(CAPACITY);

        Runnable producer = () -> {
            for (int i = 0; i<PRODUCER_WORK; i++) {
                queue.put(i, Thread.currentThread().getName());
                try {
                    Thread.sleep(PRODUCER_OFF);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable consumer = () -> {
            for (int i = 0; i<CONSUMER_WORK; i++) {
                queue.take(Thread.currentThread().getName());
                try {
                    Thread.sleep(CONSUMER_OFF);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        for (int i = 0; i<PRODUCER_CNT; i++) {
            new Thread(producer).start();
        }
        for (int i = 0; i<CONSUMER_CNT; i++) {
            new Thread(consumer).start();
        }

    }

}
