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

    public int windowSize;

    public long serverErrorRate;

    public InetAddress server;

    public DatagramSocket clientSocket;

    public long jitter;

    public long expRTT;

    public long rTT;

    public long averageRTT;

    // -------- Variables
    // current default timeout in nanoseconds
    private long timeoutValue = 100000000L;

    // ... ToDo
    private LinkedList<FCpacket> sendBuffer;

    private int nextPacketNum;


    // Constructor
    public FileCopyClient(String serverArg, String serverPortArg, String sourcePathArg,
                          String destPathArg, String windowSizeArg, String errorRateArg) throws IOException {
        servername = serverArg;
        serverport = Integer.parseInt(serverPortArg);
        sourcePath = sourcePathArg;
        destPath = destPathArg;
        windowSize = Integer.parseInt(windowSizeArg);
        serverErrorRate = Long.parseLong(errorRateArg);
        sendBuffer = new LinkedList<FCpacket>();
        server = InetAddress.getByName(servername);
    }

    public void runFileCopyClient() {
        try {
            clientEinrichten();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // ToDo!!
        FCpacket initPacket = makeControlPacket();

        sendBuffer.add(initPacket);

        System.out.println(sourcePath);
        sendBuffer = readFileToPackets(sourcePath, DATA_LENGTH);



        while (nextPacketNum<sendBuffer.size()) {
            // Sende alle Pakete
            System.out.println("Sending "+nextPacketNum);
            FCpacket packetToBeSent = sendBuffer.get(nextPacketNum);
            if (!packetToBeSent.isValidACK()) {sendPacket(packetToBeSent);}
            else {nextPacketNum++;}
        }


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
        FC_Timer timer = new FC_Timer(timeoutValue, this, packet.getSeqNum());
        packet.setTimer(timer);
        timer.start();
    }

    public void cancelTimer(FCpacket packet) {
    /* Cancel timer for the given FCpacket */
        testOut("Cancel Timer for packet" + packet.getSeqNum());

        if (packet.getTimer() != null) {
            packet.getTimer().interrupt();
        }
    }

    /**
     * Implementation specific task performed at timeout
     */
    public void timeoutTask(long seqNum) {
        testOut("Timer Timeout! Num: " + seqNum);
        FCpacket f = sendBuffer.stream().filter(x -> x.getSeqNum() == seqNum).findFirst().get();
        sendBuffer.add(f);
    }


    /**
     * Computes the current timeout value (in nanoseconds)
     */
    public void computeTimeoutValue(long sampleRTT) {
        double x = 0.25;
        double y = x / 2;

        expRTT = (long) ((1.0 - y) * expRTT + y * rTT);

        jitter = (long) ((1.0 - x) * jitter + x * Math.abs(rTT - expRTT));

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
        FileCopyClient myClient = new FileCopyClient("localhost", "23000", System.getProperty("user.dir") + "\\test.txt", "text.txt", "100", "1");
        myClient.runFileCopyClient();
    }

    private long sendbase() {
        return sendBuffer.getFirst().getSeqNum();
    }

    private long nextSeqNum() {
        return sendBuffer.get(nextPacketNum - 1).getSeqNum();
    }

    private void sendPacket(FCpacket packetOut) {
        DatagramPacket dataPacket = new DatagramPacket(packetOut.getSeqNumBytesAndData(),
                packetOut.getLen() + 8, server, SERVER_PORT);
        new sendThread(dataPacket).run();
        startTimer(packetOut);
        nextPacketNum++;
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
        FCpacket packet = sendBuffer.get(nextPacketNum);

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
                    testOut("Got Ack!");
                    long numb = ByteBuffer.wrap(data.getData()).getLong();
                    FCpacket current = sendBuffer.stream().filter(x -> x.getSeqNum() == numb).findFirst().get();
                    cancelTimer(current);
                    long duration = System.nanoTime() - current.getTimestamp();
                    computeTimeoutValue(duration);
                    averageRTT += duration;
                    current.setValidACK(true);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
