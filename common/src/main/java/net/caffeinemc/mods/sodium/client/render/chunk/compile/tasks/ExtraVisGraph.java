package net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

public class ExtraVisGraph {
    private static final int LEN = 16;
    private static final int MASK = 15;
    private static final int SIZE = 4096;
    private static final int X_SHIFT = 0;
    private static final int Z_SHIFT = 4;
    private static final int Y_SHIFT = 8;
    private static final int DX = (int) Math.pow((double) 16.0F, (double) 0.0F);
    private static final int DZ = (int) Math.pow((double) 16.0F, (double) 1.0F);
    private static final int DY = (int) Math.pow((double) 16.0F, (double) 2.0F);
    private static final int INVALID_INDEX = -1;
    private static final int PORTAL_THRESHOLD = 64;
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BitSet bitSet = new BitSet(4096);
    private static final int[] INDEX_OF_EDGES = (int[]) Util.make(new int[1352], (is) -> {
        int k = 0;
        for (int l = 0; l < LEN; ++l) {
            for (int m = 0; m < LEN; ++m) {
                for (int n = 0; n < LEN; ++n) {
                    if (l == 0 || l == 15 || m == 0 || m == 15 || n == 0 || n == 15) {
                        is[k++] = getIndex(l, m, n);
                    }
                }
            }
        }
    });
    private int empty = 4096;

    public void setOpaque(BlockPos pos) {
        this.bitSet.set(getIndex(pos), true);
        --this.empty;
    }

