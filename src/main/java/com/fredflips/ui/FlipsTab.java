package com.fredflips.ui;

import com.fredflips.FredFlipsConfig;
import com.fredflips.flip.FlipCandidate;
import com.fredflips.flip.FlipPreset;
import com.fredflips.flip.PriceFloor;
import com.fredflips.flip.ProfitFloor;
import com.fredflips.flip.SortOrder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.game.ItemManager;

/**
 * Recommended flips: "Updated 13:57" and a Filter button, an optional filter panel, ten flips per page.
 */
class FlipsTab extends JPanel
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
	private static final int PAGE_SIZE = 10;

	private final ItemManager itemManager;
	private final PanelActions actions;
	private final JComboBox<SortOrder> sortBox = new JComboBox<>(SortOrder.values());
	private final JLabel updated = Ui.muted("Loading prices...");
	private final JPanel filters;
	private final JPanel list = Ui.stack();
	private final JLabel pageLabel = Ui.muted("");
	private final JButton previous = new JButton("<");
	private final JButton next = new JButton(">");

	private List<FlipCandidate> flips = new ArrayList<>();
	private int page;

	FlipsTab(ItemManager itemManager, PanelActions actions, FredFlipsConfig config)
	{
		super(new BorderLayout(0, 4));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;

		updated.setToolTipText("Prices from the OSRS Wiki. Click to refresh now.");
		updated.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent e)
			{
				actions.refreshPrices();
			}
		});

		filters = buildFilters(config);
		filters.setVisible(false);
		JButton filterButton = new JButton("Filter");
		filterButton.setFont(Ui.SMALL_BOLD);
		filterButton.setFocusPainted(false);
		filterButton.addActionListener(e ->
		{
			filters.setVisible(!filters.isVisible());
			filterButton.setText(filters.isVisible() ? "Filter ^" : "Filter");
			revalidate();
		});

		JPanel bar = Ui.split(updated, filterButton);
		bar.setBorder(new EmptyBorder(2, 0, 2, 0));
		JPanel top = Ui.stack();
		Ui.addSpaced(top, bar);
		top.add(filters);
		add(top, BorderLayout.NORTH);

		list.add(Ui.note("Loading prices..."));
		add(list, BorderLayout.CENTER);

		previous.addActionListener(e -> showPage(page - 1));
		next.addActionListener(e -> showPage(page + 1));
		pageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		JPanel pager = new JPanel(new BorderLayout(6, 0));
		pager.setOpaque(false);
		pager.setBorder(new EmptyBorder(6, 0, 0, 0));
		pager.add(previous, BorderLayout.WEST);
		pager.add(pageLabel, BorderLayout.CENTER);
		pager.add(next, BorderLayout.EAST);
		add(pager, BorderLayout.SOUTH);
	}

	private JPanel buildFilters(FredFlipsConfig config)
	{
		JComboBox<FlipPreset> preset = new JComboBox<>(FlipPreset.values());
		preset.setSelectedItem(config.flipPreset());
		preset.setToolTipText(config.flipPreset().getDescription());
		preset.addActionListener(e ->
		{
			FlipPreset chosen = (FlipPreset) preset.getSelectedItem();
			preset.setToolTipText(chosen.getDescription());
			actions.changeSetting("flipPreset", chosen);
		});

		sortBox.setSelectedItem(config.sortOrder());
		sortBox.addActionListener(e -> actions.changeSort((SortOrder) sortBox.getSelectedItem()));

		JComboBox<PriceFloor> minPrice = new JComboBox<>(PriceFloor.values());
		minPrice.setSelectedItem(config.flipMinBuyPrice());
		minPrice.addActionListener(e -> actions.changeSetting("flipMinBuyPrice", minPrice.getSelectedItem()));

		JComboBox<ProfitFloor> minProfit = new JComboBox<>(ProfitFloor.values());
		minProfit.setSelectedItem(config.flipMinProfit());
		minProfit.addActionListener(e -> actions.changeSetting("flipMinProfit", minProfit.getSelectedItem()));

		JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
		panel.setBackground(Ui.ROW_ALT_BG);
		panel.setBorder(new EmptyBorder(8, 8, 8, 8));
		panel.add(labelled("Preset", preset));
		panel.add(labelled("Sort", sortBox));
		panel.add(labelled("Min price", minPrice));
		panel.add(labelled("Min profit", minProfit));
		panel.add(toggle("F2P only", "flipF2pOnly", config.flipF2pOnly()));
		panel.add(toggle("Tax-free only", "flipTaxFreeOnly", config.flipTaxFreeOnly()));
		panel.add(toggle("Only what I can afford", "flipCapitalFilter", config.flipCapitalFilter()));
		return panel;
	}

	private static JPanel labelled(String name, JComboBox<?> box)
	{
		box.setFont(Ui.SMALL);
		JPanel row = new JPanel(new BorderLayout(6, 0));
		row.setOpaque(false);
		JLabel label = Ui.muted(name);
		label.setPreferredSize(new java.awt.Dimension(60, 0));
		row.add(label, BorderLayout.WEST);
		row.add(box, BorderLayout.CENTER);
		return row;
	}

	private JCheckBox toggle(String name, String key, boolean selected)
	{
		JCheckBox box = new JCheckBox(name, selected);
		box.setFont(Ui.SMALL);
		box.setForeground(Color.WHITE);
		box.setOpaque(false);
		box.addActionListener(e -> actions.changeSetting(key, box.isSelected()));
		return box;
	}

	void setSortOrder(SortOrder order)
	{
		if (sortBox.getSelectedItem() != order)
		{
			sortBox.setSelectedItem(order);
		}
	}

	void showFlips(List<FlipCandidate> flips, long fetchedAtEpochSeconds)
	{
		updated.setText("Updated " + TIME_FORMAT.format(Instant.ofEpochSecond(fetchedAtEpochSeconds)));
		this.flips = flips;
		showPage(Math.min(page, pageCount() - 1));
	}

	private int pageCount()
	{
		return Math.max(1, (flips.size() + PAGE_SIZE - 1) / PAGE_SIZE);
	}

	private void showPage(int requested)
	{
		page = Math.max(0, Math.min(requested, pageCount() - 1));
		list.removeAll();
		if (flips.isEmpty())
		{
			list.add(Ui.note("No flips match. Try another preset, or loosen the filters or safety settings."));
		}
		int start = page * PAGE_SIZE;
		boolean alternate = false;
		for (FlipCandidate flip : flips.subList(Math.min(start, flips.size()), Math.min(start + PAGE_SIZE, flips.size())))
		{
			Ui.addTight(list, new FlipRow(flip, itemManager, actions, alternate));
			alternate = !alternate;
		}
		pageLabel.setText((page + 1) + " / " + pageCount());
		previous.setEnabled(page > 0);
		next.setEnabled(page < pageCount() - 1);
		list.revalidate();
		list.repaint();
	}
}
