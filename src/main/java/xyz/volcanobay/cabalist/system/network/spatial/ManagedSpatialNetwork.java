package xyz.volcanobay.cabalist.system.network.spatial;


import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@ApiStatus.Internal
public abstract class ManagedSpatialNetwork {
    private final SpatialMap cache;
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final Map<Integer, Integer> dsu = new HashMap<>();

    public ManagedSpatialNetwork(int initialCacheSize) {
        this.cache = new SpatialMap(initialCacheSize);
    }

    /**
     * Abstract method to fetch all connected points from the infinite world source.
     */
    protected abstract Collection<long[]> discoverNetworkMembers(long x, long y, long z);

    /**
     * Determines if a connection can exist between the point at (x1, y1, z1)
     * and the point at (x2, y2, z2).
     */
    protected abstract boolean canConnect(long x1, long y1, long z1, long x2, long y2, long z2);

    /**
     * Returns if a block is allowed to be part of this network.
     */
    protected abstract boolean isMember(long x, long y, long z);

    public void merge(int first, int second) {}
    public void split(int first, int second) {}

    public int getOrDiscover(long x, long y, long z) {
        int rawId = cache.get(x, y, z);
        if (rawId != -1) return findRoot(rawId);

        if (!isMember(x, y, z)) {
            return -1;
        }

        Collection<long[]> members = discoverNetworkMembers(x, y, z);
        if (members == null || members.isEmpty()) return -1;

        Set<Integer> existingIds = new HashSet<>();
        for (long[] p : members) {
            int id = cache.get(p[0], p[1], p[2]);
            if (id != -1) existingIds.add(findRoot(id));
        }

        int finalId;
        if (existingIds.isEmpty()) {
            finalId = idGenerator.getAndIncrement();
            dsu.put(finalId, finalId);
        } else {
            Iterator<Integer> it = existingIds.iterator();
            finalId = it.next();
            while (it.hasNext()) union(finalId, it.next());
        }

        for (long[] p : members) cache.put(p[0], p[1], p[2], finalId);
        return finalId;
    }

    public int addPoint(long x, long y, long z) {
        if (cache.get(x, y, z) != -1) return findRoot(cache.get(x, y, z));

        long[][] neighbors = {{x + 1, y, z}, {x - 1, y, z}, {x, y + 1, z}, {x, y - 1, z}, {x, y, z + 1}, {x, y, z - 1}};
        Set<Integer> neighborRoots = new HashSet<>();
        for (long[] n : neighbors) {
            int nid = cache.get(n[0], n[1], n[2]);
            if (nid != -1 && canConnect(x, y, z, n[0], n[1], n[2])) {
                neighborRoots.add(findRoot(nid));
            }
        }

        if (neighborRoots.isEmpty()) {
            return getOrDiscover(x, y, z);
        } else {
            int finalId = neighborRoots.iterator().next();
            for (int r : neighborRoots) union(finalId, r);
            cache.put(x, y, z, finalId);
            return finalId;
        }
    }

    public void removePoint(long x, long y, long z) {
        int netId = cache.get(x, y, z);
        if (netId == -1) return;
        int rootId = findRoot(netId);

        cache.remove(x, y, z);

        long[][] neighbors = getPhysicalNeighbors(x, y, z);
        List<long[]> oldNetworkNeighbors = new ArrayList<>();

        for (long[] n : neighbors) {
            int nRaw = cache.get(n[0], n[1], n[2]);
            if (nRaw != -1 && findRoot(nRaw) == rootId) {
                oldNetworkNeighbors.add(n);
            }
        }

        if (oldNetworkNeighbors.size() > 1) {
            handlePotentialSplit(oldNetworkNeighbors, rootId);
        }
    }

