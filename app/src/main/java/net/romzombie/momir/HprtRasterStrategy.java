package net.romzombie.momir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Printer strategy for HPRT / Poooli / Xiamen Hanin thermal printers.
 *
 * Uses the UNCOMPRESSED raster path (GS v 0, mode 0x00):
 *   Header: 0x1D 0x76 0x30 0x00  WL WH  HL HH  [raw mono data]
 *
 * Packets are chunked to 1024 bytes for stable RFCOMM transmission.
 */
public class HprtRasterStrategy implements PrinterStrategy {

    @Override
    public List<byte[]> buildPrintData(byte[] monoData, int widthPx, int heightPx) throws IOException {
        int widthBytes = widthPx / 8;
        if (widthPx % 8 != 0) widthBytes++;

        byte[] fullPacket = buildUncompressedRasterPacket(widthBytes, heightPx, monoData);

        // Chunk into 1024-byte sequences for stable RFCOMM transmission
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

    @Override
    public String getName() {
        return "HPRT Raster";
    }

    /**
     * Builds the uncompressed raster packet:
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
    private byte[] buildUncompressedRasterPacket(int widthBytes, int heightPx, byte[] monoData) {
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
