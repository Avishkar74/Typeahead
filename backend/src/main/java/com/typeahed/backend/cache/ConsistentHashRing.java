package com.typeahed.backend.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConsistentHashRing {

    public static final int TOTAL_SLOTS = 16384;

    private final List<String> nodes;

    public ConsistentHashRing(List<String> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes list cannot be null or empty");
        }
        this.nodes = Collections.unmodifiableList(new ArrayList<>(nodes));
    }

    /**
     * Map a slot number (0 to 16383) to a node using the documented range:
     * - Node 1 (index 0) gets slots 0 to 5460.
     * - Node 2 (index 1) gets slots 5461 to 10922.
     * - Node 3 (index 2) gets slots 10923 to 16383.
     * 
     * In general, for N nodes, the boundaries are dynamically computed as:
     * endSlot = Math.round((double) (i + 1) * TOTAL_SLOTS / N) - 1;
     */
    public String getNodeForSlot(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOTS) {
            throw new IllegalArgumentException("Slot must be between 0 and " + (TOTAL_SLOTS - 1));
        }
        int numNodes = nodes.size();
        for (int i = 0; i < numNodes; i++) {
            long endSlot = Math.round((double) (i + 1) * TOTAL_SLOTS / numNodes) - 1;
            if (slot <= endSlot) {
                return nodes.get(i);
            }
        }
        // Fallback
        return nodes.get(numNodes - 1);
    }

    /**
     * Hashes the given key/prefix to a slot number (0 to 16383).
     * Follows the formula: slot = hash(prefix) % 16384.
     */
    public int getSlot(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        int hash = prefix.hashCode();
        return (hash & 0x7FFFFFFF) % TOTAL_SLOTS;
    }

    /**
     * Get the node responsible for the given key/prefix.
     */
    public String getNode(String prefix) {
        int slot = getSlot(prefix);
        return getNodeForSlot(slot);
    }

    public List<String> getNodes() {
        return nodes;
    }
}
