package me.sainttx;

import java.util.ArrayList;

import me.sainttx.IAuction.EmptyHandException;
import me.sainttx.IAuction.InsufficientItemsException;
import me.sainttx.IAuction.UnsupportedItemException;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class AuctionManager {

    //private static AuctionManager am;

    private static ArrayList<IAuction> auctions = new ArrayList<IAuction>();

//    public static AuctionManager getAuctionManager() {
//        if (am == null) {
//            am = new AuctionManager();
//        }
//        return am;
////        return am == null ? am = new AuctionManager() : am;
//    }

    public static void disable() {
        for (IAuction auction : auctions) {
            auction.end();
        }
    }

    public boolean isPlayerAuctioning(Player player) {
        for (IAuction auction : auctions) {
            if (player.getUniqueId().toString().equals(auction.getOwner())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAuctionInWorld(Player player) {
        for (IAuction auction : auctions) {
            if (player.getWorld().equals(auction.getWorld())) {
                return true;
            }
        }
        return false;
    }

    public IAuction getAuctionInWorld(Player player) {
        for (IAuction auction : auctions) {
            if (player.getWorld().equals(auction.getWorld())) {
                return auction;
            }
        }
        return null;
    }

    public void startAuction(Player player, String[] args) {
        Messages messager = Auction.getMessager();
        if (isAuctionInWorld(player)) {
            messager.sendText(player, "fail-start-auction-in-progress", true);
            return;
        }
        if (args.length > 2) {
            try {
                int amount = Integer.parseInt(args[1]);
                int start = Integer.parseInt(args[2]);
                int autowin = -1;
                int fee = getConfig().getInt("auction-start-fee");
                if (fee > Auction.economy.getBalance(player.getName())) {
                    messager.sendText(player, "fail-start-no-funds", true);
                    return;
                }
                Auction.economy.withdrawPlayer(player.getName(), fee);
                if (args.length == 4) { // auction start amount startingbid autowin
                    if (getConfig().getBoolean("allow-autowin")) {
                        autowin = Integer.parseInt(args[3]);
                    } else {
                        messager.sendText(player, "fail-start-no-autowin", true);
                        return;
                    }
                }
                IAuction auction = new IAuction(Auction.getPlugin(), player, amount, start, autowin);
                auction.start();
                auctions.add(auction);
            } catch (NumberFormatException ex1) {
                messager.sendText(player, "fail-number-format", true);
            } catch (InsufficientItemsException ex2) {
                messager.sendText(player, "fail-start-not-enough-items", true);
            } catch (EmptyHandException ex3) {
                messager.sendText(player, "fail-start-handempty", true);
            } catch (UnsupportedItemException ex4) {
                messager.sendText(player, "unsupported-item", true);
            }
        } else {
            messager.sendText(player, "fail-start-syntax", true);
        }
    } 

    public void bid(Player player, int amount) {
        Messages messager = Auction.getMessager();
        IAuction auction = getAuctionInWorld(player);
        if (auction == null) {
            messager.sendText(player, "fail-bid-no-auction", true);
            return;
        }
        
        boolean autowin = false;
        if (auction.getOwner().equals(player.getUniqueId())) {
            messager.sendText(player, "fail-bid-your-auction", true);
        } else if (amount < auction.getTopBid() + auction.getIncrement()) {
            messager.sendText(player, "fail-bid-too-low", true);
            return;
        } else {
            
            if (auction.getWinning() != null) {
                if (auction.getWinning().equals(player.getUniqueId())) {
                    messager.sendText(player, "fail-bid-top-bidder", true);
                    return;
                }
            }
            // give old player his money back
            Player oldWinner = Bukkit.getPlayer(auction.getWinning());
            if (oldWinner != null) {
                Auction.economy.depositPlayer(oldWinner.getName(), auction.getTopBid());
            } else {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(auction.getWinning());
                Auction.economy.depositPlayer(offline.getName(), auction.getTopBid());
            }
            
            // placing the bid
            auction.setTopBid(amount);
            auction.setWinning(player.getUniqueId());
            Auction.economy.withdrawPlayer(player.getName(), amount);
            if (amount >= auction.getAutoWin() && auction.getAutoWin() != -1) {
                messager.messageListening(auction, "auction-ended-autowin", true);
                auction.end();
                autowin = true;
            }
            
            if (!autowin) {
              messager.messageListening(auction, "bid-broadcast", true);
            }
        }
    }

    public void end(Player player) {
        if (isAuctionInWorld(player)) {
            getAuctionInWorld(player).end();
        }
    }

    public void remove(IAuction auction) {
        auctions.remove(auction);
    }

    private FileConfiguration getConfig() {
        return Auction.getConfiguration();
    }
}
