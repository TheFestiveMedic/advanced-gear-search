package net.runelite.client.plugins.advancedgearsearch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("advancedgearsearch")
public interface AdvancedGearSearchConfig extends Config
{
    @ConfigSection(
            name = "Plugin Information",
            description = "Information on how to use the search tags",
            position = 0
    )
    String infoSection = "infoSection";

    @ConfigItem(
            keyName = "howToUse",
            name = "<html>Type these into your bank search:<br><br>" +
                    "<b>style:</b> (melee, ranged, magic)<br>" +
                    "<b>def:</b> (melee, ranged, magic)<br>" +
                    "<b>slot:</b> (head, cape, neck, weapon, body,<br>" +
                    "shield, legs, hands, boots, ring, ammo)<br><br>" +
                    "<i>Example: 'style:magic slot:neck'</i></html>",
            description = "",
            section = infoSection,
            position = 1
    )
    void howToUse(); // The void method removes the input button completely!

    @ConfigSection(
            name = "Settings",
            description = "General plugin settings",
            position = 2
    )
    String settingsSection = "settingsSection";

    @ConfigItem(
            keyName = "minStatThreshold",
            name = "Minimum Stat Threshold",
            description = "The minimum stat bonus an item needs to appear in style/def searches",
            section = settingsSection,
            position = 3
    )
    default int minStatThreshold() { return 1; }

    @ConfigItem(
            keyName = "enableAutocomplete",
            name = "Enable Search Suggestions",
            description = "Shows a popup list of available commands when typing in the bank",
            section = settingsSection,
            position = 4
    )
    default boolean enableAutocomplete() { return true; }
}