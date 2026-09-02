package net.buildtheearth.buildteamtools.modules.navigation.components.address.commands;

import com.alpsbte.alpslib.utils.ChatHelper;
import net.buildtheearth.Projection;
import net.buildtheearth.buildteamtools.modules.network.api.PhotonAPI;
import net.buildtheearth.buildteamtools.modules.network.model.Permissions;
import net.buildtheearth.model.GeographicalCoordinate;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AddressCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatHelper.getErrorComponent("This command can only be used by a player!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("get")) {
            return handleGetCommand(player, args);
        }

        if (args[0].equalsIgnoreCase("teleport")) {
            return handleTeleportCommand(player, args);
        }

        player.sendMessage(ChatHelper.getErrorComponent("Usage: /address <get|teleport>"));
        return true;
    }

    private boolean handleGetCommand(@NonNull Player player, String @NonNull [] args) {
        if (!player.hasPermission(Permissions.ADDRESS_GET)) {
            player.sendMessage(ChatHelper.getErrorComponent("You don't have the required %s to %s address.", "permission",
                    "get"));
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(ChatHelper.getErrorComponent("Usage: /address get"));
            return true;
        }

        player.sendMessage("Getting closest address...");

        try {
            Location loc = player.getLocation();

            GeographicalCoordinate geoCoord = Projection.toGeo(
                    loc.getX(),
                    loc.getZ()
            );

            PhotonAPI.getAddressFromCoordinatesAsync(geoCoord)
                    .thenAccept(address -> {
                        if (address.isBlank()) {
                            player.sendMessage(
                                    ChatHelper.getErrorComponent(
                                            "No address found for your location."
                                    )
                            );
                            return;
                        }

                        player.sendMessage(address);
                    })
                    .exceptionally(error -> {
                        ChatHelper.logError(
                                "Failed to retrieve address: %s",
                                error.getMessage()
                        );

                        player.sendMessage(
                                ChatHelper.getErrorComponent(
                                        "Failed to retrieve closest address."
                                )
                        );

                        return null;
                    });

        } catch (Exception e) {
            ChatHelper.logError(
                    "Failed to convert player location to geographical coordinates: %s",
                    e.getMessage()
            );

            player.sendMessage(
                    ChatHelper.getErrorComponent(
                            "Failed to retrieve closest address."
                    )
            );
        }


        return true;
    }

    private boolean handleTeleportCommand(@NonNull Player player, String @NonNull [] args) {
        if (!player.hasPermission(Permissions.ADDRESS_TELEPORT)) {
            player.sendMessage(ChatHelper.getErrorComponent("You don't have the required %s to %s address.", "permission",
                    "teleport"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(ChatHelper.getErrorComponent("Usage: /address teleport <address>"));
            return true;
        }

        player.sendMessage("Teleporting to the address...");

        String address = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        PhotonAPI.getCoordinatesFromAddressAsync(address)
                .thenAccept(coordinates -> {
                    player.sendMessage("Lon : " + coordinates.longitude() + " ; Lat : " + coordinates.latitude());
                })
                .exceptionally(error -> {
                    player.sendMessage(ChatHelper.getErrorComponent(error.getMessage()));
                    return null;
                });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();

        if (sender.hasPermission(Permissions.ADDRESS_GET)) list.add("get");
        if (sender.hasPermission(Permissions.ADDRESS_TELEPORT)) list.add("teleport");


        return list.stream()
                .filter(entry -> entry.startsWith(args[0].toLowerCase()))
                .toList();
    }
}
