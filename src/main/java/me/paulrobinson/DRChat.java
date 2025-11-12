package me.paulrobinson;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.InternalStructure;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.ComponentConverter;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.IntFunction;

public final class DRChat extends JavaPlugin implements Listener {

    private ProtocolManager protocolManager;

    private int health = 500;

    private final HashMap<Player, ChatTypePair> playerChat = new HashMap<>();
    private final Set<Player> suppress = new HashSet<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        protocolManager = ProtocolLibrary.getProtocolManager();
        getServer().getPluginManager().registerEvents(this, this);
        handlePackets();

    }

    @Override
    public void onDisable() {
    }

    public void handlePackets() {
        protocolManager.addPacketListener(
                new PacketAdapter(this,
                        PacketType.Play.Server.SYSTEM_CHAT,
                        PacketType.Play.Server.CHAT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        Player player = event.getPlayer();

                        if (suppress.contains(player)) return;

                        Component msg = extractMsg(event.getPacket());
                        if (msg == null) return;

                        processChat(player, msg);
                        event.setCancelled(true);
                    }
                }
        );

        protocolManager.addPacketListener(
                new PacketAdapter(this, PacketType.Play.Server.DISGUISED_CHAT) {
                    @Override
                    public void onPacketSending(PacketEvent event) {
                        getLogger().info("DISGUISED: " + event.getPacket().toString());
                    }
                }
        );
    }

    private void renderToPlayer(Player player, Component payload) {
        suppress.add(player);
        try {
            player.sendMessage(payload);
        } finally {
            suppress.remove(player);
        }
    }

    private void processChat(Player player, Component msg) {
        ChatTypePair pair = playerChat.get(player);
        if (pair == null) return;

        if (msg.toString().contains("Bandit")) { //this is damage one, prob way better way to classify
            pair.damage().add(msg);
        } else {
            pair.regular().add(msg);
        }

        Component[] regular = pair.regular().toArray();
        Component[] damage  = pair.damage().toArray();

        int pad = 20 - (regular.length + damage.length);
        if (pad < 0) pad = 0;

        Component chatPayload = Component.empty();

        for (int i = 0; i < pad; i++) {
            chatPayload = chatPayload.appendNewline();
        }

        for (Component c : regular) {
            chatPayload = chatPayload.appendNewline().append(c);
        }

        for (Component c : damage) {
            chatPayload = chatPayload.appendNewline().append(c);
        }

        renderToPlayer(player, chatPayload);
    }
    private Component extractMsg(PacketContainer packet) {
        WrappedChatComponent wrapper = packet.getChatComponents().readSafely(0);
        if (wrapper != null) {
            Component c = AdventureComponentConverter.fromWrapper(wrapper);
            return c;
        }

        String json = packet.getStrings().readSafely(0);
        if (json != null && !json.isEmpty()) {
            Component c = AdventureComponentConverter.fromJson(json);
            return c;
        }

        for (InternalStructure struct : packet.getStructures().getValues()) {
            if (struct == null) continue;

            WrappedChatComponent nestedWrapper = struct.getChatComponents().readSafely(0);
            if (nestedWrapper != null) {
                Component c = AdventureComponentConverter.fromWrapper(nestedWrapper);
                return c;
            }

            String nestedJson = struct.getStrings().readSafely(0);
            if (nestedJson != null && !nestedJson.isEmpty()) {
                Component c = AdventureComponentConverter.fromJson(nestedJson);
                return c;
            }

            Object handle = struct.getHandle();
            if (handle != null) {
                try {
                    var m = handle.getClass().getMethod("getString");
                    Object res = m.invoke(handle);
                    if (res instanceof String s && !s.isEmpty()) {
                        Component c = Component.text(s);
                        return c;
                    }
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            event.setCancelled(true);
            processChat(player, generateDamageComponent());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        IntFunction<Component[]> factory = Component[]::new;
        playerChat.put(player, new ChatTypePair(
                new BoundedQueue<>(3, factory),
                new BoundedQueue<>(17, factory)));

        processChat(player, Component.text(
                "Welcome to DRChat Example\n" +
                        "I hope this example\n" +
                        "shows that it can\n" +
                        "be used very well in reducing\n" +
                        "debug chat clutter!\n" +
                        "By pjr8"));
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        Component msg = Component.text(player.getName() + ": ")
                .append(event.message());

        if (event.isAsynchronous()) {
            Bukkit.getScheduler().runTask(this, () -> processChat(player, msg));
        } else {
            processChat(player, msg);
        }
    }

    public TextComponent generateDamageComponent() {
        int damage = random.nextInt(5) + 1;
        health = health - damage;

        return Component.text(damage + " ").color(NamedTextColor.RED)
                .append(Component.text("DMG ").decorate(TextDecoration.BOLD))
                .append(Component.text("-> "))
                .append(Component.text("Timid Bandit ").color(NamedTextColor.WHITE))
                .append(Component.text("[" + health + "]").color(NamedTextColor.WHITE));
    }
}

