package me.sainttx.auction;

import java.util.ArrayList;

import lombok.Getter;
import lombok.Setter;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class AuctionManager {

    private static AuctionManager am;
    private static AuctionPlugin plugin;
    private Messages messager;

    private static @Getter Auction currentAuction;

    private static ArrayList<Material> banned = new ArrayList<Material>();

    private @Getter @Setter boolean disabled = false;
    private @Getter @Setter boolean canAuction = true;

    private AuctionManager() {
        plugin = AuctionPlugin.getPlugin();
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

    public void sendAuctionInfo(Player player) {
        if (currentAuction != null) {
            messager.sendText(player, currentAuction, "auction-info-message", true);
        } else {
            messager.sendText(player, "fail-info-no-auction", true);    
        }
    }

    public void prepareAuction(Player player, String[] args) {
        Messages messager = Messages.getMessager();
        double minStartingPrice = plugin.getMinimumStartPrice();
        double maxStartingPrice = plugin.getMaxiumumStartPrice();

        if (disabled && !player.hasPermission("auction.bypass.disable")) {
            messager.sendText(player, "fail-start-auction-disabled", true);
            return;
        }

        if (!canAuction && !player.hasPermission("auction.bypass.startdelay")) {
            messager.sendText(player, "fail-start-cant-yet", true);
            return;
        }

        if (currentAuction != null) {
            messager.sendText(player, "fail-start-auction-in-progress", true);
            return;
        }

        if (args.length < 3) {
            messager.sendText(player, "fail-start-syntax", true);
            return;
        }

        int numItems = -1;
        double startingPrice = -1;
        double autoWin = -1;
        double fee = plugin.getStartFee();

        try {
            numItems = Integer.parseInt(args[1]);
            startingPrice = Double.parseDouble(args[2]);
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


        if (fee > AuctionPlugin.economy.getBalance(player)) {
            messager.sendText(player, "fail-start-no-funds", true);
            return;
        }

        if (args.length == 4) {
            if (plugin.isAllowAutowin()) {
                autoWin = Integer.parseInt(args[3]);
            } else {
                messager.sendText(player, "fail-start-no-autowin", true);
                return;
            }
        }

        startAuction(plugin, player, numItems, startingPrice, autoWin);
    } 

    public void startAuction(AuctionPlugin plugin, Player player, int numItems, double startingPrice, double autoWin) {
        Auction auction = null;
        try {
            auction = new Auction(AuctionPlugin.getPlugin(), player, numItems, startingPrice, autoWin);
        } catch (NumberFormatException ex1) {
            messager.sendText(player, "fail-number-format", true);
        } catch (Exception ex2) {
            messager.sendText(player, ex2.getMessage(), true);
        }

        if (auction == null)
            return;

        if (!player.hasPermission("auction.tax.exempt")) {
            auction.setTaxable(true);
        }

        AuctionPlugin.economy.withdrawPlayer(player, plugin.getStartFee());
        auction.start();
        setCanAuction(false);
        currentAuction = auction;
    }

    public void prepareBid(Player player, String amount) {
        try {
            double bid = Double.parseDouble(amount);
            prepareBid(player, bid);
        } catch (NumberFormatException ex) {
            messager.sendText(player, "fail-bid-number", true);
        }
    }

    @SuppressWarnings("static-access")
    public void prepareBid(Player player, double amount) {
        if (currentAuction == null) {
                messager.sendText(player, "fail-bid-no-auction", true);
                return;
        }

        if (currentAuction.getOwner().equals(player.getUniqueId())) {
            messager.sendText(player, "fail-bid-your-auction", true);
            return;
        }

        if (amount < currentAuction.getTopBid() + plugin.getMinBidIncrement()) { // TODO: Customizable bid increment
            messager.sendText(player, "fail-bid-too-low", true);
            return;
        }

        if (plugin.economy.getBalance(player) < amount) {
            messager.sendText(player, "fail-bid-insufficient-balance", true);
            return;
        }

        if (currentAuction.getWinning() != null) {
            if (currentAuction.getWinning().equals(player.getUniqueId())) {
                messager.sendText(player, "fail-bid-top-bidder", true);
                return;
            }

            Player oldWinner = Bukkit.getPlayer(currentAuction.getWinning());
            if (oldWinner != null) {
                AuctionPlugin.economy.depositPlayer(oldWinner, currentAuction.getTopBid());
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(currentAuction.getWinning());
                AuctionPlugin.economy.depositPlayer(offline, currentAuction.getTopBid());
            }
        }

        placeBid(player, currentAuction, amount);
    }

    public void placeBid(Player player, Auction auction, double amount) {
        auction.setTopBid(amount);
        auction.setWinning(player.getUniqueId());
        AuctionPlugin.economy.withdrawPlayer(player, amount);

        if (amount >= auction.getAutoWin() && auction.getAutoWin() != -1) {
            messager.messageListeningAll(auction, "auction-ended-autowin", true);
            auction.end();
            return;
        }

        messager.messageListeningAll(auction, "bid-broadcast", true);
    }

    public void end(Player player) {
        if (currentAuction == null) {
            Messages.getMessager().sendText(player, "fail-end-no-auction", true);    
        } else if (!plugin.isAllowEnding() && !player.hasPermission("auction.end.bypass")) {
            Messages.getMessager().sendText(player, "fail-end-disallowed", true);
        } else if (!currentAuction.getOwner().equals(player.getUniqueId()) && !player.hasPermission("auction.end.bypass")) {
            // TODO: Can't end own auction
        } else {
            currentAuction.end();
            killAuction();
        }
    }
    
    public void killAuction() {
        currentAuction = null;
    }

    /* Loads all banned items into memory */
    private void storeBannedItems() { 
        for (String string : plugin.getConfig().getStringList("banned-items")) {
            Material material = Material.getMaterial(string);
            if (material != null) {
                banned.add(material);
            }
        }
    }
}