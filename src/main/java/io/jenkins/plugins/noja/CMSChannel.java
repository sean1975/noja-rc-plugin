package io.jenkins.plugins.noja;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.DataOutputStream;
import java.io.DataInputStream;

public class CMSChannel {

    public static final int IdRelayNumber = 2138;
    public static final int IdRelaySoftwareVer = 2140;

    private static final List<String> requests;
    static {
        requests = new ArrayList<String>();
        // request to connect
        requests.add("05 00 15 00 4e f0 ad e0  01 00 09 04 00 06 01 c6 00 00 00 1e 5b 25 a8 44");
        // ack and get relay versions
        requests.add("f0 ad e1 02 00 00 f8 4f  9b 73 " + 
                     "f0 ad e0 03 00 5f 05 00  02 00 4e 05 00 02 13 cb " +
                     "05 00 02 08 64 05 00 02  08 62 05 00 02 08 61 05 " +
                     "00 02 08 63 05 00 02 08  5b 05 00 02 09 bb 05 00 " +
                     "02 08 5d 05 00 02 08 5a  05 00 02 08 5c 05 00 02 " +
                     "08 60 05 00 02 08 5e 05  00 02 08 5f 05 00 02 04 " +
                     "28 05 00 02 04 29 05 00  02 00 2f 05 00 02 00 36 " +
                     "05 00 02 04 2f d6 ab 35  d2");
    }

