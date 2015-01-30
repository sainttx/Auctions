package me.sainttx.auction.inventory;

import me.sainttx.auction.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * Created by Matthew on 29/01/2015.
 */
public class AuctionInventory implements InventoryHolder {

    /*
     * The amount of rows in the Inventory
     */
    private int rows;

    /*
     * The title of the Inventory
     */
    private String title;

    /*
     * The player this inventory is handling
     */
    private UUID player;

    /*
     * The start price for the auction
     */
    private double startPrice;

    /*
     * The Inventory object
     */
    private Inventory inventory;

    /**
     * Creates a new auction inventory with amount of rows and title
     *
     * @param rows The amount of rows for the inventory to have
     * @param title The title of the inventory
     * @param player The player who is adding items
     */
    public AuctionInventory(int rows, String title, UUID player, double startPrice) {
        this.rows = rows;
        this.title = TextUtil.color(title);
        this.player = player;
        this.startPrice = startPrice;
    }

    /**
     * Returns the players ID
     *
     * @return The players who this inventory is managing
     */
    public UUID getPlayerId() {
        return player;
    }

    /**
     * Returns the defined auction start price
     *
     * @return The auction start price
     */
    public double getStartPrice() {
        return startPrice;
    }

    /**
     * Opens the inventory to a player
     *
     * @param player The player to open the inventory
     */
    public void open(Player player) {
        if (!getInventory().getViewers().contains(player)) {
            player.openInventory(getInventory());
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory == null ? inventory = Bukkit.createInventory(this, rows * 9, title) : inventory;
    }
}
