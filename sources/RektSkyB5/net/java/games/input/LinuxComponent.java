/*
 * Decompiled with CFR 0.152.
 */
package net.java.games.input;

import java.io.IOException;
import net.java.games.input.AbstractComponent;
import net.java.games.input.LinuxAxisDescriptor;
import net.java.games.input.LinuxControllers;
import net.java.games.input.LinuxEventComponent;

class LinuxComponent
extends AbstractComponent {
    private final LinuxEventComponent component;

    public LinuxComponent(LinuxEventComponent component) {
        super(component.getIdentifier().getName(), component.getIdentifier());
        this.component = component;
    }

    @Override
    public final boolean isRelative() {
        return this.component.isRelative();
    }

    @Override
    public final boolean isAnalog() {
        return this.component.isAnalog();
    }

    @Override
    protected float poll() throws IOException {
        return this.convertValue(LinuxControllers.poll(this.component), this.component.getDescriptor());
    }

    float convertValue(float value, LinuxAxisDescriptor descriptor) {
        return this.getComponent().convertValue(value);
    }

    @Override
    public final float getDeadZone() {
        return this.component.getDeadZone();
    }

    public final LinuxEventComponent getComponent() {
        return this.component;
    }
}

