package me.trambled.ozark.ozarkclient.module.combat;

import me.trambled.ozark.ozarkclient.event.events.EventPacket;
import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;
import me.trambled.ozark.ozarkclient.util.misc.MessageUtil;
import me.trambled.ozark.ozarkclient.util.player.social.FriendUtil;
import me.trambled.ozark.ozarkclient.util.world.BlockInteractionHelper;
import me.zero.alpine.fork.listener.EventHandler;
import me.zero.alpine.fork.listener.Listener;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

//gamesense fart
public class PistonCrystal extends Module
{
    public PistonCrystal() {
        super(Category.COMBAT);

        this.name = "PistonCrystal";
        this.tag = "PistonCrystal";
        this.description = "Automatically pushes crystals into holes using pistons.";
    }

    private boolean noMaterials = false,
            hasMoved = false,
            isSneaking = false,
            yUnder = false,
            isHole = true,
            enoughSpace = true,
            redstoneBlockMode = false,
            fastModeActive = false,
            broken,
            brokenCrystalBug,
            brokenRedstoneTorch,
            deadPl,
            rotationPlayerMoved;

    private int oldSlot = -1,
            stage,
            delayTimeTicks,
            stuck = 0,
            hitTryTick,
            nCrystal;
    private long startTime, endTime;

    private int[]   slot_mat,
                    delayTable,
                    meCoordsInt,
                    enemyCoordsInt;

    private double[] enemyCoordsDouble;

    private structureTemp toPlace;

    int[][] disp_surblock = {
            {1,0,0},
            {-1,0,0},
            {0,0,1},
            {0,0,-1}
    };

    Double[][] sur_block = new Double[4][3];

    private EntityPlayer aimTarget;
	
	Setting breakType = create("Break Types", "PistonCrystalBreakTypes", "Swing", combobox("Swing", "Packet"));
	Setting placeMode = create("Place Mode", "PistonCrystalPlaceMode", "Torch", combobox("Block", "Torch", "Both"));
	Setting target = create("Target Mode", "PistonCrystalTargetMode", "Nearest", combobox("Nearest", "Looking"));
	Setting range = create("Range", "PistonCrystalRange", 4.91, 0, 6);
	Setting crystalDeltaBreak = create("Center Break", "PistonCrystalCenterBreak", 1, 0, 5);
	Setting blocksPerTick = create("Blocks Per Tick", "PistonCrystalBPS", 4, 0, 20);
	Setting supBlocksDelay = create("Surround Delay", "PistonCrystalSurroundDelay", 4, 0, 20);
	Setting startDelay = create("Start Delay", "PistonCrystalStartDelay", 4, 0, 20);
	Setting pistonDelay = create("Piston Delay", "PistonCrystalPistonDelay", 2, 0, 20);
	Setting crystalDelay = create("Crystal Delay", "PistonCrystalCrystalDelay", 2, 0, 20);
	Setting midHitDelay = create("Mid HitDelay", "PistonCrystalMidHitDelay", 5, 0, 20);
	Setting hitDelay = create("Hit Delay", "PistonCrystalHitDelay", 2, 0, 20);
	Setting stuckDetector = create("Stuck Check", "PistonCrystalStuckDetector", 35, 0, 200);
	Setting maxYincr = create("Max Y", "PistonCrystalMaxY", 3, 0, 5);
	Setting blockPlayer = create("Block Player", "PistonCrystalBlockPlayer", true);
	Setting rotate = create("Rotate", "PistonCrystalRotate", false);
	Setting confirmBreak = create("Confirm Break", "PistonCrystalConfirmBreak", true);
	Setting confirmPlace=  create("Confirm Place", "PistonCrystalConfirmPlace", true);
    Setting allowCheapMode = create("Cheap Mode", "PistonCrystalCheapMode", false);
	Setting betterPlacement = create("Better Place", "PistonCrystalBetterPlace", true);
	Setting bypassObsidian = create("Bypass", "PistonCrystalBypass", false);
	Setting antiWeakness = create("Anti Weakness", "PistonCrystalAntiWeakness", false);
	Setting chatMsg = create("Chat Messages", "PistonCrystalChatMSG", true);

    private double CrystalDeltaBreak = crystalDeltaBreak.get_value(1) * 0.1;

    @Override
    protected void enable() {
        // Init values
        initValues();
        // Get Target
        if (getAimTarget())
            return;
        // Make checks
        playerChecks();
    }


    // Get target function
    private boolean getAimTarget() {
        /// Get aimTarget
        // If nearest, get it
        if (target.in("Nearest"))
            aimTarget = findClosestTarget(range.get_value(1), aimTarget);
        // if looking
        else
            aimTarget = findLookingPlayer(range.get_value(1));

        // If we didnt found a target
        if (aimTarget == null || !target.in("Looking")){
            // if it's not looking and we didnt found a target
            if (!target.in("Looking") && aimTarget == null)
                set_disable();
            // If not found a target
            return aimTarget == null;
        }
        return false;
    }

    // Make some checks for startup
    private void playerChecks() {
        // Get all the materials
        if (getMaterialsSlot()) {
            // check if the enemy is in a hole
            if (is_in_hole()) {
                // Get enemy coordinates
                enemyCoordsDouble = new double[] {aimTarget.posX, aimTarget.posY, aimTarget.posZ};
                enemyCoordsInt = new int[] {(int) enemyCoordsDouble[0], (int) enemyCoordsDouble[1], (int) enemyCoordsDouble[2]};
                // Get me coordinates
                meCoordsInt = new int[] {(int) mc.player.posX, (int) mc.player.posY, (int) mc.player.posZ};
                // Make that things wont confict with other things
                antiAutoDestruction();
                // Start choosing where to place what
                enoughSpace = createStructure();
            // Is not in a hoke
            } else {
                isHole = false;
            }
        // No materials
        }else noMaterials = true;
    }

    private void antiAutoDestruction() {
        // You cannot use betterplacement if you are using or a redstone block or rotation on
        if (redstoneBlockMode || rotate.get_value(true))
            betterPlacement.set_value(false);
    }

    // Init some values
    private void initValues() {
        // Reset aimtarget
        aimTarget = null;
        // Create new delay table
        delayTable = new int[] {
                startDelay.get_value(1),
                supBlocksDelay.get_value(1),
                pistonDelay.get_value(1),
                crystalDelay.get_value(1),
                hitDelay.get_value(1)
        };
        // Default values reset
        toPlace = new structureTemp(0,0,null);
        isHole = true;
        hasMoved = rotationPlayerMoved = deadPl = broken = brokenCrystalBug = brokenRedstoneTorch = yUnder = redstoneBlockMode = fastModeActive = false;
        slot_mat = new int[]{-1, -1, -1, -1, -1, -1};
        stage = delayTimeTicks = stuck = 0;

        if (mc.player == null){
            set_disable();
            return;
        }

        if (chatMsg.get_value(true)){
            MessageUtil.send_client_message("PistonCrystal turned on");
        }

        oldSlot = mc.player.inventory.currentItem;
    }

