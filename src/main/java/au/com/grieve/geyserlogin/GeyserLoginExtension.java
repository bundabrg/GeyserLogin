/*
 * GeyserLogin - Log in as a different username to Geyser
 * Copyright (C) 2021 GeyserLogin Developers
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
import com.nukkitx.protocol.bedrock.packet.ModalFormRequestPacket;
import com.nukkitx.protocol.bedrock.packet.ModalFormResponsePacket;
import com.nukkitx.protocol.bedrock.packet.NetworkStackLatencyPacket;
import lombok.Getter;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.geyser.event.annotations.GeyserEventHandler;
import org.geysermc.geyser.event.events.geyser.GeyserLoginEvent;
import org.geysermc.geyser.event.events.network.SessionConnectEvent;
import org.geysermc.geyser.event.events.network.SessionDisconnectEvent;
import org.geysermc.geyser.event.events.packet.upstream.ModalFormResponsePacketReceive;
import org.geysermc.geyser.event.events.packet.upstream.SetLocalPlayerAsInitializedPacketReceive;
import org.geysermc.geyser.extension.ExtensionClassLoader;
import org.geysermc.geyser.extension.ExtensionManager;
import org.geysermc.geyser.extension.GeyserExtension;
import org.geysermc.geyser.extension.annotations.Extension;
import org.geysermc.geyser.session.GeyserSession;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Extension(
        name = "GeyserLogin",
        version = "1.1.0-dev",
        authors = {"Bundabrg"},
        description = "Provides login features for Geyser"
)
@Getter
public class GeyserLoginExtension extends GeyserExtension {
    public static final int FORM_ID = 11986700; // Base FORM we are interested in. We reserve 100 addresses
    public static final int WINDOW_MAIN = 0;
    public static final int WINDOW_ERROR = 99;

    @Getter
    public static GeyserLoginExtension instance;

    private final Map<GeyserSession, PlayerSession> playerSessions = new HashMap<>();
    private final Db db;


    public GeyserLoginExtension(ExtensionManager extensionManager, ExtensionClassLoader extensionClassLoader) {
        super(extensionManager, extensionClassLoader);

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
        event.setCancelled(true);

        // Create a connection first and wait till player is initialized
        event.getSession().connect();
    }

    @GeyserEventHandler
    public void onPlayerInitialized(SetLocalPlayerAsInitializedPacketReceive event) {
        PlayerSession playerSession = playerSessions.get(event.getSession());

        if (playerSession == null) {
            return;
        }

        showLoginWindow(playerSession);
    }

    @GeyserEventHandler
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        playerSessions.remove(event.getSession());
    }

    void showLoginWindow(PlayerSession playerSession) {
        UUID uuid = playerSession.getSession().getAuthData().uuid();
        List<String> logins = db.getRecentLogins(uuid);

        // Always add the users own login name
        if (!logins.contains(playerSession.getSession().getAuthData().name())) {
            logins.add(playerSession.getSession().getAuthData().name());
        }

        boolean showPosition = db.getSetting(uuid, "show-position", "true").equals("true");

        Form window = LoginUI.mainWindow(logins, showPosition);

        on(ModalFormResponsePacketReceive.class, (event, handler) -> {
            ModalFormResponsePacket packet = event.getPacket();
            int formId = packet.getFormId() - FORM_ID;
            if (formId < 0 || formId > 99) {
                return;
            }

            switch (formId) {
                case WINDOW_MAIN:
                    event.setCancelled(true);
                    if (packet.getFormData().replaceAll("^[ \t]+|[ \t]+$", "").equals("null")) {
                        event.getSession().disconnect("Cancelled as requested");
                        return;
                    }

                    CustomFormResponse response = (CustomFormResponse) window.parseResponse(packet.getFormData());

                    if (!response.isCorrect()) {
                        event.getSession().disconnect("Invalid Data");
                        return;
                    }

                    String login = response.getInput(1).equals("") ?
                            logins.get(response.getDropdown(0)) : response.getInput(1);

                    // Make sure its valid
                    if (login.length() < 3) {
                        sendForm(playerSession.getSession(), LoginUI.errorWindow("Username too short"), FORM_ID + WINDOW_ERROR);
                        return;
                    }

                    if (login.length() > 16) {
                        sendForm(playerSession.getSession(), LoginUI.errorWindow("Username too long"), FORM_ID + WINDOW_ERROR);
                        return;
                    }

                    if (!Pattern.matches("[a-zA-Z0-9_]+", login)) {
                        sendForm(playerSession.getSession(), LoginUI.errorWindow("Invalid username"), FORM_ID + WINDOW_ERROR);
                        return;
                    }

                    Boolean isShowPosition = response.getToggle(2);

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

                    sendForm(playerSession.getSession(), window, FORM_ID + WINDOW_MAIN);
                    break;
            }

        });

        sendForm(playerSession.getSession(), window, FORM_ID + WINDOW_MAIN);
    }

    public void sendForm(GeyserSession session, Form form, int formId) {
        ModalFormRequestPacket modalFormRequestPacket = new ModalFormRequestPacket();
        modalFormRequestPacket.setFormId(formId);
        modalFormRequestPacket.setFormData(form.getJsonData());
        session.sendUpstreamPacket(modalFormRequestPacket);

        // This packet is used to fix the image loading bug
        NetworkStackLatencyPacket networkStackLatencyPacket = new NetworkStackLatencyPacket();
        networkStackLatencyPacket.setFromServer(true);
        networkStackLatencyPacket.setTimestamp(System.currentTimeMillis());
        session.scheduleInEventLoop(() -> session.sendUpstreamPacket(networkStackLatencyPacket),
                500, TimeUnit.MILLISECONDS);
    }

}
