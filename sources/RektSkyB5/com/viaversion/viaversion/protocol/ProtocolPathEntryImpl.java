/*
 * Decompiled with CFR 0.152.
 */
package com.viaversion.viaversion.protocol;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.ProtocolPathEntry;

public class ProtocolPathEntryImpl
implements ProtocolPathEntry {
    private final int outputProtocolVersion;
    private final Protocol<?, ?, ?, ?> protocol;

    public ProtocolPathEntryImpl(int outputProtocolVersion, Protocol<?, ?, ?, ?> protocol) {
        this.outputProtocolVersion = outputProtocolVersion;
        this.protocol = protocol;
    }

    @Override
    public int outputProtocolVersion() {
        return this.outputProtocolVersion;
    }

    @Override
    public Protocol<?, ?, ?, ?> protocol() {
        return this.protocol;
    }

    public boolean equals(Object o2) {
        if (this == o2) {
            return true;
        }
        if (o2 == null || this.getClass() != o2.getClass()) {
            return false;
        }
        ProtocolPathEntryImpl that = (ProtocolPathEntryImpl)o2;
        if (this.outputProtocolVersion != that.outputProtocolVersion) {
            return false;
        }
        return this.protocol.equals(that.protocol);
    }

    public int hashCode() {
        int result = this.outputProtocolVersion;
        result = 31 * result + this.protocol.hashCode();
        return result;
    }
}

