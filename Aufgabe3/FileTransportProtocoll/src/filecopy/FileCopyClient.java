package filecopy;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.LinkedList;

public class FileCopyClient extends Thread {

    // -------- Constants
    public final static boolean TEST_OUTPUT_MODE = true;

    public final int SERVER_PORT = 23000;

    public final int UDP_PACKET_SIZE = 1008;

    public final int DATA_LENGTH = UDP_PACKET_SIZE - 64;

    public final static long DELAY = 10; // Propagation delay in ms

    // -------- Public parms
    public String servername;

    public int serverport;

    public String sourcePath;

    public String destPath;

    public static int windowSize;

    public long serverErrorRate;

    public InetAddress server;

    public DatagramSocket clientSocket;

    public long jitter = 20;

    public long expRTT = 15 * 1000000;

    public long averageRTT;

    // -------- Variables
    // current default timeout in nanoseconds
    private long timeoutValue = 300000000L;

    // ... ToDo
    private SendeBuffer sendeBuffer;

    private int nextPacketNum;

    private long startTimespamp;

    private long globalPacketCount = 0;

    public static Boolean shouldWait = false;

    public static Boolean isWaiting = false;

    private static final Object lock = new Object();


    // Constructor
    public FileCopyClient(String serverArg, String serverPortArg, String sourcePathArg,
                          String destPathArg, String windowSizeArg, String errorRateArg) throws IOException {
        servername = serverArg;
        serverport = Integer.parseInt(serverPortArg);
        sourcePath = sourcePathArg;
        destPath = destPathArg;
        windowSize = Integer.parseInt(windowSizeArg);
        serverErrorRate = Long.parseLong(errorRateArg);
        sendeBuffer = new SendeBuffer();
        server = InetAddress.getByName(servername);
    }

