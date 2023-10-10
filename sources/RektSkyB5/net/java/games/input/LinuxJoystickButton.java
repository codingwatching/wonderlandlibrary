/*
 * Decompiled with CFR 0.152.
 */
package net.java.games.input;

import java.io.IOException;
import net.java.games.input.AbstractComponent;
import net.java.games.input.Component;

final class LinuxJoystickButton
extends AbstractComponent {
    private float value;

    public LinuxJoystickButton(Component.Identifier button_id) {
        super(button_id.getName(), button_id);
    }

    @Override
    public final boolean isRelative() {
        return false;
    }

    final void setValue(float value) {
        this.value = value;
    }

    @Override
    protected final float poll() throws IOException {
        return this.value;
    }
}

