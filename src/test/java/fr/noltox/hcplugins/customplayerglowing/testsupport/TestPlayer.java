package fr.noltox.hcplugins.customplayerglowing.testsupport;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** In-memory player/PDC fixture; unexpected calls (including saveData) fail the test. */
public final class TestPlayer {

    private final UUID id = UUID.randomUUID();
    private final Map<NamespacedKey, Object> data = new HashMap<>();
    private final Set<String> permissions = new HashSet<>();
    private final List<Component> messages = new ArrayList<>();
    private boolean glowing;
    private int writes;
    private final PersistentDataContainer pdc = (PersistentDataContainer) Proxy.newProxyInstance(
            PersistentDataContainer.class.getClassLoader(),
            new Class<?>[]{PersistentDataContainer.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "get" -> {
                    Object value = data.get((NamespacedKey) args[0]);
                    var type = (PersistentDataType<?, ?>) args[1];
                    yield type.getPrimitiveType().isInstance(value) ? value : null;
                }
                case "set" -> {
                    writes++;
                    data.put((NamespacedKey) args[0], args[2]);
                    yield null;
                }
                case "remove" -> {
                    data.remove((NamespacedKey) args[0]);
                    yield null;
                }
                case "getKeys" -> Set.copyOf(data.keySet());
                case "isEmpty" -> data.isEmpty();
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "TestPDC";
                default -> throw new UnsupportedOperationException(method.toString());
            }
    );
    private final Player player = (Player) Proxy.newProxyInstance(
            Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getPersistentDataContainer" -> pdc;
                case "getUniqueId" -> id;
                case "getName" -> "TestPlayer";
                case "isOnline" -> true;
                case "isGlowing" -> glowing;
                case "setGlowing" -> {
                    glowing = (boolean) args[0];
                    yield null;
                }
                case "hasPermission" -> permissions.contains((String) args[0]);
                case "sendMessage" -> {
                    messages.add((Component) args[0]);
                    yield null;
                }
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "TestPlayer[" + id + "]";
                default -> throw new UnsupportedOperationException(method.toString());
            }
    );

    public static Plugin plugin() {
        return (Plugin) Proxy.newProxyInstance(Plugin.class.getClassLoader(), new Class<?>[]{Plugin.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> "HCGlowing";
                    case "namespace" -> "hcglowing";
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "HCGlowing";
                    default -> throw new UnsupportedOperationException(method.toString());
                });
    }

    public Player player() { return player; }
    public PersistentDataContainer pdc() { return pdc; }
    public int writes() { return writes; }
    public void allow(String permission) { permissions.add(permission); }
    public void revoke(String permission) { permissions.remove(permission); }
    public List<Component> messages() { return List.copyOf(messages); }
}
