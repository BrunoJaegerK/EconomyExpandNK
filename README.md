# EconomyExpandNK
**Secondary multi-currency economy for Nukkit** by **BrunoJaegerK**.

EconomyExpandNK runs alongside [EconomyAPI](https://cloudburstmc.org/resources/economyapi.14/) without replacing its primary balance. It provides configurable secondary wallets and its own storage.

## Features
- Configurable currencies in `currencies.yml`: IDs, display name, symbol, singular/plural, precision, starting and maximum balance.
- UUID-based wallets; API supports offline UUIDs.
- Balance, deposit, withdrawal, set and transfer methods.
- YAML, SQLite, H2, MySQL and MongoDB storage.
- UTF-8 configurable messages.
- GitHub Actions Maven build.

## Requirements
- Cloudburst Nukkit API compatible server.
- Java 8+ runtime (CI builds with Java 17).
- EconomyAPI is optional and can remain installed. EconomyExpandNK neither intercepts its commands nor changes its balances.

## Install
1. Download the JAR from the Actions artifact/release.
2. Put it into `plugins/`.
3. Start the server to generate config.
4. Configure `config.yml`, `currencies.yml`, and `messages.yml`.
5. Restart after changing storage backend or currencies.

## Currencies
`currencies.yml`:
```yaml
currencies:
  crystals:
    enabled: true
    name: Crystals
    symbol: "✦"
    singular: crystal
    plural: crystals
    decimals: 0
    starting-balance: 0
    max-balance: 1000000000
  tokens:
    enabled: true
    name: Tokens
    symbol: "T"
    singular: token
    plural: tokens
    decimals: 2
    starting-balance: 10
    max-balance: 100000000
```
IDs are lowercase identifiers. Precision is 0–8. Values are stored as decimal text; API uses `BigDecimal`. Disabled currencies preserve balances but reject operations.

## Commands
- `/ecx balance [currency]` — your balance (default: crystals).
- `/ecx balance <player> [currency]` — view online player's balance.
- `/ecx reload` — reload config/messages/currencies (admin).
- `/ecx help`

Permissions: `economyexpand.command` (everyone), `economyexpand.admin` (OP), `economyexpand.balance.others` (OP).

## Storage
Select provider in `config.yml`. Back up data before changing providers. No automatic migration between storage providers is currently provided; manually migrate/keep backups.

### YAML
```yaml
storage:
  type: yaml
```
Wallets stored in `wallets.yml`.

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

### MySQL
```yaml
storage:
  type: mysql
  sql:
    mysql:
      host: 127.0.0.1
      port: 3306
      database: economyexpand
      username: economyexpand
      password: "replace-this"
      use-ssl: false
```
Create the database and grant table/data permissions first.

### MongoDB
```yaml
storage:
  type: mongodb
  mongodb:
    uri: mongodb://127.0.0.1:27017
    database: economyexpand
    collection: wallets
```
For production use authenticated TLS URI settings. Wallet balances are stored as strings.

## Developer API
API package: `com.bruno.economyexpand.api`.
```java
EconomyExpandNK plugin = (EconomyExpandNK) Server.getInstance()
    .getPluginManager().getPlugin("EconomyExpandNK");
SecondaryEconomy economy = plugin.getEconomy();

BigDecimal wallet = economy.getBalance(player.getUniqueId(), "crystals");
boolean ok = economy.deposit(player.getUniqueId(), "crystals", new BigDecimal("20"));
economy.withdraw(player.getUniqueId(), "crystals", new BigDecimal("3"));
economy.setBalance(player.getUniqueId(), "crystals", BigDecimal.ZERO);
economy.transfer(player.getUniqueId(), recipient.getUniqueId(), "crystals",
    new BigDecimal("7"));
Currency currency = economy.getCurrency("crystals");
String display = currency.getSymbol() + currency.format(wallet);
```

### API semantics
- IDs are case-insensitive.
- `getBalance(UUID, id)` does not create a row; returns the currency starting balance for an uninitialized wallet.
- Amounts for deposit/withdraw/transfer must be positive; setBalance allows zero.
- Amounts are rounded down to currency precision.
- Max-balance applies to deposit, set and transfer receiver.
- Transfer rejects self-transfer and insufficient funds.
- Calls are synchronous; invoke from the server/main thread.
- Storage failures may throw `IllegalStateException`; integration code should handle them.
- Avoid shading the API into your plugin; declare it as provided.

### Maven
Build/install locally with `mvn clean install` then:
```xml
<dependency>
  <groupId>com.bruno</groupId><artifactId>economyexpandnk</artifactId>
  <version>1.0.0-SNAPSHOT</version><scope>provided</scope>
</dependency>
```
No public Maven repository is configured yet.

## Language
Edit `messages.yml` (UTF-8). Supported placeholders: `{player}`, `{currency}`, `{symbol}`, `{amount}`, `{balance}`, `{prefix}`.

## Build
`mvn clean package`; artifact is `target/EconomyExpandNK-1.0.0-SNAPSHOT.jar`. Actions uploads the JAR artifact on push/PR.

## EconomyAPI compatibility
EconomyAPI and EconomyExpandNK use separate wallets. EconomyAPI's `myMoney/addMoney/reduceMoney/setMoney` continue to refer to its main currency. This plugin is not an EconomyAPI bridge; use its own API to access secondary currencies.

## License
MIT. Redistribution and derivative forks are permitted with the original copyright and license notice retained. Original author: BrunoJaegerK.
