/** 
 * Copyright (c) Xfel, 2012
 * 
 * This file is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package xfel.mods.cccable.common.blocks;

import ic2.api.IPaintableBlock;

import java.util.List;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import xfel.mods.cccable.common.CommonProxy;

/**
 * The cable block class
 * 
 * @author Xfel
 *
 */
public class BlockCable extends BlockContainer implements IPaintableBlock{

	private int renderType = -1;

	/**
	 * Default constructor
	 * @param id the block id
	 */
	public BlockCable(int id) {
		super(id, Material.glass);
		setBlockName("cable.peripheral");
		setTextureFile(CommonProxy.BLOCK_TEXTURE);
//		setCreativeTab(ComputerCraft.ccTab);
	}

	@Override
	public TileEntity createNewTileEntity(World world) {
		if (world.isRemote) {
			return new TileCableCommon();
		}
		return new TileCableServer();
	}

	@Override
	public int getRenderType() {
		return renderType;
	}

	/**
	 * Set the type of render function that is called for this block. This is called by the client proxy.
	 * @param renderType the render type id
	 */
	public void setRenderType(int renderType) {
		this.renderType = renderType;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
	
	

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z,
			int blockId) {
		TileEntity te = world.getBlockTileEntity(x, y, z);

		if (te instanceof TileCableServer) {
			TileCableServer tpc = (TileCableServer) te;
			tpc.connectionStateDirty = true;
		}
	}

	@Override
	public void breakBlock(World world, int x, int y, int z, int blockId,
			int blockmeta) {
		TileEntity te = world.getBlockTileEntity(x, y, z);

		if (te instanceof TileCableServer) {
			TileCableServer tpc = (TileCableServer) te;
			tpc.cleanup();
		}
		super.breakBlock(world, x, y, z, blockId, blockmeta);
	}

	public boolean onBlockActivated(World world, int x, int y, int z,
			EntityPlayer player, int direction, float offsetX, float offsetY,
			float offsetZ) {
		TileEntity te = world.getBlockTileEntity(x, y, z);

		if (te instanceof TileCableCommon&&player.getCurrentEquippedItem()!=null) {
			TileCableCommon tpc = (TileCableCommon) te;

			ItemStack iih = player.getCurrentEquippedItem();

			if (iih.getItem() == Item.dyePowder) {
				tpc.setColorTag(iih.getItemDamage());
				if (!player.capabilities.isCreativeMode) {
					iih.stackSize--;
					if (iih.stackSize == 0) {
						player.destroyCurrentEquippedItem();
					}
				}
				return true;
			}

			if (iih.getItem() == Item.bucketWater) {
				tpc.setColorTag(-1);
				return true;
			}
		}
		return false;
	}

	public int getBlockTexture(IBlockAccess iba, int x, int y, int z, int side) {
		TileEntity te = iba.getBlockTileEntity(x, y, z);

		if (te instanceof TileCableCommon) {
			TileCableCommon tpc = (TileCableCommon) te;

			return tpc.getColorTag() + 1;
		}
		return 0;
	}

	@Override
	public boolean colorBlock(World world, int x, int y, int z, int color) {
		TileEntity te = world.getBlockTileEntity(x, y, z);

		if (te instanceof TileCableCommon) {
			TileCableCommon tpc = (TileCableCommon) te;
			
			tpc.setColorTag(color);
			return true;
		}
		return false;
	}

