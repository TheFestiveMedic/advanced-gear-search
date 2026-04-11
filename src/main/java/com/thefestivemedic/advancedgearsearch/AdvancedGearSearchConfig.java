package com.thefestivemedic.advancedgearsearch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("advancedgearsearch")
public interface AdvancedGearSearchConfig extends Config
{
    @ConfigItem(
            keyName = "showButtons",
            name = "Show Quick-Search Buttons",
            description = "Display Melee/Range/Mage buttons in the bank interface"
    )
    default boolean showButtons()
    {
        return true;
    }
}