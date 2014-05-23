package me.sainttx.auction;

import java.util.ArrayList;

import me.sainttx.auction.IAuction.EmptyHandException;
import me.sainttx.auction.IAuction.InsufficientItemsException;
import me.sainttx.auction.IAuction.UnsupportedItemException;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class AuctionManager {

    private static AuctionManager am;
    private static Auction plugin;
    private Messages messager;

    private static ArrayList<IAuction> auctions = new ArrayList<IAuction>();
    private static ArrayList<Material> banned = new ArrayList<Material>();

    private boolean disabled = false;

    private AuctionManager() {
        plugin = Auction.getPlugin();
        messager = Messages.getMessager();
        storeBannedItems();
    }

    public static AuctionManager getAuctionManager() {
        return am == null ? am = new AuctionManager() : am;
    }

    @SuppressWarnings("unchecked")
    public static ArrayList<Material> getBannedMaterials() {
        return (ArrayList<Material>) banned.clone();
    }

    
    
    public void endAllAuctions() {
        for (IAuction auction : auctions) {
            auction.end();
        }
    }

//    public boolean isPlayerAuctioning(Player player) {
//        for (IAuction auction : auctions) {
//            if (player.getUniqueId().toString().equals(auction.getOwner())) {
//                return true;
//            }
//        }
//        return false;
//    }

    public boolean isAuctionInWorld(Player player) {
        if (Auction.getConfiguration().getBoolean("per-world-auctions")) {
            for (IAuction auction : auctions) {
                if (player.getWorld().equals(auction.getWorld())) {
                    return true;
                }
            }
            return false;
        } else {
            return auctions.size() == 1;
        }
    }

    public IAuction getAuctionInWorld(Player player) {
        if (Auction.getConfiguration().getBoolean("per-world-auctions")) {
            for (IAuction auction : auctions) {
                if (player.getWorld().equals(auction.getWorld())) {
                    return auction;
                }
            }
            return null;
        } else {
            if (auctions.isEmpty()) {
                return null;
            }
            return auctions.get(0);
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void prepareAuction(Player player, String[] args) {
        Messages messager = Auction.getMessager();
        int minStartingPrice = Auction.getConfiguration().getInt("min-start-price");
        int maxStartingPrice = Auction.getConfiguration().getInt("max-start-price");

        if (disabled && !player.hasPermission("auction.bypass.disable")) {
            messager.sendText(player, "fail-start-auction-disabled", true);
            return;
        }

        if (isAuctionInWorld(player)) {
            if (Auction.getConfiguration().getBoolean("per-world-auctions")) {
                messager.sendText(player, "fail-start-auction-world", true);
                return;
            } else {
                messager.sendText(player, "fail-start-auction-in-progress", true);
                return;
            }
        }

        if (args.length < 2) {
            messager.sendText(player, "fail-start-syntax", true);
            return;
        }

        int numItems = -1;
        int startingPrice = -1;
        int autoWin = -1;
        int fee = plugin.getAuctionStartFee();

        try {
            numItems = Integer.parseInt(args[1]);
            startingPrice = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            messager.sendText(player, "fail-number-format", true);
        }

        if (numItems < 0) {
            messager.sendText(player, "fail-start-negative-number", true);
            return;
        }

        if (startingPrice < minStartingPrice) {
            messager.sendText(player, "fail-start-min", true);
            return;
        }

        if (startingPrice > maxStartingPrice) {
            messager.sendText(player, "fail-start-max", true);
            return;
        }


        if (fee > Auction.economy.getBalance(player.getName())) {
            messager.sendText(player, "fail-start-no-funds", true);
            return;
        }

        if (args.length == 4) {
            if (plugin.isAutowinAllowed()) {
                autoWin = Integer.parseInt(args[3]);
            } else {
                messager.sendText(player, "fail-start-no-autowin", true);
                return;
            }
        }

        startAuction(plugin, player, numItems, startingPrice, autoWin);
    } 

    public void startAuction(Auction plugin, Player player, int numItems, int startingPrice, int autoWin) {
        IAuction auction = null;
        try {
            auction = new IAuction(Auction.getPlugin(), player, numItems, startingPrice, autoWin);
        } catch (NumberFormatException ex1) {
            messager.sendText(player, "fail-number-format", true);
        } catch (InsufficientItemsException ex2) {
            messager.sendText(player, "fail-start-not-enough-items", true);
        } catch (EmptyHandException ex3) {
            messager.sendText(player, "fail-start-handempty", true);
        } catch (UnsupportedItemException ex4) {
            messager.sendText(player, "unsupported-item", true);
        }

        // TODO: test if it goes past the exceptions.. 
        if (!player.hasPermission("auction.tax.exempt")) {
            auction.setTaxable(true);
        }

        Auction.economy.withdrawPlayer(player.getName(), plugin.getAuctionStartFee());
        auction.start();
        auctions.add(auction);
    }
    
    public void prepareBid(Player player, String amount) {
        try {
            int bid = Integer.parseInt(amount);
            prepareBid(player, bid);
        } catch (NumberFormatException ex) {
            messager.sendText(player, "fail-bid-number", true);
        }
    }
    
    @SuppressWarnings("static-access")
    public void prepareBid(Player player, int amount) {
        IAuction auction = getAuctionInWorld(player);

        if (auction == null) {
            if (plugin.isPerWorldAuctions()) {
                messager.sendText(player, "fail-no-auction-world", true);
                return;
            } else {
                messager.sendText(player, "fail-bid-no-auction", true);
                return;
            }
        }

        if (auction.getOwner().equals(player.getUniqueId())) {
            messager.sendText(player, "fail-bid-your-auction", true);
            return;
        }

        if (amount < auction.getTopBid() + auction.getIncrement()) {
            messager.sendText(player, "fail-bid-too-low", true);
            return;
        }

        if (plugin.economy.getBalance(player.getName()) < amount) {
            messager.sendText(player, "fail-bid-insufficient-balance", true);
            return;
        }

        if (auction.getWinning() != null) {
            if (auction.getWinning().equals(player.getUniqueId())) {
                messager.sendText(player, "fail-bid-top-bidder", true);
                return;
            }

            Player oldWinner = Bukkit.getPlayer(auction.getWinning());
            if (oldWinner != null) {
                Auction.economy.depositPlayer(oldWinner.getName(), auction.getTopBid());
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(auction.getWinning());
                Auction.economy.depositPlayer(offline.getName(), auction.getTopBid());
            }
        }
        
        placeBid(player, auction, amount);
    }

    public void placeBid(Player player, IAuction auction, int amount) {
        auction.setTopBid(amount);
        auction.setWinning(player.getUniqueId());
        Auction.economy.withdrawPlayer(player.getName(), amount);

        if (amount >= auction.getAutoWin() && auction.getAutoWin() != -1) {
            messager.messageListeningWorld(auction, "auction-ended-autowin", true);
            auction.end();
            return;
        }

        messager.messageListeningAll(auction, "bid-broadcast", true, true);

        if (plugin.isAntiSnipingAllowed() && (plugin.getAntiSnipingPeriod() >= auction.getTimeLeft())) {
            int addTime = plugin.getTimeToAdd();
            auction.addTime(addTime);
            String message = messager.getMessageFile().getString("anti-snipe-add").replaceAll("%t", String.valueOf(addTime));
            messager.messageListeningAll(auction, message, false, true); // TODO add the message
        }
    }

//    public void bid(Player player, int amount) {
//        FileConfiguration config = Auction.getConfiguration();
//        Messages messager = Auction.getMessager();
//        IAuction auction = getAuctionInWorld(player);
//
//        if (auction == null && config.getBoolean("per-world-auctions")) {
//            messager.sendText(player, "fail-no-auction-world", true);
//            return;
//        } else if (auction == null && !config.getBoolean("per-world-auctions")) {
//            messager.sendText(player, "fail-bid-no-auction", true);
//            return;
//        }
//
//        boolean autowin = false;
//        if (auction.getOwner().equals(player.getUniqueId())) {
//            messager.sendText(player, "fail-bid-your-auction", true);
//        } else if (amount < auction.getTopBid() + auction.getIncrement()) {
//            messager.sendText(player, "fail-bid-too-low", true);
//            return;
//        } else if(Auction.economy.getBalance(player.getName()) < amount) {
//            messager.sendText(player, "fail-bid-insufficient-balance", true);
//            return;
//        } else {
//            if (auction.getWinning() != null) {
//                if (auction.getWinning().equals(player.getUniqueId())) {
//                    messager.sendText(player, "fail-bid-top-bidder", true);
//                    return;
//                }
//            }
//            // give old player his money back
//            if (auction.getWinning() != null) { // only if there is an old player!
//                Player oldWinner = Bukkit.getPlayer(auction.getWinning());
//                if (oldWinner != null) {
//                    Auction.economy.depositPlayer(oldWinner.getName(), auction.getTopBid());
//                } else {
//                    OfflinePlayer offline = Bukkit.getOfflinePlayer(auction.getWinning());
//                    Auction.economy.depositPlayer(offline.getName(), auction.getTopBid());
//                }
//            }
//
//            // placing the bid
//            auction.setTopBid(amount);
//            auction.setWinning(player.getUniqueId());
//            Auction.economy.withdrawPlayer(player.getName(), amount);
//            if (amount >= auction.getAutoWin() && auction.getAutoWin() != -1) {
//                messager.messageListeningWorld(auction, "auction-ended-autowin", true);
//                auction.end();
//                autowin = true;
//            }
//
//            if (config.getBoolean("anti-snipe") && (config.getInt("anti-snipe-period") >= auction.getTimeLeft()) && !autowin) {
//                int addTime = Auction.getInt("anti-snipe-add-seconds");
//                auction.addTime(addTime);
//                String message = Auction.getMessager().getMessageFile().getString("anti-snipe-add").replaceAll("%t", String.valueOf(addTime));
//                messager.messageListeningAll(auction, message, false, true); // TODO add the message
//            }
//
//            if (!autowin) {
//                messager.messageListeningAll(auction, "bid-broadcast", true, true);
//            }
//        }
//    }

    public void end(Player player) {
        if (isAuctionInWorld(player)) {
            if (!plugin.isAuctionEndingAllowed() && !player.hasPermission("auction.end.bypass")) { // TODO this might be a mistake
                Messages.getMessager().sendText(player, "fail-end-disallowed", true); // TODO add msg
                return;
            }
            getAuctionInWorld(player).end();
        } else {
            if (plugin.isPerWorldAuctions()) {
                Messages.getMessager().sendText(player, "fail-no-auction-world", true);
            } else {
                Messages.getMessager().sendText(player, "fail-end-no-auction", true);    
            }
        }
    }

    public void sendAuctionInfo(Player player) {
        if (isAuctionInWorld(player)) {
            Messages.getMessager().sendText(player, getAuctionInWorld(player), "auction-info-message", true);
        } else {
            if (plugin.isPerWorldAuctions()) {
                Messages.getMessager().sendText(player, "fail-no-auction-world", true);
            } else {
                Messages.getMessager().sendText(player, "fail-info-no-auction", true);    
            }
        }
    }

    public void remove(IAuction auction) {
        auctions.remove(auction);
    }

    /* Loads all banned items into memory */
    private void storeBannedItems() { 
        for (String string : Auction.getConfiguration().getStringList("banned-items")) {
            Material material = Material.getMaterial(string);
            if (material != null) {
                banned.add(material);
            }
        }
    }
}
