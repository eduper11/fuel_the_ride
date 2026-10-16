# FuelTheRide

FuelTheRide is an extension for the Hammerhead Karoo cycling computer. On a ride it tells you when to drink and when to eat, based on the power you are putting out, your FTP, and what you are carrying.

It does not use a fixed clock such as "drink every 15 minutes". It watches real effort: push harder and the alert comes sooner; ease off and it stretches out. Each alert plays through the Karoo speaker and stays on screen for 30 seconds.

---

## What you need for it to work

Without these four things the extension still opens, but alerts never fire or fire wrong.

1. **A paired, connected power meter.** The app only looks at watts. No power means no expenditure, no countdown, and no alerts. Heart rate is not a substitute.
2. **FTP in the Karoo profile**, or an FTP typed by hand inside FuelTheRide. FTP is the intensity reference. If it is 0, the app does not alert.
3. **Weight in the Karoo profile.** The extension does not ask for it. It scales how much you sweat. If the profile has no usable weight, the app uses 70 kg.
4. **The ⛽ FuelTheRide field on the ride page you are looking at.** The calculation advances while that field is on screen. If you swipe to a page that does not include it, the countdown and the alerts stop until you come back.

Turn the Karoo volume up before you roll out. The beep comes from the internal speaker.

Settings are read **when you start recording**. If you change them while a ride is already running, they apply on the next ride.

---

## 1. Set up the Karoo profile

Do this in the Karoo rider settings, before you open FuelTheRide.

| Value | What it does |
|---|---|
| **FTP (watts)** | Decides whether you are riding easy or hard. The same absolute power is not the same effort for two people: 200 W can be endurance for someone with a 280 W FTP and threshold for someone with a 200 W FTP. |
| **Weight (kg)** | Scales thirst. A heavier rider sweats more at the same intensity, so the drink alert comes sooner. The factor runs from 0.7 (about 49 kg or less) to 1.4 (about 98 kg or more), with 70 kg as 1.0. |

On the FuelTheRide screen, if the Karoo already has a profile, you will see a green line like `Perfil Karoo · FTP: 250 W · Peso: 72 kg`. Check that both numbers are yours. If FTP shows 0, set it in the Karoo profile, or turn the switch off and type it in.

---

## 2. Open FuelTheRide and set up the ride

Open **FuelTheRide** from the Karoo app drawer (outside a ride). The screen scrolls. When you are done, press **Save & close**. If a required value is missing, the field turns red and the screen stays open.

What you save stays for next time. Only change what actually changed (a different bottle, different food, a different breakfast).

### Power and profile

**Use FTP from Karoo profile** (on by default).

- **On (recommended).** FuelTheRide uses the FTP from the Karoo profile. The watts field disappears. If you later update FTP on the Karoo, the extension picks it up on its own.
- **Off.** You type FTP in watts, for example `250`. It has to be a whole number greater than 0. Use this only if the Karoo profile is not yours (a shared bike, a test profile) or if you want to ride against a different threshold.

If the switch is on but the profile FTP is 0, the app falls back to the manual value. If that is empty too, **there will be no alerts**.

Weight is not edited here. It always comes from the Karoo profile.

### Bottle

The bottle does not set how many minutes between drinks. It sets **how many carbohydrates each sip contributes** and **how many sips of that mix you have left**.

**Bottle size.** Size of one bottle.

- `500 ml` or `750 ml` (750 is the default).
- `Custom`: a field appears for millilitres. It has to be a number; if you leave it empty, save is blocked.

**Carbs in bottle (g total).** Carbohydrate grams in **one full bottle**, not in one sip.

- `0` for plain water. Each sip counts as hydration and adds **no** carbohydrates. You will eat more often, because food has to cover the burn on its own.
- A number, for example `60`, if the bottle is a carb drink (powder, sports drink, mix). The app spreads those grams across the whole volume.

Example: 60 g in a 750 ml bottle is 0.08 g per millilitre. Each sip is 150 ml, so that sip contributes about **12 g**. Those 12 g count as carbohydrate already taken and **delay the next eat alert**.

**Nº de bidones con esa mezcla (recargas).** How many bottles you are carrying with that same mix.

- `1` (default): only the bottle you have now. Once it is empty, later sips count as water (0 g).
- `2`: one bottle plus one identical refill. At 750 ml that is 1,500 ml of mix. At 150 ml per sip, the first 10 sips carry carbohydrates; after that the drink alert continues, but it no longer adds grams.
- An empty field counts as 1. A 0 is also stored as 1.

Running out of mix does not change the thirst alert. It only changes what that sip contributes as food. From then on, eat alerts come closer together, because the bottle is no longer covering carbohydrates.

**Contains electrolytes / salts.** Turn it on if the bottle has salts or electrolytes, so your setup remembers that. It does **not** change drink frequency, sip size, or the grams the app counts. Thirst is calculated from power, weight, and temperature, not from this switch.

