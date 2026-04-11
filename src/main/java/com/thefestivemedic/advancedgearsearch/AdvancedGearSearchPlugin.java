package com.thefestivemedic.advancedgearsearch;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID; // This is what the Hub wants
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
        name = "Advanced Gear Search",
        description = "Adds power-user filters to the bank search bar",
        tags = {"bank", "search", "gear", "stats", "filter"}
)
@SuppressWarnings("unused") // Stops IntelliJ from complaining about "never used"
public class AdvancedGearSearchPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private AdvancedGearSearchConfig config;

    @Override
    protected void startUp()
    {
        log.info("Advanced Gear Search started!");
    }

    @Override
    protected void shutDown()
    {
        clientThread.invokeLater(this::removeWidgets);
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        // Fixed: Using the correct constant name BANK_CONTAINER
        Widget bankContainer = client.getWidget(ComponentID.BANK_CONTAINER);

        if (bankContainer == null || bankContainer.isHidden())
        {
            return;
        }

        updateSearchButtons(bankContainer);
    }

    private void updateSearchButtons(Widget bank)
    {
        // Fixed: Using BANK_TITLE_BAR and removed empty if-body
        Widget titleBar = client.getWidget(ComponentID.BANK_TITLE_BAR);
        if (titleBar != null)
        {
            log.debug("Found bank title bar: {}", titleBar.getId());
            // This is where your custom button logic will live
        }
    }

    private void removeWidgets()
    {
        // Cleanup code for later
        log.debug("Cleaning up injected widgets...");
    }

    @Provides
    AdvancedGearSearchConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AdvancedGearSearchConfig.class);
    }
}