    private static int getIndex(BlockPos pos) {
        return getIndex(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
    }

    private static int getIndex(int x, int y, int z) {
        return x << 0 | y << 8 | z << 4;
    }

    public ExtraVisibilitySet resolve() {
        ExtraVisibilitySet visibilitySet = new ExtraVisibilitySet();
        if (SIZE - this.empty < 256) {
            visibilitySet.setAll(true);
            return visibilitySet;
        } else if (this.empty == 0) {
            visibilitySet.setAll(false);
            return visibilitySet;
        }
        BitSet bitSetBackup = (BitSet) this.bitSet.clone();
        for (int i : INDEX_OF_EDGES) {
            if (!this.bitSet.get(i)) {
                visibilitySet.add(this.floodFill(i));
            }
        }

        return resolvePortal(visibilitySet, bitSetBackup);
    }

    private ExtraVisibilitySet resolvePortal(ExtraVisibilitySet vis, BitSet bitSet) {
        int[] minU = new int[6], minV = new int[6],
                maxU = new int[6], maxV = new int[6];
        java.util.Arrays.fill(minU, 16);
        java.util.Arrays.fill(minV, 16);
        java.util.Arrays.fill(maxU, -1);
        java.util.Arrays.fill(maxV, -1);

        for (Direction dir : Direction.values()) {
            if (!vis.hasPortal(dir)) continue;

            switch (dir) {
                case WEST, EAST -> {
                    int x = (dir == Direction.WEST) ? 0 : 15;
                    int id = dir.ordinal();
                    for (int y = 0; y < 16; y++)
                        for (int z = 0; z < 16; z++)
                            if (!bitSet.get(getIndex(x, y, z))) {
                                minU[id] = Math.min(minU[id], z);
                                minV[id] = Math.min(minV[id], y);
                                maxU[id] = Math.max(maxU[id], z);
                                maxV[id] = Math.max(maxV[id], y);
                            }
                }
                case NORTH, SOUTH -> {
                    int z = (dir == Direction.NORTH) ? 0 : 15;
                    int id = dir.ordinal();
                    for (int y = 0; y < 16; y++)
                        for (int x = 0; x < 16; x++)
                            if (!bitSet.get(getIndex(x, y, z))) {
                                minU[id] = Math.min(minU[id], x);
                                minV[id] = Math.min(minV[id], y);
                                maxU[id] = Math.max(maxU[id], x);
                                maxV[id] = Math.max(maxV[id], y);
                            }
                }
                case DOWN, UP -> {
                    int yFixed = (dir == Direction.DOWN) ? 0 : 15;
                    int id = dir.ordinal();
                    for (int z = 0; z < 16; z++)
                        for (int x = 0; x < 16; x++)
                            if (!bitSet.get(getIndex(x, yFixed, z))) {
                                minU[id] = Math.min(minU[id], x);
                                minV[id] = Math.min(minV[id], z);
                                maxU[id] = Math.max(maxU[id], x);
                                maxV[id] = Math.max(maxV[id], z);
                            }
                }
            }
        }

        for (Direction dir : Direction.values()) {
            short portal;
            int id = dir.ordinal();

            if (!vis.hasPortal(dir) || maxU[id] < 0) {
                vis.setPortal(dir, (short) 0);
                continue;
            }

            int wU = maxU[id] - minU[id] + 1;
            int wV = maxV[id] - minV[id] + 1;

            if (wU * wV > PORTAL_THRESHOLD) {
                portal = (short) 0xFFFF;
            } else {
                portal = (short) (((wV - 1) & 0xF) << 12 |
                        ((wU - 1) & 0xF) << 8 |
                        (minV[id] & 0xF) << 4 |
                        (minU[id] & 0xF));
            }
            vis.setPortal(dir, portal);
        }
        return vis;
    }

    private Set<Direction> floodFill(int index) {
        Set<Direction> set = EnumSet.noneOf(Direction.class);
        IntPriorityQueue intPriorityQueue = new IntArrayFIFOQueue();
        intPriorityQueue.enqueue(index);
        this.bitSet.set(index, true);

        while (!intPriorityQueue.isEmpty()) {
            int i = intPriorityQueue.dequeueInt();
            this.addEdges(i, set);

            for (Direction direction : DIRECTIONS) {
                int j = this.getNeighborIndexAtFace(i, direction);
                if (j >= 0 && !this.bitSet.get(j)) {
                    this.bitSet.set(j, true);
                    intPriorityQueue.enqueue(j);
                }
            }
        }

        return set;
    }

    private void addEdges(int index, Set<Direction> faces) {
        int i = index >> X_SHIFT & MASK;
        if (i == 0) {
            faces.add(Direction.WEST);
        } else if (i == MASK) {
            faces.add(Direction.EAST);
        }

        int j = index >> Y_SHIFT & MASK;
        if (j == 0) {
            faces.add(Direction.DOWN);
        } else if (j == MASK) {
            faces.add(Direction.UP);
        }

        int k = index >> Z_SHIFT & MASK;
        if (k == 0) {
            faces.add(Direction.NORTH);
        } else if (k == MASK) {
            faces.add(Direction.SOUTH);
        }

    }

    private int getNeighborIndexAtFace(int index, Direction face) {
        return switch (face) {
            case DOWN -> {
                if ((index >> Y_SHIFT & MASK) == 0) {
                    yield INVALID_INDEX;
                }

                yield index - DY;
            }
            case UP -> {
                if ((index >> Y_SHIFT & MASK) == MASK) {
                    yield INVALID_INDEX;
                }

                yield index + DY;
            }
            case NORTH -> {
                if ((index >> Z_SHIFT & MASK) == 0) {
                    yield INVALID_INDEX;
                }

                yield index - DZ;
            }
            case SOUTH -> {
                if ((index >> Z_SHIFT & MASK) == MASK) {
                    yield INVALID_INDEX;
                }

                yield index + DZ;
            }
            case WEST -> {
                if ((index >> X_SHIFT & MASK) == 0) {
                    yield INVALID_INDEX;
                }

                yield index - DX;
            }
            case EAST -> {
                if ((index >> X_SHIFT & MASK) == MASK) {
                    yield INVALID_INDEX;
                }

                yield index + DX;
            }
            default -> INVALID_INDEX;
        };
    }
}