### Before the ride

This is for breakfast or the snack before you leave. Those carbohydrates fill an initial buffer: they **delay the first eat alert** and slow the rise in demand that appears as muscle glycogen runs down.

**g CH previos.** Grams you ate before getting on the bike. Empty or 0 means you start with no buffer, and the first eat alert comes sooner.

**horas antes.** How long ago that meal was when you press record. Decimals are allowed (`1.5`). If you leave it empty, the app uses **1 hour**.

It does not count the full amount. The longer ago you ate, the less is still available. The math uses a 3-hour half-life and ignores anything past 6 hours (if you enter 8, it is treated as 6):

| Hours before | Share that still counts |
|---|---|
| 0 (you just ate) | 100% |
| 0.5 | ~89% |
| 1 | ~79% |
| 2 | ~63% |
| 3 | 50% |
| 4 | ~40% |
| 6 or more | 25% |

Example: 80 g one hour ago leaves about 63 g of buffer. 80 g three hours ago leaves 40 g. That buffer is spent at the pace of the ride. The app will not remind you of that meal.

### Food you are carrying

Up to four items. Each row is the **name** (what you want to read on the alert) and the **carbohydrate grams of one piece**, not of the whole pocket.

The app **rotates in order**. The first eat alert asks for item 1, the second for item 2, and after the last item it returns to the first. The alert shows the name and the grams, for example `Gel 30g` or `Barrita 40g`.

Rules when you save:

- An empty row is ignored. You can leave rows 3 and 4 blank.
- You need **at least one** row with a name and with grams greater than 0. Otherwise save is blocked.
- A name with grams left at 0 or empty does not count.
- The name should not contain `:` or `;`. Those two characters are stripped when you save.

What the piece size means: the alert fires when the carbohydrate you still owe reaches **the grams of the next piece**. A 25 g gel is requested sooner, and more often, than a 45 g bar. Do not guess grams from calories. Use the carbohydrate number on the wrapper of **one** unit.

If you never set this up, the default pantry is a **Gel of 30 g**.

The first eat of the ride comes a little sooner than the later ones: when you start recording, the app already treats half a piece as spent, so you do not wait to build a full gel of deficit from zero. Later intakes wait for the full piece. Carbohydrates you drink from the bottle are subtracted from that debt and space the meals further apart.

### Save

**Save & close** closes the app and stores the setup. You will see `Settings saved!`.

Open the app again if you want to review: the fields come back with the last values you saved.

---

## 3. Put the field on the ride screen

1. Edit the Karoo ride profile (the screen you use while pedaling).
2. Add a data field and find the **FuelTheRide** extension.
3. Choose **⛽ FuelTheRide**. It is the only field. It is graphical and works best in a large slot: two halves and large numbers.

In the screen editor you will see a **short demo**: a countdown of about 8 seconds and a blink of "150 ml". That is not the real timing of your ride. It is there so you can recognize the field. Real timing appears once you are recording, with power.

Leave it on the page you look at while you ride. If you hide it on a page you never return to, the engine stops with it.

---

## 4. On the ride

Start recording as usual. FuelTheRide starts on its own. You do not open the app mid-ride.

Until you pedal with power for the first time, both halves show `--:--`. There is nothing to measure yet.

### The two halves

| Half | What it is |
|---|---|
| **Left, blue, 💧** | Drink. The bar drains from the top as the next sip gets closer. The large number is the countdown `M:SS`. |
| **Right, green, 🍌** | Food. The same, with the countdown to the next piece. |

The countdown is not a fixed timer. It is recalculated from recent pace. Hit a climb and the number drops faster. Ease off and it stretches. It can move even though a clock second has not passed, because demand changed.

### The alert

When it is time to take something:

- A beep plays through the Karoo speaker.
- That half **blinks for 30 seconds**, alternating what to take and the countdown to the next one.
- The bar on that half fills back up immediately, because the app already counts the sip or the piece as taken.

| What is due | What blinks | How it sounds |
|---|---|---|
| Drink only | `150 ml` on the blue half | Two short rising tones |
| Eat only | The name and the grams, for example `Gel 30g`, on the green half | Three rising tones |
| Drink and eat together | Both halves at once | The drink melody, a pause, then the eat melody |

After 30 seconds the normal countdown returns. You do not tap anything to confirm. **The app does not check whether you actually drank or ate.** The moment the alert fires, it already adds those 150 ml and, if it is an eat alert, those grams. If you ignore the alert, the next one is calculated as if you had followed it. If you drink or eat on your own between alerts, the app does not know and does not subtract it.

The sip is always **150 ml**. You cannot configure it. What changes is **when** it arrives: more heat, more weight, or more intensity shortens the gap between sips.

### Pause and descents