    public void runFileCopyClient() {
        startTimespamp = System.nanoTime();
        try {
            clientEinrichten();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ToDo!!
        FCpacket initPacket = makeControlPacket();

        sendeBuffer.addPacket(initPacket);
        //sendBuffer.add(initPacket);

        System.out.println(sourcePath);
        sendeBuffer.addAllPackets(readFileToPackets(sourcePath, DATA_LENGTH));
        //sendBuffer.addAll(readFileToPackets(sourcePath, DATA_LENGTH));


//        while (sendeBuffer.getNextPacketIndex() < sendeBuffer.size()-1) {
        while (!sendeBuffer.isFinished()) {
            // Sende alle Pakete
            int i=sendeBuffer.getNextPacketIndex();
            if (i < sendeBuffer.size() && i<sendeBuffer.getSendbase()+windowSize) {
                System.out.println(Thread.currentThread().getName() + ": Sending " + sendeBuffer.getNextPacketIndex());
                testOut("Count: " + sendeBuffer.size() + ", Base:  " + getSendbaseSeqNr() + ", Next:  " + sendeBuffer.getNextPacketIndex());

                FCpacket packetToBeSent = sendeBuffer.getPacket(sendeBuffer.getNextPacketIndex());
                if (!packetToBeSent.isValidACK()) {
                    sendPacket(packetToBeSent);
                } else {
                    sendeBuffer.increaseIndex();
                }
            } else {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (shouldWait) {

                try {
                    isWaiting = true;
                    testOut("going to Sleep now. sW-" + shouldWait);
                    synchronized (lock) {
                        lock.wait(1000);
                    }
                    isWaiting = false;
                    testOut("Woken Up! " + sendeBuffer.getNextPacketIndex());
                } catch (InterruptedException e) {
                    testOut("Woken Up by Bell!");
                }

            }
            sendeBuffer.computeSendbase();
        }
        writeGreen("Sending Done!");
        writeGreen("Sent " + globalPacketCount + " Packets.");
        writeGreen("Sending took " + (double) (System.nanoTime() - startTimespamp) / 1000000000 + "s");


//            if (sendBuffer.size() < windowSize) {
//                testOut("New Packet");
//
//                // Sende Bumsdings
//                FCpacket pack = sendBuffer.stream().filter(x -> x.getSeqNum() == nextSeqNum()).findFirst().get();
//                sendPacket(pack);
//                startTimer(pack);
//                nextPacketNum++;
//            }


        // Ack angekommen

    }

    /**
     * Timer Operations
     */
    public void startTimer(FCpacket packet) {
    /* Create, save and start timer for the given FCpacket */
        FC_Timer timer = new FC_Timer(getTimeoutValue(), this, packet.getSeqNum());
        packet.setTimer(timer);
        timer.start();
    }

    public void cancelTimer(FCpacket packet) {
    /* Cancel timer for the given FCpacket */
        testOut("Cancel Timer for packet" + packet.getSeqNum());

        if (packet.getTimer() != null) {
            packet.getTimer().interrupt();
        } else testOut("No timer found");
    }

    /**
     * Implementation specific task performed at timeout
     */
    public void timeoutTask(long seqNum) {
        testOut("Timer Timeout! Num: " + seqNum);

        if (seqNum < nextSeqNum() && !FileCopyClient.shouldWait) {
            setTimeoutValue(getTimeoutValue() * 2);
            FileCopyClient.shouldWait = true;
            testOut("sw-SETTRUE - " + FileCopyClient.shouldWait);


            while (!isWaiting) {
                System.out.println("WAiting.." + isWaiting);

                try {
                    Thread.sleep(10);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("2-WAiting.." + isWaiting);
            FCpacket f = sendeBuffer.findBySequenceNr(seqNum);
            //FCpacket f = sendBuffer.stream().filter(x -> x.getSeqNum() == seqNum).findFirst().get();
            int i;
            for (i = 0; i < sendeBuffer.size(); i++) {
                FCpacket c = sendeBuffer.getPacket(i);
                if (c.getSeqNum() == seqNum) break;
            }
            if (sendeBuffer.getNextPacketIndex() > i) {
                System.out.println("Jumping Back to " + i);
                sendeBuffer.setNextPacketIndex(sendeBuffer.getSendbase());
            }
            FileCopyClient.shouldWait = false;
            testOut("sw-SETFALSE - " + FileCopyClient.shouldWait);
            synchronized (lock) {
                lock.notify();
            }

        }

        //sendBuffer.add(f);
    }


    /**
     * Computes the current timeout value (in nanoseconds)
     */
    public void computeTimeoutValue(long sampleRTT) {
        double x = 0.25;
        double y = x / 2;

        expRTT = (long) ((1.0 - y) * expRTT + y * sampleRTT);

        jitter = (long) ((1.0 - x) * jitter + x * Math.abs(sampleRTT - expRTT));

        setTimeoutValue(expRTT + 4 * jitter);
    }

    synchronized public long getTimeoutValue() {
        return timeoutValue;
    }

    synchronized public void setTimeoutValue(long timeoutValue) {
        this.timeoutValue = timeoutValue;
    }


    /**
     * Return value: FCPacket with (0 destPath;windowSize;errorRate)
     */
    public FCpacket makeControlPacket() {
   /* Create first packet with seq num 0. Return value: FCPacket with
     (0 destPath ; windowSize ; errorRate) */
        String sendString = destPath + ";" + windowSize + ";" + serverErrorRate;
        byte[] sendData = null;
        try {
            sendData = sendString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new FCpacket(0, sendData, sendData.length);
    }

    public void testOut(String out) {
        if (TEST_OUTPUT_MODE) {
            System.err.printf("%,d %s: %s\n", System.nanoTime(), Thread
                    .currentThread().getName(), out);
        }
    }

    public static void main(String argv[]) throws Exception {
//    FileCopyClient myClient = new FileCopyClient(argv[0], argv[1], argv[2], argv[3], argv[4]);
        FileCopyClient myClient = new FileCopyClient("localhost", "23000", System.getProperty("user.dir") + "\\FCdata.pdf", "out.pdf", "100", "100");
        myClient.runFileCopyClient();
    }

    private long getSendbaseSeqNr() {
        return sendeBuffer.getPacket(sendeBuffer.getSendbase()).getSeqNum();
    }

    private long nextSeqNum() {
        return sendeBuffer.getPacket(sendeBuffer.getNextPacketIndex()).getSeqNum();
    }

    private void sendPacket(FCpacket packetOut) {
        DatagramPacket dataPacket = new DatagramPacket(packetOut.getSeqNumBytesAndData(),
                packetOut.getLen() + 8, server, SERVER_PORT);
        packetOut.setTimestamp(System.nanoTime());
        testOut("Send SeqNr: " + packetOut.getSeqNum());
        new sendThread(dataPacket).run();
        startTimer(packetOut);
        sendeBuffer.increaseIndex();
        globalPacketCount++;
    }

    private LinkedList<FCpacket> readFileToPackets(String path, int packetSize) {
        LinkedList<FCpacket> result = new LinkedList<FCpacket>();
        File file = new File(path);
        if (file.exists()) {
            try {
                RandomAccessFile fstream = new RandomAccessFile(path, "r");
                byte[] b = new byte[(int) fstream.length()];
                fstream.read(b);
                int bufferSize = packetSize - 8;

                for (int i = 0; i * bufferSize < fstream.length(); i++) {
                    int newBufferSize = (i + 1) * bufferSize > fstream.length() ? (int) (fstream.length() - i * bufferSize) : bufferSize;
                    byte[] buffer = new byte[newBufferSize];
                    System.arraycopy(b, i * bufferSize, buffer, 0, newBufferSize);
                    result.add(new FCpacket(i + 1, buffer, buffer.length));
                }

                fstream.close();
            } catch (FileNotFoundException e) {
                System.err.println("File not found : " + e.getMessage());
                return result;
            } catch (IOException e) {
                System.err.println("Cannot read file : " + e.getMessage());
            }

        }
        return result;
    }


    private void sendNextPacket() {
        // Take pack aus Buffer
        FCpacket packet = sendeBuffer.getPacket(sendeBuffer.getNextPacketIndex());

        // Sende Packet
        if (!packet.isValidACK() && packet.getTimer() == null) {
            sendPacket(packet);
        }
    }

    private void clientEinrichten() throws IOException {

        server = InetAddress.getByName(servername);
        // initialisieren des clientsockets
        clientSocket = new DatagramSocket();
        // Recievethread erstellen! (muss noch run durchgeführt werden ?)
        receiveThread receiver = new receiveThread();


        Date startTime = new Date();
        receiver.setDaemon(true);
        receiver.start();

    }

    public void writeGreen(String text) {
        System.out.println("\u001B[32m" + text + "\u001B[0m");
    }

    private class sendThread extends Thread {
        /* Thread for sending of one ACK-Packet with propagation delay */
        DatagramPacket packet;

        public sendThread(DatagramPacket packet) {
            this.packet = packet;
        }

        public void run() {
            try {
                Thread.sleep(DELAY);
                clientSocket.send(packet);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Unexspected Error! " + e.toString());
                System.exit(-1);
            }
        }
    }

    private class receiveThread extends Thread {

        public void run() {
            while (true) {
                byte[] receiveData = new byte[8];
                DatagramPacket data = new DatagramPacket(receiveData, 8);

                try {
                    testOut("Wait for Ack..");
                    clientSocket.receive(data);

                    long numb = ByteBuffer.wrap(data.getData()).getLong();
                    FCpacket current = sendeBuffer.findBySequenceNr(numb);
                    //FCpacket current = sendBuffer.stream().filter(x -> x.getSeqNum() == numb).findFirst().get();
                    testOut("GOT ACK! SeqNr. " + numb);
                    cancelTimer(current);
                    testOut("Canceled Timer SeqNr. " + numb);
                    long duration = System.nanoTime() - current.getTimestamp();
                    //if (getTimeoutValue() > duration) current.setValidACK(true);
                    if (getTimeoutValue() > duration) sendeBuffer.setAckForPacket(current);
                    computeTimeoutValue(duration);
                    testOut("Computed Timeout SeqNr. " + numb);
                    averageRTT += duration;
                    testOut("Got Ack for Nr: " + numb + " after " + duration + "ns");


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
