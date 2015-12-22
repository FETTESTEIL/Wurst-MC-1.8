package tk.wurst_client.gui.navigator;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.darkstorm.minecraft.gui.util.RenderUtil;
import org.lwjgl.input.Mouse;

import tk.wurst_client.WurstClient;
import tk.wurst_client.font.Fonts;
import tk.wurst_client.navigator.NavigatorItem;

public class NavigatorScreen extends GuiScreen
{
	private int scroll = 0;
	private static ArrayList<NavigatorItem> navigatorDisplayList =
		new ArrayList<>();
	private GuiTextField searchBar;
	private NavigatorItem activeItem;
	private int clickTimer = -1;
	private boolean expanding = false;
	
	public NavigatorScreen()
	{
		searchBar = new GuiTextField(0, Fonts.segoe22, 0, 32, 200, 20);
		searchBar.setEnableBackgroundDrawing(false);
		searchBar.setMaxStringLength(128);
		searchBar.setFocused(true);
		
		WurstClient.INSTANCE.navigator.copyNavigatorList(navigatorDisplayList);
	}
	
	@Override
	public void initGui()
	{
		searchBar.xPosition = width / 2 - 100;
	}
	
	@Override
	public boolean doesGuiPauseGame()
	{
		return false;
	}
	
	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException
	{
		super.mouseClicked(x, y, button);
		if(button == 0 && activeItem != null && clickTimer == -1)
			expanding = true;
	}
	
	@Override
	public void mouseReleased(int x, int y, int button)
	{
		super.mouseReleased(x, y, button);
	}
	
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException
	{
		if(keyCode == 1)
		{
			if(clickTimer == -1)
				mc.displayGuiScreen((GuiScreen)null);
		}
		
		if(clickTimer == -1)
		{
			String oldText = searchBar.getText();
			searchBar.textboxKeyTyped(typedChar, keyCode);
			String newText = searchBar.getText();
			if(newText.isEmpty())
				WurstClient.INSTANCE.navigator
					.copyNavigatorList(navigatorDisplayList);
			else if(!newText.equals(oldText))
				WurstClient.INSTANCE.navigator.getSearchResults(
					navigatorDisplayList, newText.toLowerCase());
		}
	}
	
	@Override
	public void updateScreen()
	{
		if(clickTimer == -1)
		{
			scroll += Mouse.getDWheel() / 10;
			if(scroll > 0)
				scroll = 0;
			else
			{
				int maxScroll =
					-navigatorDisplayList.size() / 3 * 20 + height - 120;
				if(maxScroll > 0)
					maxScroll = 0;
				if(scroll < maxScroll)
					scroll = maxScroll;
			}
		}
		
		searchBar.updateCursorCounter();
		
		if(expanding)
			if(clickTimer < 4)
				clickTimer++;
			else
				mc.displayGuiScreen(new NavigatorFeatureScreen(activeItem, this));
		else if(!expanding && clickTimer > -1)
			clickTimer--;
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks)
	{
		super.drawScreen(mouseX, mouseY, partialTicks);
		
		// search bar
		if(clickTimer == -1)
		{
			Fonts.segoe22.drawString("Search: ", width / 2 - 150, 32, 0xffffff);
			searchBar.drawTextBox();
		}
		
		// GL settings
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_CULL_FACE);
		glDisable(GL_TEXTURE_2D);
		glShadeModel(GL_SMOOTH);
		
		// feature list
		int x = width / 2 - 50;
		if(clickTimer == -1)
			activeItem = null;
		RenderUtil.scissorBox(0, 59, width, height - 42);
		glEnable(GL_SCISSOR_TEST);
		for(int i = Math.max(-scroll * 3 / 20 - 3, 0); i < navigatorDisplayList
			.size(); i++)
		{
			// y position
			int y = 60 + i / 3 * 20 + scroll;
			if(y < 40)
				continue;
			if(y > height - 40)
				break;
			
			// x position
			int xi = 0;
			switch(i % 3)
			{
				case 0:
					xi = x - 104;
					break;
				case 1:
					xi = x;
					break;
				case 2:
					xi = x + 104;
					break;
			}
			
			// item & area
			NavigatorItem item = navigatorDisplayList.get(i);
			Rectangle area = new Rectangle(xi, y, 100, 16);
			
			// hovering
			if(area.contains(mouseX, mouseY) && clickTimer == -1)
			{
				activeItem = item;
				glColor4f(0.375F, 0.375F, 0.375F, 0.5F);
			}else
				glColor4f(0.25F, 0.25F, 0.25F, 0.5F);
			
			// click animation
			if(clickTimer != -1)
			{
				if(item != activeItem)
					continue;
				
				float factor = clickTimer / 4F;
				float antiFactor = 1 - factor;
				
				area.x =
					(int)(area.x * antiFactor + (width / 2 - 154) * factor);
				area.y = (int)(area.y * antiFactor + 60 * factor);
				area.width = (int)(area.width * antiFactor + 308 * factor);
				area.height =
					(int)(area.height * antiFactor + (height - 103) * factor);
			}
			
			// box & shadow
			glBegin(GL_QUADS);
			{
				glVertex2d(area.x, area.y);
				glVertex2d(area.x + area.width, area.y);
				glVertex2d(area.x + area.width, area.y + area.height);
				glVertex2d(area.x, area.y + area.height);
			}
			glEnd();
			RenderUtil.boxShadow(area.x, area.y, area.x + area.width, area.y
				+ area.height);
			
			// text
			if(clickTimer == -1)
			{
				glEnable(GL_TEXTURE_2D);
				try
				{
					String buttonText = item.getName();
					Fonts.segoe15.drawString(
						buttonText,
						area.x
							+ (area.width - Fonts.segoe15
								.getStringWidth(buttonText)) / 2, area.y + 2,
						0xffffff);
				}catch(Exception e)
				{
					e.printStackTrace();
				}
				glDisable(GL_TEXTURE_2D);
			}
		}
		glDisable(GL_SCISSOR_TEST);
		
		// GL resets
		glEnable(GL_CULL_FACE);
		glEnable(GL_TEXTURE_2D);
		glDisable(GL_BLEND);
	}
	
	public void setExpanding(boolean expanding)
	{
		this.expanding = expanding;
	}
}