	@Override
	public void addCollidingBlockToList(World world, int x, int y, int z,
			AxisAlignedBB bounds, List results, Entity entity) {
		setBlockBounds(0.25f, 0.25f, 0.25f, 0.75f, 0.75f, 0.75f);
		AxisAlignedBB center = super.getCollisionBoundingBoxFromPool(world, x,
				y, z);

		if (center != null && bounds.intersectsWith(center)) {
			results.add(center);
		}

		TileEntity te = world.getBlockTileEntity(x, y, z);

		if (te instanceof TileCableCommon) {
			int connections = ((TileCableCommon) te).getConnectionState();

			if ((connections & ForgeDirection.WEST.flag) != 0) {
				setBlockBounds(0.0F, 0.25f, 0.25f, 0.75f, 0.75f, 0.75f);
				AxisAlignedBB part = super.getCollisionBoundingBoxFromPool(
						world, x, y, z);

				if (part != null && bounds.intersectsWith(part)) {
					results.add(part);
				}
			}

			if ((connections & ForgeDirection.EAST.flag) != 0) {
				setBlockBounds(0.25f, 0.25f, 0.25f, 1.0F, 0.75f, 0.75f);
				AxisAlignedBB part = super.getCollisionBoundingBoxFromPool(
						world, x, y, z);

				if (part != null && bounds.intersectsWith(part)) {
					results.add(part);
				}
			}

			if ((connections & ForgeDirection.DOWN.flag) != 0) {
				setBlockBounds(0.25f, 0.0F, 0.25f, 0.75f, 0.75f, 0.75f);
				AxisAlignedBB part = super.getCollisionBoundingBoxFromPool(
						world, x, y, z);

				if (part != null && bounds.intersectsWith(part)) {
					results.add(part);
				}
			}

			if ((connections & ForgeDirection.UP.flag) != 0) {
				setBlockBounds(0.25f, 0.25f, 0.25f, 0.75f, 1.0F, 0.75f);
				AxisAlignedBB part = super.getCollisionBoundingBoxFromPool(
						world, x, y, z);

				if (part != null && bounds.intersectsWith(part)) {
					results.add(part);
				}
			}

			if ((connections & ForgeDirection.NORTH.flag) != 0) {
				setBlockBounds(0.25f, 0.25f, 0.0F, 0.75f, 0.75f, 0.75f);
				AxisAlignedBB part = super.getCollisionBoundingBoxFromPool(
						world, x, y, z);

				if (part != null && bounds.intersectsWith(part)) {
					results.add(part);
				}
			}

			if ((connections & ForgeDirection.SOUTH.flag) != 0) {
				setBlockBounds(0.25f, 0.25f, 0.25f, 0.75f, 0.75f, 1.0F);
				AxisAlignedBB part = super.getCollisionBoundingBoxFromPool(
						world, x, y, z);

				if (part != null && bounds.intersectsWith(part)) {
					results.add(part);
				}
			}
		}
		setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess iba, int x, int y, int z) {
		float xMin = 0.25f, xMax = 0.75f, yMin = 0.25f, yMax = 0.75f, zMin = 0.25f, zMax = 0.75f;

		TileEntity te = iba.getBlockTileEntity(x, y, z);

		if (te instanceof TileCableCommon) {
			int connections = ((TileCableCommon) te).getConnectionState();

			if ((connections & ForgeDirection.WEST.flag) != 0)
				xMin = 0.0F;

			if ((connections & ForgeDirection.EAST.flag) != 0)
				xMax = 1.0F;

			if ((connections & ForgeDirection.DOWN.flag) != 0)
				yMin = 0.0F;

			if ((connections & ForgeDirection.UP.flag) != 0)
				yMax = 1.0F;

			if ((connections & ForgeDirection.NORTH.flag) != 0)
				zMin = 0.0F;

			if ((connections & ForgeDirection.SOUTH.flag) != 0)
				zMax = 1.0F;
		}

		setBlockBounds(xMin, yMin, zMin, xMax, yMax, zMax);
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x,
			int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);

		return super.getCollisionBoundingBoxFromPool(world, x, y, z)

		;
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int i,
			int j, int k) {
		return getCollisionBoundingBoxFromPool(world, i, j, k);
	}

	// @Override
	// public MovingObjectPosition collisionRayTrace(World world, int x, int y,
	// int z, Vec3 vec3d, Vec3 vec3d1) {
	// setBlockBoundsBasedOnState(world, x, y, z);
	//
	// MovingObjectPosition r = super.collisionRayTrace(world, x, y, z, vec3d,
	// vec3d1);
	//
	// setBlockBounds(0, 0, 0, 1, 1, 1);
	//
	// return r;
	// }

}
