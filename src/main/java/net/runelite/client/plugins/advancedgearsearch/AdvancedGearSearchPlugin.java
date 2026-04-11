package net.runelite.client.plugins.advancedgearsearch;
import java.awt.event.KeyEvent;
import java.awt.Component;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.VarClientStr;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
// if youre reading this im sorry i have no idea what im doing <3
@PluginDescriptor(
        name = "Advanced Gear Search",
        description = "Allows searching bank by combat style, defense, or slot",
        tags = {"bank", "search", "equipment", "stats", "filter", "slot"}
)
public class AdvancedGearSearchPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ItemManager itemManager;

    @Inject
    private AdvancedGearSearchConfig config;

    private String lastSearchText = "";
    private String pendingVisualUpdate = null; // Forces the UI to stay in sync
    private final List<Widget> injectedWidgets = new ArrayList<>();

    @Provides
    AdvancedGearSearchConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AdvancedGearSearchConfig.class);
    }

    @Override
    protected void shutDown()
    {
        clientThread.invokeLater(this::clearInjectedWidgets);
    }

    private void clearInjectedWidgets()
    {
        for (Widget w : injectedWidgets)
        {
            if (w != null) w.setHidden(true);
        }
        injectedWidgets.clear();
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        if (!config.enableAutocomplete()) return;

        Widget chatbox = client.getWidget(WidgetInfo.CHATBOX_CONTAINER);
        Widget bankContainer = client.getWidget(WidgetInfo.BANK_CONTAINER);

        if (chatbox == null || chatbox.isHidden() || bankContainer == null || bankContainer.isHidden())
        {
            clearInjectedWidgets();
            pendingVisualUpdate = null;
            return;
        }

        String rawInput = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
        String currentText = (rawInput != null) ? rawInput.replace("*", "").toLowerCase().trim() : "";

        // --- FORCE SYNC ---
        // If we just clicked a button, force the chatbox widget to show our new text
        if (pendingVisualUpdate != null)
        {
            Widget chatPrompt = client.getWidget(162, 33);
            if (chatPrompt != null)
            {
                String pText = chatPrompt.getText();
                if (pText != null && pText.contains(":"))
                {
                    String base = pText.split(":")[0];
                    chatPrompt.setText(base + ": " + pendingVisualUpdate + "*");
                }
            }
            // Once the internal game state matches the clicked text, we can stop forcing it
            if (currentText.equals(pendingVisualUpdate.trim()))
            {
                pendingVisualUpdate = null;
            }
        }

        if (currentText.equals(lastSearchText)) return;
        lastSearchText = currentText;

        buildSuggestionWidgets(currentText, chatbox);
    }

    private void buildSuggestionWidgets(String currentText, Widget chatbox)
    {
        clearInjectedWidgets();

        if (currentText.isEmpty()) return;

        String cleanInput = currentText.replace("*", "");
        String[] tokens = cleanInput.split("\\s+");
        String currentWord = (tokens.length > 0) ? tokens[tokens.length - 1] : "";

        List<String> optionsToShow = new ArrayList<>();
        String prefix = "";

        if (!currentWord.contains(":"))
        {
            String[] categories = {"style:", "def:", "slot:"};
            for (String cat : categories)
            {
                if (cat.startsWith(currentWord)) optionsToShow.add(cat);
            }
        }
        else
        {
            String[] parts = currentWord.split(":", 2);
            prefix = parts[0] + ":";
            String suffix = parts.length > 1 ? parts[1] : "";

            String[] possibleOptions = new String[0];
            if (prefix.equals("style:") || prefix.equals("def:"))
            {
                possibleOptions = new String[]{"melee", "ranged", "magic"};
            }
            else if (prefix.equals("slot:"))
            {
                possibleOptions = new String[]{"head", "cape", "neck", "weapon", "body", "shield", "legs", "hands", "boots", "ring", "ammo"};
            }

            for (String opt : possibleOptions)
            {
                if (opt.startsWith(suffix)) optionsToShow.add(opt);
            }
        }

        if (optionsToShow.isEmpty() || (optionsToShow.size() == 1 && (prefix + optionsToShow.get(0)).equals(currentWord)))
        {
            return;
        }

        // --- POSITIONING ---
        int totalWidth = 0;
        int gapBetweenWords = 25;
        int charWidth = 10;

        for (String option : optionsToShow)
        {
            totalWidth += (option.length() * charWidth);
        }
        totalWidth += (optionsToShow.size() - 1) * gapBetweenWords;

        // Put the horizontal offset at +9
        int xOffset = (chatbox.getWidth() / 2) - (totalWidth / 2) + 9;
        int yOffset = 105;

        for (String option : optionsToShow)
        {
            Widget textWidget = chatbox.createChild(-1, 4);
            textWidget.setText(option);
            textWidget.setTextColor(0x00FFFF);
            textWidget.setTextShadowed(true);
            textWidget.setFontId(496);

            int expectedWidth = option.length() * charWidth + 12;
            textWidget.setOriginalX(xOffset);
            textWidget.setOriginalY(yOffset);
            textWidget.setOriginalWidth(expectedWidth);
            textWidget.setOriginalHeight(20);

            final String finalFullCommand = prefix + option;

            textWidget.setHasListener(true);
            textWidget.setNoClickThrough(true);
            textWidget.setAction(0, "Select");

            textWidget.setOnOpListener((JavaScriptCallback) ev -> {
                clientThread.invokeLater(() -> {
                    String input = client.getVarcStrValue(VarClientStr.INPUT_TEXT);
                    if (input != null)
                    {
                        String clean = input.replace("*", "");
                        int lastSpace = clean.lastIndexOf(" ");

                        String newText = (lastSpace == -1) ? finalFullCommand : clean.substring(0, lastSpace) + " " + finalFullCommand;

                        if (!finalFullCommand.endsWith(":")) {
                            newText += " ";
                        }

                        // 1. Update the internal search string
                        client.setVarcStrValue(VarClientStr.INPUT_TEXT, newText);

                        // 2. simulate TAB keypress to update the typed text
                        Component canvas = client.getCanvas();
                        if (canvas != null)
                        {
                            KeyEvent press = new KeyEvent(canvas, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED);
                            canvas.dispatchEvent(press);

                            KeyEvent release = new KeyEvent(canvas, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_TAB, KeyEvent.CHAR_UNDEFINED);
                            canvas.dispatchEvent(release);
                        }

                        // 3. Reset cache to force immediate rebuild for next step
                        lastSearchText = "";
                        clearInjectedWidgets();
                    }
                });
            });

            textWidget.revalidate();
            injectedWidgets.add(textWidget);
            xOffset += expectedWidth + gapBetweenWords;
        }
    }
    // --- BANK FILTER LOGIC (UNTOUCHED) ---

    @Subscribe
    @SuppressWarnings("deprecation")
    public void onScriptCallbackEvent(ScriptCallbackEvent event)
    {
        if (!"bankSearchFilter".equals(event.getEventName())) return;
        String searchInput = client.getVarcStrValue(VarClientStr.INPUT_TEXT).toLowerCase();
        String[] searchTokens = searchInput.split("\\s+");
        boolean hasCustomCommand = false;
        for (String token : searchTokens) { if (token.startsWith("style:") || token.startsWith("def:") || token.startsWith("slot:")) { hasCustomCommand = true; break; } }
        if (!hasCustomCommand) return;
        int[] intStack = client.getIntStack();
        int intStackSize = client.getIntStackSize();
        int itemId = intStack[intStackSize - 1];
        ItemStats itemStats = itemManager.getItemStats(itemId);
        boolean match = true;
        if (itemStats != null && itemStats.getEquipment() != null) {
            ItemEquipmentStats equipment = itemStats.getEquipment();
            int threshold = config.minStatThreshold();
            for (String token : searchTokens) {
                if (token.startsWith("style:")) {
                    String style = token.replace("style:", "").trim(); boolean styleMatch = false;
                    switch (style) {
                        case "ranged": styleMatch = equipment.getArange() >= threshold; break;
                        case "magic":  styleMatch = equipment.getAmagic() >= threshold; break;
                        case "melee":  styleMatch = equipment.getAstab() >= threshold || equipment.getAslash() >= threshold || equipment.getAcrush() >= threshold || equipment.getStr() >= threshold; break;
                    }
                    if (!styleMatch) match = false;
                } else if (token.startsWith("def:")) {
                    String def = token.replace("def:", "").trim(); boolean defMatch = false;
                    switch (def) {
                        case "ranged": defMatch = equipment.getDrange() >= threshold; break;
                        case "magic":  defMatch = equipment.getDmagic() >= threshold; break;
                        case "melee":  defMatch = equipment.getDstab() >= threshold || equipment.getDslash() >= threshold || equipment.getDcrush() >= threshold; break;
                    }
                    if (!defMatch) match = false;
                } else if (token.startsWith("slot:")) {
                    String slot = token.replace("slot:", "").trim();
                    int targetSlot = getSlotId(slot);
                    if (equipment.getSlot() != targetSlot) match = false;
                }
            }
        } else { match = false; }
        if (match) intStack[intStackSize - 2] = 1; else intStack[intStackSize - 2] = 0;
    }

    private int getSlotId(String slotName) {
        switch (slotName) {
            case "head": case "hat": case "helmet": return 0;
            case "cape": case "back": return 1;
            case "neck": case "amulet": return 2;
            case "weapon": case "hand": return 3;
            case "body": case "torso": case "chest": return 4;
            case "shield": case "offhand": return 5;
            case "legs": case "bottoms": return 7;
            case "hands": case "gloves": return 9;
            case "feet": case "boots": return 10;
            case "ring": return 12;
            case "ammo": case "arrow": return 13;
            default: return -1;
        }
    }
}