    protected static void getVersions(String hostName, int portNumber, List<String> requests, List<String> replies) {
        try {
            Socket clientSocket = new Socket(hostName, portNumber);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            byte[] buffer = new byte[1024];
            for (String request : requests) {
                String data[] = request.split("\\s+");
                for (int i=0, j=0; i<data.length ; i++) {
                     buffer[j] = Short.valueOf(data[i], 16).byteValue();
                     if ((j+1) % buffer.length == 0) {
                         out.write(buffer, 0, buffer.length);
                         out.flush();
                         j = 0;
                         continue;
                     }
                     j++;
                     if ((i+1) == data.length) {
                         out.write(buffer, 0, data.length % buffer.length);
                         out.flush();
                     }
                }
                System.out.println("Sent " + data.length + " bytes");
                System.out.println(request);
                int received = 0;
                while (received < 2) {
                    StringBuffer reply = new StringBuffer();
                    int len = in.read(buffer);
                    if (len < 0) {
                        System.out.println("Connection broken");
                        break;
                    }
                    System.out.println("Read " + len + " bytes");
                    for (int i=0; i<len; i++) {
                        reply.append(String.format("%02x ", buffer[i]));
                    }
                    System.out.println(reply.toString());
                    int offset = 0;
                    while (offset < reply.length()) {
                        offset = reply.indexOf("f0 ad ", offset);
                        if (offset < 0) {
                            break;
                        }
                        received++;
                        if (offset > 0) {
                            replies.add(reply.substring(0, offset));
                            reply.delete(0, offset);
                            offset = 0;
                            continue;
                        }
                        offset += 6;
                    }
                    replies.add(reply.toString());
                }
            }
            clientSocket.close();
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    /*
     * Unpack data point id and value from TCP payload
     * @param payload TCP payload in hex format with space as separator
     * @param dataPointMap unpacked data point and value pairs
     * @return bytes of message payload are successfully unpacked, excluding
     *         the first 6 bytes header and last 4 bytes checksum
     *         0 for ACK (no message), and -1 for error
     */
    protected static int unpackMessage(String payload, Map<Integer, String> dataPointMap) {
        String data[] = payload.trim().split("\\s+");
        if (data.length < 10) {
            System.out.println("Unknown message");
            return -1;
        }
        // ACK payload size 10
        if (data.length == 10) {
            System.out.println("ACK");
            return 0;
        }
        // first 6 bytes are header and flags
        int begin = 6;
        // last 4 bytes are checksum
        int end = data.length - 4;
        while (begin < end) {
            if (data[begin].compareTo("06") != 0) {
                // Not reply for CMS GET command
                System.out.println("Not reply for CMS GET command");
                // return error because other commands are not supported
                return -1;
            }
            begin++;
            if (begin + 2 > end) {
                // No reply length
                System.out.println("No reply length");
                break;
            }
            int replyLength = Integer.valueOf(data[begin] + data[begin+1], 16);
            begin += 2;
            if (begin + 2 > end) {
                // No data point id
                System.out.println("No data point id");
                break;
            }
            Integer id = Integer.valueOf(data[begin] + data[begin+1], 16);
            System.out.println("id: " + id);
            begin += 2;
            int valueLength = replyLength - 2;
            if (begin + valueLength > end) {
                // No data point value
                System.out.println("No data point value");
                dataPointMap.put(id, "");
                break;
            }
            StringBuffer value = new StringBuffer();
            for (int i=0; i<valueLength; i++) {
                value.append(data[begin+i]);
                value.append(" ");
            }
            dataPointMap.put(id, value.toString());
            System.out.println("value: " + value.toString());
            begin += valueLength;
        }
        return begin - 6;
    }

    protected static String getStringData(String data) {
        StringBuffer buffer = new StringBuffer();
        String[] bytes = data.split("\\s+");
        for (int i=0; i<bytes.length; i++) {
            char c = (char) Integer.valueOf(bytes[i], 16).intValue();
            if (!Character.isISOControl(c)) {
                buffer.append(c);
            }
        }
        return buffer.toString();
    }

    protected static String getSoftwareVersion(Map<Integer, String> dataPointMap) {
        if (dataPointMap.containsKey(IdRelaySoftwareVer)) {
            return getStringData(dataPointMap.get(IdRelaySoftwareVer));
        }
        return null;
    }

    protected static String getSerialNumber(Map<Integer, String> dataPointMap) {
        if (dataPointMap.containsKey(IdRelayNumber)) {
            return getStringData(dataPointMap.get(IdRelayNumber));
        }
        return null;
    }

    public static boolean connect(String hostName, int portNumber, Map<Integer, String> dataPointMap) {
        ArrayList<String> replies = new ArrayList<String>();
        getVersions(hostName, portNumber, requests, replies);
        boolean connected = replies.size() > 0;
        for (int i=0; i<replies.size(); i++) {
            unpackMessage(replies.get(i), dataPointMap);
        }
        if (dataPointMap.containsKey(IdRelaySoftwareVer)) {
            dataPointMap.put(IdRelaySoftwareVer, getSoftwareVersion(dataPointMap));
        }
        if (dataPointMap.containsKey(IdRelayNumber)) {
            dataPointMap.put(IdRelayNumber, getSerialNumber(dataPointMap));
        }
        return connected;
    }
    
    public static void main(String [] args) {
        System.out.println("Connecting to " + args[0] + ":" + args[1]);
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        ArrayList<String> replies = new ArrayList<String>();
        getVersions(hostName, portNumber, requests, replies);
        String relayVersion = "";
        String serialNumber = "";
        for (int i=0; i<replies.size(); i++) {
            System.out.println("Msg " + i + ": " + replies.get(i));
            HashMap<Integer, String> dataPointMap = new HashMap<Integer, String>();
            unpackMessage(replies.get(i), dataPointMap);
            for (Integer id : dataPointMap.keySet()) {
                System.out.println(id + " => " + dataPointMap.get(id));
            }
            if (dataPointMap.containsKey(IdRelaySoftwareVer)) {
                relayVersion = getSoftwareVersion(dataPointMap);
            }
            if (dataPointMap.containsKey(IdRelayNumber)) {
                serialNumber = getSerialNumber(dataPointMap);
            }
        }
        System.out.println("Relay serial number: " + serialNumber);
        System.out.println("Relay software version: " + relayVersion);
    }
}
