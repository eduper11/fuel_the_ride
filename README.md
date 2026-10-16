# FuelTheRide

FuelTheRide es una extensión para el ciclocomputador Hammerhead Karoo. Durante la ruta te avisa de cuándo beber y cuándo comer, según la potencia que estás moviendo, tu FTP y lo que llevas encima.

No usa un reloj fijo del tipo «bebe cada 15 minutos». Mira el esfuerzo real: si aprietas, el aviso llega antes; si aflojas, se alarga. Cada aviso suena por el altavoz del Karoo y se queda 30 segundos en pantalla.

---

## Qué necesitas para que funcione

Sin estas cuatro cosas la extensión se abre, pero los avisos no salen o salen mal.

1. **Un potenciómetro emparejado y conectado.** La app solo mira los vatios. Sin potencia no hay gasto, no hay cuenta atrás y no hay avisos. La frecuencia cardíaca no sirve de sustituto.
2. **El FTP en el perfil del Karoo**, o un FTP escrito a mano dentro de FuelTheRide. El FTP es la referencia de intensidad. Si vale 0, la app no avisa.
3. **El peso en el perfil del Karoo.** No se pide dentro de la extensión. Sirve para calcular cuánto sudas. Si el perfil no trae un peso usable, la app usa 70 kg.
4. **El campo ⛽ FuelTheRide en la página de ruta que tienes a la vista.** El cálculo avanza mientras ese campo está en pantalla. Si pasas a otra página que no lo lleva, la cuenta atrás y los avisos se detienen hasta que vuelves.

Sube el volumen del Karoo antes de salir. El pitido sale por el altavoz interno.

Los ajustes se leen **al empezar a grabar**. Si los cambias con la actividad ya en marcha, entran en la siguiente salida.

---

## 1. Prepara el perfil del Karoo

Hazlo en los ajustes de ciclista del propio Karoo, antes de abrir FuelTheRide.

| Dato | Para qué sirve |
|---|---|
| **FTP (vatios)** | Marca si vas suave o fuerte. Misma potencia absoluta no significa lo mismo para dos personas: 200 W pueden ser rodaje para quien tiene 280 W de FTP y umbral para quien tiene 200. |
| **Peso (kg)** | Escala la sed. Una persona más pesada suda más a la misma intensidad, así que el aviso de beber llega antes. El factor va de 0,7 (unos 49 kg o menos) a 1,4 (unos 98 kg o más), tomando 70 kg como 1,0. |

En la pantalla de FuelTheRide, si el Karoo ya tiene perfil, verás una línea verde del estilo `Perfil Karoo · FTP: 250 W · Peso: 72 kg`. Comprueba que esos dos números son los tuyos. Si el FTP sale a 0, entra en el perfil del Karoo y ponlo, o desactiva el interruptor y escríbelo a mano.

---

## 2. Abre FuelTheRide y configura la salida

Abre **FuelTheRide** desde el cajón de aplicaciones del Karoo (fuera de una ruta). La pantalla se desplaza. Al terminar pulsa **Save & close**. Si falta un dato obligatorio, el campo se marca en rojo y no se cierra.

Lo que guardas queda para la siguiente vez. Solo cambia lo que cambie de verdad (otro bidón, otra comida, un desayuno distinto).

### Potencia y perfil

**Use FTP from Karoo profile** (activado por defecto).

- **Activado (recomendado).** FuelTheRide usa el FTP del perfil del Karoo. El campo para escribir vatios desaparece. Si un día actualizas el FTP en el Karoo, la extensión lo recoge sola.
- **Desactivado.** Escribes tú el FTP en vatios, por ejemplo `250`. Tiene que ser un número entero mayor que 0. Úsalo solo si el perfil del Karoo no es el tuyo (una bici compartida, un perfil de prueba) o si quieres simular otro umbral.

Si el interruptor está activado pero el perfil trae FTP 0, la app intenta el valor manual. Si ese también está vacío, **no habrá avisos**.

El peso no se edita aquí. Siempre sale del perfil del Karoo.

### Bidón

El bidón no fija cada cuántos minutos bebes. Fija **cuántos carbohidratos aporta cada trago** y **cuántos tragos de esa mezcla te quedan**.

**Bottle size.** Tamaño de un bidón.

- `500 ml` o `750 ml` (750 es el valor por defecto).
- `Custom`: aparece un campo para los mililitros. Tiene que ser un número; si lo dejas vacío, no deja guardar.

**Carbs in bottle (g total).** Gramos de carbohidrato de **un bidón lleno**, no de un trago.

