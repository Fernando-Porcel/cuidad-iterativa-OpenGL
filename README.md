# Ciudad interactiva con OpenGL

Actividad práctica de Programación Gráfica: ampliación de una ciudad interactiva.

Proyecto en Java 17 con Maven, LWJGL 3.3.3, GLFW y OpenGL 3.3 Core. Parte del proyecto base visto en clase (una ciudad de 7 × 7 con un auto) y lo amplía a una ciudad de **11 × 11** con iluminación de día y de noche, farolas, faros, decoración urbana, semáforos animados, minimapa, tráfico autónomo, tres cámaras y un recorrido de entregas.

Todo se dibuja con código a partir de un único cubo que se escala, gira y traslada. No usa imágenes ni modelos externos.

## Ejecutar

Necesitas un JDK 17 o superior, Maven (`java -version`, `mvn -version`), una pantalla y un controlador compatible con OpenGL 3.3. La primera vez, Maven descarga las dependencias.

Desde la carpeta donde está `pom.xml`:

```sh
mvn compile exec:exec
```

Ese comando abre la versión final (`com.graphics.CiudadDecorada`). `ESC` cierra la ventana.

También se puede ejecutar cada etapa por separado:

```sh
mvn compile exec:exec "-DmainClass=com.graphics.AppCuidad"
mvn compile exec:exec "-DmainClass=com.graphics.Vehiculo"
mvn compile exec:exec "-DmainClass=com.graphics.CiudadIluminada"
mvn compile exec:exec "-DmainClass=com.graphics.CiudadDecorada"
```

**Desde VS Code:** abre la carpeta del proyecto (la que contiene `pom.xml`) con las extensiones de Java instaladas y usa el botón **Run** que aparece sobre el `main` de cada clase. Cada clase se ejecuta como la etapa que representa; para la versión final, ejecuta `CiudadDecorada`.

Notas:

- Las comillas alrededor de `-DmainClass=...` son necesarias en PowerShell (Windows), que de otro modo interpreta mal el punto del nombre de la clase. En otras terminales no estorban.
- Usa `exec:exec`, no `exec:java`.
- En macOS, el perfil de Maven agrega `-XstartOnFirstThread`. Si ejecutas desde un IDE en Mac, agrega ese argumento a las opciones de la JVM.
- El `pom.xml` incluye las bibliotecas nativas para Windows, Linux (x64 y ARM64) y macOS (Intel y Apple Silicon).

## Cómo está organizado el código

Cada clase de la cadena hereda de la anterior y agrega una responsabilidad. Cada una tiene su propio `main`, así que se puede ejecutar como una etapa. Aparte hay una clase pequeña, `VehiculoAutonomo`, que representa a un auto con ruta propia y que usa `CiudadDecorada`.

```text
AppCuidad: ventana, renderizador, mapa de la ciudad
  └── Vehiculo extends AppCuidad: auto, conducción, colisiones y cámaras
        └── CiudadIluminada extends Vehiculo: día/noche, farolas y faros
              └── CiudadDecorada extends CiudadIluminada: decoración, semáforos, entregas y minimapa
```

| Clase | Responsabilidad |
|---|---|
| `AppCuidad` | Crea la ventana y el ciclo de dibujo, define el mapa, dibuja el suelo, las calles, los edificios y los parques. |
| `Vehiculo` | Dibuja el auto, lo maneja con el tiempo entre cuadros, detecta colisiones y controla las cámaras (tercera persona, primera persona y aérea). |
| `CiudadIluminada` | Alterna día y noche, y agrega las farolas y los faros del auto. |
| `CiudadDecorada` | Agrega ventanas, árboles, bancos, pasos peatonales, semáforos, el tráfico autónomo, las entregas y el minimapa. |
| `VehiculoAutonomo` | Guarda la posición, la orientación, la velocidad y la ruta de un auto que circula solo, y lo mueve en cada cuadro. No tiene `main`. |

