package net.ccbluex.liquidbounce.features.special

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer

class CombatManager : Listenable,MinecraftInstance() {
    var inCombat=false
    private val lastAttackTimer=MSTimer()
    var target: EntityLivingBase? = null
    val focusedPlayerList=mutableListOf<EntityPlayer>()

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if(mc.thePlayer==null) return
        MovementUtils.updateBlocksPerSecond()

        inCombat=false

        if(!lastAttackTimer.hasTimePassed(1000)){
            inCombat=true
            return
        }

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityLivingBase
                    && entity.getDistanceToEntity(mc.thePlayer) < 7 && EntityUtils.isSelected(entity, true)) {
                inCombat = true
                break
            }
        }

        if(target!=null){
            if(mc.thePlayer.getDistanceToEntity(target)>7||!inCombat||target!!.isDead){
                target=null
            }
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent){
        if(event.targetEntity is EntityLivingBase && EntityUtils.isSelected(event.targetEntity,true)){
            target=event.targetEntity
        }
        lastAttackTimer.reset()
    }

    @EventTarget
    fun onWorld(event: WorldEvent){
        inCombat=false
        target=null
        focusedPlayerList.clear()
    }

    fun getNearByEntity(radius: Float):EntityLivingBase?{
        return try {
            mc.theWorld.loadedEntityList
                .filter { mc.thePlayer.getDistanceToEntity(it)<radius&&EntityUtils.isSelected(it,true) }
                .sortedBy { it.getDistanceToEntity(mc.thePlayer) }[0] as EntityLivingBase?
        }catch (e: Exception){
            null
        }
    }

    fun isFocusEntity(entity: EntityPlayer):Boolean{
        if(focusedPlayerList.isEmpty())
            return true // no need 2 focus

        return focusedPlayerList.contains(entity)
    }

    override fun handleEvents() = true
}