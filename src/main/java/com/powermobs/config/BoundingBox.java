package com.powermobs.config;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoundingBox {
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;
    private final int minZ;
    private final int maxZ;
    private static final Pattern BOX_PATTERN =
            Pattern.compile("(-?\\d+|infinite)-(-?\\d+|infinite) (-?\\d+|infinite)-(-?\\d+|infinite) (-?\\d+|infinite)-(-?\\d+|infinite)",
                    Pattern.CASE_INSENSITIVE);

    public BoundingBox(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    public BoundingBox(BoundingBox box) {
        this.minX = box.minX;
        this.maxX = box.maxX;
        this.minY = box.minY;
        this.maxY = box.maxY;
        this.minZ = box.minZ;
        this.maxZ = box.maxZ;
    }

    public BoundingBox(){
        minX = Integer.MIN_VALUE;
        maxX = Integer.MAX_VALUE;
        minY = Integer.MIN_VALUE;
        maxY = Integer.MAX_VALUE;
        minZ = Integer.MIN_VALUE;
        maxZ = Integer.MAX_VALUE;
    }

    private int parseMaxValue(String s) {
        if (s.equalsIgnoreCase("infinite")) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(s);
    }
    private int parseMinValue(String s) {
        if (s.equalsIgnoreCase("infinite")) {
            return Integer.MIN_VALUE;
        }
        return Integer.parseInt(s);
    }

    public BoundingBox(String boxString) {
        Matcher m = BOX_PATTERN.matcher(boxString);

        if (!m.matches()) {
            throw new IllegalArgumentException("[PowerMobs] Invalid bounding box for mob: " + boxString);
        }

        minX = parseMinValue(m.group(1));
        maxX = parseMaxValue(m.group(2));
        minY = parseMinValue(m.group(3));
        maxY = parseMaxValue(m.group(4));
        minZ = parseMinValue(m.group(5));
        maxZ = parseMaxValue(m.group(6));
    }

    public boolean containsPosition(int x, int y, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ && y >= minY && y <= maxY;
    }

    public String getBoxString() {
        return String.format("%s-%s %s-%s %s-%s", setInfinite(minX), setInfinite(maxX), setInfinite(minY), setInfinite(maxY), setInfinite(minZ), setInfinite(maxZ));
    }
    private String setInfinite(int value) {
        return (value == Integer.MAX_VALUE || value == Integer.MIN_VALUE)
                ? "infinite"
                : String.valueOf(value);
    }

    public String getXPair() {
        return String.format("%s - %s", setInfinite(minX), setInfinite(maxX));
    }

    public String getYPair() {
        return String.format("%s - %s", setInfinite(minY), setInfinite(maxY));
    }

    public String getZPair() {
        return String.format("%s - %s", setInfinite(minZ), setInfinite(maxZ));
    }
}
