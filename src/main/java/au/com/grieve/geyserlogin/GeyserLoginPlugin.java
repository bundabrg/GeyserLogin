/*
 * EduSupport - Minecraft Educational Support for Geyser
 * Copyright (C) 2020 EduSupport Developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.grieve.geyserlogin;

import lombok.Getter;
import org.geysermc.connector.event.annotations.Event;
import org.geysermc.connector.event.events.geyser.GeyserStartEvent;
import org.geysermc.connector.event.events.plugin.PluginDisableEvent;
import org.geysermc.connector.plugin.GeyserPlugin;
import org.geysermc.connector.plugin.PluginClassLoader;
import org.geysermc.connector.plugin.PluginManager;
import org.geysermc.connector.plugin.annotations.Plugin;

@Plugin(
        name = "GeyserLogin",
        version = "1.1.0-dev",
        authors = {"Bundabrg"},
        description = "Provides login features for Geyser"
)
@Getter
public class GeyserLoginPlugin extends GeyserPlugin {
    @Getter
    public static GeyserLoginPlugin instance;

    public GeyserLoginPlugin(PluginManager pluginManager, PluginClassLoader pluginClassLoader) {
        super(pluginManager, pluginClassLoader);

        instance = this;
//        registerEvents(new UpstreamPackets());
    }

    @Event
    public void onGeyserStart(GeyserStartEvent event) {
        // Register Education command
//        getConnector().getBootstrap().getGeyserCommandManager().registerCommand(new EducationCommand(getConnector(), "education", "Education Commands", "geyser.command.education", tokenManager));
    }

    @Event
    public void onDisable(PluginDisableEvent event) {
        System.err.println("I'm dead");
    }
}
