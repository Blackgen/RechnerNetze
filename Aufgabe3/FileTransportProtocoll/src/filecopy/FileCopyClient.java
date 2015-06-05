package filecopy;

/* FileCopyClient.java
 Version 0.1 - Muss erg�nzt werden!!
 Praktikum 3 Rechnernetze BAI4 HAW Hamburg
 Autoren:
 */

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;

public class FileCopyClient extends Thread {

  // -------- Constants
  public final static boolean TEST_OUTPUT_MODE = false;

  public final int SERVER_PORT = 23000;

  public final int UDP_PACKET_SIZE = 1008;

  public final int DATA_LENGTH = UDP_PACKET_SIZE-64;

  public final static long DELAY = 10; // Propagation delay in ms

  // -------- Public parms
  public String servername;

  public String sourcePath;

  public String destPath;

  public int windowSize;

  public long serverErrorRate;

  public InetAddress server;

  public DatagramSocket clientSocket;

  // -------- Variables
  // current default timeout in nanoseconds
  private long timeoutValue = 100000000L;

  // ... ToDo
  private LinkedList<FCpacket> sendBuffer;
  private LinkedList<Long> ackList=new LinkedList<>();
  private int nextPacketNum;


  // Constructor
  public FileCopyClient(String serverArg, String sourcePathArg,
    String destPathArg, String windowSizeArg, String errorRateArg) throws IOException{
    servername = serverArg;
    sourcePath = sourcePathArg;
    destPath = destPathArg;
    windowSize = Integer.parseInt(windowSizeArg);
    serverErrorRate = Long.parseLong(errorRateArg);
    sendBuffer = new LinkedList<FCpacket>();
    clientSocket = new DatagramSocket();
    server = InetAddress.getByName(servername);
  }

  public void runFileCopyClient() {

    // ToDo!!
    FCpacket initPacket = makeControlPacket();

    sendBuffer.add(initPacket);
    startTimer(initPacket);
    nextPacketNum++;

    while(!sendBuffer.isEmpty()) {


      if (sendBuffer.size() < windowSize) {
        FCpacket fHJSB = createNewPacket(DATA_LENGTH);
        sendBuffer.add(fHJSB);

        // Sende dingsbums
        FCpacket pack = sendBuffer.stream().filter(x -> x.getSeqNum() == nextSeqNum()).findFirst().get();
        sendPacket(pack);
        startTimer(pack);
        nextPacketNum++;
      }


      // Ack angekommen
      if (ackList.size() > 0) {
        for (long numb : ackList) {
          FCpacket current = sendBuffer.stream().filter(x -> x.getSeqNum() == numb).findFirst().get();
          current.setValidACK(true);
          cancelTimer(current);
          ackList.remove(numb);
          // TODO Timer neu berechnen
          if (current.getSeqNum() == sendbase()) {
            for (FCpacket packet : sendBuffer) {
              if (packet.isValidACK()) {
                sendBuffer.remove(packet);
              } else break;
            }
          }
        }
      }
    }




  }

  /**
  *
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
    FCpacket f = sendBuffer.stream().filter(x -> x.getSeqNum()==seqNum).findFirst().get();
    sendPacket(f);
    startTimer(f);
  }


  /**
   *
   * Computes the current timeout value (in nanoseconds)
   */
  public void computeTimeoutValue(long sampleRTT) {

  // ToDo
  }


  /**
   *
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
    FileCopyClient myClient = new FileCopyClient("localhost", System.getProperty("user.dir")+ "\\test.txt", "text.txt", "100", "100");
    myClient.runFileCopyClient();
  }

  private long sendbase() {
    return sendBuffer.getFirst().getSeqNum();
  }

  private long nextSeqNum() {
    return sendBuffer.get(nextPacketNum-1).getSeqNum();
  }

  private void sendPacket(FCpacket packetOut) {
    DatagramPacket dataPacket = new DatagramPacket(packetOut.getSeqNumBytesAndData(),
            packetOut.getLen()+8, server, SERVER_PORT);
    new sendThread(dataPacket).run();
  }

  private FCpacket createNewPacket(long maxSize) {
    int position = Math.max(0, safeLongToInt(DATA_LENGTH * (nextSeqNum() - 1)));
    char[] buffer=new char[DATA_LENGTH];
    FCpacket newPacket=null;
    try {
      FileReader fstream = new FileReader(sourcePath);
      fstream.read(buffer,position,DATA_LENGTH);
      newPacket = new FCpacket(nextSeqNum(),new String(buffer).getBytes("UTF-8"),buffer.length);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return newPacket;
  }

  public static int safeLongToInt(long l) {
    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
      throw new IllegalArgumentException
              (l + " cannot be cast to int without changing its value.");
    }
    return (int) l;
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
        DatagramPacket data = null;
        try {
          clientSocket.receive(data);
          ackList.add(ByteBuffer.wrap(data.getData()).getLong());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}