- **Pause.** Time, energy, and countdowns freeze. When you resume, they continue from where they were. Settings are not re-read.
- **You stop pedaling** (a descent, a junction, a stopped wheel) **after** you have already pedaled. Demand does not drop to zero. The body is still spending, so the countdown continues, with a floor of about 30 g/h of carbohydrate and about 450 ml/h of fluid (adjusted for weight and for what you have already burned). `--:--` only appears before the first pedal stroke, or if there is no FTP or no power.
- **You stop the recording.** That ride's counters reset on the next one. The setup (bottle, food, FTP) is kept.

### Temperature

If the Karoo is reporting ambient temperature, heat raises thirst and cold lowers it a little. Reference: 20 °C changes nothing. Each degree above that adds about 4.5%, with a ceiling of 1.75 (very hot) and a floor of 0.85 (cold). If there is no temperature, the app assumes 20 °C and does not invent an extra.

Rough example, 70 kg, riding hard (near FTP): about 850 ml/h at 20 °C (a 150 ml sip every 10–11 minutes) and about 1,230 ml/h at 30 °C (a sip every 7 minutes or so).

---

## How alerts are decided

This is so you can predict what each setting will do, without changing things to see what happens.

Intensity is not the watt of this second (it jumps around) and not only the average of the whole ride (that washes out the climb you are on). It uses an **effective power**:

- 75% the average of the **last 20 minutes**
- 25% the average of the **whole activity**

That power, compared with your FTP, sets grams and millilitres per hour. Every second adds a little of "what you should already have taken". When the amount still owed reaches one sip (150 ml) or the next food item, the alert fires.

### Carbohydrates

Starting demand, before you have been out for a long time, looks like this:

| Recent effort | Grams per hour at the start |
|---|---|
| Up to 55% of FTP (easy) | 30 g/h |
| Around 80% of FTP | 60 g/h |
| 105% of FTP or more | 90 g/h |

Between 55% and 105% the rise is continuous, not in steps.

As glycogen runs down, demand also rises by up to **50%**, and never goes above **90 g/h**. That rise completes around 1,800 kJ of work (several hours, depending on power). Carbohydrates eaten before the ride delay that rise a little.

The 90 g/h ceiling is about as much as the gut usually absorbs from a glucose-plus-fructose mix. Even if you are burning more, the app will not ask for more than that per hour.

Bottle carbohydrates and food carbohydrates go into the same total. Drinking a mix makes the solid piece come later.

### Drink

While pedaling, starting thirst runs from about **450 ml/h** (at 50% of FTP and below, already at the floor) to about **850 ml/h** at FTP or above, for 70 kg and 20 °C. That is then multiplied by weight and by temperature.

The alert is a 150 ml sip. At 600 ml/h that is a sip every 15 minutes. At 900 ml/h, every 10. Those minutes are not a setting. They come from pace, weight, and heat.

---

## Examples

### Easy ride, water, and a gel

- FTP from the profile, 750 ml bottle, bottle carbohydrates `0`, refills `1`.
- Food 1: `Gel` / `30`.
- Before the ride: empty.

You will drink at a low rate (around 450–600 ml/h if it is not hot), and the gel comes when about 30 g of demand has built up, a bit sooner the first time. Water does not delay the gel.

### Long ride with a carb drink and two bottles

- 750 ml bottle, carbohydrates `60`, refills `2`.
- Food 1: `Gel` / `30`. Food 2: `Barrita` / `40`.
- Before: `60` g and `1` hour (about 48 g still counts).

The first 1,500 ml (two bottles) add carbohydrate on every 150 ml sip, about 12 g. That spaces out the gel and bar alerts. After those 1,500 ml, sips continue but at 0 g, and food is requested more often. On screen the order is Gel, Barrita, Gel, Barrita…

### You want to eat less often

Raise the grams of each piece to what you actually eat in one go (one 40 g bar, not two 20 g pieces written as 20). You cannot stretch the interval by hand: that setting does not exist. A larger piece waits for a larger deficit before it sounds.

### It is hot and you are sweating a lot

You do not need to raise any interval. With the Karoo temperature and your weight, the 150 ml sip is requested sooner. Check that the refill count covers the carb mix you are carrying. If you only have one bottle of water (`carbohydrates 0`, `refills 1`), drink alerts do not stop when it is empty: the app keeps asking for sips even if the physical bottle is dry. Refills only separate carb mix from water. They do not calculate whether you still have fluid left.

---

## Checklist before you hit record

- The power meter is showing watts on the Karoo.
- The green FuelTheRide line shows your FTP and your weight, not a 0.
- The bottle size is the real one.
- Bottle grams are for **one** full bottle. Zero if it is water.
- Refills are the bottles you are carrying **with that mix**, not the plain-water bottles you add later.
- Each food item has the name you want to read and the carbohydrate grams of one piece.
- If you ate before leaving, grams and hours are filled in. If you did not, leave them at zero.
- You pressed **Save & close** after the last change.
- The **⛽ FuelTheRide** field is on the page you will be looking at.
- The Karoo volume is audible.
