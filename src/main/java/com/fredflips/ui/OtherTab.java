package com.fredflips.ui;

import com.fredflips.flip.MoverType;
import java.awt.BorderLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * One dropdown away from every extra list: dumps, spikes, high alch and money makers.
 */
class OtherTab extends JPanel
{
	@Getter
	@RequiredArgsConstructor
	enum Kind
	{
		DUMPS("Dumps", MoverType.DUMPS),
		SPIKES("Spikes", MoverType.SPIKES),
		DIPS("Dips", MoverType.DIPS),
		VOLATILE("Volatile", MoverType.VOLATILE),
		HIGH_ALCH("High alch", MoverType.HIGH_ALCH),
		DECANT("Decant", null),
		ITEM_SETS("Item sets", null),
		ATL("ATL (all-time low)", null),
		ARBITRAGE("Arbitrage (time of day)", null),
		MONEY("Money makers", null);

		private final String displayName;
		private final MoverType moverType;

		@Override
		public String toString()
		{
			return displayName;
		}
	}

	private final MoversTab movers;
	private final MoneyTab money;
	private final DecantView decant;
	private final HistoryView history;
	private final ItemSetView sets;
	private final JComboBox<Kind> kindBox = new JComboBox<>(Kind.values());
	private final JPanel holder = new JPanel(new BorderLayout());

	OtherTab(MoversTab movers, MoneyTab money, DecantView decant, HistoryView history, ItemSetView sets)
	{
		super(new BorderLayout(0, 6));
		setOpaque(false);
		this.movers = movers;
		this.money = money;
		this.decant = decant;
		this.history = history;
		this.sets = sets;

		kindBox.setFont(Ui.SMALL);
		kindBox.addActionListener(e -> show((Kind) kindBox.getSelectedItem()));
		holder.setOpaque(false);

		add(kindBox, BorderLayout.NORTH);
		add(holder, BorderLayout.CENTER);
		show(Kind.DUMPS);
	}

	private void show(Kind kind)
	{
		JComponent content = kind == Kind.MONEY ? money
			: kind == Kind.DECANT ? decant
			: kind == Kind.ITEM_SETS ? sets
			: kind == Kind.ATL || kind == Kind.ARBITRAGE ? history
			: movers;
		holder.removeAll();
		holder.add(content, BorderLayout.CENTER);
		if (kind.getMoverType() != null)
		{
			movers.setType(kind.getMoverType());
		}
		if (kind == Kind.ATL || kind == Kind.ARBITRAGE)
		{
			history.load(kind == Kind.ARBITRAGE);
		}
		holder.revalidate();
		holder.repaint();
	}
}
