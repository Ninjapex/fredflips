# FredFlips

An open-source RuneLite plugin that finds Grand Exchange flips using the
[OSRS Wiki real-time prices API](https://oldschool.runescape.wiki/w/RuneScape:Real-time_Prices),
and tracks your own trades. No account, no server of our own, and none of your data leaves your computer.
The only thing it downloads is public price data from the wiki. Your trade log is saved in your RuneLite
settings.

## Tabs

| Tab | What it does |
|---|---|
| **Plan** | Type your GP, press Build plan: FredFlips splits it across your free GE slots using the safest flips, with live bought/sold progress, a status badge and a Swap button per item |
| **Item** | Search any item: live prices, buy-limit usage, a 2h-30d chart, technical indicators, volume, 90-day range, trend, liquidity and a price projection. Star it to add it to Favs |
| **Favs** | Your watchlist with live prices |
| **Flips** | Recommended flips, 10 per page, with a Filter panel: 12 presets, 11 sorts, min price, min profit, F2P only, tax-free only and "only what I can afford" |
| **Trades** | Profit and performance for today, this week, this month or all time; your GE slots (Active), completed flips and trade log (Recent), and profit by item (Margin) |
| **Other** | Dumps, Spikes, Dips, Volatile, High alch, Decant, Item sets, ATL (all-time lows), Arbitrage (time of day) and Money makers |

## On the Grand Exchange

- Each active offer is outlined green, grey or red by how competitive its price is, with a time-on-sale timer,
  a fill counter and the time since its last fill. Hover it for details. Colours and timings are adjustable.
- The price box shows a clickable ladder - Quick, FredFlips (or Plan) and Patient - with hotkeys (I, O, P by
  default). The quantity box suggests the plan's or FredFlips' quantity, never more than your buy limit allows.
- Find on GE fills in the item search for you, and picking an item opens it in the Item tab.
- A +gp / -gp drop floats above the GE when a sale completes a flip.
- Hovering an item you bought in your inventory shows what you paid and its profit if sold now.

Nothing is entered until you click or press a hotkey, and you always press Enter and Confirm yourself.

## Confidence, tiers and signals

These are rules of thumb, not predictions.

**Confidence (0-100)** mixes five things: liquidity (30%, trades per hour on the slower side),
stability (25%, whether the margin also existed on average over the last hour and day), trend (20%, how
far the last hour has drifted from the daily average), freshness (15%, how recently both prices traded)
and balance (10%, whether buyers and sellers trade in similar numbers).

**Tiers:** S 85+, A 70+, B 55+, C below 55.

**Signals:** Stable, Rising or Falling compare the last hour with the daily average (1.5% or more counts as
a move). Spiking or Dumping means the latest trade is 5% or more away from the daily average. Spiking,
Dumping and Falling items are hidden from the flip list by default.

## Technical indicators (Item tab)

From the item's hourly price history: % change over 1h/24h/7d, RSI (14 hours, Wilder's smoothing),
MACD (12/26/9), price vs its 24 hour moving average, 24h and 7d price ranges, and volume (last hour,
last 24h, buyer/seller split, 15-day daily average).

## Flip numbers

| Value | Where it comes from |
|---|---|
| Buy price | The highest of the latest instant-sell price, its 5 minute average and its 1 hour average, plus 1gp |
| Sell price | The lowest of the latest instant-buy price, its 5 minute average and its 1 hour average, minus 1gp |
| GE tax | 2% of the sell price, rounded down, capped at 5m per item. Exempt items pay 0 |
| Margin | Sell price - tax - buy price |
| ROI | Margin / buy price |
| Quantity | The smallest of: buy limit, what your cash stack can afford, and one hour of the slower side's trades |
| Potential profit | Margin x quantity |

Hover over an item for more detail, or click it to open it in the Item tab.

## Trade tracking

FredFlips watches your GE slots and records every fill. Sells are matched to your earlier buys of the same
item, oldest first, to work out profit after tax. Sells of items bought before tracking started are left
out of the profit figures. Trades on seasonal, Deadman, beta and other special worlds aren't tracked.

## Built to be cautious

Every estimate leans pessimistic, so the list shows margins you can realistically get, not best-case ones:

- A short spike or dump (a typo, a panic sale, a manipulation attempt) can't create a fake margin. Prices are
  checked against both the 5 minute and 1 hour averages, and the worse one is used. Items whose price is moving
  fast in either direction drop out of the list.
- Tax is always subtracted.
- Items are hidden if either side hasn't traded in the last 15 minutes, or if fewer than 10 items traded
  on either side in the last hour. You can change both under *Safety filters*.

**This is still only an estimate.** Prices can move between your buy filling and your sell filling,
and other flippers are chasing the same margins. Set *Cash stack* so the plugin never suggests more than
you're willing to risk on one item.

## Running it

You need Java 11 or newer.

```
gradlew.bat run
```

This starts a development RuneLite client with FredFlips loaded. To log in with a Jagex account, follow
[Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

Run the tests with:

```
gradlew.bat test
```

## Project layout

```
src/main/java/com/fredflips/
  FredFlipsPlugin.java   starts/stops the plugin, refresh timer
  FredFlipsConfig.java   settings
  wiki/                  downloads and parses wiki price data
  flip/                  GE tax, flip maths, confidence, Plan mode, movers
  insights/              technical indicators
  trades/                GE slot tracking, trade log, profit ledger
  money/                 money maker GP/hr
  ge/                    Find on GE and price/quantity click helpers
  ui/                    sidebar tabs
tools/
  build_money_makers.py  rebuilds the money maker list from the wiki
```

## Licence

BSD 2-Clause. See [LICENSE](LICENSE).
