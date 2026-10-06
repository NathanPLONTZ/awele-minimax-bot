# Awele AI — YANNICKBOLASIE

An alpha-beta game-playing bot for **Awele** (Oware), written in Java for a
Master 1 Artificial Intelligence course project.

The bot searches nine plies inside a 250 ms budget. It gets there not by
optimising the evaluation function but by learning two things from self-play:
which moves to search first, and what to play in positions it has already
studied one ply deeper.

---

## Table of contents

- [The game](#the-game)
- [The assignment](#the-assignment)
- [The data](#the-data)
- [How the bot works](#how-the-bot-works)
  - [1. Alpha-beta search](#1-alpha-beta-search)
  - [2. Learned move ordering](#2-learned-move-ordering)
  - [3. Learned opening book](#3-learned-opening-book)
  - [4. The evaluation function](#4-the-evaluation-function)
  - [5. Searching for the weights](#5-searching-for-the-weights)
- [Results](#results)
- [Repository layout](#repository-layout)
- [Running it](#running-it)
- [Notes and known issues](#notes-and-known-issues)

---

## The game

Awele is a traditional African game of the mancala family: an abstract
combinatorial strategy game for two players, with no hidden information and no
chance. The board has twelve holes in two rows of six, one row per player, and
starts with four seeds in each hole. The goal is to capture more seeds than the
opponent.

On your turn you take every seed from one non-empty hole on your side and sow
them one by one into the following holes, continuing round onto the opponent's
row, anticlockwise.

**Capturing.** If the last seed lands in a hole on the opponent's side that then
holds two or three seeds, you capture them. If the hole before that is also on
the opponent's side and also holds two or three seeds, you capture those too,
and so on backwards until you reach your own side or a hole holding some other
number.

**Two rules that catch people out:**

- Sowing twelve seeds or more goes all the way round the board; the hole the
  seeds came from is *skipped* on the way past.
- You may not starve your opponent. If they have no seeds left, you must play a
  move that puts at least one seed on their side. A move that would capture
  *every* seed on their side is legal, but captures nothing.

The game ends when fewer than six seeds remain on the board, or when a player
has no legal move — in which case that player collects the seeds still on their
own side.

```
      A6  A5  A4  A3  A2  A1      <- opponent
    +---+---+---+---+---+---+
    | 4 | 4 | 4 | 4 | 4 | 4 |
    +---+---+---+---+---+---+
    | 4 | 4 | 4 | 4 | 4 | 4 |
    +---+---+---+---+---+---+
      J1  J2  J3  J4  J5  J6      <- you
```

---

## The assignment

The brief was to build a decision engine that picks the best move for a given
position, using one or more AI algorithms. A game engine was supplied; only the
bot had to be written. The constraints shaped the design more than anything
else:

| Constraint | Limit |
|---|---|
| Decision time | **250 ms** per move |
| Learning time | **1 hour**, once, at load time |
| Memory retained | **64 MiB** |
| Peak memory | **1 GiB** |
| Parallelism | **None** — no threads, no distributed computing |
| External data | **None** — only the provided dataset may be loaded |
| Opponent bots | May not be instantiated |

Two further rules mattered:

- **Decisions must not be hard-coded.** The bot has to work something out, not
  look up a table someone typed in.
- Everything lives in a single package containing exactly one class extending
  `CompetitorBot`, with a no-argument constructor.

Bots from every group were then ranked in a round-robin championship: each
pairing is two games, so both bots open once, and the total seeds captured over
the pair decides the winner. A win is worth three points, a draw one, and each
pairing is repeated 100 times.

Grading weighted ingenuity of the solution at 9 points, Java quality at 3, the
written report at 3, and championship performance at 5.

---

## The data

`data/awele.data` holds **303 recorded game situations**, taken from several
games between an experienced player and a novice. The experienced player won
every one of them.

Each row has 14 fields:

| Fields | Meaning |
|---|---|
| 1–6 | Seeds in the holes of the player to move, left to right |
| 7–12 | Seeds in the opponent's holes, right to left |
| 13 | The move that player chose (`C1`–`C6`) |
| 14 | Whether that player went on to win (`G`) or lose (`P`) |

```
J1,J2,J3,J4,J5,J6,A1,A2,A3,A4,A5,A6,C,R
5,1,2,2,8,2,1,0,8,7,6,6,C6,G
6,6,1,6,5,0,0,0,0,8,7,7,C3,G
```

The file is small (12 KB) and is committed, so the project runs as cloned.

`KnnData` is the dataset-driven baseline: it treats each situation as a point in
twelve dimensions and rates a move by how often winners played it among the
nearest neighbours, minus how often losers did. **This approach was not kept.**
303 situations is simply too thin to generalise across a game tree this wide,
and that is what pushed the project towards tree search. The class is retained
as the comparison point.

---

## How the bot works

The short version: a normal alpha-beta search, made to go deeper rather than
made smarter. The central finding of the project is that **one extra ply beats a
better evaluation function** — a deeper search with a crude evaluation reliably
defeats a shallower search with a sophisticated one. Everything below is in
service of depth.

### 1. Alpha-beta search

Plain minimax explores every branch. Alpha-beta carries two bounds: `alpha`, the
best the maximising side can already guarantee, and `beta`, the best the
minimising side can. When `beta <= alpha`, no move in the remaining subtree can
change the parent's decision, so the branch is cut.

How much that saves depends entirely on move order. In the best case — strongest
move first everywhere — alpha-beta examines roughly the square root of the nodes
minimax would. In the worst case it saves nothing. Which leads directly to the
next piece.

### 2. Learned move ordering

Rather than guessing a good order from the position, the bot *measures* it.

Every move is reduced to a key built from three things: which hole it starts
from, how many seeds it sows, and how full the hole it lands in is.

```
moveId(c) = startHole(c) + 6 × seeds(c) + 288 × seedsInLandingHole(c)
```

Moves that look alike under those three criteria share a key, so what is learned
about one transfers to the others. Each key keeps a running score: **+1 every
time that move produced a cut-off, −1 every time it was searched without
producing one.** The search visits moves from the highest score down.

The scores keep updating during matches, so the ordering improves as the bot
plays. This is what buys the ninth ply.

> `BoardSignature.moveId`, `MoveOrdering` in [`src/awele/bot/common/`](src/awele/bot/common/)

### 3. Learned opening book

During self-play the bot is not bound by the 250 ms limit, so it searches to
**depth 10** — one ply deeper than it ever will in a match. After choosing a
move it stores it, keyed by the position:

```
hash ← 17
for each of the 12 holes:  hash ← 31 × hash + seeds
```

A classic polynomial hash over the twelve hole counts, ignoring the scores. No
collision was observed over the self-play runs used to build the book.

In a match, a position found in the book is answered instantly — and answered
with a depth-10 move while the search itself only reaches depth 9. Because the
self-play games are the bot against itself, both sides contribute, so the book
fills twice as fast.

> `BoardSignature.positionId`, `YANNICKBOLASIE.getDecision`

### 4. The evaluation function

The first version was the **score difference alone**. That sounds too simple to
work, and it is remarkably effective — it is the single most informative thing
about a position, and it is cheap, which matters when the evaluation runs at
every leaf.

The version that shipped adds four positional terms. Holes on the opponent's
side holding zero, one or two seeds are *vulnerable* — they are the ones that
become capturable. A hole of our own holding twelve or more is a **krou**, big
enough to sweep the whole board:

```
h(b) = α·scoreDiff(b) + β·emptyHoles(b) + γ·oneSeedHoles(b)
     + δ·twoSeedHoles(b) + ω·krous(b)
```

with the shipped weights `α=5, β=1, γ=2, δ=2, ω=2`.

A second, material-only evaluation is also in the repository, scoring each hole
against fixed thresholds instead of weighting features. It is used by the
NegaMax variant and by `YANNICKBOLASIE2`, which exist to compare the two
evaluations at equal depth.

> `Heuristic.evaluate`, `Heuristic.evaluateMaterial`

### 5. Searching for the weights

Five weights cannot be set by hand without guessing, and the brief forbids
hard-coding decisions. They are searched for instead, by a deliberately simple
hill-climbing loop that runs inside `learn()`:

1. Start from a hand-tuned candidate.
2. Draw a challenger with random weights — `0–10` for the score difference,
   which matters most, `0–5` for the others, `0–2` for empty holes.
3. Play 11 games. Keep whichever side won more.
4. Repeat for 15 minutes.
5. Finally, play the surviving candidate against the one that held the longest
   winning streak, to guard against a lucky last draw.

This runs at **depth 6**, not 9. At depth 9 there would be time for too few
games for any result to mean anything, so the search assumes weights that are
good at a shallow depth stay good deeper. That is a strong assumption, and it is
the weakest link in the method.

> **Honest caveat.** The shipped bot then *discards* what this search found and
> restores the hand-tuned `(5, 1, 2, 2, 2)`, which held up better in testing
> than anything the search produced within its budget. The search is kept
> because it is what justifies those values, but its output is not trusted
> blindly. See the comment at the end of `YANNICKBOLASIE.learn()`.

---

## Results

Measurements below come from the project report unless marked otherwise.

**Depth reached**

| Configuration | Depth within budget |
|---|---|
| Alpha-beta, no optimisation | 8 |
| Alpha-beta + learned move ordering | 9 |
| Self-play, unconstrained | 10 |
| Reference minimax bot provided with the assignment | 6 |

Gaining a single ply was the project's stated goal, and the one it is judged on.
Move ordering alone produced a large drop in decision time at depth 10; the
opening book closed the remaining gap.

**Does the learning pay off?** Two otherwise identical bots searching at depth 9,
one with a book built from depth-10 self-play and one with nothing: the bot with
the book wins. Roughly **20 000 positions** were stored during a learning run.

**Why depth over cleverness.** Increasing depth by one, with a simple
evaluation, beat a shallower search with a more sophisticated one in every test
run. This is the finding the whole design rests on.

**Measured while preparing this repository** (not from the report):

- The reference minimax bot supplied with the assignment **maintains `alpha` and
  `beta` but never breaks out of its loop**, so it performs no pruning at all
  and explores the full tree. Together with allocating one object per node, that
  explains why it stalls at depth 6 while this bot reaches 9. The comparison
  against it is therefore less flattering than it looks.
- Only **2 of the 36 bundled JARs** are actually used.

**Not included here:** the official championship ranking against the other
groups' bots. That was run by the course staff and the result is not part of
this repository, so no placement is claimed.

---

## Repository layout

```
src/awele/
├── bot/
│   ├── Bot.java                  Base class: learn / initialize / getDecision / finish
│   ├── CompetitorBot.java        Extend this to enter the championship
│   ├── DemoBot.java              Extend this to play without being ranked
│   ├── ChampionBot.java          Reference bots from past years
│   │
│   ├── common/                   ── Shared search components ──
│   │   ├── AlphaBetaSearch.java  The search, with optional move ordering
│   │   ├── BoardSignature.java   Position and move keys
│   │   ├── MoveOrdering.java     Cut-off statistics and ranking
│   │   ├── Heuristic.java        Both evaluation functions
│   │   └── KnnData.java          Dataset-driven baseline (not used by the bot)
│   │
│   ├── YANNICKBOLASIE/           ★ The competition bot
│   ├── YANNICKBOLASIE2/          Ablation: plain alpha-beta, nothing learned
│   ├── YANNICKBOLASIEBIS/        Sparring partner: learning, no weight search
│   ├── minmax/                   Reference minimax supplied with the assignment
│   ├── random/                   Example stub, body commented out
│   └── demo/
│       ├── NegaMaxBot/           Negamax formulation, material-only evaluation
│       ├── MemoirePosition/      Ablation: opening book only, no ordering
│       ├── TriCoups/             Ablation: ordering only, no book
│       ├── random/               Plays at random; used to calibrate timings
│       ├── first/, last/         Example stubs: always play leftmost / rightmost
│       └── knn1/, knn2/          Example stubs for the dataset-driven approach
│
├── core/
│   ├── Board.java                Board state and the rules
│   ├── Awele.java                One encounter: two games, sides swapped
│   └── InvalidBotException.java
├── data/                         Loader for data/awele.data
├── output/                       Console and file logging
└── run/Main.java                 Discovers bots, qualifies them, runs the championship

data/awele.data                   303 recorded game situations
requirements.txt                  The two JARs needed, and where to get them
```

**The ablation variants are the point of the `demo/` folder.** `TriCoups` has
the move ordering but no book; `MemoirePosition` has the book but no ordering;
`YANNICKBOLASIE2` has neither. Running them against each other is what shows
which of the two learned components does the work.

### What is provided and what is mine

The game engine — `core/`, `data/`, `output/`, `run/Main.java`, the `Bot` class
hierarchy, the reference `minmax/` bot and the random bot — came with the course
assignment. **The bot is mine:** everything under `bot/common/`,
`bot/YANNICKBOLASIE*/`, and the ablation variants in `bot/demo/`.

---

## Running it

**1. Fetch the two dependencies**

```bash
mkdir -p lib
curl -L -o lib/reflections-0.9.12.jar \
  https://repo1.maven.org/maven2/org/reflections/reflections/0.9.12/reflections-0.9.12.jar
curl -L -o lib/javassist-3.21.0-GA.jar \
  https://repo1.maven.org/maven2/org/javassist/javassist/3.21.0-GA/javassist-3.21.0-GA.jar
```

**2. Compile**

```bash
# Linux / macOS
find src -name "*.java" > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d bin @sources.txt
```

```powershell
# Windows PowerShell
Get-ChildItem -Recurse src -Filter *.java | % FullName > sources.txt
javac -encoding UTF-8 -cp "lib/*" -d bin "@sources.txt"
```

**3. Run the championship**

```bash
# Linux / macOS
java -Xmx2g -cp "bin:lib/*" awele.run.Main
```

```powershell
# Windows
java -Xmx2g -cp "bin;lib/*" awele.run.Main
```

Run it from the repository root, so that `data/awele.data` resolves.

Results are written to the console and to two log files: `awele.log`, and
`awele.anonymous.log` with the author lines stripped. Both are regenerated on
every run and are not committed.

> **Expect a long first run.** `YANNICKBOLASIE.learn()` plays 100 self-play games
> at depth 10, then spends 15 minutes searching for weights — around 20 minutes
> before the championship starts. For a quick look, run `YANNICKBOLASIEBIS`
> instead: same learned components, no weight search, trains in seconds.

**Tuning the run.** Both knobs are at the top of
[`src/awele/run/Main.java`](src/awele/run/Main.java): `NB_RUNS` (games per
pairing, 10 here; the assignment evaluates over 100) and `TOURNAMENT`, which
selects whether competition bots, champion bots, or everything takes part.

---

## Notes and known issues

**Reproducibility.** Results are not bit-for-bit reproducible between runs. When
several moves tie on evaluation, the engine breaks the tie with an unseeded
random number generator, so identical inputs can produce different games. The
competition bot itself is deterministic: given a position it always returns the
same decision vector.

**A preserved defect in `MemoirePosition`.** In its `getDecision`, the decision
vector is only cleared on the match-play branch. During self-play several holes
stay set to `1` at once, the engine sees an ambiguous vector, falls back on its
random tie-break, and the moves actually simulated are not the ones intended.
This makes that bot's output vary from run to run. **It has been left in place**
so the ablation experiment still reports what it reported at the time. The
competition bot does not share this defect.

**Weights are searched, then overridden.** Described in
[section 5](#5-searching-for-the-weights). Worth knowing before reading
`learn()`.

**Identifiers are in French.** Comments and documentation are in English, but
method and field names — `heuristique`, `trieCoup`, `idCoups`, `profondeur`,
`memoirePosition` — were left as they were, so that the code still lines up with
the project report.

**The `this-escape` warnings** on compilation come from the supplied framework:
`setBotName` and `addAuthor` must be called from the bot's constructor, which
the assignment mandates.

**The working notes were kept.** This repository is the project as it was
actually built, not a tidied-up version of it. The example stubs that came with
the assignment are still there with their bodies commented out, the debug
counters and `System.out.println` traces are still in place, and so is the
abandoned weight-search attempt inside `YANNICKBOLASIEBIS.learn()`. The
`probeScore` flag in `AlphaBetaSearch` preserves a redundant capture simulation
that the two earliest ablation bots performed and never read — it is dead work,
but their reported timings were measured with it running, so removing it would
have quietly invalidated the comparison. Expect the first run to be noisy on
standard output.

---

## License

Coursework, published for reference. Feel free to read it, learn from it, and
borrow ideas — but if you are taking the same course, write your own bot.
