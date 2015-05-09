//
//    /*
//     * Check if an item has a denied String of lore
//     */
//    private boolean hasBannedLore() {
//        List<String> bannedLore = plugin.getConfig().getStringList("general.blockedLore");
//
//        if (bannedLore != null && !bannedLore.isEmpty()) {
//            if (item.getItemMeta().hasLore()) {
//                List<String> lore = item.getItemMeta().getLore();
//
//                for (String loreItem : lore) {
//                    for (String banned : bannedLore) {
//                        if (loreItem.contains(banned)) {
//                            return true;
//                        }
//                    }
//                }
//            }
//        }
//
//        return false;
//    }
//
