package net.romzombie.momir;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class PrinterStrategyFactoryTest {

    @Test
    public void testFromDeviceName_HprtPrinters() {
        // Test various names that should resolve to HprtRasterStrategy
        assertTrue(PrinterStrategyFactory.fromDeviceName("PRT-001") instanceof HprtRasterStrategy);
        assertTrue(PrinterStrategyFactory.fromDeviceName("Poooli_L3") instanceof HprtRasterStrategy);
        assertTrue(PrinterStrategyFactory.fromDeviceName("S1 Printer") instanceof HprtRasterStrategy);
        assertTrue(PrinterStrategyFactory.fromDeviceName("HPRT_Q1") instanceof HprtRasterStrategy);
    }

    @Test
    public void testFromDeviceName_GenericPrinters() {
        // Test names that should fall back to GenericEscPosStrategy
        assertTrue(PrinterStrategyFactory.fromDeviceName("Generic Printer") instanceof GenericEscPosStrategy);
        assertTrue(PrinterStrategyFactory.fromDeviceName("UnknownDevice") instanceof GenericEscPosStrategy);
        assertTrue(PrinterStrategyFactory.fromDeviceName("") instanceof GenericEscPosStrategy);
    }

    @Test
    public void testFromDeviceName_Null() {
        // Test null input
        assertTrue(PrinterStrategyFactory.fromDeviceName(null) instanceof GenericEscPosStrategy);
    }
}
