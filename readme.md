# Armor_Keeper

A lightweight PaperMC plugin that lets players **keep their armor and shield upon death**, while all other inventory items drop normally.

---

## 🧩 Features
- Keeps **helmet, chestplate, leggings, boots**, and **offhand shield** after death.
- All other inventory items drop as usual.
- Simple **permission system** to control who benefits.

---

## 🧠 Permissions
| Permission | Default | Description |
|-------------|----------|-------------|
| `armorkeeper.keep` | `true` | Players with this permission keep their armor and shield on death. |

To disable for a user or group (using LuckPerms or another manager):
```bash
lp user <player> permission set armorkeeper.keep false
