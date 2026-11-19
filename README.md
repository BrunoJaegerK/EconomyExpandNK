# EconomyExpandNK

**Economía secundaria, configurable y multimoneda para Nukkit.**  
Desarrollado por **BrunoJaegerK**.

EconomyExpandNK funciona junto a [EconomyAPI](https://cloudburstmc.org/resources/economyapi.14/) sin reemplazar su saldo principal. Permite crear monedas adicionales, administrar balances y conectar distintos sistemas de almacenamiento.

## Características

- Multimoneda configurable desde `currencies.yml`.
- Nombre, símbolo, singular/plural, decimales y límites configurables por moneda.
- Saldos vinculados a UUID.
- Comandos de consulta, transferencia y administración.
- API Java para otros plugins.
- Almacenamiento MySQL (predeterminado), SQLite, H2, MongoDB y YAML.
- Mensajes personalizables en `messages.yml`.
- Recarga de configuración y monedas.
- GitHub Actions para compilar automáticamente.

## Requisitos

- Servidor Nukkit compatible con Cloudburst.
- Java 8 o superior (el workflow de compilación utiliza Java 17).
- MySQL recomendado para la configuración predeterminada. EconomyAPI es opcional y puede permanecer instalado.

## Instalación

1. Descarga el JAR compilado desde **Actions** o Releases.
2. Coloca el archivo en la carpeta `plugins/`.
3. Inicia el servidor para generar los archivos de configuración.
4. Configura MySQL y las monedas en `config.yml` y `currencies.yml`.
5. Reinicia el servidor después de cambiar el proveedor de almacenamiento.

## Configuración

### `config.yml`

MySQL es el proveedor predeterminado. Crea la base de datos y un usuario con permisos para crear/leer/escribir tablas antes de iniciar el plugin.

```yaml
language: es
default-currency: crystals
storage:
  type: mysql
  sql:
    mysql:
      host: 127.0.0.1
      port: 3306
      database: economyexpand
      username: economyexpand
      password: "change-me"
      use-ssl: false
  mongodb:
    uri: mongodb://127.0.0.1:27017
    database: economyexpand
    collection: wallets
```

### `currencies.yml`

Puedes añadir cuantas monedas necesites. El ID se usa en comandos y API; el símbolo se muestra antes de cada cantidad.

```yaml
currencies:
  crystals:
    enabled: true
    name: Cristales
    symbol: "✦"
    singular: cristal
    plural: cristales
    decimals: 0
    starting-balance: 0
    max-balance: 999999999
  tokens:
    enabled: true
    name: Tokens
    symbol: "T"
    singular: token
    plural: tokens
    decimals: 2
    starting-balance: 0
    max-balance: 999999999
```

- `enabled`: habilita o deshabilita la moneda.
- `name`: nombre visible.
- `symbol`: prefijo que se imprime delante de las cantidades.
- `singular` / `plural`: formas del nombre.
- `decimals`: precisión de 0 a 8.
- `starting-balance`: saldo inicial de una wallet nueva.
- `max-balance`: saldo máximo permitido.

Deshabilitar una moneda no elimina los balances guardados. Los IDs son insensibles a mayúsculas/minúsculas. Las cantidades se normalizan a la precisión configurada.

## Comandos

| Comando | Descripción |
|---|---|
| `/ecx balance [moneda]` | Consulta tu saldo. |
| `/ecx bal [moneda]` / `/ecx money [moneda]` | Alias de balance. |
| `/ecx balance <jugador> [moneda]` | Consulta el saldo de un jugador conectado. |
| `/ecx currencies` | Lista las monedas habilitadas y sus símbolos. |
| `/ecx pay <jugador> <cantidad> [moneda]` | Transfiere saldo a otro jugador conectado. |
| `/ecx give <jugador> <cantidad> [moneda]` / `add` | Añade saldo. |
| `/ecx take <jugador> <cantidad> [moneda]` / `remove`, `reduce`, `withdraw` | Retira saldo. |
| `/ecx set <jugador> <cantidad> [moneda]` | Establece el saldo exacto. |
| `/ecx reload` | Recarga configuración, monedas y mensajes. |
| `/ecx help` | Muestra la ayuda. |

También puedes usar `/economyexpand`, `/ec` como alias del comando principal.

**Permisos**

- `economyexpand.command`: acceso a comandos de usuario (predeterminado: todos).
- `economyexpand.balance.others`: consultar el saldo de otros (predeterminado: OP).
- `economyexpand.admin`: give, take/reduce, set y reload (predeterminado: OP).

Ejemplos:
```
/ecx balance
/ecx balance tokens
/ecx balance Steve crystals
/ecx pay Alex 25 crystals
/ecx give Alex 100 tokens
/ecx reduce Alex 15 crystals
/ecx set Alex 500 crystals
/ecx currencies
```

## Almacenamiento

Selecciona el proveedor en `config.yml`. **Haz una copia de seguridad antes de cambiar de backend**; el plugin no migra automáticamente los datos entre proveedores.

### MySQL (predeterminado)

```yaml
storage:
  type: mysql
  sql:
    mysql:
      host: 127.0.0.1
      port: 3306
      database: economyexpand
      username: economyexpand
      password: "change-me"
      use-ssl: false
```

### SQLite

```yaml
storage:
  type: sqlite
  sql:
    sqlite-file: wallets.db
```

### H2

```yaml
storage:
  type: h2
  sql:
    h2-file: wallets-h2
```

### MongoDB

```yaml
storage:
  type: mongodb
  mongodb:
    uri: mongodb://127.0.0.1:27017
    database: economyexpand
    collection: wallets
```

### YAML

```yaml
storage:
  type: yaml
```

Los datos de YAML se guardan en `wallets.yml`. Para producción, configura autenticación y TLS en tu base de datos.

## API para desarrolladores

Paquete: `com.bruno.economyexpand.api`

```java
EconomyExpandNK plugin = (EconomyExpandNK) Server.getInstance()
    .getPluginManager().getPlugin("EconomyExpandNK");
SecondaryEconomy economy = plugin.getEconomy();

BigDecimal balance = economy.getBalance(player.getUniqueId(), "crystals");
economy.deposit(player.getUniqueId(), "crystals", new BigDecimal("20"));
economy.withdraw(player.getUniqueId(), "crystals", new BigDecimal("3"));
economy.setBalance(player.getUniqueId(), "crystals", BigDecimal.ZERO);
economy.transfer(player.getUniqueId(), target.getUniqueId(), "crystals",
    new BigDecimal("7"));

Currency currency = economy.getCurrency("crystals");
String display = currency.getSymbol() + currency.format(balance);
```

### Comportamiento de la API

- Los IDs de moneda no distinguen mayúsculas/minúsculas.
- `getBalance(UUID, id)` devuelve el saldo inicial configurado si la wallet no existe todavía.
- `deposit`, `withdraw` y `transfer` requieren cantidades positivas; `setBalance` acepta cero.
- Los importes se redondean hacia abajo a la precisión de la moneda.
- Se aplican límites máximos al depósito, al saldo establecido y al destinatario de transferencias.
- No permite transferencias a uno mismo ni saldo insuficiente.
- Las llamadas son síncronas: usa la API desde el hilo principal del servidor.
- Errores de almacenamiento pueden lanzar `IllegalStateException`; maneja excepciones al integrarla.
- No empaquetes (shade) la API dentro de tu plugin; utiliza la dependencia como `provided`.

### Maven

Compila e instala localmente con `mvn clean install`, y declara la dependencia:

```xml
<dependency>
  <groupId>com.bruno</groupId>
  <artifactId>economyexpandnk</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```

Actualmente no se publica en un repositorio Maven remoto.

## Mensajes y traducción

Edita `messages.yml` (UTF-8). Se pueden personalizar el prefijo, los mensajes y los placeholders:

`{player}`, `{sender}`, `{currency}`, `{symbol}`, `{amount}`, `{balance}`.

## Compilación

Requiere Maven:

```bash
mvn clean package
```

El JAR se genera en `target/economyexpandnk-1.0.0-SNAPSHOT.jar`. GitHub Actions compila el proyecto en cada push y pull request y publica el artefacto de compilación.

## Compatibilidad con EconomyAPI

EconomyAPI y EconomyExpandNK mantienen balances separados. Los comandos y API de EconomyAPI (`myMoney`, `addMoney`, `reduceMoney`, `setMoney`) continúan operando sobre la moneda principal de EconomyAPI. Este plugin no reemplaza ni intercepta esos balances; usa su propia API para las monedas secundarias.

## Licencia

Este proyecto se distribuye bajo la licencia **MIT**. Consulta el archivo [LICENSE](LICENSE). Se permite reutilizar, modificar y distribuir el software conservando el aviso de copyright y la licencia.
