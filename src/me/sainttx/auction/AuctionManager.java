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
    private boolean canAuction = true;

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

    public boolean areAuctionsDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void endAllAuctions() {
        for (IAuction auction : auctions) {
            auction.end();
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

    public void removeAuctionFromMemory(IAuction auction) {
        auctions.remove(auction);
    }

    public boolean isAuctionInWorld(Player player) {
        if (!plugin.isPerWorldAuctions()) {
            return auctions.size() == 1;
        }
        for (IAuction auction : auctions) {
            if (player.getWorld().equals(auction.getWorld())) {
                return true;
            }
        }
        return false;
    }

    public IAuction getAuctionInWorld(Player player) {
        if (!plugin.isPerWorldAuctions()) {
            return auctions.isEmpty() ? null : auctions.get(0);
        }
        for (IAuction auction : auctions) {
            if (player.getWorld().equals(auction.getWorld())) {
                return auction;
            }
        }
        return null;
    }
    
    public void setCanAuction(boolean canAuction) {
        this.canAuction = canAuction;
    }

    public void prepareAuction(Player player, String[] args) {
        Messages messager = Messages.getMessager();
        int minStartingPrice = plugin.getMinimumStartingPrice();
        int maxStartingPrice = plugin.getMaximumStartingPrice();

        if (disabled && !player.hasPermission("auction.bypass.disable")) {
            messager.sendText(player, "fail-start-auction-disabled", true);
            return;
        }
        
        if (!canAuction && !player.hasPermission("auction.bypass.startdelay")) {
            messager.sendText(player, "fail-start-cant-yet", true);
            return;
        }

        if (isAuctionInWorld(player)) {
            if (plugin.isPerWorldAuctions()) {
                messager.sendText(player, "fail-start-auction-world", true);
                return;
            } else {
                messager.sendText(player, "fail-start-auction-in-progress", true);
                return;
            }
        }

        if (args.length < 3) {
            messager.sendText(player, "fail-start-syntax", true);
            return;
        }

        int numItems = -1;
        double startingPrice = -1;
        double autoWin = -1;
        int fee = plugin.getAuctionStartFee();

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

    public void startAuction(Auction plugin, Player player, int numItems, double startingPrice, double autoWin) {
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
        
        if (auction == null)
            return;

        // TODO: test if it goes past the exceptions.. 
        if (!player.hasPermission("auction.tax.exempt")) {
            auction.setTaxable(true);
        }

        Auction.economy.withdrawPlayer(player.getName(), plugin.getAuctionStartFee());
        auction.start();
        setCanAuction(false);
        auctions.add(auction);
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

    public void placeBid(Player player, IAuction auction, double amount) {
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