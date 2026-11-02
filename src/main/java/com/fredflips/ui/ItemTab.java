package com.fredflips.ui;

import com.fredflips.FredFlipsConfig;
import com.fredflips.flip.FlipCandidate;
import com.fredflips.insights.ChartPeriod;
import com.fredflips.insights.ItemInsights;
import com.fredflips.wiki.ItemMapping;
import com.fredflips.wiki.TimeseriesPoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

/**
 * Everything about one item: live prices, chart, technical indicators and volume.
 */
class ItemTab extends JPanel
{
	private static final String WIKI_ITEM_URL = "https://prices.runescape.wiki/osrs/item/";
	private static final int MAX_SEARCH_RESULTS = 8;

	private final ItemManager itemManager;
	private final PanelActions actions;
	private final FredFlipsConfig config;

	private final JTextField search = new JTextField();
	private final JPanel searchResults = Ui.stack();
	private final JPanel detail = Ui.stack();
	private final PriceChart chart = new PriceChart();
	private final JPanel periodButtons = new JPanel(new GridLayout(1, 0, 2, 0));
	private final JPanel indicators = Ui.card();
	private final JPanel volume = Ui.card();

	private int itemId = -1;
	private ChartPeriod period;

	ItemTab(ItemManager itemManager, PanelActions actions, FredFlipsConfig config)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;
		this.config = config;
		this.period = config.defaultChartPeriod();

