package net.romzombie.momir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PoooliProtocol {

    /**
     * Build print packets using the UNCOMPRESSED raster path.
     * This matches HPRTPrinterHelper.a([BII)[B exactly:
     *   Header: 0x1D 0x76 0x30 0x00 WL WH HL HH [raw mono data]
     * The mode byte 0x00 tells the printer firmware the payload is uncompressed.
     */
    public static List<byte[]> buildPrintPackets(byte[] monoData, int widthPx, int heightPx) throws IOException {
        int widthBytes = widthPx / 8;
        if (widthPx % 8 != 0) widthBytes++;

        byte[] fullPacket = buildUncompressedRasterPacket(widthBytes, heightPx, monoData);

        // Chunk into 1024-byte sequences for stable rfcomm transmission
        List<byte[]> chunks = new ArrayList<>();
        int i = 0;
        while (i < fullPacket.length) {
            int length = Math.min(1024, fullPacket.length - i);
            byte[] chunk = new byte[length];
            System.arraycopy(fullPacket, i, chunk, 0, length);
            chunks.add(chunk);
            i += length;
        }

        return chunks;
    }

    /**
     * Builds the uncompressed raster packet matching HPRTPrinterHelper.a([BII)[B:
     *   Byte 0: 0x1D (GS)
     *   Byte 1: 0x76 (v)
     *   Byte 2: 0x30 (raster mode)
     *   Byte 3: 0x00 (uncompressed flag)
     *   Byte 4: widthBytes & 0xFF
     *   Byte 5: (widthBytes >> 8) & 0xFF
     *   Byte 6: height & 0xFF  
     *   Byte 7: (height >> 8) & 0xFF
     *   Byte 8+: raw monochrome pixel data
     */
    private static byte[] buildUncompressedRasterPacket(int widthBytes, int heightPx, byte[] monoData) throws IOException {
        byte[] packet = new byte[8 + monoData.length];

        packet[0] = 0x1D;  // GS
        packet[1] = 0x76;  // v
        packet[2] = 0x30;  // raster mode
        packet[3] = 0x00;  // uncompressed

        packet[4] = (byte) (widthBytes % 256);
        packet[5] = (byte) (widthBytes / 256);
        packet[6] = (byte) (heightPx % 256);
        packet[7] = (byte) (heightPx / 256);

        System.arraycopy(monoData, 0, packet, 8, monoData.length);

        return packet;
    }
}