- `0` si es agua sola. Cada trago cuenta como hidratación y **no** suma carbohidratos. Comerás con más frecuencia, porque la comida tiene que cubrir sola el gasto.
- Un número, por ejemplo `60`, si el bidón lleva bebida con carbohidratos (polvo, bebida deportiva, mixto). La app reparte esos gramos en todo el volumen.

Ejemplo: 60 g en un bidón de 750 ml son 0,08 g por mililitro. Cada trago es de 150 ml, así que ese trago aporta unos **12 g**. Esos 12 g cuentan como carbohidrato ya tomado y **retrasan el siguiente aviso de comida**.

**Nº de bidones con esa mezcla (recargas).** Cuántos bidones llevas con esa misma mezcla.

- `1` (por defecto): solo el bidón que llevas ahora. Al vaciarlo, los tragos siguientes cuentan como agua (0 g).
- `2`: un bidón más una recarga igual. Con 750 ml son 1 500 ml de mezcla. A 150 ml por trago, los 10 primeros tragos llevan carbohidratos y a partir de ahí el aviso de beber sigue, pero ya no suma gramos.
- Si dejas el campo vacío, cuenta como 1. Un 0 también se guarda como 1.

Cuando se acaba la mezcla no cambia el aviso de sed. Solo cambia lo que ese trago aporta de comida. A partir de ese momento los avisos de comer se acercan, porque el bidón ya no te está cubriendo carbohidratos.

**Contains electrolytes / salts.** Actívalo si el bidón lleva sales o electrolitos, para dejarlo anotado en tu configuración. **No cambia** ni la frecuencia de beber, ni los mililitros del trago, ni los gramos que cuenta la app. La sed se calcula por potencia, peso y temperatura, no por este interruptor.

### Antes de la actividad

Sirve para el desayuno o el tentempié de antes de salir. Esos carbohidratos llenan un margen inicial: **retrasan la primera comida** y frenan un poco la subida de demanda que aparece cuando el depósito de glucógeno se va vaciando.

**g CH previos.** Gramos que comiste antes de montar. Vacío o 0 = sales sin ese margen y el primer aviso de comida llega antes.

**horas antes.** Cuánto tiempo ha pasado desde esa comida hasta que das a grabar. Admite decimales (`1.5`). Si lo dejas vacío, la app usa **1 hora**.

No cuenta el total entero. Cuanto más tiempo ha pasado, menos queda disponible. La cuenta usa una vida media de 3 horas y no mira más allá de 6 horas (si pones 8, se trata como 6):

| Horas antes | Parte que aún cuenta |
|---|---|
| 0 (acabas de comer) | 100 % |
| 0,5 | ~89 % |
| 1 | ~79 % |
| 2 | ~63 % |
| 3 | 50 % |
| 4 | ~40 % |
| 6 o más | 25 % |

Ejemplo: 80 g hace 1 hora dejan unos 63 g de margen. 80 g hace 3 horas dejan 40 g. Ese margen se gasta al ritmo de la ruta; no es una comida que la app te vaya a recordar.

### Comida que llevas

Hasta cuatro piezas. En cada fila: el **nombre** (lo que quieres leer en el aviso) y los **gramos de carbohidrato de una pieza**, no de todo el bolsillo.

La app **rota en orden**. El primer aviso de comida pide la pieza 1, el segundo la pieza 2, y al acabar la lista vuelve a la primera. El aviso enseña el nombre y los gramos, por ejemplo `Gel 30g` o `Barrita 40g`.

Reglas al guardar:

- Una fila vacía se ignora. Puedes dejar la 3 y la 4 en blanco.
- Hace falta **al menos una** fila con nombre y con gramos mayores que 0. Si no, no deja guardar.
- Si escribes nombre pero dejas los gramos a 0 o vacíos, esa fila no cuenta.
- El nombre no debe llevar `:` ni `;`. Esos dos caracteres se quitan al guardar.

Qué implica el tamaño de la pieza: el aviso salta cuando el carbohidrato que te falta alcanza **los gramos de la siguiente pieza**. Un gel de 25 g se pide antes y más a menudo que una barrita de 45 g. No elijas los gramos «a ojo de calorías»: pon los de carbohidrato del envoltorio de **una** unidad.

Si no configuras nada, la despensa por defecto es un **Gel de 30 g**.

La primera comida de la ruta llega un poco antes que las siguientes: al dar a grabar la app ya considera medio pieza gastada, para no esperar a acumular un gel entero desde cero. Las tomas de después sí esperan la pieza completa. Los carbohidratos que bebas en el bidón descuentan de esa cuenta y separan más las comidas.

### Guardar

**Save & close** cierra la app y deja la configuración lista. Verás el aviso `Settings saved!`.