		search.setToolTipText("Search for an item");
		search.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				updateSearch();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				updateSearch();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				updateSearch();
			}
		});

		for (ChartPeriod option : ChartPeriod.values())
		{
			JButton button = new JButton(option.getDisplayName());
			button.addActionListener(e ->
			{
				period = option;
				chart.setPoints(java.util.Collections.emptyList());
				actions.changeChartPeriod(option);
			});
			periodButtons.add(button);
		}

		JPanel top = Ui.stack();
		Ui.addSpaced(top, Ui.muted("Search any item"));
		Ui.addSpaced(top, search);
		top.add(searchResults);
		add(top, BorderLayout.NORTH);

		detail.add(Ui.note("Search for an item, or click one in another tab."));
		add(detail, BorderLayout.CENTER);
	}

	int getItemId()
	{
		return itemId;
	}

	ChartPeriod getPeriod()
	{
		return period;
	}

	private void updateSearch()
	{
		searchResults.removeAll();
		String query = search.getText().trim();
		if (query.length() >= 2)
		{
			List<ItemMapping> matches = actions.searchItems(query);
			for (ItemMapping match : matches.subList(0, Math.min(MAX_SEARCH_RESULTS, matches.size())))
			{
				JLabel result = Ui.label(match.getName(), Color.WHITE);
				result.setOpaque(true);
				result.setBackground(ColorScheme.DARKER_GRAY_COLOR);
				result.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 6, 4, 6));
				result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				result.addMouseListener(new MouseAdapter()
				{
					@Override
					public void mouseClicked(MouseEvent e)
					{
						actions.openItem(match.getId());
					}
				});
				Ui.addSpaced(searchResults, result);
			}
			if (matches.isEmpty())
			{
				Ui.addSpaced(searchResults, Ui.muted("No items found"));
			}
		}
		searchResults.revalidate();
		searchResults.repaint();
	}

	/**
	 * Shows the item straight away with live prices; chart, indicators and volume fill in when loaded.
	 */
	void showItem(ItemMapping item, FlipCandidate numbers)
	{
		itemId = item.getId();
		search.setText("");
		detail.removeAll();

		JPanel header = new JPanel(new BorderLayout(6, 0));
		header.setOpaque(false);
		JLabel icon = new JLabel();
		icon.setPreferredSize(new Dimension(36, 32));
		itemManager.getImage(item.getId()).addTo(icon);
		header.add(icon, BorderLayout.WEST);
		header.add(Ui.heading(item.getName()), BorderLayout.CENTER);
		Ui.addSpaced(detail, header);

		JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
		buttons.setOpaque(false);
		JButton find = new JButton("Find on GE");
		find.setToolTipText("Fills in the GE item search: click this, then Buy on an empty GE slot");
		find.addActionListener(e -> actions.findOnGe(item.getId()));
		JButton wiki = new JButton("Wiki chart");
		wiki.setToolTipText("Open the full price chart on the OSRS Wiki");
		wiki.addActionListener(e -> LinkBrowser.browse(WIKI_ITEM_URL + item.getId()));
		buttons.add(find);
		buttons.add(wiki);
		Ui.addSpaced(detail, buttons);

		if (config.itemLivePrices())
		{
			detail.add(Ui.heading("Live prices"));
			Ui.addSpaced(detail, livePrices(item, numbers));
		}
		if (config.itemChart())
		{
			detail.add(Ui.heading("Price chart"));
			Ui.addSpaced(detail, periodButtons);
			chart.setPoints(java.util.Collections.emptyList());
			Ui.addSpaced(detail, chart);
			JPanel legend = new JPanel(new BorderLayout());
			legend.setOpaque(false);
			legend.add(Ui.label("Sell side (instant-buy)", PriceChart.SELL_LINE), BorderLayout.WEST);
			legend.add(Ui.label("Buy side", PriceChart.BUY_LINE), BorderLayout.EAST);
			Ui.addSpaced(detail, legend);
		}
		if (config.itemIndicators())
		{
			detail.add(Ui.heading("Technical indicators"));
			indicators.removeAll();
			indicators.add(Ui.muted("Loading..."));
			Ui.addSpaced(detail, indicators);
		}
		if (config.itemVolume())
		{
			detail.add(Ui.heading("Volume"));
			volume.removeAll();
			if (numbers != null)
			{
				volume.add(Ui.row("Buyers last hour", Ui.gp(numbers.getBuyersPerHour())));
				volume.add(Ui.row("Sellers last hour", Ui.gp(numbers.getSellersPerHour())));
			}
			volume.add(Ui.muted("Loading history..."));
			Ui.addSpaced(detail, volume);
		}
		Ui.addSpaced(detail, Ui.note("Indicators describe what has happened, not what will happen. Use them as one input, not a guarantee."));

		detail.revalidate();
		detail.repaint();
	}

	void showChart(int forItemId, ChartPeriod forPeriod, List<TimeseriesPoint> points)
	{
		if (forItemId != itemId || forPeriod != period)
		{
			return;
		}
		chart.setPoints(points.subList(Math.max(0, points.size() - forPeriod.getPoints()), points.size()));
	}

	void showInsights(int forItemId, ItemInsights insights, FlipCandidate numbers)
	{
		if (forItemId != itemId)
		{
			return;
		}

		indicators.removeAll();
		indicators.add(Ui.row("Change 1h", Ui.percent(insights.getChange1h()), Ui.changeColor(insights.getChange1h())));
		indicators.add(Ui.row("Change 24h", Ui.percent(insights.getChange24h()), Ui.changeColor(insights.getChange24h())));
		indicators.add(Ui.row("Change 7d", Ui.percent(insights.getChange7d()), Ui.changeColor(insights.getChange7d())));
		indicators.add(Ui.row("RSI (14h)", insights.getRsi() == null ? "-"
			: String.format("%.0f %s", insights.getRsi(), insights.rsiLabel())));
		indicators.add(Ui.row("MACD", insights.macdLabel(),
			insights.getMacdHistogram() == null ? Color.WHITE : insights.getMacdHistogram() >= 0 ? Ui.GOOD : Ui.BAD));
		indicators.add(Ui.row("vs 24h average", Ui.percent(insights.getPriceVsSma24Percent()), Ui.changeColor(insights.getPriceVsSma24Percent())));
		indicators.add(Ui.row("24h range", range(insights.getLow24h(), insights.getHigh24h())));
		indicators.add(Ui.row("7d range", range(insights.getLow7d(), insights.getHigh7d())));
		indicators.revalidate();

		volume.removeAll();
		if (numbers != null)
		{
			volume.add(Ui.row("Buyers last hour", Ui.gp(numbers.getBuyersPerHour())));
			volume.add(Ui.row("Sellers last hour", Ui.gp(numbers.getSellersPerHour())));
		}
		volume.add(Ui.row("Traded last 24h", Ui.gp(insights.getVolume24h())));
		long total = insights.getVolume24h();
		int buyerShare = total == 0 ? 50 : (int) (insights.getBuyers24h() * 100 / total);
		volume.add(Ui.row("Buyers / sellers 24h", buyerShare + "% / " + (100 - buyerShare) + "%"));
		volume.add(Ui.row("Average per day (15d)", Ui.gp(insights.getAverageDailyVolume())));
		if (insights.getAverageDailyVolume() > 0)
		{
			long ratio = insights.getVolume24h() * 100 / insights.getAverageDailyVolume();
			volume.add(Ui.row("Today vs average", ratio + "%", ratio >= 100 ? Ui.GOOD : Ui.WARN));
		}
		volume.revalidate();

		detail.revalidate();
		detail.repaint();
	}

	void showInsightsError(int forItemId)
	{
		if (forItemId != itemId)
		{
			return;
		}
		indicators.removeAll();
		indicators.add(Ui.muted("Couldn't load price history"));
		volume.removeAll();
		volume.add(Ui.muted("Couldn't load price history"));
		detail.revalidate();
		detail.repaint();
	}

	private static JPanel livePrices(ItemMapping item, FlipCandidate numbers)
	{
		JPanel card = Ui.card();
		if (numbers == null)
		{
			card.add(Ui.muted("No recent trades on the GE"));
			return card;
		}
		card.add(Ui.row("Buy at", Ui.gp(numbers.getBuyPrice()) + "  (" + numbers.getBuyPriceAgeMinutes() + "m ago)"));
		card.add(Ui.row("Sell at", Ui.gp(numbers.getSellPrice()) + "  (" + numbers.getSellPriceAgeMinutes() + "m ago)"));
		card.add(Ui.row("GE tax", Ui.gp(numbers.getTax())));
		card.add(Ui.row("Margin after tax", Ui.gp(numbers.getMargin()), Ui.profitColor(numbers.getMargin())));
		card.add(Ui.row("ROI", String.format("%.2f%%", numbers.getRoiPercent()), Ui.profitColor(numbers.getMargin())));
		card.add(Ui.row("Buy limit", numbers.getBuyLimit() > 0 ? Ui.gp(numbers.getBuyLimit()) + " / 4h" : "unknown"));
		card.add(Ui.row("Confidence", "Tier " + numbers.getTier() + " - " + numbers.getConfidence() + "%", numbers.getTier().getColor()));
		// Cards use equal-height rows, so long explanations go in the tooltip rather than a wrapping line.
		JPanel signal = Ui.row("Signal", numbers.getSignal().getDisplayName(), numbers.getSignal().getColor());
		signal.setToolTipText(numbers.getSignal().getDescription());
		card.add(signal);
		if (item.isMembers())
		{
			card.add(Ui.muted("Members item"));
		}
		return card;
	}

	private static String range(Long low, Long high)
	{
		return low == null || high == null ? "-" : Ui.shortGp(low) + " - " + Ui.shortGp(high);
	}
}
