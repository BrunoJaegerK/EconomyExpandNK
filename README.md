# EconomyExpandNK
**Secondary multi-currency economy plugin for Nukkit** by **BrunoJaegerK**.

EconomyExpandNK is designed to run alongside [EconomyAPI](https://cloudburstmc.org/resources/economyapi.14/) without replacing its primary balance. It manages its own named currencies, balances and persistence.

> Project status: initial implementation. YAML storage and core wallet API are included; database backends and command surface are being developed. Treat this build as experimental and back up player data before testing on a production server.

## Features
- Currency definitions in `currencies.yml`: ID, display name, symbol, singular/plural, precision, starting balance, maximum balance, enabled flag.
- UUID-based accounts; online/offline account operations.
- Balance, deposit, withdrawal, set and transfer methods in Java API.
- YAML storage provider in this first build.
- Configurable language/messages.
- Maven build and GitHub Actions artifact.
- Coexists with EconomyAPI; does not alter its balances or intercept its commands.

## Requirements
- Nukkit server API (compatible with the Cloudburst Nukkit API line).
- Java 8+ runtime; CI builds on Java 17.
- EconomyAPI is optional and may remain installed. It is not a hard dependency.

## Installation
1. Download the JAR from the Actions artifact or a release.
2. Put it in `plugins/`.
3. Start the server once to generate configuration files.
4. Edit `plugins/EconomyExpandNK/currencies.yml` and `messages.yml`.
5. Restart the server after changing currency definitions.

## Configuration
### config.yml
```yaml
language: es
storage:
  type: yaml # yaml (implemented), sqlite/mysql/h2/mongodb (planned)
```

### currencies.yml
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
IDs are lowercase identifiers and are used by commands and the API. Decimal precision is limited to 0–8. Balances are represented as `BigDecimal` in the API and persisted as decimal strings to avoid floating-point rounding.

### messages.yml
Messages support `{player}`, `{currency}`, `{symbol}`, `{amount}`, `{balance}`, and `{prefix}`.

## Commands
This initial implementation exposes:
- `/ecx balance [currency]` — your wallet.
- `/ecx balance <player> [currency]` — inspect another player's wallet (permission required).
- `/ecx reload` — reload configuration (permission required).
- `/ecx help`

Permission: `economyexpand.command`; administration: `economyexpand.admin`.

## Developer API
API classes are in `com.bruno.economyexpand.api`. Obtain the plugin instance from the plugin manager, then call `getEconomy()`.

```java
EconomyExpandNK economyPlugin = (EconomyExpandNK)
    Server.getInstance().getPluginManager().getPlugin("EconomyExpandNK");
SecondaryEconomy economy = economyPlugin.getEconomy();

BigDecimal current = economy.getBalance(player.getUniqueId(), "crystals");
economy.deposit(player.getUniqueId(), "crystals", new BigDecimal("25"));
economy.withdraw(player.getUniqueId(), "crystals", new BigDecimal("5"));
economy.setBalance(player.getUniqueId(), "crystals", BigDecimal.ZERO);
economy.transfer(player.getUniqueId(), target.getUniqueId(), "crystals",
    new BigDecimal("10"));
```

### API notes
- Currency IDs are case-insensitive.
- Invalid, negative or zero transaction amounts are rejected (setBalance accepts zero).
- Deposits/withdrawals are validated against configured maximum balances.
- Transfer checks both wallets before applying the operation.
- API operations are synchronous in this version; call them on the server thread.
- For integration projects, declare EconomyExpandNK as `provided` and do not shade its API into your plugin.

Maven dependency for a local/installed artifact:
```xml
<dependency>
  <groupId>com.bruno</groupId>
  <artifactId>economyexpandnk</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
```
There is no published Maven repository yet; developers can build/install locally with `mvn install`.

## Storage
YAML is the active storage provider. SQLite, H2, MySQL and MongoDB are planned in the provider architecture; do not select them yet. SQL and Mongo configuration will include host/database/credentials, pool settings and safe shutdown. Never commit credentials.

## Build
```bash
mvn clean package
```
Output: `target/EconomyExpandNK-1.0.0-SNAPSHOT.jar`. GitHub Actions runs on pushes and pull requests and publishes the JAR as an artifact.

## License
MIT. Redistribution and forks are permitted, provided the copyright and license notice are retained. Original author: **BrunoJaegerK**.