Abre la app otra vez si quieres revisar: los campos vuelven con lo último que guardaste.

---

## 3. Pon el campo en la pantalla de ruta

1. Edita el perfil de ruta del Karoo (la pantalla que usas al pedalear).
2. Añade un campo de datos y busca la extensión **FuelTheRide**.
3. Elige **⛽ FuelTheRide**. Es el único campo. Es gráfico y ocupa bien un espacio grande: tiene dos mitades y números grandes.

En el editor de pantallas verás una **demostración corta**: una cuenta atrás de unos 8 segundos y un parpadeo de «150 ml». Eso no es el ritmo real de tu salida. Sirve para reconocer el campo. Los tiempos de verdad salen al grabar, con potencia.

Déjalo en la página que miras mientras pedaleas. Si lo escondes en una página a la que no vuelves, el motor se para con ella.

---

## 4. Durante la ruta

Empieza a grabar como siempre. FuelTheRide arranca solo. No hay que abrir la app en marcha.

Hasta que das la primera pedalada con potencia, las dos mitades muestran `--:--`. En ese momento aún no hay esfuerzo que medir.

### Las dos mitades

| Mitad | Qué es |
|---|---|
| **Izquierda, azul, 💧** | Bebida. La barra se vacía de arriba abajo según se acerca el siguiente trago. El número grande es la cuenta atrás `M:SS`. |
| **Derecha, verde, 🍌** | Comida. Igual, con la cuenta atrás hasta la siguiente pieza. |

La cuenta atrás no es un temporizador fijo. Se recalcula con el ritmo reciente. Si entras en un puerto, el número baja más rápido. Si el ritmo cae, se alarga. Puede moverse sin que haya pasado un segundo «de reloj» en la comida, porque ha cambiado la demanda.

### El aviso

Cuando toca ingerir:

- Suena un pitido por el altavoz del Karoo.
- Esa mitad **parpadea 30 segundos**, alternando lo que tienes que tomar y la cuenta atrás del siguiente.
- La barra de esa mitad se vuelve a llenar en ese instante, porque la app ya da el trago o la pieza por tomados.

| Qué toca | Qué parpadea | Cómo suena |
|---|---|---|
| Solo beber | `150 ml` en la mitad azul | Dos tonos que suben, cortos |
| Solo comer | El nombre y los gramos, por ejemplo `Gel 30g`, en la mitad verde | Tres tonos que suben |
| Beber y comer a la vez | Las dos mitades a la vez | Primero la melodía de beber, una pausa, y luego la de comer |

A los 30 segundos vuelve la cuenta atrás normal. No hay que pulsar nada para confirmar. **La app no comprueba si has bebido o comido de verdad.** En el momento del aviso ya suma esos 150 ml y, si toca, esos gramos. Si ignoras el aviso, el siguiente se calcula como si lo hubieras seguido. Si bebes o comes por tu cuenta entre avisos, la app no lo sabe y no lo descuenta.

El trago es siempre **150 ml**. No se configura. Lo que cambia es **cuándo** llega: más calor, más peso o más intensidad acortan el hueco entre tragos.

### Pausa y bajadas

- **Pausa.** El tiempo, la energía y las cuentas atrás se congelan. Al reanudar siguen desde donde estaban. Los ajustes no se releen.
- **Dejas de pedalear** (bajada, cruce, rueda parada) **después** de haber pedaleado ya. La demanda no cae a cero. El cuerpo sigue gastando, así que la cuenta atrás sigue, con un mínimo de unos 30 g/h de carbohidratos y unos 450 ml/h de bebida (ajustados por el peso y por lo que ya llevas gastado). `--:--` solo sale antes de la primera pedalada, o si no hay FTP o no hay potencia.
- **Paras la grabación.** Los contadores de esa salida se reinician en la siguiente. La configuración (bidón, comida, FTP) se conserva.

### Temperatura

Si el Karoo está dando temperatura ambiente, el calor sube la sed y el frío la baja un poco. Referencia: 20 °C no cambia nada. Cada grado por encima suma aproximadamente un 4,5 %, con un tope de 1,75 (mucho calor) y un suelo de 0,85 (frío). Si no hay temperatura, la app asume 20 °C y no inventa un extra.

Ejemplo orientativo, 70 kg, rodando fuerte (cerca del FTP): unos 850 ml/h a 20 °C (un trago de 150 ml cada 10–11 minutos) y unos 1 230 ml/h a 30 °C (un trago cada 7 minutos, más o menos).

---

## Cómo decide los avisos

Así puedes prever qué va a hacer cada ajuste, sin tocar nada «a ver qué pasa».

