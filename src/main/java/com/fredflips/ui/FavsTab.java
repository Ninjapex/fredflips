package com.fredflips.ui;

import com.fredflips.flip.FlipCandidate;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JPanel;
import net.runelite.client.game.ItemManager;

/**
 * Your watchlist: items you starred in the Item tab, with live prices.
 * "Default" keeps the order you starred them in, "Margin" puts the best margin first.
 */
class FavsTab extends JPanel
{
	private final ItemManager itemManager;
	private final PanelActions actions;
	private final Pills view;
	private final JPanel list = Ui.stack();
	private List<FlipCandidate> favourites = new ArrayList<>();

	FavsTab(ItemManager itemManager, PanelActions actions)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.itemManager = itemManager;
		this.actions = actions;
		view = new Pills(new String[]{"Default", "Margin"}, 0, index -> render());
		add(view, BorderLayout.NORTH);
		add(list, BorderLayout.CENTER);
		render();
	}

	void show(List<FlipCandidate> favourites)
	{
		this.favourites = favourites;
		render();
	}

	private void render()
	{
		List<FlipCandidate> shown = new ArrayList<>(favourites);
		if (view.getSelected() == 1)
		{
			shown.sort(Comparator.comparingLong(FlipCandidate::getMargin).reversed());
		}
		list.removeAll();
		if (shown.isEmpty())
		{
			list.add(Ui.note("No favourites yet. Open an item and click the star to add it here."));
		}
		boolean alternate = false;
		for (FlipCandidate flip : shown)
		{
			Ui.addTight(list, new FlipRow(flip, itemManager, actions, alternate));
			alternate = !alternate;
		}
		list.revalidate();
		list.repaint();
	}
}
