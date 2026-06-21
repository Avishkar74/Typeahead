package com.typeahed.backend.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsistentHashRingTest {

    @Test
    void testConstructorRequiresNodes() {
        assertThatThrownBy(() -> new ConsistentHashRing(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConsistentHashRing(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testThreeNodeBoundaries() {
        List<String> nodes = List.of("node1", "node2", "node3");
        ConsistentHashRing ring = new ConsistentHashRing(nodes);

        // Node 1: slots 0 to 5460
        assertThat(ring.getNodeForSlot(0)).isEqualTo("node1");
        assertThat(ring.getNodeForSlot(5460)).isEqualTo("node1");

        // Node 2: slots 5461 to 10922
        assertThat(ring.getNodeForSlot(5461)).isEqualTo("node2");
        assertThat(ring.getNodeForSlot(10922)).isEqualTo("node2");

        // Node 3: slots 10923 to 16383
        assertThat(ring.getNodeForSlot(10923)).isEqualTo("node3");
        assertThat(ring.getNodeForSlot(16383)).isEqualTo("node3");
    }

    @Test
    void testInvalidSlotIndex() {
        ConsistentHashRing ring = new ConsistentHashRing(List.of("node1"));
        assertThatThrownBy(() -> ring.getNodeForSlot(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.getNodeForSlot(16384))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testDeterministicHashing() {
        ConsistentHashRing ring = new ConsistentHashRing(List.of("node1", "node2", "node3"));

        String prefix1 = "iphone";
        String prefix2 = "java";

        int slot1a = ring.getSlot(prefix1);
        int slot1b = ring.getSlot(prefix1);
        assertThat(slot1a).isEqualTo(slot1b);

        int slot2a = ring.getSlot(prefix2);
        int slot2b = ring.getSlot(prefix2);
        assertThat(slot2a).isEqualTo(slot2b);

        String node1a = ring.getNode(prefix1);
        String node1b = ring.getNode(prefix1);
        assertThat(node1a).isEqualTo(node1b);
    }

    @Test
    void testEmptyAndNullPrefix() {
        ConsistentHashRing ring = new ConsistentHashRing(List.of("node1", "node2", "node3"));

        // Null check
        assertThatThrownBy(() -> ring.getSlot(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ring.getNode(null))
                .isInstanceOf(IllegalArgumentException.class);

        // Empty string check
        int slot = ring.getSlot("");
        assertThat(slot).isEqualTo(0);
        assertThat(ring.getNode("")).isEqualTo("node1");
    }

    @Test
    void testDistributionOfPrefixes() {
        ConsistentHashRing ring = new ConsistentHashRing(List.of("node1", "node2", "node3"));

        int node1Count = 0;
        int node2Count = 0;
        int node3Count = 0;

        // Generate 1000 different prefixes and count distribution
        for (int i = 0; i < 1000; i++) {
            String node = ring.getNode(java.util.UUID.randomUUID().toString());
            switch (node) {
                case "node1" -> node1Count++;
                case "node2" -> node2Count++;
                case "node3" -> node3Count++;
            }
        }

        // With 1000 keys, each node should receive a fair share (typically between 25% and 40% of the keys)
        assertThat(node1Count).isGreaterThan(200);
        assertThat(node2Count).isGreaterThan(200);
        assertThat(node3Count).isGreaterThan(200);
    }

    @Test
    void testDynamicPartitioningWithDifferentNodeCount() {
        // Two nodes
        ConsistentHashRing twoNodeRing = new ConsistentHashRing(List.of("node1", "node2"));
        // Dynamic: Node 1 owns 0 to 8191, Node 2 owns 8192 to 16383
        assertThat(twoNodeRing.getNodeForSlot(0)).isEqualTo("node1");
        assertThat(twoNodeRing.getNodeForSlot(8191)).isEqualTo("node1");
        assertThat(twoNodeRing.getNodeForSlot(8192)).isEqualTo("node2");
        assertThat(twoNodeRing.getNodeForSlot(16383)).isEqualTo("node2");

        // Four nodes
        ConsistentHashRing fourNodeRing = new ConsistentHashRing(List.of("node1", "node2", "node3", "node4"));
        // Dynamic: Node 1 (0 to 4095), Node 2 (4096 to 8191), Node 3 (8192 to 12287), Node 4 (12288 to 16383)
        assertThat(fourNodeRing.getNodeForSlot(4095)).isEqualTo("node1");
        assertThat(fourNodeRing.getNodeForSlot(4096)).isEqualTo("node2");
        assertThat(fourNodeRing.getNodeForSlot(8191)).isEqualTo("node2");
        assertThat(fourNodeRing.getNodeForSlot(8192)).isEqualTo("node3");
        assertThat(fourNodeRing.getNodeForSlot(12287)).isEqualTo("node3");
        assertThat(fourNodeRing.getNodeForSlot(12288)).isEqualTo("node4");
    }
}
