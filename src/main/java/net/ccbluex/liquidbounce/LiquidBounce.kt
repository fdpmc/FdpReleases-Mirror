/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/Project-EZ4H/FDPClient/
 */
package net.ccbluex.liquidbounce

import net.ccbluex.liquidbounce.event.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.macro.MacroManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.special.AntiForge
import net.ccbluex.liquidbounce.features.special.CombatManager
import net.ccbluex.liquidbounce.features.special.PacketFixer
import net.ccbluex.liquidbounce.features.special.ServerSpoof
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.file.MetricsLite
import net.ccbluex.liquidbounce.file.config.ConfigManager
import net.ccbluex.liquidbounce.launch.EnumLaunchFilter
import net.ccbluex.liquidbounce.launch.LaunchFilterInfo
import net.ccbluex.liquidbounce.launch.LaunchOption
import net.ccbluex.liquidbounce.script.ScriptManager
import net.ccbluex.liquidbounce.script.remapper.Remapper
import net.ccbluex.liquidbounce.ui.cape.GuiCapeManager
import net.ccbluex.liquidbounce.ui.client.hud.HUD
import net.ccbluex.liquidbounce.ui.client.keybind.KeyBindManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager
import net.ccbluex.liquidbounce.ui.sound.TipSoundManager
import net.ccbluex.liquidbounce.utils.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ResourceLocation
import org.apache.commons.io.IOUtils

object LiquidBounce {

    // Client information
    const val CLIENT_NAME = "FDPClient"
    const val COLORED_NAME = "§c§lFDP§6§lClient"
    const val CLIENT_REAL_VERSION = "v2.0.0-PRE1"
    const val CLIENT_CREATOR = "CCBlueX & UnlegitMC"
    const val CLIENT_WEBSITE="GetFDP.Today"
    const val CLIENT_STORAGE = "https://res.getfdp.today/"
    const val MINECRAFT_VERSION = "1.8.9"

    // 自动读取客户端版本
    @JvmField
    val CLIENT_VERSION: String

    var isStarting = true
    var isLoadingConfig = true

    // Managers
    lateinit var moduleManager: ModuleManager
    lateinit var commandManager: CommandManager
    lateinit var eventManager: EventManager
    lateinit var fileManager: FileManager
    lateinit var scriptManager: ScriptManager
    lateinit var tipSoundManager: TipSoundManager
    lateinit var combatManager: CombatManager
    lateinit var macroManager: MacroManager
    lateinit var configManager: ConfigManager

    // Some UI things
    lateinit var hud: HUD
    lateinit var mainMenu: GuiScreen
    lateinit var keyBindManager: KeyBindManager

    lateinit var metricsLite: MetricsLite

    // Menu Background
    var background: ResourceLocation? = null

    private val dynamicLaunchOptions: Array<LaunchOption>

    init {
        // check if this artifact is build from github actions
        val commitId=LiquidBounce::class.java.classLoader.getResourceAsStream("FDP_GIT_COMMIT_ID")
        CLIENT_VERSION=if (commitId==null){
            CLIENT_REAL_VERSION
        }else{
            val str=IOUtils.toString(commitId,"utf-8").replace("\n","")
            "git-"+(str.substring(0, 7.coerceAtMost(str.length)))
        }
        // initialize dynamic launch options
        val launchFilters=mutableListOf<EnumLaunchFilter>()
        if(System.getProperty("fdp-legacy-ui")!=null){
            launchFilters.add(EnumLaunchFilter.LEGACY_UI)
        }else{
            launchFilters.add(EnumLaunchFilter.ULTRALIGHT)
        }
        dynamicLaunchOptions=ReflectUtils.getReflects("${LaunchOption::class.java.`package`.name}.options", LaunchOption::class.java)
            .filter {
                val annotation=it.getDeclaredAnnotation(LaunchFilterInfo::class.java)
                if(annotation!=null){
                    return@filter annotation.filters.toMutableList() == launchFilters
                }
                false
            }
            .map { try { it.newInstance() } catch (e: IllegalAccessException) { ClassUtils.getObjectInstance(it) as LaunchOption } }.toTypedArray()
    }

    /***
     * Execute if client will be started
     */
    fun startClient() {
        ClientUtils.logInfo("Starting $CLIENT_NAME $CLIENT_VERSION, by $CLIENT_CREATOR")
        val startTime=System.currentTimeMillis()
        isStarting = true

        // Create file manager
        fileManager = FileManager()
        configManager = ConfigManager()

        // Create event manager
        eventManager = EventManager()

        // Load language
        LanguageManager.switchLanguage(Minecraft.getMinecraft().gameSettings.language)

        dynamicLaunchOptions.forEach {
            it.head()
        }

        // Register listeners
        eventManager.registerListener(RotationUtils())
        eventManager.registerListener(AntiForge)
        eventManager.registerListener(InventoryUtils())
        eventManager.registerListener(ServerSpoof)

        // Create command manager
        commandManager = CommandManager()

        macroManager = MacroManager()
        eventManager.registerListener(macroManager)

        // Load client fonts
        Fonts.loadFonts()

        // Setup module manager and register modules
        moduleManager = ModuleManager()
        moduleManager.registerModules()

        // Remapper
        try {
            Remapper.loadSrg()

            // ScriptManager
            scriptManager = ScriptManager()
            scriptManager.loadScripts()
            scriptManager.enableScripts()
        } catch (throwable: Throwable) {
            ClientUtils.getLogger().error("Failed to load scripts.", throwable)
        }

        // Register commands
        commandManager.registerCommands()

        tipSoundManager = TipSoundManager()

        // Load configs
        configManager.loadLegacySupport()
        configManager.loadConfigSet()
        fileManager.loadConfigs(fileManager.accountsConfig, fileManager.friendsConfig, fileManager.xrayConfig, fileManager.specialConfig)

        // KeyBindManager
        keyBindManager=KeyBindManager()

        // Set HUD
        hud = HUD.createDefault()
        fileManager.loadConfig(fileManager.hudConfig)

        // Disable optifine fastrender
        ClientUtils.disableFastRender()

        // bstats.org user count display
        metricsLite=MetricsLite(11076)

        combatManager=CombatManager()
        eventManager.registerListener(combatManager)

        eventManager.registerListener(PacketFixer())

        GuiCapeManager.load()

        dynamicLaunchOptions.forEach {
            it.after()
        }

        // Set is starting status
        isStarting = false
        isLoadingConfig=false

        ClientUtils.logInfo("$CLIENT_NAME $CLIENT_VERSION started in ${(System.currentTimeMillis()-startTime)}ms!")
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        // Call client shutdown
        eventManager.callEvent(ClientShutdownEvent())

        // Save all available configs
        GuiCapeManager.save()
        configManager.save(true,true)
        fileManager.saveAllConfigs()

        dynamicLaunchOptions.forEach {
            it.stop()
        }
    }
}
