# FredFlips

An open-source RuneLite plugin that finds Grand Exchange flips using the
[OSRS Wiki real-time prices API](https://oldschool.runescape.wiki/w/RuneScape:Real-time_Prices),
and tracks your own trades. No account, no server of our own, and none of your data leaves your computer.
The only thing it downloads is public price data from the wiki. Your trade log is saved in your RuneLite
settings.

## Tabs

| Tab | What it does |
|---|---|
| **Flips** | Recommended flips with buy/sell prices, margin after tax, quantity, confidence, tier and signal |
| **Plan** | Splits your Cash stack across your free GE slots using the safest flips, and tracks their progress |
| **Active** | Your 8 GE slots live: status, fill progress, time waiting, and whether your price is still competitive. Plus items you've bought but not sold |
| **Done** | Completed flips with real profit after tax, today's and all-time stats, and the full trade log |
| **Movers** | Dumps, spikes and high alch profits |
| **Item** | Search any item: live prices, a 24h/7d/30d chart, technical indicators and volume |
| **Money** | Processing money makers (enchanting, fletching, herblore, crafting...) ranked by live GP/hr |

## Money makers

The Money tab uses about 300 buy-make-sell methods from the OSRS Wiki's
[money making guides](https://oldschool.runescape.wiki/w/Money_making_guide), with live GE prices. Guides that
pick the best option on the fly (Plank Make, Superglass Make, Tan Leather, enchanting topaz and dragonstone
jewellery, gem-tipped dragon bolts, poisoning ammunition and others) are split into one method per option, so
FredFlips can rank them all. By default only bank-standing methods are shown.

- **Instant:** inputs bought at the instant-buy price, outputs sold at the instant-sell price, minus GE tax (worst case).
- **Realistic:** instant GP/hr, cut down when an input's buy limit or the output's hourly market volume can't
  keep up with a full hour. This is the default ranking.
- **With offers:** inputs and outputs traded with patient offers (best case).

Actions per hour are the wiki's estimates. When you're logged in, methods you don't have the levels for are
hidden. To refresh the list of methods, run `python tools/build_money_makers.py`.

The money maker data in `src/main/resources/com/fredflips/money_makers.json` comes from the OSRS Wiki and is
licensed [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/), not BSD.

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

## GE helpers

- **Find on GE:** click it on a Plan entry or in the Item tab, then click Buy on an empty GE slot. The item
  search is filled in for you.
- **Price and quantity clicks:** when the GE's quantity or price box opens, a clickable line shows the plan's
  numbers (or FredFlips' usual prices). Click it to put the number in the box.

Nothing is entered until you click, and you always press Enter and Confirm yourself. Both can be turned off
under *GE helpers*.

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
