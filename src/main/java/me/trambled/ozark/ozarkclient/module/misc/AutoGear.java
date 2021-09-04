package me.trambled.ozark.ozarkclient.module.misc;

import me.trambled.ozark.ozarkclient.command.commands.AutoGearCommand;
import me.trambled.ozark.ozarkclient.module.Category;
import me.trambled.ozark.ozarkclient.module.Module;
import me.trambled.ozark.ozarkclient.module.Setting;
import me.trambled.ozark.ozarkclient.util.misc.MessageUtil;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.ContainerShulkerBox;
import net.minecraft.item.ItemStack;

import java.util.*;

/**
 * @Author TechAle
 */

/*
    The algo is the same as AutoSort, just with few difference because of how inventory works
 */



public class AutoGear extends Module {

    public AutoGear() {

        super(Category.MISC);

        this.name = "AutoGear";
        this.tag = "AutoGear";
        this.description = "Sorts ur inv.";
    }

	Setting tickDelay = create("Delay","AutoGearTickDelay",0, 0, 20);
	Setting chatMsg = create("Chat MSG","AutoGearChatMsgs",true);
	Setting confirmSort = create("Confirm Sort","AutoGearConfirmSort",true);
	Setting invasive = create("Invasive","AutoGearInvasive",false);
	Setting enderChest = create("Ender Chest","AutoGearEchest",true);
	Setting closeAfter = create("Close After","AutoGearCloseAfter",true);
	Setting debugMode = create("Debug","AutoGearDebug",false);


	// Our inventory variables
	private HashMap<Integer, String> planInventory = new HashMap<>();
	private final HashMap<Integer, String> containerInv = new HashMap<>();
	// Sort item
	private ArrayList<Integer> sortItems = new ArrayList<>();

	// Tickets
	private int delayTimeTicks;
	private int stepNow;
	// If we had opened before a chest/inventory
	private boolean openedBefore,
			finishSort,
			doneBefore;


	@Override
	protected void enable() {
		// Msg
		if (chatMsg.get_value(true))
			MessageUtil.send_client_message("AutoSort Turned On!");
		// Get name of the config
		// Config variables
		String curConfigName = AutoGearCommand.getCurrentSet();
		// If none, exit
		if (curConfigName.equals("")) {
			disable();
			return;
		}
		// Print the config
		if (chatMsg.get_value(true))
			MessageUtil.send_client_message("Config " + curConfigName + " activated");
		// Get the inventory
		String inventoryConfig = AutoGearCommand.getInventoryKit(curConfigName);
		// If none, exit
		if (inventoryConfig.equals("")) {
			disable();
			return;
		}
		// Split it into array
		String[] inventoryDivided = inventoryConfig.split(" ");
		// Reset variables
		planInventory = new HashMap<>();
		HashMap<String, Integer> nItems = new HashMap<>();
		// Iterate for creating planInventory and nItems
		for(int i = 0; i < inventoryDivided.length; i++) {
			// Add to planInventory if it's not air
			if (!inventoryDivided[i].contains("air")) {
				// Add it
				planInventory.put(i, inventoryDivided[i]);
				// Lets add it to our list
				if (nItems.containsKey(inventoryDivided[i]))
					// If it exist, incr of 1
					nItems.put(inventoryDivided[i], nItems.get(inventoryDivided[i]) + 1);
				else
					// If it doesnt exist, add it with value 1
					nItems.put(inventoryDivided[i], 1);
			}
		}
		// Reset tickdelay
		delayTimeTicks = 0;
		// Reset opened
		openedBefore = doneBefore = false;
	}

	@Override
	public void update() {
		// Wait
		if (delayTimeTicks < tickDelay.get_value(1)) {
			delayTimeTicks++;
			return;
		} else {
			delayTimeTicks = 0;
		}

		// Since this is in the misc category, it did not turn off. This can cause some problems, so i have to turn it off manually with this
		if (planInventory.size() == 0)
			disable();

		// When you open an inventory
		if (((mc.player.openContainer instanceof ContainerChest && (enderChest.get_value(true) || !((ContainerChest) mc.player.openContainer).getLowerChestInventory().getDisplayName().getUnformattedText().equals("Ender Chest")))
				|| mc.player.openContainer instanceof ContainerShulkerBox)
		) {
			// Start sorting
			sortInventoryAlgo();
		}else openedBefore = false;

	}

