package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.extensions.getFullName
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S0BPacketAnimation
import net.minecraft.network.play.server.S14PacketEntity
import net.minecraft.network.play.server.S38PacketPlayerListItem

@ModuleInfo(name = "AntiBot", category = ModuleCategory.MISC)
object AntiBot : Module() {

    private val tabValue = BoolValue("Tab", true)
    private val tabModeValue = ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains").displayable { tabValue.get() }
    private val entityIDValue = BoolValue("EntityID", true)
    private val colorValue = BoolValue("Color", false)
    private val livingTimeValue = BoolValue("LivingTime", false)
    private val livingTimeTicksValue = IntegerValue("LivingTimeTicks", 40, 1, 200).displayable { livingTimeValue.get() }
    private val groundValue = BoolValue("Ground", true)
    private val airValue = BoolValue("Air", false)
    private val invalidGroundValue = BoolValue("InvalidGround", true)
    private val swingValue = BoolValue("Swing", false)
    private val healthValue = BoolValue("Health", false)
    private val derpValue = BoolValue("Derp", true)
    private val wasInvisibleValue = BoolValue("WasInvisible", false)
    private val armorValue = BoolValue("Armor", false)
    private val pingValue = BoolValue("Ping", false)
    private val needHitValue = BoolValue("NeedHit", false)
    private val duplicateInWorldValue = BoolValue("DuplicateInWorld", false)
    private val duplicateInTabValue = BoolValue("DuplicateInTab", false)
    private val matrixValue = BoolValue("MatrixBot", false)
    private val alwaysInRadiusValue = BoolValue("AlwaysInRadius", false)
    private val alwaysRadiusValue = FloatValue("AlwaysInRadiusBlocks", 20f, 5f, 30f).displayable { alwaysInRadiusValue.get() }

    private val ground = mutableListOf<Int>()
    private val air = mutableListOf<Int>()
    private val invalidGround = mutableMapOf<Int, Int>()
    private val swing = mutableListOf<Int>()
    private val invisible = mutableListOf<Int>()
    private val hitted = mutableListOf<Int>()
    private val notAlwaysInRadius = mutableListOf<Int>()

