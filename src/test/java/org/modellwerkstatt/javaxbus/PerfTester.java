package org.modellwerkstatt.javaxbus;

import org.modellwerkstatt.javaxbus.EventBus;
import org.modellwerkstatt.javaxbus.ErrorHandler;
import org.modellwerkstatt.javaxbus.Message;

public class PerfTester {


    public static void main(String[] args) {
        // EventBus bus = EventBus.create("modwerk-test.mpreis.co.at", 2128);
        EventBus bus = EventBus.create("localhost", 8089);


        bus.addErrorHandler(new ErrorHandler() {
            public void handleMsgFromBus(boolean stillConnected, boolean eventLoopRunning, Message msg) {
                String errorText = "Eventbus related problem: connected-now " + stillConnected + ", bus-receiver-running " + eventLoopRunning + ", ";
                if (msg.isErrorMsg()) {
                    errorText += "bus-error-msg " + msg.getErrMessage() + ", code " + msg.getErrFailureCode() + ", type " + msg.getErrFailureType();
                } else {
                    errorText += " message " + msg.getBodyAsMJson().toString();
                }

                if (!(eventLoopRunning)) {
                    errorText = "EVENT RECEIVER DISABLED! " + errorText;
                }
                System.err.println(errorText);
            }

            public void handleException(boolean stillConnected, boolean eventLoopRunning, Exception ex) {
                String errorText = "Eventbus related problem: connected-now " + stillConnected + ", bus-receiver-running " + eventLoopRunning + ", ";
                if (!(eventLoopRunning)) {
                    errorText = "EVENT RECEIVER DISABLED! " + errorText;
                }
                System.err.println(errorText);
                ex.printStackTrace();
            }
        });


        int numThreads = 1;
        int waitMs = 5000;
        HeavyWriter[] writers = new HeavyWriter[numThreads];
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            writers[i] = new HeavyWriter(bus, "Writer_" + i);
            threads[i] = new Thread(writers[i]);
        }

        // start them...
        for (int i = 0; i < numThreads; i++) {
            threads[i].start();
        }

        try {
            Thread.sleep(waitMs);

            for (int i = 0; i < numThreads; i++) {
                writers[i].stopLoop();
            }
            for (int i = 0; i < numThreads; i++) {
                threads[i].join();
            }

            bus.close();

            System.err.println("Bus closed ... ");
            Thread.sleep(waitMs);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        long total = 0;
        for (int i = 0; i < numThreads; i++) {
            total += writers[i].getCount();
            System.err.println("Writer " + i + " => " + writers[i].getCount());
        }
        System.err.println("Total of  => " + total);



    }

}


