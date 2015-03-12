package me.sainttx.auction.fanciful;

import com.google.gson.stream.JsonWriter;
import me.sainttx.auction.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;

import static me.sainttx.auction.fanciful.TextualComponent.rawText;

/**
 * Represents a formattable message. Such messages can use elements such as colors, formatting codes, hover and click data, and other features provided by the vanilla Minecraft <a href="http://minecraft.gamepedia.com/Tellraw#Raw_JSON_Text">JSON message formatter</a>.
 * This class allows plugins to emulate the functionality of the vanilla Minecraft <a href="http://minecraft.gamepedia.com/Commands#tellraw">tellraw command</a>.
 * <p>
 * This class follows the builder pattern, allowing for method chaining.
 * It is set up such that invocations of property-setting methods will affect the current editing component,
 * and a call to {@link #then()} or {@link (Object)} will append a new editing component to the end of the message,
 * optionally initializing it with text. Further property-setting method calls will affect that editing component.
 * </p>
 */
public class FancyMessage implements JsonRepresentedObject, Cloneable, Iterable<MessagePart>, ConfigurationSerializable {

    static {
        ConfigurationSerialization.registerClass(FancyMessage.class);
    }

    private List<MessagePart> messageParts;
    private String jsonString;
    private boolean dirty;

    private static Constructor<?> nmsPacketPlayOutChatConstructor;

    @Override
    public FancyMessage clone() throws CloneNotSupportedException {
        FancyMessage instance = (FancyMessage) super.clone();
        instance.messageParts = new ArrayList<MessagePart>(messageParts.size());
        for (int i = 0 ; i < messageParts.size() ; i++) {
            instance.messageParts.add(i, messageParts.get(i).clone());
        }
        instance.dirty = false;
        instance.jsonString = null;
        return instance;
    }

    /**
     * Creates a JSON message with text.
     *
     * @param firstPartText The existing text in the message.
     */
    public FancyMessage(final String firstPartText) {
        this(rawText(firstPartText));
    }

    public FancyMessage(final TextualComponent firstPartText) {
        messageParts = new ArrayList<MessagePart>();
        messageParts.add(new MessagePart(firstPartText));
        jsonString = null;
        dirty = false;

        if (nmsPacketPlayOutChatConstructor == null) {
            try {
                nmsPacketPlayOutChatConstructor = ReflectionUtil.getNMSClass("PacketPlayOutChat").getDeclaredConstructor(ReflectionUtil.getNMSClass("IChatBaseComponent"));
                nmsPacketPlayOutChatConstructor.setAccessible(true);
            } catch (NoSuchMethodException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Could not find Minecraft method or constructor.", e);
            } catch (SecurityException e) {
                Bukkit.getLogger().log(Level.WARNING, "Could not access constructor.", e);
            }
        }
    }

    /**
     * Sets the text of the current editing component to a value.
     *
     * @param text The new text of the current editing component.
     * @return This builder instance.
     */
    public FancyMessage text(String text) {
        MessagePart latest = latest();
        latest.text = rawText(text);
        dirty = true;
        return this;
    }

    /**
     * Sets the text of the current editing component to a value.
     *
     * @param text The new text of the current editing component.
     * @return This builder instance.
     */
    public FancyMessage text(TextualComponent text) {
        MessagePart latest = latest();
        latest.text = text;
        dirty = true;
        return this;
    }

    /**
     * Sets the color of the current editing component to a value.
     *
     * @param color The new color of the current editing component.
     * @return This builder instance.
     * @throws IllegalArgumentException If the specified {@code ChatColor} enumeration value is not a color (but a format value).
     */
    public FancyMessage color(final ChatColor color) {
        if (!color.isColor()) {
            throw new IllegalArgumentException(color.name() + " is not a color");
        }
        latest().color = color;
        dirty = true;
        return this;
    }