Las etapas posteriores necesitan los archivos anteriores (y `CiudadDecorada` también necesita `VehiculoAutonomo`): `extends` reutiliza lo hecho y `super.escena()` dibuja lo anterior antes de agregar contenido nuevo.

## Diseño de la ciudad

El mapa es una matriz de **11 × 11** celdas. Cada celda mide 10 × 10 unidades y se toma **1 unidad como 1 metro**, así que la ciudad mide 110 × 110 m.

```java
// 0 = calle, 1 = edificio, 2 = parque
{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
{0, 1, 0, 1, 0, 1, 0, 1, 0, 2, 0},
{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
{0, 2, 0, 1, 0, 1, 0, 1, 0, 1, 0},
{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
{0, 1, 0, 2, 0, 1, 0, 1, 0, 1, 0},
{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
{0, 1, 0, 1, 0, 1, 0, 2, 0, 1, 0},
{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
{0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0},
{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}
```

- Las filas y columnas pares son calles y forman una cuadrícula conectada: todos los destinos son accesibles por calles.
- Hay **25 manzanas**: 21 edificios y 4 parques. La altura y el color de cada edificio varían según su posición.
- La ampliación conserva el mapa original de 7 × 7 en la esquina superior izquierda y agrega calles y manzanas hacia el sur y el este.
- Todo lo que depende del tamaño se calcula desde la matriz: el suelo, el límite de circulación, las colisiones, la cámara aérea, la posición inicial del auto y la escala del minimapa. Si cambias el mapa, todo se adapta.

## Funciones implementadas

### Iluminación

- **Día y noche (`N`):** de día hay luz ambiental y la luz direccional del sol; de noche, una luz ambiental azulada y tenue y una luz direccional más débil, como la de la luna. El cielo cambia de color.
- **Luz difusa:** cada superficie se ilumina según hacia dónde mira, así que las caras de los edificios se ven con tonos distintos.
- **Farolas:** una en la esquina noroeste de cada manzana (25 en total). Iluminan el suelo y las paredes cercanas, y su luz disminuye con la distancia. De noche, la bombilla se ve encendida; eso es distinto de la luz que aporta al entorno.
- **Faros del auto (`F`):** dos focos delanteros que siguen la posición y la orientación del auto. Iluminan solo hacia delante, dentro de un cono y con alcance limitado.

### Decoración urbana

- **Ventanas** en las cuatro fachadas de cada edificio. De noche, unas se ven encendidas y otras apagadas; el patrón es distinto en cada edificio, pero siempre el mismo para la misma ventana.
- **Parques** con tres árboles y un banco (orientado al oeste).
- **Pasos peatonales** junto a los semáforos, cruzando la calle que cada semáforo regula.
- **Semáforos animados:** 9 semáforos, con una secuencia repetitiva de verde, amarillo y rojo que depende del tiempo (ciclo de 12 segundos). Sus luces miran al norte y al sur.

### Tráfico autónomo

- **Tres autos** de colores (amarillo, azul y verde) circulan solos, sin intervención del usuario.
- Cada uno sigue una ruta por las calles, pasando por intersecciones: el amarillo recorre el borde de la ciudad, el azul un anillo intermedio y el verde rodea la manzana central. Al terminar la ruta, la repiten.
- Hacen giros suaves en las esquinas y mantienen su posición, orientación y velocidad (entre 18 y 29 km/h).
- Las rutas siguen el centro de las calles, así que no atraviesan edificios ni salen del mapa. Las tres rutas no comparten tramos ni se cruzan entre sí.
- Aparecen en el minimapa con su color.

### Cámaras

- **Tercera persona:** la cámara sigue al auto desde atrás y arriba (vista por defecto).
- **Primera persona (`V`):** la cámara se coloca a la altura del conductor, por delante de la cabina, y mira hacia donde apunta el auto.
- **Aérea (`C`):** una vista general de la ciudad.

### Entregas

