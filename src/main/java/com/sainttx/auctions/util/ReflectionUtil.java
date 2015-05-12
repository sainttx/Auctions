/*
 * Copyright (C) SainttX <http://sainttx.com>
 * Copyright (C) contributors
 *
 * This file is part of Auctions.
 *
 * Auctions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Auctions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Auctions.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sainttx.auctions.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ReflectionUtil {

    /*
     * The server version string to location NMS & OBC classes
     */
    private static String versionString;

    /*
     * Cache of NMS classes that we've searched for
     */
    private static Map<String, Class<?>> loadedNMSClasses = new HashMap<String, Class<?>>();

    /*
     * Cache of methods that we've found in particular classes
     */
    private static Map<Class<?>, Map<String, Method>> loadedMethods = new HashMap<Class<?>, Map<String, Method>>();

    /*
     * Cache of fields that we've found in particular classes
     */
    private static Map<Class<?>, Map<String, Field>> loadedFields = new HashMap<Class<?>, Map<String, Field>>();

    /**
     * Gets the version string for NMS & OBC class paths
     *
     * @return The version string of OBC and NMS packages
     */
    public static String getVersion() {
        if (versionString == null) {
            String name = Bukkit.getServer().getClass().getPackage().getName();
            versionString = name.substring(name.lastIndexOf('.') + 1) + ".";
        }

        return versionString;
    }

    /**
     * Get an NMS Class
     *
     * @param nmsClassName The name of the class
     * @return The class
     */
    public static Class<?> getNMSClass(String nmsClassName) {
        if (loadedNMSClasses.containsKey(nmsClassName)) {
            return loadedNMSClasses.get(nmsClassName);
        }

        String clazzName = "net.minecraft.server." + getVersion() + nmsClassName;
        Class<?> clazz;

        try {
            clazz = Class.forName(clazzName);
        } catch (Throwable t) {
            t.printStackTrace();
            return loadedNMSClasses.put(nmsClassName, null);
        }

        loadedNMSClasses.put(nmsClassName, clazz);
        return clazz;
    }

    /**
     * Get a Bukkit {@link Player} players NMS playerConnection object
     *
     * @param player The player
     * @return The players connection
     */
    public static Object getConnection(Player player) {
        Method getHandleMethod = getMethod(player.getClass(), "getHandle");

        if (getHandleMethod != null) {
            try {
                Object nmsPlayer = getHandleMethod.invoke(player);
                Field playerConField = getField(nmsPlayer.getClass(), "playerConnection");
                return playerConField.get(nmsPlayer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Get a classes constructor
     *
     * @param clazz  The constructor class
     * @param params The parameters in the constructor
     * @return The constructor object
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... params) {
        try {
            return clazz.getConstructor(params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Gets an Enum value from a class
     *
     * @param clazz The enum class
     * @param value The value of the enum
     * @return The enum object
     */
    public static Object getEnumerateFromClass(Class<?> clazz, String value) {
        Method valueOfMethod = getMethod(clazz, "valueOf", String.class);
        try {
            return valueOfMethod.invoke(null, value);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Get a method from a class that has the specific paramaters
     *
     * @param clazz      The class we are searching
     * @param methodName The name of the method
     * @param params     Any parameters that the method has
     * @return The method with appropriate paramaters
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        if (!loadedMethods.containsKey(clazz)) {
            loadedMethods.put(clazz, new HashMap<String, Method>());
        }

        Map<String, Method> methods = loadedMethods.get(clazz);

        if (methods.containsKey(methodName)) {
            return methods.get(methodName);
        }

        try {
            Method method = clazz.getMethod(methodName, params);
            methods.put(methodName, method);
            loadedMethods.put(clazz, methods);
            return method;
        } catch (Exception e) {
            e.printStackTrace();
            methods.put(methodName, null);
            loadedMethods.put(clazz, methods);
            return null;
        }
    }

    /**
     * Get a field with a particular name from a class
     *
     * @param clazz     The class
     * @param fieldName The name of the field
     * @return The field object
     */
    public static Field getField(Class<?> clazz, String fieldName) {
        if (!loadedFields.containsKey(clazz)) {
            loadedFields.put(clazz, new HashMap<String, Field>());
        }

        Map<String, Field> fields = loadedFields.get(clazz);

        if (fields.containsKey(fieldName)) {
            return fields.get(fieldName);
        }

        try {
            Field field = clazz.getField(fieldName);
            fields.put(fieldName, field);
            loadedFields.put(clazz, fields);
            return field;
        } catch (Exception e) {
            e.printStackTrace();
            fields.put(fieldName, null);
            loadedFields.put(clazz, fields);
            return null;
        }
    }


    /**
     * A utility class that will send a player the PacketPlayOutCamera packet
     * to put them into spectate mode of a player. The player will not be able to
     * get out of the spectator mode because the server won't detect them and won't
     * send the packet to fix their view.
     *
     * @param player The player who will be spectating
     * @param target The target that will be spectated
     */
    public static void makePlayerSpectateEntity(Player player, Entity target) {
        try {
            // Get the NMS player and the NMS target
            Method playerHandle = getMethod(player.getClass(), "getHandle");
            Object nmsPlayer = playerHandle.invoke(player);

            Method targetHandle = getMethod(target.getClass(), "getHandle");
            Object nmsTargetEntity = targetHandle.invoke(target);

            /* The following method is the method that sets the entity as the players
            'spectatee' and will send the PacketPlayOutCamera method. The only issue
            is that the Player will likely be able to dismount the entity. This issue can be
            avoided by listening to PlayerSneakEvent and cancelling the event if certain
            criteria are met (ie. if the player is in GameMode.SPECTATE, etc).
            This method name is currently "e" inside EntityPlayer.class (found around) line 1020
            and is likely to change whenever a major server version update takes place. */
            Method setEntityAsPassengerMethod = getMethod(nmsPlayer.getClass(), "e", getNMSClass("Entity"));
            setEntityAsPassengerMethod.invoke(nmsPlayer, nmsTargetEntity);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to make player spectate entity", e);
            e.printStackTrace();
        }
    }
}