La intensidad no es el vatio de este segundo (baila demasiado) ni solo la media de toda la salida (diluye el puerto en el que estás). Usa una **potencia efectiva**:

- 75 % la media de los **últimos 20 minutos**
- 25 % la media **de toda la actividad**

Esa potencia, comparada con tu FTP, fija los gramos y los mililitros por hora. Cada segundo se acumula un poco de «lo que ya deberías llevar tomado». Cuando lo pendiente llega a un trago (150 ml) o a la siguiente pieza de comida, salta el aviso.

### Carbohidratos

La demanda de partida, antes de llevar mucho tiempo encima, va así:

| Esfuerzo reciente | Gramos por hora al inicio |
|---|---|
| Hasta el 55 % del FTP (suave) | 30 g/h |
| Alrededor del 80 % del FTP | 60 g/h |
| Al 105 % del FTP o más | 90 g/h |

Entre 55 % y 105 % la subida es continua, no a saltos.

Además, según se vacía el glucógeno la demanda sube hasta un **50 %** más, sin pasar nunca de **90 g/h**. Esa subida se completa en torno a unos 1 800 kJ de trabajo (varias horas, según la potencia). Los carbohidratos de antes de salir retrasan un poco esa subida.

El techo de 90 g/h es el límite de lo que el intestino suele absorber con una mezcla de glucosa y fructosa. Aunque el gasto sea mayor, la app no te pedirá más de eso por hora.

Los carbohidratos del bidón y los de la comida van al mismo saco. Beber mezcla hace que la pieza sólida tarde más en salir.

### Bebida

Pedaleando, la sed de partida va de unos **450 ml/h** (desde el 50 % del FTP hacia abajo, ya con el mínimo) hasta unos **850 ml/h** al FTP o por encima, para 70 kg y 20 °C. Luego se multiplica por el peso y por la temperatura.

El aviso es un trago de 150 ml. A 600 ml/h toca un trago cada 15 minutos. A 900 ml/h, cada 10. Esos minutos no se configuran: salen del ritmo, del peso y del calor.

---

## Ejemplos

### Salida suave, agua y un gel

- FTP del perfil, bidón 750 ml, carbohidratos del bidón `0`, recargas `1`.
- Comida 1: `Gel` / `30`.
- Antes de salir: vacío.

Beberás a un ritmo bajo (cerca de 450–600 ml/h si no hace calor) y el gel saldrá cuando se acumulen unos 30 g de demanda, la primera vez un poco antes. El agua no retrasa el gel.

### Salida larga con bebida de carbohidratos y dos bidones

- Bidón 750 ml, carbohidratos `60`, recargas `2`.
- Comida 1: `Gel` / `30`. Comida 2: `Barrita` / `40`.
- Antes: `60` g y `1` hora (cuentan unos 48 g).

Los primeros 1 500 ml (dos bidones) aportan carbohidrato en cada trago de 150 ml, unos 12 g. Eso separa los avisos de gel y de barrita. Cuando se acaban los 1 500 ml, los tragos siguen pero a 0 g, y la comida se pide más seguida. El orden en pantalla será Gel, Barrita, Gel, Barrita…

### Quieres comer menos a menudo

Sube los gramos de cada pieza a lo que de verdad te comes de una vez (una barrita de 40 g, no dos de 20 escritas como 20). No alargues el intervalo a mano: no existe ese ajuste. Una pieza más grande espera más déficit antes de sonar.

### Hace calor y sudas mucho

No hace falta subir ningún intervalo. Con la temperatura del Karoo y tu peso, los 150 ml se piden antes. Revisa que el número de recargas alcanza para el agua que vas a gastar; si solo llevas un bidón de agua (`carbohidratos 0`, `recargas 1`), los avisos de beber no se paran al acabarlo: la app sigue pidiendo tragos aunque el bidón físico esté vacío. Las recargas solo distinguen mezcla con carbohidratos de agua, no calculan si te queda líquido.

---

## Lista rápida antes de dar a grabar

- El potenciómetro marca vatios en el Karoo.
- La línea verde de FuelTheRide muestra tu FTP y tu peso, y no un 0.
- El tamaño del bidón es el real.
- Los gramos del bidón son los de **un** bidón lleno. Cero si es agua.
- Las recargas son los bidones que llevas **con esa mezcla**, no los de agua que repongas después.
- Cada comida tiene el nombre que quieres leer y los gramos de carbohidrato de una pieza.
- Si has comido antes de salir, están puestos los gramos y las horas. Si no, déjalo a cero.
- Has pulsado **Save & close** después del último cambio.
- El campo **⛽ FuelTheRide** está en la página que vas a mirar.
- El volumen del Karoo se oye.
