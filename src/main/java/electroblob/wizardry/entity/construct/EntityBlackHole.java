package electroblob.wizardry.entity.construct;

import electroblob.wizardry.Wizardry;
import electroblob.wizardry.item.ItemArtefact;
import electroblob.wizardry.registry.WizardryItems;
import electroblob.wizardry.registry.WizardrySounds;
import electroblob.wizardry.util.MagicDamage;
import electroblob.wizardry.util.MagicDamage.DamageType;
import electroblob.wizardry.util.WizardryUtilities;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class EntityBlackHole extends EntityMagicConstruct {
	
	private static final double SUCTION_STRENGTH = 0.075;

	public int[] randomiser;
	public int[] randomiser2;

	public EntityBlackHole(World world){
		super(world);
		this.width = 6.0f;
		this.height = 3.0f;
		randomiser = new int[30];
		for(int i = 0; i < randomiser.length; i++){
			randomiser[i] = this.rand.nextInt(10);
		}
		randomiser2 = new int[30];
		for(int i = 0; i < randomiser2.length; i++){
			randomiser2[i] = this.rand.nextInt(10);
		}
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound nbttagcompound){
		super.readEntityFromNBT(nbttagcompound);
		randomiser = nbttagcompound.getIntArray("randomiser");
		randomiser2 = nbttagcompound.getIntArray("randomiser2");
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound nbttagcompound){
		super.writeEntityToNBT(nbttagcompound);
		nbttagcompound.setIntArray("randomiser", randomiser);
		nbttagcompound.setIntArray("randomiser2", randomiser2);
	}

	public void onUpdate(){

		super.onUpdate();

		// System.out.println("Client side: " + this.world.isRemote + ", Caster: " + this.caster);

		// Particle effect. Finishes 40 ticks before the end so the particles disappear at the same time.
		if(this.ticksExisted + 40 < this.lifetime){
			for(int i = 0; i < 5; i++){
				// this.world.spawnParticle(EnumParticleTypes.PORTAL, this.posX + (this.rand.nextDouble() - 0.5D) *
				// (double)this.width, this.posY + this.rand.nextDouble() * (double)this.height - 0.75D, this.posZ +
				// (this.rand.nextDouble() - 0.5D) * (double)this.width, (this.rand.nextDouble() - 0.5D) * 2.0D,
				// -this.rand.nextDouble(), (this.rand.nextDouble() - 0.5D) * 2.0D);
				this.world.spawnParticle(EnumParticleTypes.PORTAL, this.posX, this.posY, this.posZ,
						(this.rand.nextDouble() - 0.5D) * 4.0D, (this.rand.nextDouble() - 0.5D) * 4.0D - 1,
						(this.rand.nextDouble() - 0.5D) * 4.0D);
			}
		}

		if(this.lifetime - this.ticksExisted == 75){
			this.playSound(WizardrySounds.ENTITY_BLACK_HOLE_VANISH, 1.5f, 1.0f);
		}else if(this.ticksExisted % 80 == 1 && this.ticksExisted + 80 < this.lifetime){
			this.playSound(WizardrySounds.ENTITY_BLACK_HOLE_AMBIENT, 1.5f, 1.0f);
		}

		if(!this.world.isRemote){

			List<EntityLivingBase> targets = WizardryUtilities.getEntitiesWithinRadius(6.0d, this.posX, this.posY,
					this.posZ, this.world);

			for(EntityLivingBase target : targets){

				if(this.isValidTarget(target)){

					// If the target can't be moved, it isn't sucked in but is still damaged if it gets too close
					if(!(target instanceof EntityPlayer && ((getCaster() instanceof EntityPlayer && !Wizardry.settings.playersMoveEachOther)
							|| ItemArtefact.isArtefactActive((EntityPlayer)target, WizardryItems.amulet_anchoring)))){

						WizardryUtilities.undoGravity(target);

						// Sucks the target in
						if(this.posX > target.posX && target.motionX < 1){
							target.motionX += SUCTION_STRENGTH;
						}else if(this.posX < target.posX && target.motionX > -1){
							target.motionX -= SUCTION_STRENGTH;
						}

						if(this.posY > target.posY && target.motionY < 1){
							target.motionY += SUCTION_STRENGTH;
						}else if(this.posY < target.posY && target.motionY > -1){
							target.motionY -= SUCTION_STRENGTH;
						}

						if(this.posZ > target.posZ && target.motionZ < 1){
							target.motionZ += SUCTION_STRENGTH;
						}else if(this.posZ < target.posZ && target.motionZ > -1){
							target.motionZ -= SUCTION_STRENGTH;
						}

						// Player motion is handled on that player's client so needs packets
						if(target instanceof EntityPlayerMP){
							((EntityPlayerMP)target).connection.sendPacket(new SPacketEntityVelocity(target));
						}
					}

					if(this.getDistance(target) <= 2){
						// Damages the target if it is close enough
						if(this.getCaster() != null){
							target.attackEntityFrom(
									MagicDamage.causeIndirectMagicDamage(this, getCaster(), DamageType.MAGIC),
									2 * damageMultiplier);
						}else{
							target.attackEntityFrom(DamageSource.MAGIC, 2 * damageMultiplier);
						}
					}
				}
			}
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isInRangeToRenderDist(double distance){
		return true;
	}

	@Override
	public boolean shouldRenderInPass(int pass){
		return pass == 1;
	}

}
