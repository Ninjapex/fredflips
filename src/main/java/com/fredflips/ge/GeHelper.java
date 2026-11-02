package com.fredflips.ge;

import java.util.function.BiFunction;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.util.QuantityFormatter;

/**
 * Click-to-fill helpers for the Grand Exchange, in the same style as other Plugin Hub flipping plugins.
 * Nothing happens without a click from you, and you still press Enter and Confirm yourself:
 * <ul>
 *     <li>Find on GE: after you click "Find on GE" in the sidebar, the next GE item search you open
 *     is filled with that item's name.</li>
 *     <li>When the GE's quantity or price box opens, clickable FredFlips suggestions are added to it.
 *     Clicking one puts the number in the box.</li>
 * </ul>
 * All methods run on the client thread unless noted.
 */
@Slf4j
@Singleton
public class GeHelper
{
	// Chatbox input modes (VarClientID.MESLAYERMODE).
	static final int MODE_GE_SEARCH = 14;
	static final int MODE_NUMBER = 7;
	static final int MODE_LARGE_NUMBER = 30;

	// GE_NEWOFFER_TYPE is 1 while setting up a sell offer.
	private static final int SELL_OFFER = 1;
	// A Find on GE request is forgotten if you don't open a GE search within this time.
	private static final long SEARCH_REQUEST_TIMEOUT_MS = 120_000;

	private static final int TEXT_COLOR = 0x800000;
	private static final int HOVER_COLOR = 0xFFFFFF;

	private final Client client;
	private final ClientThread clientThread;

	@Setter
	private boolean searchEnabled = true;
	@Setter
	private boolean suggestionsEnabled = true;
	/**
	 * (itemId, isSell) -> suggestion. Set by the plugin.
	 */
	@Setter
	private BiFunction<Integer, Boolean, GeSuggestion> suggestions;

	private String pendingSearchName;
	private long pendingSearchUntil;

	@Inject
	GeHelper(Client client, ClientThread clientThread)
	{
		this.client = client;
		this.clientThread = clientThread;
	}

	/**
	 * Called from the sidebar (any thread) when you click Find on GE.
	 */
	public void requestSearch(String itemName)
	{
		clientThread.invokeLater(() ->
		{
			pendingSearchName = itemName;
			pendingSearchUntil = System.currentTimeMillis() + SEARCH_REQUEST_TIMEOUT_MS;
			// If the search box is already open, fill it straight away.
			tryFillSearch();
		});
	}

	/**
	 * Forward VarClientIntChanged events here.
	 */
	public void onVarClientIntChanged(int index)
	{
		if (index != VarClientID.MESLAYERMODE)
		{
			return;
		}
		int mode = client.getVarcIntValue(VarClientID.MESLAYERMODE);
		if (mode == MODE_GE_SEARCH)
		{
			// The search box's key listener is set up later in the same tick.
			clientThread.invokeLater(this::tryFillSearch);
		}
		else if (mode == MODE_NUMBER || mode == MODE_LARGE_NUMBER)
		{
			clientThread.invokeLater(this::addSuggestions);
		}
	}

	private void tryFillSearch()
	{
		if (!searchEnabled || pendingSearchName == null || System.currentTimeMillis() > pendingSearchUntil
			|| client.getVarcIntValue(VarClientID.MESLAYERMODE) != MODE_GE_SEARCH)
		{
			return;
		}
		Widget input = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		Object[] refreshSearch = input == null || input.isHidden() ? null : input.getOnKeyListener();
		if (refreshSearch == null)
		{
			return;
		}
		client.setVarcStrValue(VarClientID.MESLAYERINPUT, pendingSearchName);
		// Re-run the GE's own search script so the results update for the new text.
		client.runScript(refreshSearch);
		pendingSearchName = null;
	}

	private void addSuggestions()
	{
		if (!suggestionsEnabled || suggestions == null || !isGeOfferSetupOpen())
		{
			return;
		}
		Widget title = client.getWidget(InterfaceID.Chatbox.MES_TEXT);
		Widget layer = client.getWidget(InterfaceID.Chatbox.MES_LAYER);
		if (title == null || layer == null || title.getText() == null)
		{
			return;
		}

		String prompt = title.getText().toLowerCase();
		boolean askingQuantity = prompt.contains("how many");
		boolean askingPrice = prompt.contains("price");
		int itemId = client.getVarpValue(VarPlayerID.TRADINGPOST_SEARCH);
		if ((!askingQuantity && !askingPrice) || itemId <= 0)
		{
			return;
		}

		boolean sell = client.getVarbitValue(VarbitID.GE_NEWOFFER_TYPE) == SELL_OFFER;
		GeSuggestion suggestion = suggestions.apply(itemId, sell);
		if (suggestion == null)
		{
			return;
		}

		if (askingPrice && suggestion.getPrice() != null && suggestion.getPrice() > 0)
		{
			String label = suggestion.getSource() + (sell ? " sell price: " : " buy price: ")
				+ QuantityFormatter.formatNumber(suggestion.getPrice()) + " gp";
			addClickableLine(layer, label, "Set price", suggestion.getPrice());
		}
		else if (askingQuantity && !sell && suggestion.getQuantity() != null && suggestion.getQuantity() > 0)
		{
			String label = suggestion.getSource() + " quantity: " + QuantityFormatter.formatNumber(suggestion.getQuantity());
			addClickableLine(layer, label, "Set quantity", suggestion.getQuantity());
		}
	}

	private boolean isGeOfferSetupOpen()
	{
		Widget setup = client.getWidget(InterfaceID.GeOffers.SETUP);
		return setup != null && !setup.isHidden();
	}

	private void addClickableLine(Widget layer, String text, String action, long value)
	{
		Widget line = layer.createChild(-1, WidgetType.TEXT);
		line.setText(text);
		line.setTextColor(TEXT_COLOR);
		line.setFontId(FontID.BOLD_12);
		line.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
		line.setOriginalX(10);
		line.setOriginalY(5);
		line.setOriginalHeight(20);
		line.setXTextAlignment(WidgetTextAlignment.LEFT);
		line.setWidthMode(WidgetSizeMode.MINUS);
		line.setAction(0, action);
		line.setHasListener(true);
		line.setOnOpListener((JavaScriptCallback) event -> fillInput(value));
		line.setOnMouseRepeatListener((JavaScriptCallback) event -> line.setTextColor(HOVER_COLOR));
		line.setOnMouseLeaveListener((JavaScriptCallback) event -> line.setTextColor(TEXT_COLOR));
		line.revalidate();
	}

	/**
	 * Puts a number in the open quantity/price box, exactly as if you'd typed it. You still press Enter.
	 */
	private void fillInput(long value)
	{
		Widget input = client.getWidget(InterfaceID.Chatbox.MES_TEXT2);
		if (input == null || input.isHidden())
		{
			return;
		}
		input.setText(value + "*");
		client.setVarcStrValue(VarClientID.MESLAYERINPUT, String.valueOf(value));
	}
}
