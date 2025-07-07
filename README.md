# Daily Rewards Plugin

A Minecraft plugin that provides daily rewards to players with a streak system. Players can claim rewards for consecutive days of logging in, with different reward tiers based on player ranks.

## Features

- Daily rewards system with streak tracking
- GUI interface for claiming rewards
- Admin interface for editing rewards
- Support for different reward tiers (normal, premium, deluxe)
- Automatic reminders for unclaimed rewards
- Database storage for player data
- Economy integration via Vault
- Inventory management (drops items if inventory is full)

## Requirements

- Minecraft 1.20+
- Paper/Spigot server
- Vault (for economy integration)
- MySQL database

## Installation

1. Download the latest release from the releases page
2. Place the JAR file in your server's `plugins` folder
3. Start or restart your server
4. Configure the plugin by editing the `config.yml` file in the `plugins/DailyRewardsPlugin` directory

## Configuration

The plugin uses a configuration file (`config.yml`) to store database connection settings:

```yaml
database:
  host: your-database-host
  port: your-database-port
  database: your-database-name
  username: your-database-username
  password: your-database-password
```

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/daily` | Opens the daily rewards GUI | daily.use |
| `/editdaily` | Opens the daily rewards editor for administrators | daily.edit |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| daily.use | Allows players to claim daily rewards | true |
| daily.edit | Allows administrators to edit daily rewards | op |
| daily.premium | Identifies premium rank players for reward tiers | false |
| daily.deluxe | Identifies deluxe rank players for reward tiers | false |

## Usage

### For Players

1. Log in to the server daily to increase your streak
2. Use `/daily` to open the rewards GUI
3. Click on available rewards to claim them
4. Rewards include items and money (if Vault is installed)

### For Administrators

1. Use `/editdaily` to open the admin interface
2. Configure rewards for different days and tiers
3. Set money rewards and item rewards for each tier

## Dependencies

- [Vault](https://github.com/MilkBowl/VaultAPI) - For economy integration
- [LuckPerms](https://luckperms.net/) (optional) - For permission management
- MySQL - For database storage

## Building from Source

This plugin uses Maven for dependency management. To build from source:

1. Clone the repository
2. Run `mvn clean package`
3. The compiled JAR will be in the `target` directory

## License

This project is licensed under the MIT License - see the LICENSE file for details.