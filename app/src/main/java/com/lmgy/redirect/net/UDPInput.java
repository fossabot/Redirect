package com.lmgy.redirect.net;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/*
 * Created by lmgy on 14/8/2019
 */
public class UDPInput implements Runnable {
    private static final String TAG = "UDPInput";

    private Selector selector;
    private ConcurrentLinkedQueue<ByteBuffer> outputQueue;

    public UDPInput(ConcurrentLinkedQueue<ByteBuffer> outputQueue, Selector selector)
    {
        this.outputQueue = outputQueue;
        this.selector = selector;
    }

    @Override
    public void run()
    {
        try
        {
            Log.i(TAG, "Started");
            while (!Thread.interrupted())
            {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    Thread.sleep(11);
                    continue;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();

                while (keyIterator.hasNext() && !Thread.interrupted())
                {
                    SelectionKey key = keyIterator.next();
                    if (key.isValid() && key.isReadable())
                    {
                        keyIterator.remove();

                        ByteBuffer receiveBuffer = ByteBufferPool.acquire();
                        // Leave space for the header


                        DatagramChannel inputChannel = (DatagramChannel) key.channel();
                        Packet referencePacket = (Packet) key.attachment();
                        receiveBuffer.position(referencePacket.IP_TRAN_SIZE);
                        int readBytes=0;
                        try {
                            readBytes = inputChannel.read(receiveBuffer);
                        }catch (Exception e){
                            Log.e(TAG, "Network read error", e);
                        }
                        referencePacket.updateUDPBuffer(receiveBuffer, readBytes);
                        receiveBuffer.position(referencePacket.IP_TRAN_SIZE+ readBytes);
                        outputQueue.offer(receiveBuffer);

                    }
                }
            }
        }
        catch (InterruptedException e)
        {
            Log.i(TAG, "Stopping");
        }
        catch (IOException e)
        {
            Log.w(TAG, e.toString(), e);
        }
    }
}
