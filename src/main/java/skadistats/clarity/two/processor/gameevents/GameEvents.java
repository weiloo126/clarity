package skadistats.clarity.two.processor.gameevents;

import com.dota2.proto.Netmessages;
import com.dota2.proto.Networkbasetypes;
import com.google.common.base.Predicate;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.two.framework.annotation.Initializer;
import skadistats.clarity.two.framework.annotation.Provides;
import skadistats.clarity.two.framework.invocation.EventListener;
import skadistats.clarity.two.processor.reader.OnMessage;
import skadistats.clarity.two.processor.runner.Context;

import java.util.Map;
import java.util.TreeMap;

@Provides({OnGameEvent.class})
public class GameEvents {

    private final Map<Integer, GameEventDescriptor> byId = new TreeMap<>();
    private final Map<String, GameEventDescriptor> byName = new TreeMap<>();

    @Initializer(OnGameEvent.class)
    public void initOnGameEvent(final Context ctx, final EventListener<OnGameEvent> eventListener) {
        eventListener.setInvocationPredicate(new Predicate<Object[]>() {
            @Override
            public boolean apply(Object[] args) {
                String v = eventListener.getAnnotation().value();
                GameEvent ev = (GameEvent) args[0];
                return v.length() == 0 || v.equals(ev.getName());
            }
        });
    }

    @OnMessage(Netmessages.CSVCMsg_GameEventList.class)
    public void onGameEventList(Context ctx, Netmessages.CSVCMsg_GameEventList message) {
        for (Netmessages.CSVCMsg_GameEventList.descriptor_t d : message.getDescriptorsList()) {
            String[] keys = new String[d.getKeysCount()];
            for (int i = 0; i < d.getKeysCount(); i++) {
                Netmessages.CSVCMsg_GameEventList.key_t k = d.getKeys(i);
                keys[i] = k.getName();
            }
            GameEventDescriptor gev = new GameEventDescriptor(
                d.getEventid(),
                d.getName(),
                keys
            );
            byName.put(gev.getName(), gev);
            byId.put(gev.getEventId(), gev);
        }
    }

    @OnMessage(Networkbasetypes.CSVCMsg_GameEvent.class)
    public void onGameEvent(Context ctx, Networkbasetypes.CSVCMsg_GameEvent message) {
        GameEventDescriptor desc = byId.get(message.getEventid());
        GameEvent e = new GameEvent(desc);
        for (int i = 0; i < message.getKeysCount(); i++) {
            Networkbasetypes.CSVCMsg_GameEvent.key_t key = message.getKeys(i);
            Object value = null;
            switch (key.getType()) {
                case 1:
                    value = key.getValString();
                    break;
                case 2:
                    value = key.getValFloat();
                    break;
                case 3:
                    value = key.getValLong();
                    break;
                case 4:
                    value = key.getValShort();
                    break;
                case 5:
                    value = key.getValByte();
                    break;
                case 6:
                    value = key.getValBool();
                    break;
                case 7:
                    value = key.getValUint64();
                    break;
                default:
                    throw new RuntimeException("cannot handle game event key type " + key.getType());
            }
            e.set(i, value);
        }
        ctx.createEvent(OnGameEvent.class, GameEvent.class).raise(e);
    }


}