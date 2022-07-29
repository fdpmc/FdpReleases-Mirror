package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.KeyEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.render.Animations;
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer;
import net.ccbluex.liquidbounce.ui.i18n.LanguageManager;
import net.ccbluex.liquidbounce.utils.render.EaseUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends MixinGuiScreen {
    @Shadow
    private int xSize;
    @Shadow
    private int ySize;
    @Shadow
    private int guiLeft;
    @Shadow
    private int guiTop;

    private long guiOpenTime=-1;
    private boolean translated=false;

    @Inject(method = "initGui", at = @At("RETURN"), cancellable = true)
    public void injectInitGui(CallbackInfo callbackInfo){
        GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
        if (guiScreen instanceof GuiChest) {
            buttonList.add(new GuiButton(114514, this.width / 2 - 100, this.guiTop - 30, 99, 20, LanguageManager.INSTANCE.getAndFormat("ui.chest.disable","%module.KillAura.name%")));
            buttonList.add(new GuiButton(1919810, this.width / 2 + 1, this.guiTop - 30, 99, 20, LanguageManager.INSTANCE.getAndFormat("ui.chest.disable","%module.ChestStealer.name%")));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 114514)
            LiquidBounce.moduleManager.getModule(KillAura.class).setState(false);
        if (button.id == 1919810)
            LiquidBounce.moduleManager.getModule(ChestStealer.class).setState(false);
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void drawScreenHead(CallbackInfo callbackInfo){
        ChestStealer chestStealer=LiquidBounce.moduleManager.getModule(ChestStealer.class);
        Minecraft mc=Minecraft.getMinecraft();
        GuiScreen guiScreen=mc.currentScreen;
        if(chestStealer.getState()&&chestStealer.getSilentTitleValue().get()&&guiScreen instanceof GuiChest){
            GuiChest chest=(GuiChest)guiScreen;
            if(!(chestStealer.getChestTitleValue().get()&&(chest.lowerChestInventory == null||!chest.lowerChestInventory.getName().contains(new ItemStack(Item.itemRegistry.getObject(new ResourceLocation("minecraft:chest"))).getDisplayName())))){
                //mouse focus
                mc.setIngameFocus();
                mc.currentScreen=guiScreen;
                //hide GUI
                if(chestStealer.getSilentTitleValue().get()) {
                    String tipString = "%ui.chest.stealing%";
                    mc.fontRendererObj.drawString(tipString, (width / 2) - (mc.fontRendererObj.getStringWidth(tipString) / 2), (height / 2) + 30, 0xffffffff);
                }
                callbackInfo.cancel();
            }
        }else{
            mc.currentScreen.drawWorldBackground(0);

            Animations animations = LiquidBounce.moduleManager.getModule(Animations.class);
            if(animations.getState()) {
                float pct = Math.max(animations.getTimeValue().get() - (System.currentTimeMillis() - guiOpenTime), 0) / ((float)animations.getTimeValue().get());
                if (pct != 0) {
                    GL11.glPushMatrix();

                    switch (animations.getModeValue().get().toLowerCase()){
                        case "slide":{
                            pct=(float)EaseUtils.easeInBack(pct);
                            GL11.glTranslatef(0F, -(guiTop + ySize) * pct, 0F);
                            break;
                        }
                        case "zoom":{
                            float scale=1-pct;
                            GL11.glScalef(scale,scale,scale);
                            GL11.glTranslatef(((guiLeft+(xSize*0.5F*pct))/scale)-guiLeft,((guiTop+(ySize*0.5F*pct))/scale)-guiTop, 0F);
                        }
                    }

                    translated = true;
                }
            }
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawScreenReturn(CallbackInfo callbackInfo){
        if(translated){
            GL11.glPopMatrix();
            translated=false;
        }
    }

    @Inject(method = "keyTyped", at = @At("HEAD"))
    private void keyTyped(char typedChar, int keyCode, CallbackInfo ci){
        ChestStealer chestStealer=LiquidBounce.moduleManager.getModule(ChestStealer.class);
        if(chestStealer.getState() && chestStealer.getSilentTitleValue().get() && mc.currentScreen instanceof GuiChest)
            LiquidBounce.eventManager.callEvent(new KeyEvent(keyCode == 0 ? typedChar + 256 : keyCode));
    }
}
