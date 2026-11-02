package com.fredflips.ui;

import com.fredflips.flip.FlipCandidate;
import com.fredflips.flip.SortOrder;
import java.awt.BorderLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;

/**
 * Recommended flips.
 */
class FlipsTab extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final JComboBox<SortOrder> sortBox = new JComboBox<>(SortOrder.values());
	private final JPanel list = Ui.stack();

	FlipsTab(ItemManager itemManager, PanelActions actions, SortOrder initialSort)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;

		JPanel controls = new JPanel(new BorderLayout(4, 0));
		controls.setOpaque(false);
		sortBox.setSelectedItem(initialSort);
		sortBox.addActionListener(e -> actions.changeSort((SortOrder) sortBox.getSelectedItem()));
		JButton refresh = new JButton("Refresh");
		refresh.addActionListener(e -> actions.refreshPrices());
		controls.add(sortBox, BorderLayout.CENTER);
		controls.add(refresh, BorderLayout.EAST);

		JPanel top = Ui.stack();
		Ui.addSpaced(top, controls);
		Ui.addSpaced(top, Ui.warning("Estimates only. Prices can move before your offers fill."));
		add(top, BorderLayout.NORTH);

		list.add(Ui.note("Loading prices..."));
		add(list, BorderLayout.CENTER);
	}

	void setSortOrder(SortOrder order)
	{
		if (sortBox.getSelectedItem() != order)
		{
			sortBox.setSelectedItem(order);
		}
	}

	void showFlips(List<FlipCandidate> flips)
	{
		list.removeAll();
		if (flips.isEmpty())
		{
			list.add(Ui.note("No flips pass your filters. Try loosening the safety filters in the settings."));
		}
		for (FlipCandidate flip : flips)
		{
			Ui.addSpaced(list, new FlipRow(flip, itemManager, actions));
		}
		list.revalidate();
		list.repaint();
	}
}
