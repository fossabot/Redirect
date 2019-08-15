package com.lmgy.redirect.utils;

import android.util.Log;

import com.lmgy.redirect.bean.HostData;
import com.lmgy.redirect.net.Packet;
import org.xbill.DNS.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Created by lmgy on 15/8/2019
 */
public class DnsUtils {
    private static final String TAG = "DnsUtils";
    private static ConcurrentHashMap<String, String> DOMAINS_IP_MAPS4 = null;
    private static ConcurrentHashMap<String, String> DOMAINS_IP_MAPS6 = null;


    public static ByteBuffer handle_dns_packet(Packet packet) {
        if (DOMAINS_IP_MAPS4 == null) {
            Log.d(TAG, "DOMAINS_IP_MAPS IS　NULL　HOST FILE ERROR");
            return null;
        }
        try {
            ByteBuffer packet_buffer = packet.backingBuffer;
            packet_buffer.mark();
            byte[] tmp_bytes = new byte[packet_buffer.remaining()];
            packet_buffer.get(tmp_bytes);
            packet_buffer.reset();
            Message message = new Message(tmp_bytes);
            Record question = message.getQuestion();
            ConcurrentHashMap<String, String> DOMAINS_IP_MAPS;
            int type = question.getType();
            if (type == Type.A)
                DOMAINS_IP_MAPS = DOMAINS_IP_MAPS4;
            else if (type == Type.AAAA)
                DOMAINS_IP_MAPS = DOMAINS_IP_MAPS6;
            else return null;
            Name query_domain = message.getQuestion().getName();
            String query_string = query_domain.toString();
            Log.d(TAG, "query: " + question.getType() + " :" + query_string);
            if (!DOMAINS_IP_MAPS.containsKey(query_string)) {
                query_string = "." + query_string;
                int j = 0;
                while (true) {
                    int i = query_string.indexOf(".", j);
                    if (i == -1) {
                        return null;
                    }
                    String str = query_string.substring(i);

                    if (".".equals(str) || "".equals(str)) {
                        return null;
                    }
                    if (DOMAINS_IP_MAPS.containsKey(str)) {
                        query_string = str;
                        break;
                    }
                    j = i + 1;
                }
            }
            InetAddress address = Address.getByAddress(DOMAINS_IP_MAPS.get(query_string));
            Record record;
            if (type == Type.A) record = new ARecord(query_domain, 1, 86400, address);
            else record = new AAAARecord(query_domain, 1, 86400, address);
            message.addRecord(record, 1);
            message.getHeader().setFlag(Flags.QR);
            packet_buffer.limit(packet_buffer.capacity());
            packet_buffer.put(message.toWire());
            packet_buffer.limit(packet_buffer.position());
            packet_buffer.reset();
            packet.swapSourceAndDestination();
            packet.updateUDPBuffer(packet_buffer, packet_buffer.remaining());
            packet_buffer.position(packet_buffer.limit());
            Log.d(TAG, "hit: " + question.getType() + " :" + query_domain.toString() + " :" + address.getHostName());
            return packet_buffer;
        } catch (Exception e) {
            Log.d(TAG, "dns hook error", e);
            return null;
        }

    }

    public static int handle_hosts(List<HostData> savedHostDataList) {
        try {
            Iterator<HostData> savedHostDataIterator = savedHostDataList.iterator();
            HostData savedHostData;

            DOMAINS_IP_MAPS4 = new ConcurrentHashMap<>();
            DOMAINS_IP_MAPS6 = new ConcurrentHashMap<>();

            while (!Thread.interrupted() && savedHostDataIterator.hasNext()) {
                savedHostData = savedHostDataIterator.next();
                if (!savedHostData.getType()) continue;
                String ip = savedHostData.getIpAddress().trim();
                String hostName = savedHostData.getHostName().trim();
                Log.e(TAG, "handle_hosts: " + ip + " - " + hostName);
                try {
                    Address.getByAddress(ip);
                } catch (Exception e) {
                    continue;
                }
                if (ip.contains(":")) {
                    DOMAINS_IP_MAPS6.put(hostName + ".", ip);
                } else {
                    DOMAINS_IP_MAPS4.put(hostName + ".", ip);
                }

            }
            Log.d(TAG, DOMAINS_IP_MAPS4.toString());
            Log.d(TAG, DOMAINS_IP_MAPS6.toString());
            return DOMAINS_IP_MAPS4.size() + DOMAINS_IP_MAPS6.size();
        } catch (Exception e) {
            Log.d(TAG, "Hook dns error", e);
            return 0;
        }
    }
}