- Hay **4 destinos**, uno por parque, sobre la calle que queda al oeste de cada uno. Se recorren de sur a norte.
- El destino activo se ve como una plataforma verde con un cubo flotante giratorio, y también aparece en el minimapa.
- Al entrar el auto a la plataforma, la entrega se completa y aparece el siguiente destino.
- El título de la ventana muestra el avance (`Entregas: n/4`) y avisa cuando están **completadas**.

### Minimapa

- Se muestra en la esquina superior derecha. `M` lo muestra u oculta.
- Representa toda la ciudad, la posición y orientación del auto (con una punta que indica hacia dónde mira) y el destino activo. El norte queda hacia arriba.
- Mantiene su forma y su esquina al redimensionar la ventana, y no altera la vista principal.

### Reinicio

`R` devuelve el auto a la esquina inferior izquierda, con la orientación inicial y velocidad cero, y reinicia las entregas. Así se puede completar toda la actividad de nuevo.

## Controles

| Tecla | Acción |
|---|---|
| `W` / `S` o flechas arriba / abajo | Acelerar y frenar o retroceder |
| `A` / `D` o flechas izquierda / derecha | Girar mientras el auto se mueve |
| `Espacio` | Freno |
| `C` | Cámara aérea / vista de conducción |
| `V` | Primera persona / tercera persona |
| `N` | Día / noche |
| `F` | Encender / apagar los faros |
| `M` | Mostrar / ocultar el minimapa |
| `R` | Reiniciar el auto y las entregas |
| `ESC` | Salir |

El título de la ventana muestra la velocidad (en km/h, suponiendo que 1 unidad es 1 metro), el modo día o noche, el estado de los faros y las entregas. Puede quedar cortado si la ventana es muy pequeña.

## Qué hace cada método

### `AppCuidad`

| Método | Para qué sirve |
|---|---|
| `run()` | Arranca la aplicación: abre la ventana, ejecuta el ciclo de dibujo y cierra al final. |
| `iniciar()` | Prepara la ventana, OpenGL, los shaders y el cubo. |
| `loop()` | Repite en cada cuadro: leer el teclado, actualizar y dibujar. |
| `limpiar()` | Libera los recursos al cerrar. |
| `tecla(key)` | Reacciona a una tecla presionada (por ejemplo, `ESC` cierra). |
| `pulsado(key)` | Dice si una tecla se está manteniendo presionada. |
| `actualizar(deltaTime)` | Actualiza lo que cambia con el tiempo. |
| `configurarCamara()` | Define desde dónde se mira la ciudad (cámara aérea). |
| `prepararLuces()` | Punto donde las clases hijas envían sus luces. En esta clase está vacío. |
| `colorFondo()` | Define el color del cielo. |
| `centro(indice)` | Convierte un índice del mapa en una coordenada del mundo. |
| `alturaEdificio(fila, columna)` | Da la altura de cada edificio según su posición. |
| `dibujarFrame()` | Dibuja un cuadro completo: limpia la pantalla, coloca la cámara y las luces, y dibuja la escena. |
| `escena()` | Dibuja el suelo, las calles, las aceras, los edificios y los parques. |
| `dibujarMarcasCalle()` | Dibuja las rayas amarillas del centro de las calles. |
| `caja(...)` y `cajaGirada(...)` | Dibujan una caja (sin giro o con giro). Con ellas se construye todo. |
| `uniform()`, `vector()`, `decimal()`, `entero()` | Envían datos desde Java a los shaders. |
| `vertexShader()` | Programa de la tarjeta gráfica que decide en qué punto de la pantalla cae cada vértice. También tiene la vista superior del minimapa. |
| `fragmentShader()` | Programa de la tarjeta gráfica que decide el color de cada píxel. |
| `compilar()` y `crearPrograma()` | Preparan los shaders para usarlos. |
| `crearCubo()` | Crea el cubo con el que se dibuja todo. |
| `main()` | Ejecuta esta etapa por sí sola. |

