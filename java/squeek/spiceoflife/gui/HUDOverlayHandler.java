package squeek.spiceoflife.gui;

import java.text.DecimalFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.FoodStats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.ForgeSubscribe;
import org.lwjgl.opengl.GL11;
import squeek.spiceoflife.ModConfig;
import squeek.spiceoflife.ModInfo;
import squeek.spiceoflife.foodtracker.FoodValues;

public class HUDOverlayHandler
{
	float flashAlpha = 0f;
	byte alphaDir = 1;

	private static final ResourceLocation modIcons = new ResourceLocation(ModInfo.MODID.toLowerCase(), "textures/icons.png");
	private static final DecimalFormat df = new DecimalFormat("##.##");

	@ForgeSubscribe
	public void onTextRender(RenderGameOverlayEvent.Text textEvent)
	{
		if (textEvent.type != RenderGameOverlayEvent.ElementType.TEXT)
			return;

		Minecraft mc = Minecraft.getMinecraft();
		if (mc.gameSettings.showDebugInfo)
		{
			FoodStats stats = mc.thePlayer.getFoodStats();
			textEvent.left.add("hunger: " + stats.getFoodLevel() + ", saturation: " + df.format(stats.getSaturationLevel()));
		}
	}

	@ForgeSubscribe
	public void onRender(RenderGameOverlayEvent.Post event)
	{
		if (event.type != RenderGameOverlayEvent.ElementType.FOOD)
			return;

		if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY && !ModConfig.SHOW_SATURATION_OVERLAY)
			return;

		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer player = mc.thePlayer;
		ItemStack heldItem = player.getHeldItem();
		FoodStats stats = player.getFoodStats();

		ScaledResolution scale = event.resolution;

		int left = scale.getScaledWidth() / 2 + 91;
		int top = scale.getScaledHeight() - GuiIngameForge.right_height + 10;

		// saturation overlay
		if (ModConfig.SHOW_SATURATION_OVERLAY)
			drawSaturationOverlay(0, stats.getSaturationLevel(), mc, left, top, 1f);

		if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY || heldItem == null || !(heldItem.getItem() instanceof ItemFood))
		{
			flashAlpha = 0;
			alphaDir = 1;
			return;
		}

		// restored hunger/saturation overlay while holding food
		FoodValues foodValues = FoodValues.getModified(heldItem, player);
		drawHungerOverlay(foodValues.hunger, stats.getFoodLevel(), mc, left, top, flashAlpha);
		int newFoodValue = stats.getFoodLevel() + foodValues.hunger;
		float newSaturationValue = stats.getSaturationLevel() + foodValues.getSaturationIncrement();
		drawSaturationOverlay(newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodValues.getSaturationIncrement(), stats.getSaturationLevel(), mc, left, top, flashAlpha);

		flashAlpha += alphaDir * 0.025f;
		if (flashAlpha >= 1.5f)
		{
			flashAlpha = 1f;
			alphaDir = -1;
		}
		else if (flashAlpha <= -0.5f)
		{
			flashAlpha = 0f;
			alphaDir = 1;
		}
	}

	public static void drawSaturationOverlay(float saturationGained, float saturationLevel, Minecraft mc, int left, int top, float alpha)
	{
		int startBar = saturationGained != 0 ? (int) saturationLevel / 2 : 0;
		int endBar = (int) Math.ceil(Math.min(20, saturationLevel + saturationGained) / 2f);
		int barsNeeded = endBar - startBar;
		mc.getTextureManager().bindTexture(modIcons);

		enableAlpha(alpha);
		for (int i = startBar; i < startBar + barsNeeded; ++i)
		{
			int x = left - i * 8 - 9;
			int y = top;
			float effectiveSaturationOfBar = (saturationLevel + saturationGained) / 2 - i;

			if (effectiveSaturationOfBar >= 1)
				mc.ingameGUI.drawTexturedModalRect(x, y, 27, 0, 9, 9);
			else if (effectiveSaturationOfBar > .5)
				mc.ingameGUI.drawTexturedModalRect(x, y, 18, 0, 9, 9);
			else if (effectiveSaturationOfBar > .25)
				mc.ingameGUI.drawTexturedModalRect(x, y, 9, 0, 9, 9);
			else if (effectiveSaturationOfBar > 0)
				mc.ingameGUI.drawTexturedModalRect(x, y, 0, 0, 9, 9);
		}
		disableAlpha(alpha);

		// rebind default icons
		mc.getTextureManager().bindTexture(Gui.icons);
	}

	public static void drawHungerOverlay(int hungerRestored, int foodLevel, Minecraft mc, int left, int top, float alpha)
	{
		if (hungerRestored == 0)
			return;

		int startBar = foodLevel / 2;
		int endBar = (int) Math.ceil(Math.min(20, foodLevel + hungerRestored) / 2f);
		int barsNeeded = endBar - startBar;
		mc.getTextureManager().bindTexture(Gui.icons);

		enableAlpha(alpha);
		for (int i = startBar; i < startBar + barsNeeded; ++i)
		{
			int idx = i * 2 + 1;
			int x = left - i * 8 - 9;
			int y = top;
			int icon = 16;
			int background = 13;

			if (mc.thePlayer.isPotionActive(Potion.hunger))
			{
				icon += 36;
				background = 13;
			}

			mc.ingameGUI.drawTexturedModalRect(x, y, 16 + background * 9, 27, 9, 9);

			if (idx < foodLevel + hungerRestored)
				mc.ingameGUI.drawTexturedModalRect(x, y, icon + 36, 27, 9, 9);
			else if (idx == foodLevel + hungerRestored)
				mc.ingameGUI.drawTexturedModalRect(x, y, icon + 45, 27, 9, 9);
		}
		disableAlpha(alpha);
	}

	public static void enableAlpha(float alpha)
	{
		if (alpha == 1f)
			return;

		GL11.glColor4f(1f, 1f, 1f, alpha);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	public static void disableAlpha(float alpha)
	{
		if (alpha == 1f)
			return;

		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1f, 1f, 1f, 1f);
	}
}
