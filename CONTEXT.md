# Glossary

How Spade's words are used in code, docs and issues. Format: **Term**: definition. _Avoid_: words that cause confusion.

- **Poker night**: one evening of play by the group, from the first buy-in to the last cash-out. The unit the ledger records. _Avoid_: session (the code's `GameSession` is a different thing).
- **Table**: one physical table and its Spade state: seats, the current hand, the pot. _Avoid_: room, lobby.
- **Seat**: a player's place at the table, holding their stack. _Avoid_: slot.
- **Host**: the player who opens a table for a poker night, sets its blinds and closes it. _Avoid_: owner, admin.
- **Dealer**: the player holding the dealer button for the current hand. They deal the cards, start the hand in Spade and enter the board until the table camera reads it. The duty moves with the button. _Avoid_: croupier, host.
- **Hand**: one deal, from the blinds to the showdown or the last fold. _Avoid_: round (the code's `Round` means a hand; new code says hand).
- **Street**: one betting round inside a hand: pre-flop, flop, turn, river. _Avoid_: stage (the code's `Stage`).
- **Hole cards**: a player's two private cards. _Avoid_: player cards, hand cards.
- **Board**: the up to five shared cards in the middle. _Avoid_: community cards in UI copy (fine in code), table cards.
- **Scan**: one camera read of cards: a phone reading its owner's hole cards or the board, or the table camera reading the board. _Avoid_: detection (that is the model's output), recognition.
- **Typed entry**: cards tapped in on a phone's card picker instead of scanned. As good as a scan; never a fallback mode. _Avoid_: manual mode.
- **Correction**: a person overriding by hand what Spade recorded: a scanned or typed card, or a stack (by a chip count). Always possible, and the hand never stops for it (PRODUCT.md). _Avoid_: manual mode.
- **Chips**: the physical chips on the table. They are the truth for every stack and pot ([ADR 0006](docs/adr/0006-physical-chips-are-the-truth-spade-mirrors-them.md)). _Avoid_: tokens.
- **Stack**: Spade's mirror of the chips in front of a player right now. When the two disagree, the chips win. _Avoid_: chips (the physical objects), balance.
- **Point**: the unit of chips, bets, stacks and pots at the table. The chips are 5, 10, 25 and 100 points; 100 points = €1. Ledger amounts are in euros. _Avoid_: euros at the table, credits.
- **Chip count**: counting a player's chips by hand to set their stack: at cash-out, or whenever the mirror drifts. A kind of correction.
- **Buy-in**: money a player turns into chips when sitting down or topping up; Spade records the amount.
- **Cash-out**: the chips a player counts and hands back when leaving; Spade records the counted amount, not its mirror.
- **Ledger**: the record of every poker night: each player's buy-ins, cash-out and result. It replaces the spreadsheet. Money changes hands outside Spade. _Avoid_: bankroll, balance (the legacy `User.balance`; the group never kept running balances), wallet.
- **Result**: one player's cash-out minus their buy-ins for one poker night. A night's results sum to zero.
- **P&L**: a player's results summed over poker nights. _Avoid_: balance.
- **Pot**: the chips bet in the current hand. **Side pot**: a pot only some players can win, created when someone is all-in for less.
- **Showdown**: the end of a hand where the remaining hole cards are compared and Spade says who wins each pot. The chips are pushed by hand.
- **Table view**: what one viewer may see of a table right now. The hub sees the public view; a player sees it plus their own seat. Nobody's view holds another seat's hole cards before showdown.
- **Invite code**: the group's shared code a person enters once to create an account. The host passes it on; without it nobody can join. _Avoid_: password, key.
- **Device**: a phone or hub screen that is logged in to an account. Each has its own session, a player sees theirs listed, and revoking one ends it at once. _Avoid_: login, token (in UI copy).
- **Pairing**: how a hub screen logs in: it shows a code, the host approves it on their phone, and the hub gets the public view only.
- **Win probability**: each remaining player's chance to win the pot, shown once all their hole cards are known and no more betting can happen (an all-in, or the river). _Avoid_: odds, equity in UI copy (fine in code).
- **Hub**: the web dashboard on the big shared screen: the table, plus the night's pages (ledger, cheatsheet, music). _Avoid_: TV app, client (the legacy `client/`).
- **Player app**: the native iPhone app each player uses at the table: join, scan, bet. _Avoid_: webapp (the legacy `webapp/` it replaces), phone app.