### `Vehiculo`

| Método | Para qué sirve |
|---|---|
| `tecla(key)` | Atiende `C` (cámara aérea), `V` (primera o tercera persona) y `R` (reiniciar). |
| `reiniciar()` | Devuelve el auto a su posición inicial, con velocidad cero. |
| `actualizar(deltaTime)` | Maneja el auto: acelera, frena, gira y avanza según el tiempo transcurrido. |
| `actualizarTitulo()` | Escribe la velocidad y el estado en el título de la ventana. |
| `estadosExtras()` | Texto adicional para el título, que completan las clases hijas. |
| `puedeCircular(x, z)` | Comprueba si el auto puede ocupar una posición sin chocar con una manzana ni salir del mapa. |
| `configurarCamara()` | Coloca la cámara según el modo elegido: detrás del auto, a la altura del conductor o aérea. |
| `escena()` | Dibuja la ciudad y encima el auto. |
| `dibujarAuto()` | Dibuja el auto con cajas: carrocería, cabina, ruedas y faros. |
| `pieza(...)` | Dibuja una pieza del auto de modo que gire junto con él. |
| `main()` | Ejecuta esta etapa por sí sola. |

### `CiudadIluminada`

| Método | Para qué sirve |
|---|---|
| Constructor | Calcula dónde va cada farola. |
| `fragmentShader()` | Calcula el color de cada píxel teniendo en cuenta las luces. |
| `tecla(key)` | Atiende `N` (día/noche) y `F` (faros). |
| `prepararLuces()` | Envía a la tarjeta gráfica el sol o la luna, la luz ambiental, las farolas y los faros. |
| `colorFondo()` | Cielo claro de día y oscuro de noche. |
| `estadosExtras()` | Agrega el modo día/noche y el estado de los faros al título. |
| `escena()` | Dibuja lo anterior y las farolas. |
| `dibujarFarolas()` | Dibuja el poste y la bombilla de cada farola. |
| `main()` | Ejecuta esta etapa por sí sola. |

### `CiudadDecorada`

| Método | Para qué sirve |
|---|---|
| Constructor | Calcula los destinos de las entregas (uno por parque) y crea los tres autos autónomos con sus rutas. |
| `tecla(key)` | Atiende `M` (minimapa) y hace que `R` también reinicie las entregas. |
| `actualizar(deltaTime)` | Avanza el reloj de los semáforos, mueve los autos autónomos y comprueba si el auto llegó al destino. |
| `comprobarEntrega()` | Detecta si el auto está dentro de la plataforma del destino activo y pasa al siguiente. |
| `estadosExtras()` | Agrega el avance de las entregas al título. |
| `escena()` | Dibuja lo anterior, la decoración, los autos autónomos y el destino activo. |
| `decorarCiudad()` | Recorre el mapa y coloca la decoración que corresponde a cada celda. |
| `dibujarVentanas(x, z, altura, semilla)` | Dibuja las ventanas de un edificio. |
| `encendida(...)` | Decide si una ventana está encendida de noche, con un patrón fijo. |
| `ventana(...)` | Dibuja una ventana, con brillo propio si está encendida. |
| `dibujarParque(x, z)` | Coloca los árboles y el banco de un parque. |
| `arbol(x, z)` y `banco(x, z)` | Dibujan un árbol y un banco. |
| `pasoPeatonal(x, z)` | Dibuja un paso peatonal. |
| `estadoSemaforo()` | Dice qué luz del semáforo toca encender según el tiempo. |
| `dibujarSemaforo(x, z)` | Dibuja un semáforo con su luz encendida. |
| `ruta(...)` | Convierte una lista de intersecciones del mapa en los puntos por los que pasa un auto autónomo. |
| `dibujarTrafico()` | Dibuja los autos autónomos (y un marcador de color en el minimapa). |
| `dibujarAutoAutonomo(v)` y `piezaDe(...)` | Dibujan un auto autónomo con cajas, en su posición y orientación. |
| `dibujarDestino()` | Dibuja la plataforma y el cubo flotante del destino activo. |
| `dibujarFrame()` | Dibuja la vista principal y, si está activo, el minimapa encima. |
| `dibujarMinimapa()` | Calcula el recuadro de la esquina y coordina el dibujo del minimapa. |
| `rellenarRecuadro(...)` | Pinta el marco y el fondo del minimapa sin tocar el resto de la pantalla. |
| `dibujarCiudadDesdeArriba()` | Dibuja la ciudad vista desde arriba para el minimapa. |
| `main()` | Ejecuta la versión final. |

