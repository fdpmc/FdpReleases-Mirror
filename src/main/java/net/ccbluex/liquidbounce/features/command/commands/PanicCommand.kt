/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/UnlegitMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class PanicCommand : Command("panic", emptyArray()) {
    private val selections: Array<String>

    init {
        val list=mutableListOf<String>()
        list.add("all")
        list.add("norender")
        list.addAll(ModuleCategory.values().map { it.configName.toLowerCase() })
        selections=list.toTypedArray()
    }

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        var modules = LiquidBounce.moduleManager.modules.filter { it.state }
        val msg: String

        if (args.size > 1 && args[1].isNotEmpty()) {
            when (args[1].toLowerCase()) {
                "all" -> msg = "all"

                "nonrender" -> {
                    modules = modules.filter { it.category != ModuleCategory.RENDER }
                    msg = "all non-render"
                }

                else -> {
                    val categories = ModuleCategory.values().filter { it.configName.equals(args[1], true) }

                    if (categories.isEmpty()) {
                        chat("Category ${args[1]} not found")
                        return
                    }

                    val category = categories[0]
                    modules = modules.filter { it.category == category }
                    msg = "all ${category.displayName}"
                }
            }
        } else {
            chatSyntax("panic <${StringUtils.toCompleteString(selections)}>")
            return
        }

        for (module in modules)
            module.state = false

        chat("Disabled $msg modules.")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> selections.filter { it.startsWith(args[0], true) }
            else -> emptyList()
        }
    }
}
