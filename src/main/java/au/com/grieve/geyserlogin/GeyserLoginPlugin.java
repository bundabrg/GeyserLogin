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
import org.geysermc.connector.event.events.geyser.GeyserAuthenticationEvent;
import org.geysermc.connector.event.events.geyser.GeyserStartEvent;
import org.geysermc.connector.event.events.network.SessionConnectEvent;
import org.geysermc.connector.event.events.network.SessionDisconnectEvent;
import org.geysermc.connector.event.events.plugin.PluginDisableEvent;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.plugin.GeyserPlugin;
import org.geysermc.connector.plugin.PluginClassLoader;
import org.geysermc.connector.plugin.PluginManager;
import org.geysermc.connector.plugin.annotations.Plugin;

import java.util.HashMap;
import java.util.Map;

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

    private final Map<GeyserSession, PlayerSession> playerSessions = new HashMap<>();

    public GeyserLoginPlugin(PluginManager pluginManager, PluginClassLoader pluginClassLoader) {
        super(pluginManager, pluginClassLoader);

        instance = this;
    }

    @Event
    public void onSessionConnect(SessionConnectEvent event) {
        playerSessions.put(event.getSession(), new PlayerSession(event.getSession()));
    }

    @Event
    public void onAuthenticate(GeyserAuthenticationEvent event) {

    }

    @Event
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        playerSessions.remove(event.getSession());
    }

    @Event
    public void onDisable(PluginDisableEvent event) {
        System.err.println("I'm dead");
    }
}
