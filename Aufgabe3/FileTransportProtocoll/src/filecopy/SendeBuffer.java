package filecopy;


import java.util.LinkedList;

public class SendeBuffer{
    private LinkedList<FCpacket> sendebuffer = new LinkedList<FCpacket>();;
    private int sequenceNumber = 0;
    private int nextPacketIndex = 0;
    private int sendbase=0;


    public SendeBuffer() {}

    // Fuege dem Sendebuffer ein neues Packet hinzu.
    public synchronized void addPacket(FCpacket newPacket) {
        if (newPacket != null) {
            sendebuffer.add(newPacket);
        } else {
            throw new NullPointerException();
        }
    }

    public synchronized void addPacket(FCpacket packet, int index) {
        sendebuffer.add(index, packet);
    }

    public synchronized void addAllPackets(LinkedList<FCpacket> incomingPackets) {
        sendebuffer.addAll(incomingPackets);
    }

    // Loesche ein Packet aus dem Sendebuffer anhand der Sequenznummer.
    public synchronized void deletePacketIfAlreadyAcked(FCpacket packet) {
        if (packet.isValidACK()) {
            sendebuffer.remove(packet);
        } else {
            throw new NullPointerException();
        }
    }

    // Falls nicht benötigt wegschmeißen
    public synchronized void setAckForPacket(FCpacket packetToAck) {

        packetToAck.setValidACK(true);
        if (!findBySequenceNr(packetToAck.getSeqNum()).isValidACK()) System.exit(-1);
    }

    // Hole die Sequenznummer, des nächsten Packets, welches verschickt werden soll.
    public synchronized int getNextPacketIndex() {
        return nextPacketIndex;
    }

    public synchronized void setNextPacketIndex(int newPacketIndex) {
        this.nextPacketIndex = newPacketIndex;
    }

    // Erhöhen der NextSequenzNummer
    public synchronized void increaseIndex() {
        if (nextPacketIndex<sendebuffer.size()-1) nextPacketIndex++;
    }

    public synchronized int size() {
        return sendebuffer.size();
    }

    public synchronized FCpacket getPacket(int index) {
        return sendebuffer.get(index);
    }

    public synchronized int getSendbase() {
        return sendbase;
    }

    public synchronized FCpacket findBySequenceNr(long seqNr) {
        return sendebuffer.stream().filter(x -> x.getSeqNum() == seqNr).findFirst().get();
    }

    public synchronized Boolean isFinished() {
        Boolean result=true;
        for (int i = sendbase; i < sendebuffer.size(); i++) {
            if (!sendebuffer.get(i).isValidACK()) {result=false;
                System.out.println("ERR "+sendbase+" - "+i);break;}
        }
        return result;
    }

    public synchronized void computeSendbase() {
        while(sendebuffer.get(sendbase).isValidACK() && sendbase + FileCopyClient.windowSize < sendebuffer.size()) {
            sendbase++;
        }
        System.out.println("new SB: "+sendbase);
    }
}