    @JvmStatic
    fun isBot(entity: EntityLivingBase): Boolean {
        // Check if entity is a player
        if (entity !is EntityPlayer)
            return false

        // Check if anti bot is enabled
        if (!state)
            return false

        // Anti Bot checks

        if (colorValue.get() && !entity.displayName!!.formattedText.replace("§r", "").contains("§"))
            return true

        if (livingTimeValue.get() && entity.ticksExisted < livingTimeTicksValue.get())
            return true

        if (groundValue.get() && !ground.contains(entity.entityId))
            return true

        if (airValue.get() && !air.contains(entity.entityId))
            return true

        if (swingValue.get() && !swing.contains(entity.entityId))
            return true

        if (healthValue.get() && entity.health > 20F)
            return true

        if (entityIDValue.get() && (entity.entityId >= 1000000000 || entity.entityId <= -1))
            return true

        if (derpValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true

        if (wasInvisibleValue.get() && invisible.contains(entity.entityId))
            return true

        if (armorValue.get()) {
            if (entity.inventory.armorInventory[0] == null && entity.inventory.armorInventory[1] == null &&
                entity.inventory.armorInventory[2] == null && entity.inventory.armorInventory[3] == null)
                return true
        }

        if (pingValue.get()) {
            if (mc.netHandler.getPlayerInfo(entity.uniqueID)?.responseTime == 0)
                return true
        }

        if (needHitValue.get() && !hitted.contains(entity.entityId))
            return true

        if (invalidGroundValue.get() && invalidGround.getOrDefault(entity.entityId, 0) >= 10)
            return true

        if (tabValue.get()) {
            val equals = tabModeValue.get().equals("Equals", ignoreCase = true)
            val targetName = stripColor(entity.displayName!!.formattedText)

            if (targetName != null) {
                for (networkPlayerInfo in mc.netHandler.playerInfoMap) {
                    val networkName = stripColor(networkPlayerInfo.getFullName()) ?: continue

                    if (if (equals) targetName == networkName else targetName.contains(networkName))
                        return false
                }

                return true
            }
        }

        if (duplicateInWorldValue.get() &&
                mc.theWorld!!.loadedEntityList.filter { it is EntityPlayer && it.displayNameString == it.displayNameString }.count() > 1)
            return true

        if (duplicateInTabValue.get() &&
                mc.netHandler.playerInfoMap.filter { entity.name == stripColor(it.getFullName()) }.count() > 1)
            return true

        if (alwaysInRadiusValue.get() && !notAlwaysInRadius.contains(entity.entityId))
            return true

        return entity.name!!.isEmpty() || entity.name == mc.thePlayer!!.name
    }

    override fun onDisable() {
        clearAll()
        super.onDisable()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return

        val packet = event.packet
        if (matrixValue.get()) {
            if (packet is S38PacketPlayerListItem) {
                if (packet.action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                    for (i in mc.theWorld.loadedEntityList) {
                        val entityLivingBase = i as EntityLivingBase
                        for (j in packet.entries) {
                            if (entityLivingBase !is EntityPlayerSP && j.profile.name.equals(
                                    entityLivingBase.name,
                                    ignoreCase = true
                                ) && (j.profile.id !== entityLivingBase.uniqueID || j.profile.id !== entityLivingBase.persistentID) || MovementUtils.getDirection() === getEntityDirection(
                                    entityLivingBase
                                )
                            ) {
                                event.cancelEvent()
                            }
                        }
                    }
                }
            }
        }
        if (packet is S14PacketEntity) {
            val entity = packet.getEntity(mc.theWorld!!)

            if (entity is EntityPlayer) {
                if (packet.onGround && !ground.contains(entity.entityId))
                    ground.add(entity.entityId)

                if (!packet.onGround && !air.contains(entity.entityId))
                    air.add(entity.entityId)

                if (packet.onGround) {
                    if (entity.prevPosY != entity.posY)
                        invalidGround[entity.entityId] = invalidGround.getOrDefault(entity.entityId, 0) + 1
                } else {
                    val currentVL = invalidGround.getOrDefault(entity.entityId, 0) / 2
                    if (currentVL <= 0)
                        invalidGround.remove(entity.entityId)
                    else
                        invalidGround[entity.entityId] = currentVL
                }

                if (entity.isInvisible && !invisible.contains(entity.entityId))
                    invisible.add(entity.entityId)

                if (!notAlwaysInRadius.contains(entity.entityId) && mc.thePlayer!!.getDistanceToEntity(entity) > alwaysRadiusValue.get())
                    notAlwaysInRadius.add(entity.entityId);
            }
        }

        if (packet is S0BPacketAnimation) {
            val entity = mc.theWorld!!.getEntityByID(packet.entityID)

            if (entity != null && entity is EntityLivingBase && packet.animationType == 0
                    && !swing.contains(entity.entityId))
                swing.add(entity.entityId)
        }
    }

    fun getEntityDirection(entityLivingBase: EntityLivingBase): Double {
        var rotationYaw = entityLivingBase.rotationYaw
        if (entityLivingBase.moveForward < 0f) rotationYaw += 180f
        var forward = 1f
        if (entityLivingBase.moveForward < 0f) forward = -0.5f else if (entityLivingBase.moveForward > 0f) forward =
            0.5f
        if (entityLivingBase.moveStrafing > 0f) rotationYaw -= 90f * forward
        if (entityLivingBase.moveStrafing < 0f) rotationYaw += 90f * forward
        return Math.toRadians(rotationYaw.toDouble())
    }

    @EventTarget
    fun onAttack(e: AttackEvent) {
        val entity = e.targetEntity

        if (entity != null && entity is EntityLivingBase && !hitted.contains(entity.entityId))
            hitted.add(entity.entityId)
    }

    @EventTarget
    fun onWorld(event: WorldEvent?) {
        clearAll()
    }

    private fun clearAll() {
        hitted.clear()
        swing.clear()
        ground.clear()
        invalidGround.clear()
        invisible.clear()
        notAlwaysInRadius.clear();
    }

}