    /**
     * Sets the stylization of the current editing component.
     *
     * @param styles The array of styles to apply to the editing component.
     * @return This builder instance.
     * @throws IllegalArgumentException If any of the enumeration values in the array do not represent formatters.
     */
    public FancyMessage style(ChatColor... styles) {
        for (final ChatColor style : styles) {
            if (!style.isFormat()) {
                throw new IllegalArgumentException(style.name() + " is not a style");
            }
        }
        latest().styles.addAll(Arrays.asList(styles));
        dirty = true;
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to open a file on the client side filesystem when the currently edited part of the {@code FancyMessage} is clicked.
     *
     * @param path The path of the file on the client filesystem.
     * @return This builder instance.
     */
    public FancyMessage file(final String path) {
        onClick("open_file", path);
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to open a webpage in the client's web browser when the currently edited part of the {@code FancyMessage} is clicked.
     *
     * @param url The URL of the page to open when the link is clicked.
     * @return This builder instance.
     */
    public FancyMessage link(final String url) {
        onClick("open_url", url);
        return this;
    }

    /**
     * Set the behavior of the current editing component to instruct the client to send the specified string to the server as a chat message when the currently edited part of the {@code FancyMessage} is clicked.
     * The client <b>will</b> immediately send the command to the server to be executed when the editing component is clicked.
     *
     * @param command The text to display in the chat bar of the client.
     * @return This builder instance.
     */
    public FancyMessage command(final String command) {
        onClick("run_command", command);
        return this;
    }

    /**
     * Set the behavior of the current editing component to display information about an item when the client hovers over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the message component on which they are applied.</p>
     *
     * @param itemJSON A string representing the JSON-serialized NBT data tag of an {@link ItemStack}.
     * @return This builder instance.
     */
    public FancyMessage itemTooltip(final String itemJSON) {
        onHover("show_item", new JsonString(itemJSON)); // Seems a bit hacky, considering we have a JSON object as a parameter
        return this;
    }

    /**
     * Set the behavior of the current editing component to display information about an item when the client hovers over the text.
     * <p>Tooltips do not inherit display characteristics, such as color and styles, from the message component on which they are applied.</p>
     *
     * @param itemStack The stack for which to display information.
     * @return This builder instance.
     */
    public FancyMessage itemTooltip(final ItemStack itemStack) {
        try {
            Object nmsItem = ReflectionUtil.getMethod(ReflectionUtil.getOBCClass("inventory.CraftItemStack"), "asNMSCopy", ItemStack.class).invoke(null, itemStack);
            return itemTooltip(ReflectionUtil.getMethod(ReflectionUtil.getNMSClass("ItemStack"), "save", ReflectionUtil.getNMSClass("NBTTagCompound")).invoke(nmsItem, ReflectionUtil.getNMSClass("NBTTagCompound").newInstance()).toString());
        } catch (Exception e) {
            e.printStackTrace();
            return this;
        }
    }

    /**
     * Terminate construction of the current editing component, and begin construction of a new message component.
     * After a successful call to this method, all setter methods will refer to a new message component, created as a result of the call to this method.
     *
     * @param text The text which will populate the new message component.
     * @return This builder instance.
     */
    public FancyMessage then(final String text) {
        return then(rawText(text));
    }

    /**
     * Terminate construction of the current editing component, and begin construction of a new message component.
     * After a successful call to this method, all setter methods will refer to a new message component, created as a result of the call to this method.
     *
     * @param text The text which will populate the new message component.
     * @return This builder instance.
     */
    public FancyMessage then(final TextualComponent text) {
        if (!latest().hasText()) {
            throw new IllegalStateException("previous message part has no text");
        }
        messageParts.add(new MessagePart(text));
        dirty = true;
        return this;
    }

    /**
     * Terminate construction of the current editing component, and begin construction of a new message component.
     * After a successful call to this method, all setter methods will refer to a new message component, created as a result of the call to this method.
     *
     * @return This builder instance.
     */
    public FancyMessage then() {
        if (!latest().hasText()) {
            throw new IllegalStateException("previous message part has no text");
        }
        messageParts.add(new MessagePart());
        dirty = true;
        return this;
    }

    @Override
    public void writeJson(JsonWriter writer) throws IOException {
        if (messageParts.size() == 1) {
            latest().writeJson(writer);
        } else {
            writer.beginObject().name("text").value("").name("extra").beginArray();
            for (final MessagePart part : this) {
                part.writeJson(writer);
            }
            writer.endArray().endObject();
        }
    }

    /**
     * Serialize this fancy message, converting it into syntactically-valid JSON using a {@link JsonWriter}.
     * This JSON should be compatible with vanilla formatter commands such as {@code /tellraw}.
     *
     * @return The JSON string representing this object.
     */
    public String toJSONString() {
        if (!dirty && jsonString != null) {
            return jsonString;
        }
        StringWriter string = new StringWriter();
        JsonWriter json = new JsonWriter(string);
        try {
            writeJson(json);
            json.close();
        } catch (IOException e) {
            throw new RuntimeException("invalid message");
        }
        jsonString = string.toString();
        dirty = false;
        return jsonString;
    }

    /**
     * Sends this message to a player. The player will receive the fully-fledged formatted display of this message.
     *
     * @param player The player who will receive the message.
     */
    public void send(Player player) {
        send(player, toJSONString());
    }

    private void send(CommandSender sender, String jsonString) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(toOldMessageFormat());
            return;
        }
        Player player = (Player) sender;
        try {
            Object handle = ReflectionUtil.getHandle(player);
            Object connection = ReflectionUtil.getField(handle.getClass(), "playerConnection").get(handle);
            ReflectionUtil.getMethod(connection.getClass(), "sendPacket", ReflectionUtil.getNMSClass("Packet")).invoke(connection, createChatPacket(jsonString));
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().log(Level.WARNING, "Argument could not be passed.", e);
        } catch (IllegalAccessException e) {
            Bukkit.getLogger().log(Level.WARNING, "Could not access method.", e);
        } catch (InstantiationException e) {
            Bukkit.getLogger().log(Level.WARNING, "Underlying class is abstract.", e);
        } catch (InvocationTargetException e) {
            Bukkit.getLogger().log(Level.WARNING, "A error has occured durring invoking of method.", e);
        } catch (NoSuchMethodException e) {
            Bukkit.getLogger().log(Level.WARNING, "Could not find method.", e);
        }
    }

