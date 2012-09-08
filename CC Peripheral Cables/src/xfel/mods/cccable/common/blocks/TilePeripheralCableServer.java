/** 
 * Copyright (c) Xfel, 2012
 * 
 * This file is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package xfel.mods.cccable.common.blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;

import net.minecraft.src.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import xfel.mods.cccable.api.ICableConnectable;
import xfel.mods.cccable.api.IPeripheralCable;
import xfel.mods.cccable.common.PeripheralAttachment;
import xfel.mods.cccable.common.routing.IRoutingTableListener;
import xfel.mods.cccable.common.routing.RoutingTable;
import xfel.mods.cccable.common.routing.RoutingTableEntry;

public class TilePeripheralCableServer extends TilePeripheralCableCommon
		implements IRoutingTableListener, IPeripheralCable, IPeripheral {

	public static final ForgeDirection[] DIRS = { ForgeDirection.DOWN,
			ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.SOUTH,
			ForgeDirection.WEST, ForgeDirection.EAST };

	protected boolean connectionStateDirty = true;

	private RoutingTable routingTable;

	private Map<ForgeDirection, TilePeripheralCableServer> adjacentCables;

	private IPeripheral localPeripheral;

	private Map<IComputerAccess, String> localComputers;

	public TilePeripheralCableServer() {
		routingTable = new RoutingTable();
		adjacentCables = new EnumMap<ForgeDirection, TilePeripheralCableServer>(
				ForgeDirection.class);
		localComputers = new HashMap<IComputerAccess, String>();
	}

	@Override
	public void updateEntity() {
		if (connectionStateDirty) {
			updateConnections();
			connectionStateDirty = false;
		}
		updateRoutingTable();
	}

	/**
	 * Immediately disconnects all local peripherals and computers
	 */
	protected void cleanup() {
		doDetachPeripheral();
		localPeripheral = null;

		connectionStateDirty = true;
	}

	/**
	 * Updates all connections and connects/disconnects peripherals if
	 * necessary.
	 */
	protected void updateConnections() {
		adjacentCables.clear();
		connectionState = 0;

		IPeripheral newPeripheral = null;

		for (ForgeDirection dir : DIRS) {
			int tx = xCoord + dir.offsetX;
			int ty = yCoord + dir.offsetY;
			int tz = zCoord + dir.offsetZ;

			if (ty < 0 || ty > worldObj.getHeight()) {
				continue;
			}

			TileEntity te = worldObj.getBlockTileEntity(tx, ty, tz);

			if (te == null) {
				continue;
			}
			ForgeDirection peripheralSide = ForgeDirection.UNKNOWN;
			if (te instanceof TilePeripheralCableServer) {
				TilePeripheralCableServer cable = (TilePeripheralCableServer) te;

				if (this.colorTag == -1 || cable.colorTag == -1
						|| this.colorTag == cable.colorTag) {
					adjacentCables.put(dir, cable);
					connectionState |= dir.flag;
				}
			} else if (colorTag != -1 && newPeripheral == null
					&& te instanceof IPeripheral) {
				if (te instanceof ICableConnectable
						&& ((ICableConnectable) te).canAttachCableToSide(dir
								.getOpposite().ordinal())) {
					newPeripheral = (IPeripheral) te;
					peripheralSide = dir;
					connectionState |= dir.flag;
				} else if (((IPeripheral) te).canAttachToSide(dir.getOpposite()
						.ordinal())) {
					newPeripheral = (IPeripheral) te;
					connectionState |= dir.flag;
					peripheralSide = dir;
				}
			} else if (te.getClass().getName()
					.equals("dan200.computer.shared.TileEntityComputer")) {
				connectionState |= dir.flag;
			}

			if (newPeripheral != localPeripheral) {
				doDetachPeripheral();
				localPeripheral = newPeripheral;
				doAttachPeripheral(peripheralSide);
			}
		}

		worldObj.markBlockNeedsUpdate(xCoord, yCoord, zCoord);
	}

	/**
	 * Updates the routing table
	 */
	protected void updateRoutingTable() {
		for (Map.Entry<ForgeDirection, TilePeripheralCableServer> entry : adjacentCables
				.entrySet()) {
			routingTable.recieveUpdate(entry.getValue().routingTable,
					entry.getKey());
		}

		routingTable.updateEntries();
	}

	private void doAttachPeripheral(ForgeDirection peripheralSide) {
		if (localPeripheral != null) {
			if (localPeripheral instanceof ICableConnectable) {
				((ICableConnectable) localPeripheral).attach(this,
						peripheralSide.getOpposite().ordinal(), colorTag);
			}

			synchronized (routingTable) {
				for (RoutingTableEntry entry : routingTable) {
					if (entry.isPeripheralTarget())
						continue;

					PeripheralAttachment.attachPeripheral(localPeripheral,
							colorTag, entry.getTargetComputer(),
							entry.getComputerSide());
				}
				routingTable.addLocalEntry(new RoutingTableEntry(
						localPeripheral, colorTag));
			}
		}
	}

	private void doDetachPeripheral() {
		if (localPeripheral != null) {
			if (localPeripheral instanceof ICableConnectable) {
				((ICableConnectable) localPeripheral).detach(this);
			}

			synchronized (routingTable) {
				for (RoutingTableEntry entry : routingTable) {
					if (entry.isPeripheralTarget())
						continue;

					PeripheralAttachment.detachPeripheral(localPeripheral,
							colorTag, entry.getTargetComputer(),
							entry.getComputerSide());
				}
				routingTable.removeLocalEntry(new RoutingTableEntry(
						localPeripheral, colorTag));
			}
		}
	}

	/*
	 * routing table listener hooks
	 */

	@Override
	public void peripheralAdded(RoutingTable routingTable,
			IPeripheral peripheral, int colorTag) {
		for (Map.Entry<IComputerAccess, String> entry : localComputers
				.entrySet()) {
			PeripheralAttachment.attachPeripheral(peripheral, colorTag,
					entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void peripheralRemoved(RoutingTable routingTable,
			IPeripheral peripheral, int colorTag) {
		for (Map.Entry<IComputerAccess, String> entry : localComputers
				.entrySet()) {
			PeripheralAttachment.detachPeripheral(peripheral, colorTag,
					entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void computerAdded(RoutingTable routingTable,
			IComputerAccess computer, String computerSide) {
		if (localPeripheral != null) {
			PeripheralAttachment.attachPeripheral(localPeripheral, colorTag,
					computer, computerSide);
		}
	}

	@Override
	public void computerRemoved(RoutingTable routingTable,
			IComputerAccess computer, String computerSide) {
		if (localPeripheral != null) {
			PeripheralAttachment.detachPeripheral(localPeripheral, colorTag,
					computer, computerSide);
		}
	}

	/*
	 * IPeriperalCable interface implementation
	 */

	@Override
	public synchronized Map<Integer, IPeripheral> getPeripheralMap() {
		Map<Integer, IPeripheral> result = new HashMap<Integer, IPeripheral>();

		for (RoutingTableEntry entry : routingTable) {
			if (!entry.isPeripheralTarget())
				continue;

			result.put(Integer.valueOf(entry.getId()),
					entry.getTargetPeripheral());
		}
		return result;
	}

	@Override
	public IPeripheral[] getPeripherals() {
		List<IPeripheral> result = new ArrayList<IPeripheral>(16);

		for (RoutingTableEntry entry : routingTable) {
			if (!entry.isPeripheralTarget())
				continue;

			result.add(entry.getTargetPeripheral());
		}
		return (IPeripheral[]) result.toArray(new IPeripheral[result.size()]);
	}

	@Override
	public IPeripheral getPeripheral(int colorTag) {
		RoutingTableEntry entry = routingTable.getPeripheralEntry(colorTag);

		if (entry == null) {
			return null;
		}

		return entry.getTargetPeripheral();
	}

	@Override
	public IComputerAccess[] getComputers() {
		List<IComputerAccess> result = new ArrayList<IComputerAccess>(16);

		for (RoutingTableEntry entry : routingTable) {
			if (entry.isPeripheralTarget())
				continue;

			result.add(entry.getTargetComputer());
		}
		return (IComputerAccess[]) result.toArray(new IComputerAccess[result
				.size()]);
	}

	@Override
	public String getComputerSide(IComputerAccess computer) {
		List<IPeripheral> result = new ArrayList<IPeripheral>(16);

		for (RoutingTableEntry entry : routingTable) {
			if (entry.isPeripheralTarget())
				continue;

			if (entry.getTargetComputer() == computer) {
				return entry.getComputerSide();
			}
		}
		return null;
	}

	/*
	 * IPeriperal implementation
	 */

	@Override
	public String getType() {
		return "cable";
	}

	@Override
	public String[] getMethodNames() {
		return new String[] { "isPresent", "getType", "getMethods", "call",
				"list" };
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int method,
			Object[] arguments) throws Exception {

		if (method == 4) {// list
			return listPeripherals(localComputers.get(computer));
		}

		if (arguments.length < 1 || !(arguments[0] instanceof Number)) {
			throw new Exception("invalid color tag");
		}

		int ctag = ((Number) arguments[0]).intValue();

		RoutingTableEntry entry = routingTable.getPeripheralEntry(ctag);

		if (method == 0) {// isPresent
			return new Object[] { Boolean.valueOf(entry != null) };
		}

		if (method == 3 && entry == null)
			throw new Exception("No peripheral attached");
		if (entry == null)
			return null;

		PeripheralAttachment att = PeripheralAttachment.getComputerWrapper(
				entry.getTargetPeripheral(), ctag, computer,
				getComputerSide(computer));

		switch (method) {
		case 1:// getType
			return new Object[] { att.getType() };
		case 2:// getMethods
			return new Object[] { att.getMethods() };
		case 3:// call

			if ((arguments.length < 2) || (arguments[1] == null)
					|| (!(arguments[1] instanceof String))) {
				throw new Exception("string expected");
			}

			return att.call((String) arguments[1],
					Arrays.copyOfRange(arguments, 2, arguments.length - 1));
		}
		assert false;
		return null;
	}

	private Object[] listPeripherals(String cside) {
		List<String> result = new ArrayList<String>(16);

		for (RoutingTableEntry entry : routingTable) {
			if (!entry.isPeripheralTarget())
				continue;

			result.add(PeripheralAttachment.getVirtualSide(cside, entry.getId()));
		}
		return result.toArray();
	}

	@Override
	public boolean canAttachToSide(int side) {
		return true;
	}

	@Override
	public void attach(IComputerAccess computer, String computerSide) {
		localComputers.put(computer, computerSide);

		synchronized (routingTable) {
			for (RoutingTableEntry entry : routingTable) {
				if (!entry.isPeripheralTarget())
					continue;

				PeripheralAttachment.attachPeripheral(
						entry.getTargetPeripheral(), entry.getId(), computer,
						computerSide);
			}
			routingTable.addLocalEntry(new RoutingTableEntry(computer,
					computerSide));
		}
	}

	@Override
	public void detach(IComputerAccess computer) {
		String computerSide = localComputers.remove(computer);

		synchronized (routingTable) {
			for (RoutingTableEntry entry : routingTable) {
				if (!entry.isPeripheralTarget())
					continue;

				PeripheralAttachment.detachPeripheral(
						entry.getTargetPeripheral(), entry.getId(), computer,
						computerSide);
			}
			routingTable.addLocalEntry(new RoutingTableEntry(computer,
					computerSide));
		}
	}

}