    public void updatePoint(long x, long y, long z) {
        int currentRawId = cache.get(x, y, z);
        if (currentRawId == -1) return;
        int oldRootId = findRoot(currentRawId);

        cache.remove(x, y, z);

        long[][] physicalNeighbors = getPhysicalNeighbors(x, y, z);
        List<long[]> oldNetworkNeighbors = new ArrayList<>();

        for (long[] n : physicalNeighbors) {
            int nRaw = cache.get(n[0], n[1], n[2]);
            if (nRaw != -1 && findRoot(nRaw) == oldRootId) {
                oldNetworkNeighbors.add(n);
            }
        }

        if (oldNetworkNeighbors.size() > 1) {
            handlePotentialSplit(oldNetworkNeighbors, oldRootId);
        }

        Set<Integer> currentNeighborRoots = new HashSet<>();
        for (long[] n : physicalNeighbors) {
            int nRaw = cache.get(n[0], n[1], n[2]);
            if (nRaw != -1 && canConnect(x, y, z, n[0], n[1], n[2])) {
                currentNeighborRoots.add(findRoot(nRaw));
            }
        }

        if (currentNeighborRoots.isEmpty()) {
            int newId = idGenerator.getAndIncrement();
            dsu.put(newId, newId);
            cache.put(x, y, z, newId);
        } else {
            Iterator<Integer> it = currentNeighborRoots.iterator();
            int finalId = it.next();
            while (it.hasNext()) {
                union(finalId, it.next());
            }
            cache.put(x, y, z, finalId);
        }
    }

    private Set<SmallOctPos> performBFS(long[] start, int rootId) {
        Set<SmallOctPos> visited = new HashSet<>();
        Queue<long[]> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            long[] curr = queue.poll();
            SmallOctPos c = new SmallOctPos(curr[0], curr[1], curr[2]);
            if (visited.contains(c)) continue;
            visited.add(c);

            for (long[] n : getPhysicalNeighbors(curr)) {
                int neighborId = cache.get(n[0], n[1], n[2]);
                if (neighborId != -1 && findRoot(neighborId) == rootId && canConnect(curr[0], curr[1], curr[2], n[0], n[1], n[2])) {
                    queue.add(n);
                }
            }
        }
        return visited;
    }

    private void handlePotentialSplit(List<long[]> neighbors, int oldRootId) {
        if (neighbors.size() <= 1) return;

        Set<SmallOctPos> primaryComponent = performBFS(neighbors.getFirst(), oldRootId);
        Set<SmallOctPos> processedNodes = new HashSet<>(primaryComponent);

        for (int i = 1; i < neighbors.size(); i++) {
            long[] n = neighbors.get(i);
            SmallOctPos nCoord = new SmallOctPos(n[0], n[1], n[2]);

            if (!processedNodes.contains(nCoord)) {
                Set<SmallOctPos> newComponent = reindexComponent(n, oldRootId);
                processedNodes.addAll(newComponent);
            }
        }
    }
    private Set<SmallOctPos> reindexComponent(long[] startNode, int oldRootId) {
        int newId = idGenerator.getAndIncrement();
        dsu.put(newId, newId);

        Set<SmallOctPos> componentNodes = new HashSet<>();
        Queue<long[]> queue = new LinkedList<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            long[] curr = queue.poll();
            SmallOctPos c = new SmallOctPos(curr[0], curr[1], curr[2]);

            if (componentNodes.contains(c)) continue;
            componentNodes.add(c);

            cache.put(curr[0], curr[1], curr[2], newId);

            for (long[] n : getPhysicalNeighbors(curr)) {
                int currentRawId = cache.get(n[0], n[1], n[2]);
                if (currentRawId != -1) {
                    int currentRoot = findRoot(currentRawId);
                    if (currentRoot == oldRootId && canConnect(curr[0], curr[1], curr[2], n[0], n[1], n[2])) {
                        queue.add(n);
                    }
                }
            }
        }
        split(oldRootId, newId);
        return componentNodes;
    }

    private long[][] getPhysicalNeighbors(long x, long y, long z) {
        return new long[][]{
                {x + 1, y, z}, {x - 1, y, z},
                {x, y + 1, z}, {x, y - 1, z},
                {x, y, z + 1}, {x, y, z - 1}
        };
    }

    private long[][] getPhysicalNeighbors(long[] pos) {
        return getPhysicalNeighbors(pos[0], pos[1], pos[2]);
    }

    private int findRoot(int id) {
        if (id == -1) return -1;
        if (!dsu.containsKey(id)) dsu.put(id, id);
        int parent = dsu.get(id);
        if (parent == id) return id;
        int root = findRoot(parent);
        dsu.put(id, root);
        return root;
    }

    private void union(int id1, int id2) {
        int r1 = findRoot(id1);
        int r2 = findRoot(id2);
        if (r1 != r2) {
            merge(r1,r2);
            dsu.put(r2, r1);
        }
    }

    public record SmallOctPos(long x, long y, long z) {
    }
}