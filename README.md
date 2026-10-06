# AirDrops (Purpur / Paper 1.21.8, Java 21)

Аирдропы в стиле FunTime: каждые 7 минут в случайной точке падает сундук, в чат идёт объявление
с координатами, над сундуком голограмма с отсчётом до открытия, по ПКМ открывается двойной сундук (54 слота) с лутом.

## Сборка и установка
1. `mvn package` (JDK 21 + Maven) -> `target/AirDrops.jar`  (или залей папку на GitHub: вкладка Actions соберёт jar сама)
2. Положи `AirDrops.jar` в `plugins/`, перезапусти сервер. Настройки: `plugins/AirDrops/config.yml`.

## Команды и права
- `/airdrops` (`/airdrop`, `/ad`) — таймер до следующего дропа, координаты, список активных. Право `airdrops.use` (всем).
- `/airdrops spawn [COMMON|RARE|EPIC|LEGENDARY] [here]` — заспавнить сейчас. `airdrops.admin`
- `/airdrops remove` — удалить все. `/airdrops reload` — перечитать конфиг.

## Редкости
| Редкость | Блок | Шанс | Ожидание |
|---|---|---|---|
| COMMON | BARREL | 55 | 30 с |
| RARE | CHEST | 28 | 40 с |
| EPIC | ENDER_CHEST | 14 | 50 с |
| LEGENDARY | BEACON | 3 | 60 с |
Элитры и незеритовая броня — только LEGENDARY, бонус-роллы 3–4% на предмет (`LootTables.java`).

## Структура кода (для ИИ / интеграции)
- `AirDropsPlugin` — вход; `AirDropsPlugin.getInstance().getManager()` — API.
- `AirDropManager` — таймер (1 тик/сек), выбор координат `pickLocation()`, `spawnDrop(Rarity, Location)`, `getDrops()`, `getSecondsUntilNext()`, `getNextLocation()`, `removeAll()`.
- `AirDrop` — один дроп: блок, голограмма (TextDisplay), `Inventory` на 54 слота, `isReady()`, `getInventory()`, `remove(reason)`.
- `LootTables` — таблицы лута и бонусы. `Rarity` / `RaritySettings` — редкости.
- `AirDropListener` — ПКМ, защита от ломания/взрывов, удаление после полного лута.
- События (пакет `events`): `AirDropSpawnEvent` (cancellable, можно менять `drop.getInventory()`), `AirDropReadyEvent`, `AirDropRemoveEvent` (причина: LOOTED/EXPIRED/ADMIN/SHUTDOWN).
- Все тексты — в `config.yml` (`messages.*`, плейсхолдеры `{id} {rarity} {x} {y} {z} {world} {delay} {time} {prefix}`).

Примечание: дропы не сохраняются при перезапуске (при выключении удаляются).
