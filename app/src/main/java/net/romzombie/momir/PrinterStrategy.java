package net.romzombie.momir;

import java.io.IOException;
import java.util.List;

/**
 * Strategy interface for printer communication protocols.
 * Each implementation encodes monochrome bitmap data into the
 * byte-level packet format expected by a specific printer family.
 */
public interface PrinterStrategy {

    /**
     * Convert raw 1-bit monochrome pixel data into printer-ready packets.
     *
     * @param monoData  packed 1-bit-per-pixel data (MSB first, row-major)
     * @param widthPx   image width in pixels (must be a multiple of 8)
     * @param heightPx  image height in pixels
     * @return ordered list of byte[] chunks to write to the OutputStream
     */
    List<byte[]> buildPrintData(byte[] monoData, int widthPx, int heightPx) throws IOException;

    /**
     * Human-readable name for logging / toasts.
     */
    String getName();
}