    @Override
    protected void disable() {
        if (mc.player == null){
            return;
        }
        // If output
        if (chatMsg.get_value(true)){
            String output = "";
            String materialsNeeded = "";
            // No target found
            if (aimTarget == null) {
                output = "No target found...";
            }else
                // H distance not avaible
                if (yUnder) {
                    output = String.format("Sorry but you cannot be 2+ blocks under the enemy or %d above...", maxYincr.get_value(1));
                }else if (noMaterials){
                    output = "No Materials Detected   ";
                    materialsNeeded = getMissingMaterials();
                }else if (!isHole) {
                    output = "The enemy is not in a hole  ";
                    // No Space
                }else if(!enoughSpace) {
                    output = "Not enough space   ";
                    // Has Moved
                }else if(hasMoved) {
                    output = "Out of range";
                }else if(deadPl) {
                    output = "Enemy is dead, ezzzzzzz! ";
                }else if(rotationPlayerMoved) {
                    output = "You cannot move from your hole if you have rotation on. ";
                }

            if (!materialsNeeded.equals(""))
                MessageUtil.send_client_error_message("Materials missing:" + materialsNeeded);

            // Output in chat
            MessageUtil.send_client_message(output + "PistonCrystal turned off");

        }

        if (isSneaking){
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING));
            isSneaking = false;
        }

        if (oldSlot != mc.player.inventory.currentItem && oldSlot != -1){
            mc.player.inventory.currentItem = oldSlot;
            oldSlot = -1;
        }

        noMaterials = false;
    }


    // Every updates
	@Override
    public void update() {
        // If no mc.player
        if (mc.player == null){
            set_disable();
            return;
        }

        CrystalDeltaBreak = crystalDeltaBreak.get_value(1) * 0.1;


        // Wait
        if (delayTimeTicks < delayTable[stage]){
            delayTimeTicks++;
            return;
        }
        // If the delay is finished
        else {
            delayTimeTicks = 0;
        }

        // Check if something is not ok
        if (enemyCoordsDouble == null || aimTarget == null) {
            if (aimTarget == null) {
                aimTarget = findLookingPlayer(range.get_value(1));
                if (aimTarget != null) {
                    playerChecks();
                }
            }else
                checkVariable();
            return;
        }

        // Check if he is dead
        if (aimTarget.isDead) {
            deadPl = true;
        }
        // If the guy moved from his hole with rotation on
        if (rotate.get_value(true) && !((int) mc.player.posX == meCoordsInt[0] || (int) mc.player.posZ == meCoordsInt[2]))
            rotationPlayerMoved = true;

        // Check if he is not in the hole
        if ((int) aimTarget.posX != (int) enemyCoordsDouble[0] || (int) aimTarget.posZ != (int) enemyCoordsDouble[2])
            hasMoved = true;

        // If we have to left
        if (checkVariable()){
            return;
        }

        /*
            This is how it works:
            a)
                First of all, it check if  we have all the supportive blocks + if we have arleady something.
                (i want it to prevent some bugs)
            If there is some missing one, it is going to place a block and it will finish.
            then, it place:
                b) Piston
                c) Crystal
                d) redstone torch
            then, it wait and
                e) break the crystal.
         */

        /// Start Placing ///
        // A) Lets place all the supports blocks
        if (placeSupport()) {
            switch (stage) {
                // Place the piston
                case 1:
                    // Check if there is a redstone torch to break
                    if (fastModeActive || breakRedstone()) {
                        if (!fastModeActive || checkCrystalPlace())
                            placeBlockThings(stage);
                        else
                            stage = 2;
                    }
                    break;

                // Place crystal
                case 2:
                    // Check pistonPlace if confirmPlace
                    if (fastModeActive || !confirmPlace.get_value(true) || checkPistonPlace())
                        placeBlockThings(stage);
                    break;

                // Place redstone torch
                case 3:
                    // Check crystal if confirmPlace
                    if (fastModeActive || !confirmPlace.get_value(true) || checkCrystalPlace()) {
                        placeBlockThings(stage);
                        hitTryTick = 0;
                        if (fastModeActive && !checkPistonPlace())
                            stage = 1;
                    }
                    break;

                // Break crystal
                case 4:
                    // Start destroy crystal
                    destroyCrystalAlgo();
                    break;
            }
        }

    }

    // Algo for destroying the crystal
    public void destroyCrystalAlgo() {
        // Get the crystal
        Entity crystal = null;
        // Check if the crystal exist
        for(Entity t : mc.world.loadedEntityList) {
            // If it's a crystal
            if (t instanceof EntityEnderCrystal) {
                /// Check if the crystal is in the enemy
                // One coordinate is going to be always the same, the other is going to change (because we are pushing it)
                // We have to check if that coordinate is the same as the enemy. Ww add "crystalDeltaBreak" so we can break the crystal before
                // It go to the hole, for a better speed (we find the frame perfect for every servers)
                if (    (int) t.posX == enemyCoordsInt[0] &&
                    ( (int) (t.posZ - CrystalDeltaBreak) == enemyCoordsInt[2] || (int) ( t.posZ + CrystalDeltaBreak) == enemyCoordsInt[2])
                    ||  (int) t.posZ == enemyCoordsInt[2] &&
                    ( (int) ( t.posX - CrystalDeltaBreak) == enemyCoordsInt[0] || (int) ( t.posX + CrystalDeltaBreak) == enemyCoordsInt[0]))
                    // If found, yoink
                    crystal = t;
            }
        }
        // If we have confirmBreak, we have found 0 crystal and we broke a crystal before
        if (confirmBreak.get_value(true) && broken && crystal == null) {
            /// That means the crystal was broken 100%
            // Reset
            stage = stuck = 0;
            broken = false;
        }
        // If found the crystal
        if (crystal != null) {
            // Break it
            breakCrystalPiston(crystal);
            // If we have to check
            if (confirmBreak.get_value(true))
                broken = true;
                // If not, left
            else {
                stage = stuck = 0;
            }
        }else {
            // If it got stuck
            if (++stuck >= stuckDetector.get_value(1)) {
                /// Try to find the error
                // First error: crystal not found
                boolean found = false;
                for(Entity t : mc.world.loadedEntityList) {
                    // If crystal
                    if (t instanceof EntityEnderCrystal
                            // If coordinates the same as where is the crystal
                            && (int) t.posX == (int) toPlace.to_place.get(toPlace.supportBlock + 1).x &&
                            (int) t.posZ == (int) toPlace.to_place.get(toPlace.supportBlock + 1).z ) {
                        // Found
                        found = true;
                        break;
                    }
                }
                // If not
                if (!found) {
                    //// The error is that the crystal has not been placed
                    /// Destroy the redstone torch
                    // If rotation
                    BlockPos offsetPosPist = new BlockPos(toPlace.to_place.get(toPlace.supportBlock + 2));
                    BlockPos pos = new BlockPos(aimTarget.getPositionVector()).add(offsetPosPist.getX(), offsetPosPist.getY(), offsetPosPist.getZ());

                    // Check if there is the redstone torch. It is has been destroyed before
                    if (confirmBreak.get_value(true) && brokenRedstoneTorch && get_block(pos.getX(), pos.getY(), pos.getZ()) instanceof BlockAir) {
                        // Reset
                        stage = 1;
                        brokenRedstoneTorch = false;
                    } else {
                        // Else, get the side
                        EnumFacing side = BlockInteractionHelper.getPlaceableSide(pos);
                        if (side != null) {
                            breakRedstone();
                            /// Restart from the crystal
                            if (confirmBreak.get_value(true))
                                brokenRedstoneTorch = true;
                            else {
                                stage = 1;
                            }
                            // print
                            MessageUtil.send_client_message("Stuck detected: crystal not placed");
                        }
                    }

                }else {
                    // Try to see if the crystal, somehow, is in the piston extended thing (it happened sometimes)
                    boolean ext = false;
                    // Find the crystal
                    for(Entity t : mc.world.loadedEntityList) {
                        if (t instanceof EntityEnderCrystal
                                && (int) t.posX == (int) toPlace.to_place.get(toPlace.supportBlock + 1).x &&
                                (int) t.posZ == (int) toPlace.to_place.get(toPlace.supportBlock + 1).z ) {
                            ext = true;
                            break;
                        }
                    }
                    // If it hasnt been found and confirmBreak
                    if (confirmBreak.get_value(true) && brokenCrystalBug && !ext) {
                        // Reset
                        stage = stuck = 0;
                        brokenCrystalBug = false;
                    }
                    // If yes
                    if (ext) {
                        // Break the crystal
                        breakCrystalPiston(crystal);
                        // If we have to check
                        if (confirmBreak.get_value(true))
                            brokenCrystalBug = true;
                        else
                            stage = stuck = 0;
                        // Print
                        MessageUtil.send_client_message("Stuck detected: crystal is stuck in the moving piston");
                    }
                }
            }
        }
    }

    private String getMissingMaterials() {
        /*
			// I use this as a remind to which index refers to what
			0 => obsidian
			1 => piston
			2 => Crystals
			3 => redstone
			4 => sword
			5 => pick
		 */
        StringBuilder output = new StringBuilder();

        if (slot_mat[0] == -1)
            output.append(" Obsidian");
        if (slot_mat[1] == -1)
            output.append(" Piston");
        if (slot_mat[2] == -1)
            output.append(" Crystals");
        if (slot_mat[3] == -1)
            output.append(" Redstone");
        if (antiWeakness.get_value(true) && slot_mat[4] == -1)
            output.append(" Sword");
        if (redstoneBlockMode && slot_mat[5] == -1)
            output.append(" Pick");

        return output.toString();
    }

    // Get time for 3 crystals
    private void printTimeCrystals() {
        endTime = System.currentTimeMillis();
        MessageUtil.send_client_message("3 crystal, time took: " + (endTime - startTime));
        nCrystal = 0;
        startTime = System.currentTimeMillis();
    }

    // Actual break crystal
    private void breakCrystalPiston (Entity crystal) {
        // HitDelay
        if (hitTryTick++ < midHitDelay.get_value(1))
            return;
        else
            hitTryTick = 0;
        // If weaknes
        if (antiWeakness.get_value(true))
            mc.player.inventory.currentItem = slot_mat[4];
        // If rotate
        if (rotate.get_value(true)) {
            lookAtPacket(crystal.posX, crystal.posY, crystal.posZ, mc.player);
        }
        /// Break type
        // Swing
        if (breakType.in("Swing")) {
            breakCrystal(crystal);
            // Packet
        }else if(breakType.in("Packet")) {
            try {
                mc.player.connection.sendPacket(new CPacketUseEntity(crystal));
                mc.player.swingArm(EnumHand.MAIN_HAND);
            }catch(NullPointerException ignored ) {

            }
        }
        // Rotate
        if (rotate.get_value(true))
            resetRotation();
    }

    // Break redstone torch
    private boolean breakRedstone() {
        BlockPos offsetPosPist = new BlockPos(toPlace.to_place.get(toPlace.supportBlock + 2));
        BlockPos pos = new BlockPos(aimTarget.getPositionVector()).add(offsetPosPist.getX(), offsetPosPist.getY(), offsetPosPist.getZ());
        if (!(get_block(pos.getX(), pos.getY() , pos.getZ()) instanceof BlockAir)) {
            breakBlock(pos);
            return false;
        }
        return true;
    }

    // Algo for breaking a block
    private void breakBlock(BlockPos pos) {
        // If we have a redstone block
        if (redstoneBlockMode) {
            // Switch to the pick
            mc.player.inventory.currentItem = slot_mat[5];
        }
        EnumFacing side = BlockInteractionHelper.getPlaceableSide(pos);
        if (side != null) {
            // If rotate, look at the redstone torch
            if (rotate.get_value(true)) {
                BlockPos neighbour = pos.offset(side);
                EnumFacing opposite = side.getOpposite();
                Vec3d hitVec = new Vec3d(neighbour).add(0.5, 1, 0.5).add(new Vec3d(opposite.getDirectionVec()).scale(0.5));
                BlockInteractionHelper.faceVectorPacketInstant(hitVec);

            }
            // Destroy it
            mc.player.swingArm(EnumHand.MAIN_HAND);
            mc.player.connection.sendPacket(new CPacketPlayerDigging(
                    CPacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, side
            ));
            mc.player.connection.sendPacket(new CPacketPlayerDigging(
                    CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK, pos, side
            ));
        }
    }

    // Check if the piston has been placed
    private boolean checkPistonPlace() {
        // Check for the piston 255 3 -56
        BlockPos targetPosPist = compactBlockPos(1);
        if (!(get_block(targetPosPist.getX(), targetPosPist.getY(), targetPosPist.getZ()) instanceof BlockPistonBase)) {
            // Go back placing the piston
            stage--;
            return false;
        }else return true;
    }

    // Check if the crystal has been placed
    private boolean checkCrystalPlace() {
        // Check if the crystal has been placed
        for (Entity t : mc.world.loadedEntityList) {
            // If is an endcrystal
            if (t instanceof EntityEnderCrystal
                    // If the position is right
                    && (int) t.posX == (int) (aimTarget.posX + toPlace.to_place.get(toPlace.supportBlock + 1).x) &&
                    (int) t.posZ == (int) (aimTarget.posZ + toPlace.to_place.get(toPlace.supportBlock + 1).z)) {
                return true;
            }
        }

        stage--;

        return false;
    }

    // Place the supports blocks
    private boolean placeSupport() {
        // N^ blocks checked
        int checksDone = 0;
        // N^ blocks placed
        int blockDone = 0;
        // If we have to place
        if (toPlace.supportBlock > 0) {
            // Lets iterate and check
            // Lets finish
            do {
                BlockPos targetPos = getTargetPos(checksDone);

                // Try to place the block
                if (placeBlock(targetPos, 0, 0, 0, 1)) {
                    // If we reached the limit
                    if (++blockDone == blocksPerTick.get_value(1))
                        // Return false
                        return false;
                }

            // If we reached the limit, exit
            } while (++checksDone != toPlace.supportBlock);
        }
        stage = stage == 0 ? 1 : stage;
        return true;
    }

    // Place a block
    private boolean placeBlock(BlockPos pos, int step, double offsetX, double offsetZ, double offsetY){
        // Get the block
        Block block = mc.world.getBlockState(pos).getBlock();
        // Get all sides
        EnumFacing side = BlockInteractionHelper.getPlaceableSide(pos);

        // If there is a solid block
        if (!(block instanceof BlockAir) && !(block instanceof BlockLiquid)){
            return false;
        }
        // If we cannot find any side
        if (side == null){
            return false;
        }

        // Get position of the side
        BlockPos neighbour = pos.offset(side);
        EnumFacing opposite = side.getOpposite();


        // If that block can be clicked
        if (!BlockInteractionHelper.canBeClicked(neighbour)){
            return false;
        }

        // Get the position where we are gonna click
        Vec3d hitVec = new Vec3d(neighbour).add(0.5 + offsetX, offsetY, 0.5 + offsetZ).add(new Vec3d(opposite.getDirectionVec()).scale(0.5));
        Block neighbourBlock = mc.world.getBlockState(neighbour).getBlock();

        /*
			// I use this as a remind to which index refers to what
			0 => obsidian
			1 => piston
			2 => Crystals
			3 => redstone
		 */
        // Get what slot we are going to select
        // If it's not empty
        try {
            if (slot_mat[step] == 11 || mc.player.inventory.getStackInSlot(slot_mat[step]) != ItemStack.EMPTY) {
                // Is it is correct
                if (mc.player.inventory.currentItem != slot_mat[step]) {
                    // Change the hand's item (è qui l'errore)
                    mc.player.inventory.currentItem = slot_mat[step] == 11 ? mc.player.inventory.currentItem : slot_mat[step];
                }
            } else {
                noMaterials = true;
                return false;
            }
        }catch (Exception e) {
            MessageUtil.send_client_message("Fatal Error during the creation of the structure. Please, report this bug in the gamesense disc server");
            final Logger LOGGER = LogManager.getLogger("OzarkClient");
            LOGGER.error("[PistonCrystal] error during the creation of the structure.");
            if (e.getMessage() != null)
                LOGGER.error("[PistonCrystal] error message: " + e.getClass().getName() + " " + e.getMessage());
            else
                LOGGER.error("[PistonCrystal] cannot find the cause");
            int i5 = 0;

            if (e.getStackTrace().length != 0) {
                LOGGER.error("[PistonCrystal] StackTrace Start");
                for (StackTraceElement errorMess : e.getStackTrace()) {
                    LOGGER.error("[PistonCrystal] " + errorMess.toString());
                }
                LOGGER.error("[PistonCrystal] StackTrace End");
            }
            MessageUtil.send_client_message(Integer.toString(step));
            set_disable();
        }

        // Why?
        if (!isSneaking && BlockInteractionHelper.blackList.contains(neighbourBlock) || BlockInteractionHelper.shulkerList.contains(neighbourBlock)){
            mc.player.connection.sendPacket(new CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING));
            isSneaking = true;
        }

        // For the rotation
        if (rotate.get_value(true) || step == 1){
            Vec3d positionHit = hitVec;
            // if rotate and we are not in rotate
            if (!rotate.get_value(true) && step == 1) {
                // Dont look at the block but just the direction
                positionHit = new Vec3d(mc.player.posX + offsetX, mc.player.posY + (offsetY == -1 ? offsetY : 0), mc.player.posZ + offsetZ);
            }
            // Look
            BlockInteractionHelper.faceVectorPacketInstant(positionHit);
        }
        // If we are placing with the main hand
        EnumHand handSwing = EnumHand.MAIN_HAND;
        // If we are placing with the offhand
        if (slot_mat[step] == 11)
            handSwing = EnumHand.OFF_HAND;

        // Place the block
        mc.playerController.processRightClickBlock(mc.player, mc.world, neighbour, opposite, hitVec, handSwing);
        mc.player.swingArm(handSwing);

        return true;
    }

    // Given a step, place the block
    public void placeBlockThings(int step) {
        // Get absolute position
        BlockPos targetPos = compactBlockPos(step);
        // Place 93 4 -29
        placeBlock(targetPos, step, toPlace.offsetX, toPlace.offsetZ, toPlace.offsetY);
        // Next step
        stage++;
    }

    // Given a step, return the absolute block position
    public BlockPos compactBlockPos(int step) {
        // Get enemy's relative position of the block
        BlockPos offsetPos = new BlockPos(toPlace.to_place.get(toPlace.supportBlock + step - 1));
        // Get absolute position and return
        return new BlockPos(enemyCoordsDouble[0] + offsetPos.getX(), enemyCoordsDouble[1] + offsetPos.getY(), enemyCoordsDouble[2] + offsetPos.getZ());

    }

    // Given a index of a block, get the target position (this is used for support blocks)
    private BlockPos getTargetPos(int idx) {
        BlockPos offsetPos = new BlockPos(toPlace.to_place.get(idx));
        return new BlockPos(enemyCoordsDouble[0] + offsetPos.getX(), enemyCoordsDouble[1] + offsetPos.getY(), enemyCoordsDouble[2] + offsetPos.getZ());
    }

    // Check if we have to disable
    private boolean checkVariable() {
        // If something went wrong
        if (noMaterials || !isHole || !enoughSpace || hasMoved || deadPl || rotationPlayerMoved){
            set_disable();
            return true;
        }
        return false;
    }

    // Class for the structure
    private static class structureTemp {
        public double distance;
        public int supportBlock;
        public List<Vec3d> to_place;
        public int direction;
        public float offsetX;
        public float offsetY;
        public float offsetZ;

        public structureTemp(double distance, int supportBlock, List<Vec3d> to_place) {
            this.distance = distance;
            this.supportBlock = supportBlock;
            this.to_place = to_place;
            this.direction = -1;
        }

        public void replaceValues(double distance, int supportBlock, List<Vec3d> to_place, int direction, float offsetX, float offsetZ, float offsetY) {
            this.distance = distance;
            this.supportBlock = supportBlock;
            this.to_place = to_place;
            this.direction = direction;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
            this.offsetY = offsetY;
        }
    }

    // Create the skeleton of the structure
    private boolean createStructure() {
        /*
            I decided to split rotate and non rotate when needed.
            I wont do like i did in the last pistonCrystal that was all toogheter
            (yes, it's less code, but it's more complex with all in one)
         */
        /// Create default start value
        // Structure default
        structureTemp addedStructure = new structureTemp(Double.MAX_VALUE, 0, null);
        // Distance we are going to find
        double distanceNowCrystal;
        // Since they may happens some errors that i did not expect, i use a try-catch
        try {
            // First check, h check.
            if (   meCoordsInt[1] - enemyCoordsInt[1] > -1
                && meCoordsInt[1] - enemyCoordsInt[1] <= maxYincr.get_value(1)) {
                // Because we have 2 option for the place (1 block higher or same h)
                // We have to iterete between these 2 options. The first one, is the higher
                for(int startH = 1; startH >= 0; startH--) {
                    // If there was a no before or we found nothing
                    if (addedStructure.to_place == null) {
                        /// If we are above the enemy, we have to create al the structure.
                        // How much we are going above
                        int incr = 0;
                        // Blocks we are going to add
                        List<Vec3d> highSup = new ArrayList <> ( );
                        // Create the structure until it's on us
                        while (meCoordsInt[1] > enemyCoordsInt[1] + incr) {
                            incr++;
                            for (int[] cordSupport : disp_surblock)
                                highSup.add(new Vec3d(cordSupport[0], incr, cordSupport[2]));
                        }
                        // We go 1 block above (+1) and startH (if we looking 1 block above or not)
                        incr += startH;
                        // After, we are going to do a foreach. But we need also an index, so here
                        int i = -1;

                        // Lets iterate for every 4 positions
                        for(Double[] cord_b : sur_block) {
                            /// Note:
                            /*
                                Abs = Absolute
                                Rel = Relative
                             */
                            /*
                                Since they are a lot of if, i prefer keeping them
                                separated but, also, on the same tab.
                                I'll use "continue"
                             */
                            i++;
                            /// Crystal Coordinates ///
                            // Init + Get
                            double[] crystalCordsAbs = {cord_b[0], cord_b[1] + incr, cord_b[2]};
                            int[] crystalCordsRel = {disp_surblock[i][0], disp_surblock[i][1] + incr, disp_surblock[i][2]};

                            /// Crystal Position Checks ///
                            // Check, first of all, the distance
                            if (!((distanceNowCrystal = mc.player.getDistance(crystalCordsAbs[0], crystalCordsAbs[1], crystalCordsAbs[2])) < addedStructure.distance))
                                continue;
                            // Check if there is enough space
                            if  ((!((get_block(crystalCordsAbs[0], crystalCordsAbs[1], crystalCordsAbs[2])) instanceof BlockAir)
                                || !((get_block(crystalCordsAbs[0], crystalCordsAbs[1] + 1, crystalCordsAbs[2])) instanceof BlockAir)))
                                continue;
                            // Check if someone is in that block
                            if (someoneInCoords(crystalCordsAbs[0], crystalCordsAbs[2]))
                                continue;
                            /// Piston Coordinates ///
                            // Init
                            double[] pistonCordAbs = new double[3];
                            int[] pistonCordRel = new int[3];
                            Block tempBlock;

                            /// Piston Position ///
                            // If rotation or not betterplacement
                            if (rotate.get_value(true) || !betterPlacement.get_value(true)) {
                                pistonCordAbs = new double[] { crystalCordsAbs[0] + disp_surblock[i][0], crystalCordsAbs[1], crystalCordsAbs[2] + disp_surblock[i][2] };
                                /// If statements ///
                                // If it's not air or piston and if someone is here
                                if ((tempBlock = get_block(pistonCordAbs[0], pistonCordAbs[1], pistonCordAbs[2])) instanceof BlockPistonBase
                                     == tempBlock instanceof BlockAir
                                    || someoneInCoords(pistonCordAbs[0], pistonCordAbs[2]))
                                    // Exit
                                    continue;
                                // Get relative coords
                                pistonCordRel = new int[] { crystalCordsRel[0] * 2, crystalCordsRel[1], crystalCordsRel[2] * 2};

                            } else {
                                /// Try to find the best for distance
                                // Min distance found
                                double distancePist = Double.MAX_VALUE;
                                double distanceNowPiston;
                                // Iterate for every 4 blocks
                                for (int[] disp : disp_surblock) {
                                    // Get Position
                                    BlockPos blockPiston = new BlockPos(crystalCordsAbs[0] + disp[0], crystalCordsAbs[1], crystalCordsAbs[2] + disp[2]);
                                    /// If statements ////
                                    // If distance is not ok ok
                                    if ((distanceNowPiston = mc.player.getDistanceSqToCenter(blockPiston)) > distancePist)
                                        continue;
                                    // If it's not air or piston and if someone is here
                                    if (!(     get_block(blockPiston.getX(), blockPiston.getY(), blockPiston.getZ()) instanceof BlockPistonBase
                                            || get_block(blockPiston.getX(), blockPiston.getY(), blockPiston.getZ()) instanceof BlockAir)
                                            || someoneInCoords(crystalCordsAbs[0] + disp[0], crystalCordsAbs[2] + disp[2]))
                                        continue;
                                    // The block in front of the piston should be air
                                    if (!(get_block(blockPiston.getX() - crystalCordsRel[0], blockPiston.getY(), blockPiston.getZ() - crystalCordsRel[2]) instanceof BlockAir))
                                        continue;
                                    // Add new coordinates
                                    distancePist = distanceNowPiston;
                                    pistonCordAbs = new double[] { crystalCordsAbs[0] + disp[0], crystalCordsAbs[1], crystalCordsAbs[2] + disp[2] };
                                    pistonCordRel = new int[] { crystalCordsRel[0] + disp[0], crystalCordsRel[1], crystalCordsRel[2] + disp[2]};
                                }
                                // If it change nothing
                                if (distancePist == Double.MAX_VALUE)
                                    continue;
                               }

                            // Checks in case you have rotate on
                            if (rotate.get_value(true)) {
                                int[] pistonCordInt = new int[] {(int) pistonCordAbs[0], (int) pistonCordAbs[1], (int) pistonCordAbs[2]};
                                /*
                                    We cannot allow these options:
                                    If we are behind the piston
                                    If we are in a vertex of a square with center the enemy
                                    If we are 3 blocks away and not in the same x/z
                                 */
                                // If we are behind
                                boolean behindBol = false;
                                for(int checkBehind : new int[] {0,2})
                                    // If we have an axis that is the same
                                    if (meCoordsInt[checkBehind] == pistonCordInt[checkBehind]) {
                                        int idx = checkBehind == 2 ? 0 : 2;
                                        // If we are behind
                                        if (pistonCordInt[idx] >= enemyCoordsInt[idx] == meCoordsInt[idx] >= enemyCoordsInt[idx])
                                            behindBol = true;
                                    }
                                // If we are in a quarter
                                if (!behindBol && Math.abs(meCoordsInt[0] - enemyCoordsInt[0]) == 2 && Math.abs(meCoordsInt[2] - enemyCoordsInt[2]) == 2) {
                                    // If one of the two coords are the same and the distance is >= 2
                                    if ( (meCoordsInt[0] == pistonCordInt[0] && (Math.abs(meCoordsInt[2] - pistonCordInt[2]) >= 2)
                                        || (meCoordsInt[2] == pistonCordInt[2] && (Math.abs(meCoordsInt[0] - pistonCordInt[0]) >= 2))) )
                                        behindBol = true;
                                }
                                // If our distance is more then 3 blocks (and one coordinate is different), exit
                                if (!behindBol && (Math.abs(meCoordsInt[0] - enemyCoordsInt[0]) > 2 && meCoordsInt[2] != enemyCoordsInt[2])
                                    || (Math.abs(meCoordsInt[2] - enemyCoordsInt[2]) > 2 && meCoordsInt[0] != enemyCoordsInt[0]))
                                    behindBol = true;
                                // Exit
                                if (behindBol)
                                    continue;

                            }

                            /// Redstone Coordinates
                            double[] redstoneCoordsAbs = new double[3];
                            int[] redstoneCoordsRel = new int[3];
                            double minFound = Double.MAX_VALUE;
                            double minNow = -1;
                            boolean foundOne = true;

                            // Iterate for all 4 positions
                            for(int[] pos : disp_surblock) {
                                // Get coordinates
                                double[] torchCoords = new double[]{pistonCordAbs[0] + pos[0], pistonCordAbs[1], pistonCordAbs[2] + pos[2]};
                                // If it's min of what we have now
                                if ((minNow = mc.player.getDistance(torchCoords[0], torchCoords[1], torchCoords[2])) >= minFound)
                                    continue;
                                // if it's a redstone block, lets remove all the sides
                                if (redstoneBlockMode && !(pos[0] == crystalCordsRel[0]))
                                    continue;
                                /*
                                tempBlock = get_block(pistonCordAbs[0], pistonCordAbs[1], pistonCordAbs[2])) instanceof BlockPistonBase
                                     == tempBlock instanceof BlockAir
                                 */
                                // Check if: Someone is here, if it's air, if it's the position of the crystal
                                if (    someoneInCoords(torchCoords[0], torchCoords[2])
                                    || !(   get_block(torchCoords[0], torchCoords[1], torchCoords[2]) instanceof BlockRedstoneTorch
                                         || get_block(torchCoords[0], torchCoords[1], torchCoords[2]) instanceof BlockAir )
                                    || (int) torchCoords[0] == (int) crystalCordsAbs[0] && (int) torchCoords[2] == (int) crystalCordsAbs[2]) {
                                    continue;
                                }
                                // If the redstone torchs is in front of the piston
                                boolean torchFront = false;
                                for(int part : new int[] {0,2}) {
                                    int contPart = part == 0 ? 2 : 0;
                                    if ((int) torchCoords[contPart] == (int) pistonCordAbs[contPart] && (int) torchCoords[part] == enemyCoordsInt[part])
                                        torchFront = true;
                                }
                                if (torchFront)
                                    continue;
                                redstoneCoordsAbs = new double[] {torchCoords[0], torchCoords[1], torchCoords[2]};
                                redstoneCoordsRel = new int[] {pistonCordRel[0] + pos[0], pistonCordRel[1], pistonCordRel[2] + pos[2]};
                                foundOne = false;
                                minFound = minNow;
                            }
                            if (foundOne)
                                continue;

                            /// Create structure ///
                            // If fast mode
                            if (redstoneBlockMode && allowCheapMode.get_value(true)) {
                                /// Check if it's possible
                                // First, lets if there is space for the redstone block
                                if (get_block(redstoneCoordsAbs[0], redstoneCoordsAbs[1] - 1, redstoneCoordsAbs[2]) instanceof BlockAir) {
                                    /// We can change everything
                                    // Piston
                                    pistonCordAbs = new double[] {redstoneCoordsAbs[0], redstoneCoordsAbs[1], redstoneCoordsAbs[2]};
                                    pistonCordRel = new int[] {redstoneCoordsRel[0], redstoneCoordsRel[1], redstoneCoordsRel[2]};
                                    // Redstone block
                                    redstoneCoordsAbs = new double[] {redstoneCoordsAbs[0], redstoneCoordsAbs[1] - 1, redstoneCoordsRel[2]};
                                    redstoneCoordsRel = new int[] {redstoneCoordsRel[0], redstoneCoordsRel[1] - 1, redstoneCoordsRel[2]};
                                    // Active fastMode
                                    fastModeActive = true;
                                }
                            }

                            /// Create the structure
                            // Skeleton
                            List<Vec3d> toPlaceTemp = new ArrayList<>();
                            int supportBlock = 0;

                            /// Lets check if, under the crystal, piston and redstone there is a solid block
                            // Crystal
                            if(get_block(crystalCordsAbs[0], crystalCordsAbs[1] - 1, crystalCordsAbs[2]) instanceof BlockAir) {
                                toPlaceTemp.add(new Vec3d(crystalCordsRel[0], crystalCordsRel[1] - 1, crystalCordsRel[2]));
                                supportBlock++;
                            }
                            // Piston
                            if(!fastModeActive && get_block(pistonCordAbs[0], pistonCordAbs[1] - 1, pistonCordAbs[2]) instanceof BlockAir) {
                                toPlaceTemp.add(new Vec3d(pistonCordRel[0], pistonCordRel[1] - 1, pistonCordRel[2]));
                                supportBlock++;
                            }
                            // Redstone

                            if (!fastModeActive) {
                                if (!redstoneBlockMode && get_block(redstoneCoordsAbs[0], redstoneCoordsAbs[1] - 1, redstoneCoordsAbs[2]) instanceof BlockAir) {
                                    toPlaceTemp.add(new Vec3d(redstoneCoordsRel[0], redstoneCoordsRel[1] - 1, redstoneCoordsRel[2]));
                                    supportBlock++;
                                }
                            }else {
                                if (get_block(redstoneCoordsAbs[0] - crystalCordsRel[0], redstoneCoordsAbs[1] - 1, redstoneCoordsAbs[2] - crystalCordsRel[2]) instanceof BlockAir) {
                                    toPlaceTemp.add(new Vec3d(redstoneCoordsRel[0] - crystalCordsRel[0], redstoneCoordsRel[1], redstoneCoordsRel[2] - crystalCordsRel[2]));
                                    supportBlock++;
                                }
                            }


                            /// Add all others blocks
                            // Piston
                            toPlaceTemp.add(new Vec3d(pistonCordRel[0], pistonCordRel[1], pistonCordRel[2]));
                            // Crystal
                            toPlaceTemp.add(new Vec3d(crystalCordsRel[0], crystalCordsRel[1], crystalCordsRel[2]));
                            // Redstone
                            toPlaceTemp.add(new Vec3d(redstoneCoordsRel[0], redstoneCoordsRel[1], redstoneCoordsRel[2]));

                            // If we are above the enemy
                            if (incr > 1) {
                                // Lets add everything
                                for (int i2 = 0; i2 < highSup.size(); i2++) {
                                    toPlaceTemp.add(0, highSup.get(i2));
                                    supportBlock++;
                                }
                            }

                            /// Rotation calculation
                            float offsetX, offsetZ, offsetY;
                            // If horrizontaly
                            if (disp_surblock[i][0] != 0) {
                                offsetX = rotate.get_value(true) ? disp_surblock[i][0] / 2f : disp_surblock[i][0];
                                // Check which is better for distance
                                if (rotate.get_value(true)) {
                                    if (mc.player.getDistanceSq(pistonCordAbs[0], pistonCordAbs[1], pistonCordAbs[2] + 0.5) > mc.player.getDistanceSq(pistonCordAbs[0], pistonCordAbs[1], pistonCordAbs[2] - 0.5))
                                        offsetZ = -0.5f;
                                    else
                                        offsetZ = 0.5f;
                                } else offsetZ = disp_surblock[i][2];
                                // If vertically
                            } else {
                                offsetZ = rotate.get_value(true) ? disp_surblock[i][2] / 2f : disp_surblock[i][2];
                                // Check which is better for distance
                                if (rotate.get_value(true)) {
                                    if (mc.player.getDistanceSq(pistonCordAbs[0] + 0.5, pistonCordAbs[1], pistonCordAbs[2]) > mc.player.getDistanceSq(pistonCordAbs[0] - 0.5, pistonCordAbs[1], pistonCordAbs[2]))
                                        offsetX = -0.5f;
                                    else
                                        offsetX = 0.5f;
                                } else offsetX = disp_surblock[i][0];
                            }
                            /// Calculate the y offset.
                            // If we are above, 1, if we are belove, 0
                            offsetY = meCoordsInt[1] - enemyCoordsInt[1] == -1 ? 0 : 1;

                            // Repleace the structure
                            addedStructure.replaceValues(distanceNowCrystal, supportBlock, toPlaceTemp, -1, offsetX, offsetZ, offsetY);

                            // If trapPlayer
                            if (blockPlayer.get_value(true)) {
                                // Get the values
                                Vec3d valuesStart = addedStructure.to_place.get(addedStructure.supportBlock + 1);
                                // Get the opposit
                                int[] valueBegin = new int[]{(int) -valuesStart.x, (int) valuesStart.y, (int) -valuesStart.z};
                                // Add
                                if (!bypassObsidian.get_value(true) || (int) mc.player.posY == enemyCoordsInt[1]) {
                                    addedStructure.to_place.add(0, new Vec3d(0, incr + 1, 0));
                                    addedStructure.to_place.add(0, new Vec3d(valueBegin[0], incr + 1, valueBegin[2]));
                                    addedStructure.to_place.add(0, new Vec3d(valueBegin[0], incr, valueBegin[2]));
                                    addedStructure.supportBlock += 3;
                                }else {
                                    addedStructure.to_place.add(0, new Vec3d(0, incr, 0));
                                    addedStructure.to_place.add(0, new Vec3d(valueBegin[0], incr, valueBegin[2]));
                                    addedStructure.supportBlock += 2;
                                }
                            }
                            toPlace = addedStructure;
                        }
                    }
                }
            }
            // Error h non compatible
            else
            yUnder = true;

        }
        catch (Exception e) {
            MessageUtil.send_client_message("Fatal Error during the creation of the structure. Please, report this bug in the GameSense discord server");
            final Logger LOGGER = LogManager.getLogger("OzarkClient");
            LOGGER.error("[PistonCrystal] error during the creation of the structure.");
            if (e.getMessage() != null)
                LOGGER.error("[PistonCrystal] error message: " + e.getClass().getName() + " " + e.getMessage());
            else
                LOGGER.error("[PistonCrystal] cannot find the cause");
            int i5 = 0;

            if (e.getStackTrace().length != 0) {
                LOGGER.error("[PistonCrystal] StackTrace Start");
                for(StackTraceElement errorMess : e.getStackTrace()) {
                    LOGGER.error("[PistonCrystal] " + errorMess.toString());
                }
                LOGGER.error("[PistonCrystal] StackTrace End");
            }

            if (aimTarget != null) {
                LOGGER.error("[PistonCrystal] closest target is not null");
            }else LOGGER.error("[PistonCrystal] closest target is null somehow");
            for (Double[] cord_b : sur_block) {
                if (cord_b != null) {
                    LOGGER.error("[PistonCrystal] " + i5 + " is not null");
                }else {
                    LOGGER.error("[PistonCrystal] " + i5 + " is null");
                }
                i5++;
            }

        }


        return addedStructure.to_place != null;
    }

    public static boolean someoneInCoords(double x, double z) {
        int xCheck = (int) x;
        int zCheck = (int) z;
        // Get player's list
        List<EntityPlayer> playerList = mc.world.playerEntities;
        // Iterate
        for(EntityPlayer player : playerList) {
            // I dont think you need to check also the yLevel
            if ((int) player.posX == xCheck && (int) player.posZ == zCheck)
                return true;
        }

        return false;
    }

    // Get all the materials
    private boolean getMaterialsSlot() {
		/*
			// I use this as a remind to which index refers to what
			0 => obsidian
			1 => piston
			2 => Crystals
			3 => redstone
			4 => sword
			5 => pick
		 */

        if (mc.player.getHeldItemOffhand().getItem() instanceof ItemEndCrystal) {
            slot_mat[2] = 11;
        }

        if (placeMode.in("Block"))
            redstoneBlockMode = true;

        // Iterate for all the inventory
        for(int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);

            // If there is no block
            if (stack == ItemStack.EMPTY){
                continue;
            }
            // If endCrystal
            if (slot_mat[2] == -1 && stack.getItem() instanceof ItemEndCrystal) {
                slot_mat[2] = i;
            // If sword
            }else if (antiWeakness.get_value(true) && stack.getItem() instanceof ItemSword) {
                slot_mat[4] = i;
            }else
            // If Pick
            if (stack.getItem() instanceof ItemPickaxe) {
                slot_mat[5] = i;
            }
            if (stack.getItem() instanceof ItemBlock){

                // If yes, get the block
                Block block = ((ItemBlock) stack.getItem()).getBlock();

                // Obsidian
                if (block instanceof BlockObsidian) {
                    slot_mat[0] = i;
                } else
                    // PistonBlock
                    if (block instanceof BlockPistonBase) {
                        slot_mat[1] = i;
                    } else
                        // RedstoneTorch / RedstoneBlock
                        if (!placeMode.in("Block") && block instanceof BlockRedstoneTorch) {
                            slot_mat[3] = i;
                            redstoneBlockMode = false;
                        }
                        else if (!placeMode.in("Torch") && block.translationKey.equals("blockRedstone")) {
                            slot_mat[3] = i;
                            redstoneBlockMode = true;
                        }
            }
        }
        if (!redstoneBlockMode)
            slot_mat[5] = -1;
        // Count what we found
        int count = 0;
        for(int val : slot_mat) {
            if (val != -1)
                count++;
        }

        // If we have everything we need, return true
        return count >= 4 + (antiWeakness.get_value(true) ? 1 : 0) + (redstoneBlockMode ? 1 : 0);

    }

    // Check if it's in a hole
    private boolean is_in_hole() {
        sur_block = new Double[][] {
                {aimTarget.posX + 1, aimTarget.posY, aimTarget.posZ},
                {aimTarget.posX - 1, aimTarget.posY, aimTarget.posZ},
                {aimTarget.posX, aimTarget.posY, aimTarget.posZ + 1},
                {aimTarget.posX, aimTarget.posY, aimTarget.posZ - 1}
        };

        
        // Check if the guy is in a hole
        return !(get_block(sur_block[0][0], sur_block[0][1], sur_block[0][2]) instanceof BlockAir) &&
                !(get_block(sur_block[1][0], sur_block[1][1], sur_block[1][2]) instanceof BlockAir) &&
                !(get_block(sur_block[2][0], sur_block[2][1], sur_block[2][2]) instanceof BlockAir) &&
                !(get_block(sur_block[3][0], sur_block[3][1], sur_block[3][2]) instanceof BlockAir);
    }

    // Get block from coordinates
    private Block get_block(double x, double y, double z) {
        return mc.world.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    // Find player you are looking
    public static EntityPlayer findLookingPlayer(double rangeMax) {
        // Get player
        ArrayList<EntityPlayer> listPlayer = new ArrayList<>();
        // Only who is in a distance of enemyRange
        for(EntityPlayer playerSin : mc.world.playerEntities) {
            if (basicChecksEntity(playerSin))
                continue;
            if (mc.player.getDistance(playerSin) <= rangeMax) {
                listPlayer.add(playerSin);
            }
        }

        EntityPlayer target = null;
        // Get coordinate eyes + rotation
        Vec3d positionEyes = mc.player.getPositionEyes(mc.getRenderPartialTicks());
        Vec3d rotationEyes = mc.player.getLook(mc.getRenderPartialTicks());
        // Precision
        int precision = 2;
        // Iterate for every blocks
        for(int i = 0; i < (int) rangeMax; i++) {
            // Iterate for the precision
            for(int j = precision; j > 0 ; j--) {
                // Iterate for all players
                for(EntityPlayer targetTemp : listPlayer) {
                    // Get box of the player
                    AxisAlignedBB playerBox = targetTemp.getEntityBoundingBox();
                    // Get coordinate of the vec3d
                    double xArray = positionEyes.x + (rotationEyes.x * i) + rotationEyes.x/j;
                    double yArray = positionEyes.y + (rotationEyes.y * i) + rotationEyes.y/j;
                    double zArray = positionEyes.z + (rotationEyes.z * i) + rotationEyes.z/j;
                    // If it's inside
                    if (   playerBox.maxY >= yArray && playerBox.minY <= yArray
                            && playerBox.maxX >= xArray && playerBox.minX <= xArray
                            && playerBox.maxZ >= zArray && playerBox.minZ <= zArray) {
                        // Get target
                        target = targetTemp;
                    }
                }
            }
        }

        return target;
    }

    // Basic checks for an entity
    public static boolean basicChecksEntity(Entity pl) {
        return pl == mc.player || FriendUtil.isFriend(pl.getName()) || pl.isDead;
    }

    // Find closest target
    public static EntityPlayer findClosestTarget(double rangeMax, EntityPlayer aimTarget){
        List<EntityPlayer> playerList = mc.world.playerEntities;

        EntityPlayer closestTarget_test = null;

        for (EntityPlayer entityPlayer : playerList){

            if (basicChecksEntity(entityPlayer))
                continue;

            if (aimTarget == null && mc.player.getDistance(entityPlayer) <= rangeMax){
                closestTarget_test = entityPlayer;
                continue;
            }
            if (aimTarget != null && mc.player.getDistance(entityPlayer) <= rangeMax && mc.player.getDistance(entityPlayer) < mc.player.getDistance(aimTarget)){
                closestTarget_test = entityPlayer;
            }
        }
        return closestTarget_test;
    }

    /// AutoCrystal break things ///
    public static void lookAtPacket(double px, double py, double pz, EntityPlayer me) {
        double[] v = calculateLookAt(px, py, pz, me);
        setYawAndPitch((float) v[0], (float) v[1]);
    }
    public static double[] calculateLookAt(double px, double py, double pz, EntityPlayer me) {
        double dirx = me.posX - px;
        double diry = me.posY - py;
        double dirz = me.posZ - pz;

        double len = Math.sqrt(dirx*dirx + diry*diry + dirz*dirz);

        dirx /= len;
        diry /= len;
        dirz /= len;

        double pitch = Math.asin(diry);
        double yaw = Math.atan2(dirz, dirx);

        pitch = pitch * 180.0d / Math.PI;
        yaw = yaw * 180.0d / Math.PI;

        yaw += 90f;

        return new double[]{yaw,pitch};
    }
    private static boolean isSpoofingAngles;
    private static double yaw;
    private static double pitch;

    public static void setYawAndPitch(float yaw1, float pitch1) {
        yaw = yaw1;
        pitch = pitch1;
        isSpoofingAngles = true;
    }
    public static void breakCrystal(Entity crystal) {
        try {
            mc.playerController.attackEntity(mc.player, crystal);
            mc.player.swingArm(EnumHand.MAIN_HAND);
        }catch (NullPointerException ignored ) {

        }
    }

    public static void resetRotation() {
        if (isSpoofingAngles) {
            yaw = mc.player.rotationYaw;
            pitch = mc.player.rotationPitch;
            isSpoofingAngles = false;
        }
    }

    @EventHandler
    private final Listener<EventPacket.SendPacket> packetSendListener = new Listener<>(event -> {
        Packet packet = event.get_packet();
        if (packet instanceof CPacketPlayer) {
            if (isSpoofingAngles) {
                ((CPacketPlayer) packet).yaw = (float) yaw;
                ((CPacketPlayer) packet).pitch = (float) pitch;
            }
        }
    });
}