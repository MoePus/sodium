package net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.Direction;

import java.util.Arrays;
import java.util.Set;

public class ExtraVisibilitySet extends VisibilitySet {
    private final short[] Portal = new short[6];
    private final boolean[] hasPortal = new boolean[6];

    public void setPortal(Direction direction, short value) {
        Portal[direction.ordinal()] = value;
    }

    public short getPortal(int index) {
        return Portal[index];
    }

    public void setAll(boolean visible) {
        super.setAll(visible);
        if (visible) {
            Arrays.fill(Portal, (short) 0xFFFF);
            Arrays.fill(hasPortal, true);
        } else {
            Arrays.fill(Portal, (short) 0);
            Arrays.fill(hasPortal, false);
        }
    }

    public void add(Set<Direction> faces) {
        super.add(faces);
        for (Direction face : faces) {
            int index = face.ordinal();
            hasPortal[index] = true;
        }
    }

    public boolean hasPortal(Direction direction) {
        return hasPortal[direction.ordinal()];
    }
}
