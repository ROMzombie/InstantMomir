package net.romzombie.momir;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TextFormatStrategyTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
    }

    @Test
    public void testFormatReturnsValidBytes() throws Exception {
        TextFormatStrategy strategy = new TextFormatStrategy();
        
        JSONObject cardData = new JSONObject();
        cardData.put("name", "Test Card");
        cardData.put("mana_cost", "{2}{G}");
        cardData.put("type_line", "Creature - Human");
        cardData.put("oracle_text", "When this enters the battlefield, you win the game.");
        cardData.put("power", "2");
        cardData.put("toughness", "2");

        byte[] result = strategy.format(context, cardData);
        
        // Output format strategy should return a non-null byte array representing monochrome bitmap
        assertNotNull(result);
        assertTrue(result.length > 0);
        assertTrue(strategy.getWidth() > 0);
        assertTrue(strategy.getHeight() > 0);
    }
}