	private void sortInventoryAlgo() {
		if (!openedBefore) {
			// Print
			if (chatMsg.get_value(true)&& !doneBefore)
				MessageUtil.send_client_message("Start sorting inventory...");

			int maxValue = mc.player.openContainer instanceof ContainerChest ? ((ContainerChest) mc.player.openContainer).getLowerChestInventory().getSizeInventory()
					: 27;
			// Iterate for every value
			for (int i = 0; i < maxValue; i++) {
				ItemStack item = mc.player.openContainer.getInventory().get(i);
				containerInv.put(i, Objects.requireNonNull(item.getItem().getRegistryName()).toString() + item.getMetadata());
			}
			openedBefore = true;
			// Copy of the inventory
			HashMap<Integer, String> inventoryCopy = getInventoryCopy(maxValue);
			// Copy of what we want
			HashMap<Integer, String> aimInventory = getInventoryCopy(maxValue, planInventory);
			// Start sorting items
			sortItems = getInventorySort(inventoryCopy, aimInventory, maxValue);
			// This is for doubleCheck
			if (sortItems.size() == 0 && !doneBefore) {
				// If we 0 items to sort + we have done it before
				finishSort = false;
				// Print
				if (chatMsg.get_value(true))
					MessageUtil.send_client_error_message("Inventory already sorted...");
				if (closeAfter.get_value(true))
					mc.player.closeScreen();
			}else {
				// Else, start sorting
				finishSort = true;
				stepNow = 0;
			}
			openedBefore = true;
		} else if (finishSort) {
			int slotChange;
			// This is the sort area
			if (sortItems.size() != 0) {
				// Get where we are now
				slotChange = sortItems.get(stepNow++);
				// Sort the inventory
				mc.playerController.windowClick(mc.player.openContainer.windowId, slotChange, 0, ClickType.PICKUP, mc.player);
			}
			// If we have at the limit
			if (stepNow == sortItems.size()) {
				// If confirm sort but we have not done yet
				if (confirmSort.get_value(true)) {
					if (!doneBefore) {
						// Reset
						openedBefore = false;
						finishSort = false;
						doneBefore = true;
						// The last item sometimes fuck up. This reduce the possibilites
						checkLastItem();
						return;
					}
				}

				finishSort = false;
				// Print
				if (chatMsg.get_value(true)) {
					MessageUtil.send_client_message("Inventory sorted");
				}
				// Check if the last slot has been placed
				checkLastItem();
				doneBefore = false;
				// If he want to close the inventory
				if (closeAfter.get_value(true))
					mc.player.closeScreen();
			}
		}
	}

	// This is for checking the last item
	private void checkLastItem() {
		if (sortItems.size() != 0) {
			// Get last
			int slotChange = sortItems.get(sortItems.size() - 1);
			// Check if it's empty
			if (mc.player.openContainer.getInventory().get(slotChange).isEmpty()) {
				// If yes, change
				mc.playerController.windowClick(0, slotChange, 0, ClickType.PICKUP, mc.player);
			}
		}
	}

