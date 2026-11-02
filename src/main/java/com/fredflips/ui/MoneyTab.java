package com.fredflips.ui;

import com.fredflips.money.MoneyMethod;
import com.fredflips.money.MoneyResult;
import com.fredflips.money.MoneySort;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;

/**
 * Processing money makers (enchanting, fletching, herblore...) ranked by live GP/hr.
 */
class MoneyTab extends JPanel
{
	private static final String WIKI_URL = "https://oldschool.runescape.wiki/w/";

	private final JComboBox<MoneySort> sortBox = new JComboBox<>(MoneySort.values());
	private final JPanel list = Ui.stack();

	MoneyTab(PanelActions actions, MoneySort initialSort)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);

		sortBox.setSelectedItem(initialSort);
		sortBox.addActionListener(e -> actions.changeMoneySort((MoneySort) sortBox.getSelectedItem()));

		JPanel top = Ui.stack();
		Ui.addSpaced(top, sortBox);
		Ui.addSpaced(top, Ui.note("Live GP/hr from the OSRS Wiki's money making guides. Instant: buy and sell straight"
			+ " away, after tax. Realistic: instant, cut down when buy limits or market volume can't keep up."
			+ " With offers: patient offers, best case."));
		add(top, BorderLayout.NORTH);

		list.add(Ui.note("Loading prices..."));
		add(list, BorderLayout.CENTER);
	}

	void showResults(List<MoneyResult> results, boolean levelsKnown)
	{
		list.removeAll();
		if (!levelsKnown)
		{
			Ui.addSpaced(list, Ui.note("Log in to hide methods you don't have the levels for."));
		}
		if (results.isEmpty())
		{
			list.add(Ui.note("No profitable methods right now."));
		}
		for (MoneyResult result : results)
		{
			Ui.addSpaced(list, row(result));
		}
		list.revalidate();
		list.repaint();
	}

	private static JPanel row(MoneyResult result)
	{
		MoneyMethod method = result.getMethod();
		JPanel card = Ui.card();
		card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		card.setToolTipText(tooltip(result));

		card.add(Ui.label(method.getName(), Color.WHITE));
		card.add(Ui.row("Realistic", Ui.signedShortGp(result.getRealisticGpPerHour()) + "/hr", Ui.profitColor(result.getRealisticGpPerHour())));
		card.add(Ui.row("Instant", Ui.signedShortGp(result.getInstantGpPerHour()) + "/hr", Ui.profitColor(result.getInstantGpPerHour())));
		card.add(Ui.row("With offers", Ui.signedShortGp(result.getOffersGpPerHour()) + "/hr", Ui.profitColor(result.getOffersGpPerHour())));
		card.add(Ui.row("Needs", requirements(method), Ui.MUTED));
		if (result.getLimitingInput() != null)
		{
			card.add(Ui.label("Buy limit: " + result.getLimitingInput(), Ui.WARN));
		}
		else if (!result.getThinOutputs().isEmpty())
		{
			card.add(Ui.label("Market can't take a full hour", Ui.WARN));
		}
		else if (result.isStalePrices())
		{
			card.add(Ui.label("Some prices are old", Ui.WARN));
		}

		card.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				LinkBrowser.browse(WIKI_URL + method.getPage().replace(' ', '_'));
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				card.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
			}
		});
		return card;
	}

	private static String requirements(MoneyMethod method)
	{
		if (method.getRequirements() == null || method.getRequirements().isEmpty())
		{
			return method.isMembers() ? "Members" : "Nothing";
		}
		StringBuilder text = new StringBuilder();
		for (MoneyMethod.Requirement requirement : method.getRequirements())
		{
			if (text.length() > 0)
			{
				text.append(", ");
			}
			text.append(requirement.getSkill()).append(' ').append(requirement.getLevel());
		}
		return text.toString();
	}

	private static String tooltip(MoneyResult result)
	{
		MoneyMethod method = result.getMethod();
		StringBuilder html = new StringBuilder("<html><b>").append(method.getName()).append("</b><br>");
		if (method.getActionsPerHour() != null)
		{
			html.append(Ui.gp(Math.round(method.getActionsPerHour()))).append(" actions per hour (wiki estimate)<br>");
		}
		if (method.getNotes() != null)
		{
			html.append("<i>").append(method.getNotes()).append("</i><br>");
		}
		html.append("<br><b>Per hour you buy:</b><br>");
		for (MoneyMethod.Ingredient input : method.getInputsPerHour())
		{
			html.append(Ui.gp(Math.round(input.getQuantity()))).append(" x ").append(input.getName()).append("<br>");
		}
		html.append("<b>Per hour you sell:</b><br>");
		for (MoneyMethod.Ingredient output : method.getOutputsPerHour())
		{
			html.append(Ui.gp(Math.round(output.getQuantity()))).append(" x ").append(output.getName()).append("<br>");
		}
		html.append("<br>Cost per hour: ").append(Ui.gp(result.getCostPerHour())).append(" gp<br>");
		html.append("GE tax per hour: ").append(Ui.gp(result.getTaxPerHour())).append(" gp<br>");
		if (result.getLimitingInput() != null)
		{
			html.append("Buy limit on ").append(result.getLimitingInput()).append(" only covers ")
				.append(Math.round(result.getBuyLimitShare() * 100)).append("% of an hour's use<br>");
		}
		for (String output : result.getThinOutputs())
		{
			html.append("Fewer ").append(output).append(" trade per hour than you'd make<br>");
		}
		return html.append("<i>Click to open the wiki guide</i></html>").toString();
	}
}