### `VehiculoAutonomo`

| Método | Para qué sirve |
|---|---|
| Constructor | Crea el auto en el primer punto de su ruta, mirando hacia el segundo. |
| `actualizar(deltaTime)` | Hace avanzar el auto hacia el siguiente punto de la ruta, girando poco a poco, y pasa al siguiente punto al llegar. |
| `anguloHacia(dx, dz)` | Calcula hacia dónde debe mirar el auto para ir a un punto. |
| `diferencia(meta, actual)` | Calcula cuánto le falta girar, siempre por el lado más corto. |

## Limitaciones

- Los semáforos son solo visuales: cambian de color, pero no detienen al auto.
- La iluminación es local y no calcula sombras: una luz puede atravesar un edificio.
- El auto choca con las manzanas y con el borde del mapa, pero no con los postes, los árboles ni los bancos, que son decorativos.
- Los faros iluminan la calle y las paredes, pero las cajas que los representan en el auto no brillan.
- Los pasos peatonales están solo junto a los semáforos (9 en total), no en todas las calles.
- La decoración se dibuja con muchas cajas por cuadro y no está optimizada. En el minimapa se omiten los detalles pequeños.
- Los autos autónomos no chocan con el auto del jugador y no obedecen los semáforos. Siguen rutas fijas por el centro de las calles.
- No incluye peatones, sonido ni modelos importados.
- De las mejoras opcionales se implementó el tráfico autónomo. No se implementó el panel dentro de la ventana: el indicador de estado sigue en el título.
- En primera persona no hay parabrisas ni tablero: se ve el capó del auto y el camino.

## Prueba manual sugerida

1. Ejecutar `mvn compile exec:exec`.
2. Conducir por toda la ciudad y chocar contra edificios nuevos y contra el borde del mapa.
3. Cambiar de cámara con `C` (aérea) y `V` (primera y tercera persona).
4. Probar `N`, `F` y `M`, y comprobar que de noche solo algunas ventanas están encendidas.
5. Observar los tres autos autónomos: deben seguir las calles, girar en las esquinas y verse también en el minimapa.
6. Redimensionar la ventana y comprobar que el minimapa conserva su forma y su esquina.
7. Completar las 4 entregas y comprobar que el título lo indica.
8. Presionar `R` y comprobar que comienza un nuevo recorrido.

## Recursos y fragmentos externos

- **Proyecto base de clase:** las clases del proyecto base hechas con el profesor (ciudad de 7 × 7, renderizador, auto, conducción y colisiones) son el punto de partida de este trabajo.
- **Minimapa:** la técnica (un segundo dibujo del cuadro en un recuadro, con `glViewport`, `glScissor` y una proyección vista desde arriba en el shader) se basa en el ejemplo visto en clase. Se reorganizó en métodos propios y se adaptó al mapa de 11 × 11 con un alcance configurable (`uAlcanceMapa`).
- **Pasos peatonales:** las proporciones de las franjas se tomaron como referencia de una solución de un compañero de clase.
- **Asistente de IA:** se usó Claude (Anthropic) como apoyo para guiar la implementación paso a paso y explicar los conceptos de OpenGL.
- No se usaron imágenes, modelos, texturas ni bibliotecas externas aparte de LWJGL.
