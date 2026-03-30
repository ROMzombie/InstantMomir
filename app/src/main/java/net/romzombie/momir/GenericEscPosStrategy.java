package net.romzombie.momir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default fallback strategy for generic ESC/POS thermal printers.
 *
 * Uses standard GS v 0 raster bit-image command:
 *   ESC @            — initialize printer
 *   1D 76 30 00      — GS v 0  (print raster bit image)
 *   WL WH HL HH      — width in bytes (little-endian), height in dots (little-endian)
 *   [data]            — raw monochrome pixel data
 *
 * This is the most widely supported raster format across generic
 * thermal receipt / label printers.
 */
public class GenericEscPosStrategy implements PrinterStrategy {

    @Override
    public List<byte[]> buildPrintData(byte[] monoData, int widthPx, int heightPx) throws IOException {
        int widthBytes = widthPx / 8;
        if (widthPx % 8 != 0) widthBytes++;

        List<byte[]> chunks = new ArrayList<>();

        // ESC @ — initialize printer
        chunks.add(new byte[]{0x1B, 0x40});

        // GS v 0 — standard raster bit-image header
        byte[] header = new byte[8];
        header[0] = 0x1D;  // GS
        header[1] = 0x76;  // v
        header[2] = 0x30;  // 0
        header[3] = 0x00;  // normal mode
        header[4] = (byte) (widthBytes & 0xFF);
        header[5] = (byte) ((widthBytes >> 8) & 0xFF);
        header[6] = (byte) (heightPx & 0xFF);
        header[7] = (byte) ((heightPx >> 8) & 0xFF);

        // Combine header + image data, then chunk at 1024 bytes
        byte[] fullPacket = new byte[header.length + monoData.length];
        System.arraycopy(header, 0, fullPacket, 0, header.length);
        System.arraycopy(monoData, 0, fullPacket, header.length, monoData.length);

        int i = 0;
        while (i < fullPacket.length) {
            int length = Math.min(1024, fullPacket.length - i);
            byte[] chunk = new byte[length];
            System.arraycopy(fullPacket, i, chunk, 0, length);
            chunks.add(chunk);
            i += length;
        }

        // Feed paper after print
        chunks.add(new byte[]{0x0A, 0x0A, 0x0A});

        return chunks;
    }

    @Override
    public String getName() {
        return "Generic ESC/POS";
    }
}
