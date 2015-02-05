package eu.j3t.gps;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class GPSMap {
    private GPSMapDimension dimension;
    private World world;
    
    protected static boolean canWalkThru(World world, int x, int y, int z)
    {
        Location above = new Location(world, x + 0.5, y, z + 0.5);
        
        return canPassThru(above.getBlock().getType());
    }

    @SuppressWarnings("deprecation")
    protected static boolean canPassThru(Material material)
    {
        /* we can walk through all those types of material */
        return material == Material.AIR
                || material == Material.SIGN_POST
                || material == Material.CARPET
                || material == Material.LADDER
                || material == Material.DEAD_BUSH
                || material == Material.VINE
                || material == Material.DOUBLE_PLANT
                || material == Material.RED_ROSE
                || material == Material.SUGAR_CANE_BLOCK
                || material == Material.RED_MUSHROOM
                || material == Material.BROWN_MUSHROOM
                || material == Material.SAPLING
                || material == Material.STONE_BUTTON
                || material == Material.WOOD_BUTTON
                || material == Material.POWERED_RAIL
                || material == Material.DETECTOR_RAIL
                || material == Material.ACTIVATOR_RAIL
                || material == Material.RAILS
                || material == Material.TORCH
                || material == Material.TRAP_DOOR
                || material == Material.FENCE_GATE
                || material == Material.WOOD_DOOR
                || material == Material.IRON_DOOR_BLOCK
                || material == Material.REDSTONE_TORCH_OFF
                || material == Material.LEVER
                || material == Material.GOLD_PLATE
                || material == Material.WOOD_PLATE
                || material == Material.IRON_PLATE
                || material == Material.STONE_PLATE
                || material == Material.TRIPWIRE_HOOK
                || material == Material.TRIPWIRE
                || material == Material.REDSTONE_TORCH_ON
                || material == Material.REDSTONE_COMPARATOR_OFF
                || material == Material.REDSTONE_COMPARATOR_ON
                || material == Material.REDSTONE_WIRE
                || material.getId() == 36; /* Piston Extension, not really used anymore */
    }

    /* m0 is the block material where the player stand on
     * m1 and m2 are the block material where the player is 
     * 
     * One case not supported: while climbing on vine or standing on top of vine.
     * 
     * Specific case: the user can walk on a ladder
     * 
     * Case not well supported: I don't deal the height of a snow block. If a snow block
     * is on top of a solid block, and below a "pass through" block, the player will
     * be able to go through. If the snow is too high, he will have to remove the snow
     */
    protected static boolean canWalkOn(Material m0, Material m1, Material m2)
    {
        boolean canWalkByM1 = canPassThru(m1);
        boolean canWalkByM2 = canPassThru(m2);
        
        return (m0.isSolid() && canWalkByM1 && canWalkByM2)
                || (m0.isSolid() && m1 == Material.SNOW && canWalkByM2)
                || (m0 == Material.SNOW && m1 == Material.SNOW && canWalkByM2)
                || (m0 == Material.SNOW && m1 != Material.AIR && canWalkByM1 && canWalkByM2)
                || (m1 == Material.LADDER && m2 == Material.LADDER)
                || (m0 == Material.LADDER && canWalkByM1 && canWalkByM2);
    }
    
    protected GPSMap(World newWorld)
    {
        this.world = newWorld;
        this.dimension = new GPSMapDimension();
    }
    
    protected GPSNode getNode(int x, int y, int z)
    {
        int[] position = { x, y, z };
        
        return this.dimension.getElement(position);
    }
    
    protected GPSNode getOrCreateNode(GPSNode parent, int x, int y, int z)
    {
        GPSNode result;
        
        result = this.getNode(x, y, z);
        if (result == null) {
            int[] position = { x, y, z };
            
            result = new GPSNode(parent, x, y, z);
            this.dimension.addElement(result, position);
        }
        return result;
    }
    
    /*
     * This test if we can be at:
     *      <x> <y> <z> position
     *      <x> <y - 1> <z> position (this is tested only if <testFallAndJump> is true)
     *      <x> <y - 2> <z> position (this is tested only if <testFallAndJump> is true)
     *      <x> <y + 1> <z> position (this is tested only if (<testFallAndJump> && <canJump>) is true)
     */
    private boolean addGPSNode(GPSNode parent, boolean canJump, int x, int y, int z, List<GPSNode> list, boolean testFallAndJump)
    {
        GPSNode newNode = null;
        
        newNode = this.getNode(x, y, z);
        if (newNode == null) {
            Material[] materials = {
                    new Location(this.world, x + 0.5, y - 3, z + 0.5).getBlock().getType(),
                    new Location(this.world, x + 0.5, y - 2, z + 0.5).getBlock().getType(),
                    new Location(this.world, x + 0.5, y - 1, z + 0.5).getBlock().getType(),
                    new Location(this.world, x + 0.5, y, z + 0.5).getBlock().getType(),
                    new Location(this.world, x + 0.5, y + 1, z + 0.5).getBlock().getType(),
                    new Location(this.world, x + 0.5, y + 2, z + 0.5).getBlock().getType()
            };
            
            if (GPSMap.canWalkOn(materials[2], materials[3], materials[4])) {
                newNode = this.getOrCreateNode(parent, x, y, z);
            } else if (testFallAndJump
                    && GPSMap.canWalkOn(materials[1], materials[2], materials[3])
                    && GPSMap.canPassThru(materials[4])) {
                newNode = this.getOrCreateNode(parent, x, y - 1, z);
            } else if (testFallAndJump
                    && GPSMap.canWalkOn(materials[0], materials[1], materials[2])
                    && GPSMap.canPassThru(materials[3])
                    && GPSMap.canPassThru(materials[4])) {
                newNode = this.getOrCreateNode(parent, x, y - 2, z);
            } else if (testFallAndJump
                    && canJump
                    && GPSMap.canWalkOn(materials[3], materials[4], materials[5])) {
                newNode = this.getOrCreateNode(parent, x, y + 1, z);
            }
        }
        if (newNode != null) {
            list.add(newNode);
            return true;
        } else {
            return false;
        }
    }
    
    /*
     *  Get the list of nodes that a player can go to from a <node>
     *  
     *  If the list has been already computed, otherwise, we test all positions arround
     *  
     *  Let's all blocks around the current position (with going up, going down,
     *  jumping one block, jumping 2 blocks)
     */
    protected List<GPSNode> childNodes(GPSNode node)
    {
        List<GPSNode> nodes;
        
        nodes = node.getNextNodes();
        if (nodes == null) {
            nodes = new LinkedList<GPSNode>();
            int x = node.getX();
            int y = node.getY();
            int z = node.getZ();
            boolean canJump = canWalkThru(this.world, x, y + 2, z);
            
            /* let's try to be on x + 1 */
            if (!this.addGPSNode(node, canJump, x + 1, y, z, nodes, true)
                    && canWalkThru(this.world, x + 1, y, z)
                    && canWalkThru(this.world, x + 1, y + 1, z)
                    && canWalkThru(this.world, x + 1, y + 2, z)) {
                /* it was not possible, but at least, we could jump to reach x + 2 */
                /* let's try if we can be on x + 2 */
                if (!this.addGPSNode(node, canJump, x + 2, y, z, nodes, true)
                        && canWalkThru(this.world, x + 2, y, z)
                        && canWalkThru(this.world, x + 2, y + 1, z)
                        && canWalkThru(this.world, x + 2, y + 2, z)) {
                    /* it was not possible, but at least, we could jump to reach x + 3 */
                    /*let's try if we can be on x + 3 */
                    this.addGPSNode(node, canJump, x + 3, y, z, nodes, true);                    
                }
            }
            /* we try now with x - 1, y + 1 and y - 1, the same way */
            if (!this.addGPSNode(node, canJump, x - 1, y, z, nodes, true)
                    && canWalkThru(this.world, x - 1, y, z)
                    && canWalkThru(this.world, x - 1, y + 1, z)
                    && canWalkThru(this.world, x - 1, y + 2, z)) {
                if (!this.addGPSNode(node, canJump, x - 2, y, z, nodes, true)
                        && canWalkThru(this.world, x - 2, y, z)
                        && canWalkThru(this.world, x - 2, y + 1, z)
                        && canWalkThru(this.world, x - 2, y + 2, z)) {
                    this.addGPSNode(node, canJump, x - 3, y, z, nodes, true);                
                }
            }
            if (!this.addGPSNode(node, canJump, x, y, z + 1, nodes, true)
                    && canWalkThru(this.world, x, y, z + 1)
                    && canWalkThru(this.world, x, y + 1, z + 1)
                    && canWalkThru(this.world, x, y + 2, z + 1)) {
                if (!this.addGPSNode(node, canJump, x, y, z + 2, nodes, true)
                        && canWalkThru(this.world, x, y, z + 2)
                        && canWalkThru(this.world, x, y + 1, z + 2)
                        && canWalkThru(this.world, x, y + 2, z + 2)) {
                    this.addGPSNode(node, canJump, x, y, z + 3, nodes, true);                
                }
            }
            if (!this.addGPSNode(node, canJump, x, y, z - 1, nodes, true)
                    && canWalkThru(this.world, x, y, z - 1)
                    && canWalkThru(this.world, x, y + 1, z - 1)
                    && canWalkThru(this.world, x, y + 2, z - 1)) {
                if (!this.addGPSNode(node, canJump, x, y, z - 2, nodes, true)
                        && canWalkThru(this.world, x, y, z - 2)
                        && canWalkThru(this.world, x, y + 1, z - 2)
                        && canWalkThru(this.world, x, y + 2, z - 2)) {
                    this.addGPSNode(node, canJump, x, y, z - 3, nodes, true);                
                }
            }
            
            /* test if we can down or go up (with a ladder) */
            if (!this.addGPSNode(node, false, x, y - 1, z, nodes, false)) {
                this.addGPSNode(node, canJump, x, y - 2, z, nodes, false);                
            }
            this.addGPSNode(node, false, x, y + 1, z, nodes, false);
            
            node.setNextNodes(nodes);
        }
        return nodes;
    }
    
    /*
     * compute a distance between <node1> and <node2>
     */
    protected double distanceNode(GPSNode node1, GPSNode node2)
    {
        int xDifference = node1.getX() - node2.getX();
        int yDifference = node1.getY() - node2.getY();
        int zDifference = node1.getZ() - node2.getZ();
        
        return Math.sqrt(xDifference * xDifference + yDifference * yDifference + zDifference * zDifference);
    }

    /*
     * compute a distance between <node> and <location>
     */
    protected double distanceNodeFromLocation(GPSNode node, Location location)
    {
        int xDifference = node.getX() - location.getBlockX();
        int yDifference = node.getY() - location.getBlockY();
        int zDifference = node.getZ() - location.getBlockZ();
        
        return Math.sqrt(xDifference * xDifference + yDifference * yDifference + zDifference * zDifference);
    }

    protected Location getLocation(GPSNode node)
    {
        return new Location(this.world, node.getX() + 0.5, node.getY(), node.getZ() + 0.5);
    }
}
