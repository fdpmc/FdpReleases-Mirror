/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.*
import net.ccbluex.liquidbounce.value.FloatValue

@ModuleInfo(name = "ItemPhysics", category = ModuleCategory.RENDER)
class ItemPhysics : Module() {
    val itemWeight = FloatValue("Weight", 0.5F, 0F, 1F)
    override val tag: String?
        get() = "${itemWeight.get()}"
}