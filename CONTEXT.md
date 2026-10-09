# Glossary

How Spade's words are used in code, docs and issues. Format: **Term**: definition. _Avoid_: words that cause confusion.

- **Poker night**: one evening of play by the group, from the first buy-in to the last cash-out. The unit the ledger records. _Avoid_: session (the code's `GameSession` is a different thing).
- **Table**: one physical table and its Spade state: seats, the current hand, the pot. _Avoid_: room, lobby.
- **Seat**: a player's place at the table, holding their stack. _Avoid_: slot.
- **Hand**: one deal, from the blinds to the showdown or the last fold. _Avoid_: round (the code's `Round` means a hand; new code says hand).
- **Street**: one betting round inside a hand: pre-flop, flop, turn, river. _Avoid_: stage (the code's `Stage`).
- **Hole cards**: a player's two private cards. _Avoid_: player cards, hand cards.
- **Board**: the up to five shared cards in the middle. _Avoid_: community cards in UI copy (fine in code), table cards.
- **Scan**: one camera read of cards: a phone reading hole cards, or the table camera reading the board. _Avoid_: detection (that is the model's output), recognition.
- **Correction**: a person overriding a scan by hand. Always possible (PRODUCT.md). _Avoid_: manual mode.
- **Stack**: the chips a player has at the table right now. _Avoid_: chips (ambiguous with physical chips), balance.
- **Bankroll**: a player's money outside any table; buy-ins come from it, cash-outs go back to it. _Avoid_: balance (the code's `User.balance`), wallet.
- **Buy-in**: moving money from bankroll to a new stack when sitting down.
- **Cash-out**: moving a stack back to the bankroll when leaving.
- **Ledger**: the record of buy-ins, cash-outs and results per poker night; it replaces the spreadsheet.
- **Pot**: the chips bet in the current hand. **Side pot**: a pot only some players can win, created when someone is all-in for less.
- **Showdown**: the end of a hand where the remaining hole cards are compared and the pot is paid out.
- **Hub**: the web dashboard on the big shared screen: the table, plus the night's pages (ledger, cheatsheet, music). _Avoid_: TV app, client (the legacy `client/`).
- **Player app**: the native iPhone app each player uses at the table: join, scan, bet. _Avoid_: webapp (the legacy `webapp/` it replaces), phone app.
