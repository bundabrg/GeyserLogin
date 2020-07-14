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

package au.com.grieve.geyserlogin.ui;

import lombok.experimental.UtilityClass;
import org.geysermc.common.window.CustomFormBuilder;
import org.geysermc.common.window.CustomFormWindow;
import org.geysermc.common.window.FormWindow;
import org.geysermc.common.window.SimpleFormWindow;
import org.geysermc.common.window.button.FormButton;
import org.geysermc.common.window.component.DropdownComponent;
import org.geysermc.common.window.component.InputComponent;

import java.util.Collections;
import java.util.List;

@UtilityClass
public class LoginUI {
    public CustomFormWindow mainWindow(List<String> logins) {
        DropdownComponent dropdown = new DropdownComponent();
        dropdown.setText("Recent Logins");

        boolean first = true;
        for (String login : logins) {
            dropdown.addOption(login, first);
            first = false;
        }

        CustomFormBuilder builder = new CustomFormBuilder("Login As")
                .addComponent(dropdown)
                .addComponent(new InputComponent("Custom", "username", ""));

        return builder.build();
    }

    public FormWindow errorWindow(String message) {
        return new SimpleFormWindow("Error", message, Collections.singletonList(new FormButton("OK")));
    }

}
