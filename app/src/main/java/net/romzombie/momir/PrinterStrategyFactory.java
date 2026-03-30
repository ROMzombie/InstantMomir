package net.romzombie.momir;

import android.util.Log;

/**
 * Factory that selects a {@link PrinterStrategy} based on the
 * Bluetooth device name reported by the connected printer.
 *
 * Add new name patterns here as additional printers are tested.
 */
public class PrinterStrategyFactory {

    private static final String TAG = "PrinterStrategyFactory";

    /**
     * Returns the appropriate strategy for the given Bluetooth device name.
     * Falls back to {@link GenericEscPosStrategy} when the name is null
     * or does not match any known printer family.
     */
    public static PrinterStrategy fromDeviceName(String deviceName) {
        if (deviceName != null) {
            String upper = deviceName.toUpperCase();

            // Xiamen Hanin / HPRT / Poooli family
            if (upper.contains("S1") || upper.contains("PRT")
                    || upper.contains("HPRT") || upper.contains("POOOLI")) {
                PrinterStrategy strategy = new HprtRasterStrategy();
                Log.i(TAG, "Matched device \"" + deviceName + "\" → " + strategy.getName());
                return strategy;
            }
        }

        // Default fallback
        PrinterStrategy strategy = new GenericEscPosStrategy();
        Log.i(TAG, "No specific match for \"" + deviceName + "\" → " + strategy.getName());
        return strategy;
    }
}
