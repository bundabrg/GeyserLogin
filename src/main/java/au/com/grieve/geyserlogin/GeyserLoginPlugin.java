/*
 * GeyserLogin - Log in as a different username to Geyser
 * Copyright (C) 2020 GeyserLogin Developers
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

import au.com.grieve.geyserlogin.ui.LoginUI;
import com.nukkitx.protocol.bedrock.data.GameRuleData;
import com.nukkitx.protocol.bedrock.packet.GameRulesChangedPacket;
import com.nukkitx.protocol.bedrock.packet.ModalFormResponsePacket;
import lombok.Getter;
import org.geysermc.common.window.CustomFormWindow;
import org.geysermc.common.window.response.CustomFormResponse;
import org.geysermc.connector.event.annotations.GeyserEventHandler;
import org.geysermc.connector.event.events.geyser.GeyserLoginEvent;
import org.geysermc.connector.event.events.network.SessionConnectEvent;
import org.geysermc.connector.event.events.network.SessionDisconnectEvent;
import org.geysermc.connector.event.events.packet.upstream.ModalFormResponsePacketReceive;
import org.geysermc.connector.network.session.GeyserSession;
import org.geysermc.connector.plugin.GeyserPlugin;
import org.geysermc.connector.plugin.PluginClassLoader;
import org.geysermc.connector.plugin.PluginManager;
import org.geysermc.connector.plugin.annotations.Plugin;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Plugin(
        name = "GeyserLogin",
        version = "1.1.0-dev",
        authors = {"Bundabrg"},
        description = "Provides login features for Geyser"
)
@Getter
public class GeyserLoginPlugin extends GeyserPlugin {
    public static final int FORM_ID = 11986700; // Base FORM we are interested in. We reserve 100 addresses
    public static final int WINDOW_MAIN = 0;
    public static final int WINDOW_ERROR = 99;

    @Getter
    public static GeyserLoginPlugin instance;

    private final Map<GeyserSession, PlayerSession> playerSessions = new HashMap<>();
    private final Db db;


    public GeyserLoginPlugin(PluginManager pluginManager, PluginClassLoader pluginClassLoader) {
        super(pluginManager, pluginClassLoader);

        Db db;
        try {
            db = new Db(new File(getDataFolder(), "data.db"));
        } catch (SQLException throwables) {
            getLogger().error("Unable to connect to SQLite Db: db.sqlite");
            db = null;
        }
        this.db = db;

        instance = this;
    }

    @GeyserEventHandler
    public void onSessionConnect(SessionConnectEvent event) {
        playerSessions.put(event.getSession(), new PlayerSession(event.getSession()));
    }

    @GeyserEventHandler
    public void onLogin(GeyserLoginEvent event) {
        PlayerSession playerSession = playerSessions.get(event.getSession());

        if (playerSession == null) {
            return;
        }

        event.setCancelled(true);
        showLoginWindow(playerSession);
    }

    @GeyserEventHandler
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        playerSessions.remove(event.getSession());
    }

    void showLoginWindow(PlayerSession playerSession) {
        UUID uuid = playerSession.getSession().getAuthData().getUUID();
        List<String> logins = db.getRecentLogins(uuid);

        // Always add the users own login name
        if (!logins.contains(playerSession.getSession().getAuthData().getName())) {
            logins.add(playerSession.getSession().getAuthData().getName());
        }

        boolean showPosition = db.getSetting(uuid, "show-position", "true").equals("true");

        CustomFormWindow window = LoginUI.mainWindow(logins, showPosition);

        on(ModalFormResponsePacketReceive.class, (event, handler) -> {
            ModalFormResponsePacket packet = event.getPacket();
            int formId = packet.getFormId() - FORM_ID;
            if (formId < 0 || formId > 99) {
                return;
            }

            switch (formId) {
                case WINDOW_MAIN:
                    event.setCancelled(true);
                    if (packet.getFormData().strip().equals("null")) {
                        event.getSession().disconnect("Cancelled as requested");
                        return;
                    }

                    window.setResponse(packet.getFormData());
                    CustomFormResponse response = (CustomFormResponse) window.getResponse();

                    String login = response.getInputResponses().get(1).equals("") ?
                            response.getDropdownResponses().get(0).getElementContent() : response.getInputResponses().get(1);

                    // Make sure its valid
                    if (login.length() < 3) {
                        playerSession.getSession().sendForm(LoginUI.errorWindow("Username too short"), FORM_ID + WINDOW_ERROR);
                        return;
                    }

                    if (login.length() > 16) {
                        playerSession.getSession().sendForm(LoginUI.errorWindow("Username too long"), FORM_ID + WINDOW_ERROR);
                        return;
                    }

                    if (!Pattern.matches("[a-zA-Z0-9_]+", login)) {
                        playerSession.getSession().sendForm(LoginUI.errorWindow("Invalid username"), FORM_ID + WINDOW_ERROR);
                        return;
                    }

                    Boolean isShowPosition = response.getToggleResponses().getOrDefault(2, true);

                    handler.unregister();

                    // Add to database
                    db.addLogin(uuid, login);
                    db.setSetting(uuid, "show-position", isShowPosition ? "true" : "false");

                    // Update GameRule
                    GameRulesChangedPacket gameRulesChangedPacket = new GameRulesChangedPacket();
                    gameRulesChangedPacket.getGameRules().add(new GameRuleData<>("showcoordinates", isShowPosition));
                    playerSession.getSession().sendUpstreamPacket(gameRulesChangedPacket);

                    // Authenticate
                    playerSession.getSession().authenticate(login);
                    break;
                case WINDOW_ERROR:
                    event.setCancelled(true);

                    playerSession.getSession().sendForm(window, FORM_ID + WINDOW_MAIN);
                    break;
            }

        }).build();

        playerSession.getSession().sendForm(window, FORM_ID + WINDOW_MAIN);
    }

}