	// This give the inventory to sort
	private ArrayList<Integer> getInventorySort(HashMap<Integer, String> copyInventory,
												HashMap<Integer, String> planInventoryCopy,
												int startValues) {
		// Plan to move
		ArrayList<Integer> planMove = new ArrayList<>();
		// The copy of the inventory
		// Lets get nItems
		HashMap<String, Integer> nItemsCopy = new HashMap<>();
		// Lets add everything to nItems
		for(String value : planInventoryCopy.values()) {
			if (nItemsCopy.containsKey(value)) {
				nItemsCopy.put(value, nItemsCopy.get(value) + 1);
			}else {
				nItemsCopy.put(value, 1);
			}
		}
		// Ignore values
		ArrayList<Integer> ignoreValues = new ArrayList<>();

		// Iterate and check if we are ok for certain items
		int[] listValue = new int[planInventoryCopy.size()];
		// Lets add everything
		int id = 0;
		for(int idx : planInventoryCopy.keySet()) {
			listValue[id++] = idx;
		}


		for(int item : listValue) {
			if (copyInventory.get(item).equals(planInventoryCopy.get(item))) {
				// Add a value to ignore later
				ignoreValues.add(item);
				// Update the value in nItemsCopy
				nItemsCopy.put(planInventoryCopy.get(item), nItemsCopy.get(planInventoryCopy.get(item)) - 1);
				// If it's == 0, just remove it
				if (nItemsCopy.get(planInventoryCopy.get(item)) == 0)
					nItemsCopy.remove(planInventoryCopy.get(item));
				// Lets remove it on planInventory
				planInventoryCopy.remove(item);
			}
		}

		String pickedItem = null;


		// Try to sort
		int i;
		for (i = startValues; i < startValues + copyInventory.size(); i++) {
			// Check if the i is in the ignoreList
			if (!ignoreValues.contains(i)) {
				// Lets check if it's one of the items we have
				String itemCheck = copyInventory.get(i);
				// Get the first possibilities
				Optional<Map.Entry<Integer, String>> momentAim = planInventoryCopy.entrySet().stream().filter(x -> x.getValue().equals(itemCheck)).findFirst();
				// Check if we found something (this should be always true, but because i fear NullPointerExceptor, i add this
				if (momentAim.isPresent()) {
					/// add values
					// Lets start with the beginning. If pickedItem is null, that means our hand is empty
					if (pickedItem == null)
						planMove.add(i);
					// Get end key
					int aimKey = momentAim.get().getKey();
					planMove.add(aimKey);
					// Ignore the end key
					if (pickedItem == null || !pickedItem.equals(itemCheck))
						ignoreValues.add(aimKey);
					/// We also have to update the list of item we need
					// Update the value in nItemsCopy
					nItemsCopy.put(itemCheck, nItemsCopy.get(itemCheck) - 1);
					// If it's == 0, just remove it
					if (nItemsCopy.get(itemCheck) == 0)
						nItemsCopy.remove(itemCheck);

					copyInventory.put(i, copyInventory.get(aimKey));
					copyInventory.put(aimKey, itemCheck);

					// Check if that determinated item is empty or not
					if (!copyInventory.get(aimKey).equals("minecraft:air0")) {
						// If it's not air, in this case we'll have an item in our pick hand.
						// We have to do not incr i
						// And then, lets add this value to pickedItem
						if (i >=  startValues + copyInventory.size())
							// Somehow, sometimes i go over the size of our inventory. I dunno how since the for cicle should
							// Stop it, but ok
							continue;
						pickedItem = copyInventory.get(i);
						i--;
					} else {
						// Else, it means we are placing on air. Lets remove pickedItem
						pickedItem = null;
					}
					// Lets remove it on planInventory
					planInventoryCopy.remove(aimKey);
				} else {
					// If we found nothing, lets check if we have something in the pick
					if (pickedItem != null) {
						// In this case, lets place this item in i
						planMove.add(i);
						copyInventory.put(i, pickedItem);
						// Reset pickedItem
						pickedItem = null;
					}
				}
			}

		}

		// For how the code above is done, for some reasons, the last item, sometimes, appears two times. This prevent this
		if (planMove.size() != 0 && planMove.get(planMove.size() - 1).equals(planMove.get(planMove.size() - 2))) {
			planMove.remove(planMove.size() - 1);
		}

		Object[] keyList = containerInv.keySet().toArray();

		// Lets take items from chest
		for(int values = 0; values < keyList.length; values++) {
			// Which index we are referring
			int itemC = (int) keyList[values];
			// If nItems contains what we are looking
			if (nItemsCopy.containsKey(containerInv.get(itemC))) {
				// If yes, get the item
				int start = planInventoryCopy.entrySet().stream().filter(x -> x.getValue().equals(containerInv.get(itemC))).findFirst().get().getKey();
				// If we are using invasive, change it everytimes. Else, only if it's empty
				if (invasive.get_value(true) || mc.player.openContainer.getInventory().get(start).isEmpty()) {
					// Make the switch
					planMove.add(start);
					planMove.add(itemC);
					planMove.add(start);
					// Lets remove the item from nItems and planInventory
					nItemsCopy.put(planInventoryCopy.get(start), nItemsCopy.get(planInventoryCopy.get(start)) - 1);
					if (nItemsCopy.get(planInventoryCopy.get(start)) == 0) {
						nItemsCopy.remove(planInventoryCopy.get(start));
					}
					planInventoryCopy.remove(start);
				}

			}
		}

		// Print all path
		if (debugMode.get_value(true)) {
			// Print every values
			for(int valuePath : planMove) {
				MessageUtil.send_client_message(Integer.toString(valuePath));
			}
		}

		return planMove;
	}

	// This give a copy of our inventory
	private HashMap<Integer, String> getInventoryCopy(int startPoint) {
		// The output. Is formed by: idx + name
		HashMap<Integer, String> output = new HashMap<>();
		// Size of the mainInventory
		int sizeInventory = mc.player.inventory.mainInventory.size();
		int value;
		// Iterate
		for(int i = 0; i < sizeInventory; i++) {
			// Get the starting value
			value = i + startPoint + (i < 9 ? sizeInventory - 9 : -9);
			// Get the item in that specific slot
			ItemStack item = mc.player.openContainer.getInventory().get(value);
			// Lets add it
			output.put(value, Objects.requireNonNull(item.getItem().getRegistryName()).toString() + item.getMetadata());
		}

		return output;
	}

	// This give a copy of our inventory
	private HashMap<Integer, String> getInventoryCopy(int startPoint, HashMap<Integer, String> inventory) {
		// Our output
		HashMap<Integer, String> output = new HashMap<>();
		// Size of mainInventory
		int sizeInventory = mc.player.inventory.mainInventory.size();
		for(int val : inventory.keySet()) {
			// Add it
			output.put(val + startPoint + (val < 9 ? sizeInventory - 9 : -9), inventory.get(val));
		}

		return output;
	}



}