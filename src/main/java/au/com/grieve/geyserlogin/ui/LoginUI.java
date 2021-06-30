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

package au.com.grieve.geyserlogin.ui;

import lombok.experimental.UtilityClass;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.component.ButtonComponent;
import org.geysermc.cumulus.component.DropdownComponent;
import org.geysermc.cumulus.component.InputComponent;
import org.geysermc.cumulus.component.ToggleComponent;

import java.util.Collections;
import java.util.List;

@UtilityClass
public class LoginUI {
    public CustomForm mainWindow(List<String> logins, boolean showPosition) {
        DropdownComponent.Builder dropdownBuilder = DropdownComponent.builder()
                .text("Recent Logins");

        boolean first = true;
        for (String login : logins) {
            dropdownBuilder.option(login, first);
            first = false;
        }

        return CustomForm.builder()
                .title("Login as")
                .component(dropdownBuilder.build())
                .component(InputComponent.of("Custom", "username", ""))
                .component(ToggleComponent.of("Show Position", showPosition))
                .build();
    }

    public Form errorWindow(String message) {
        return SimpleForm.of("Error", message, Collections.singletonList(ButtonComponent.of("OK")));
    }

}