    private static Object chatSerializerInstance = null;
    private static Class<?> chatSerializerClazz = null;

    private Object createChatPacket(String json) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        if (chatSerializerInstance == null || chatSerializerClazz == null) {
            Class<?> chatComponentClazz = ReflectionUtil.getNMSClass("IChatBaseComponent");

            for (Class<?> clazz : chatComponentClazz.getDeclaredClasses()) {
                if (clazz.getSimpleName().equals("ChatSerializer")) {
                    chatSerializerClazz = clazz;
                    chatSerializerInstance = clazz.newInstance();
                }
            }
        }

        Method titleFormatMethod = ReflectionUtil.getMethod(chatSerializerClazz, "a", String.class);
        Object formatted = titleFormatMethod.invoke(chatSerializerInstance, json);
        Class<?> chatBaseComponent = ReflectionUtil.getNMSClass("IChatBaseComponent");

        // Construct the packet
        Class<?> packetPlayOutChatClazz = ReflectionUtil.getNMSClass("PacketPlayOutChat");
        Constructor<?> chatPacketConstructor = ReflectionUtil.getConstructor(packetPlayOutChatClazz, chatBaseComponent);

        return chatPacketConstructor.newInstance(formatted);
    }

    /**
     * Sends this message to a command sender.
     * If the sender is a player, they will receive the fully-fledged formatted display of this message.
     * Otherwise, they will receive a version of this message with less formatting.
     *
     * @param sender The command sender who will receive the message.
     * @see #toOldMessageFormat()
     */
    public void send(CommandSender sender) {
        send(sender, toJSONString());
    }

    /**
     * Sends this message to multiple command senders.
     *
     * @param senders The command senders who will receive the message.
     * @see #send(CommandSender)
     */
    public void send(final Iterable<? extends CommandSender> senders) {
        String string = toJSONString();
        for (final CommandSender sender : senders) {
            send(sender, string);
        }
    }

    /**
     * Convert this message to a human-readable string with limited formatting.
     * This method is used to send this message to clients without JSON formatting support.
     * <p>
     * Serialization of this message by using this message will include (in this order for each message part):
     * <ol>
     * <li>The color of each message part.</li>
     * <li>The applicable stylizations for each message part.</li>
     * <li>The core text of the message part.</li>
     * </ol>
     * The primary omissions are tooltips and clickable actions. Consequently, this method should be used only as a last resort.
     * </p>
     * <p>
     * Color and formatting can be removed from the returned string by using {@link ChatColor#stripColor(String)}.</p>
     *
     * @return A human-readable string representing limited formatting in addition to the core text of this message.
     */
    public String toOldMessageFormat() {
        StringBuilder result = new StringBuilder();
        for (MessagePart part : this) {
            result.append(part.color == null ? "" : part.color);
            for (ChatColor formatSpecifier : part.styles) {
                result.append(formatSpecifier);
            }
            result.append(part.text);
        }
        return result.toString();
    }

    private MessagePart latest() {
        return messageParts.get(messageParts.size() - 1);
    }

    private void onClick(final String name, final String data) {
        final MessagePart latest = latest();
        latest.clickActionName = name;
        latest.clickActionData = data;
        dirty = true;
    }

    private void onHover(final String name, final JsonRepresentedObject data) {
        final MessagePart latest = latest();
        latest.hoverActionName = name;
        latest.hoverActionData = data;
        dirty = true;
    }

    // Doc copied from interface
    public Map<String, Object> serialize() {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("messageParts", messageParts);
//		map.put("JSON", toJSONString());
        return map;
    }

    /**
     * <b>Internally called method. Not for API consumption.</b>
     */
    public Iterator<MessagePart> iterator() {
        return messageParts.iterator();
    }
}
