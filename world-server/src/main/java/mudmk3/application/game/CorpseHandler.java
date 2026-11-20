package mudmk3.application.game;

import lombok.RequiredArgsConstructor;
import mudmk3.application.game.required.CorpseRepository;
import mudmk3.application.game.required.GameMessage;
import mudmk3.application.game.required.GameMessage.NoticeMessage;
import mudmk3.application.game.required.GameMessage.SystemMessage;
import mudmk3.application.game.required.PresenceRepository;
import mudmk3.application.inventory.provided.InventoryEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.mob.provided.MobTypeFinder;
import mudmk3.domain.combat.Corpse;
import mudmk3.domain.combat.MobInstance;
import mudmk3.domain.game.OnlinePlayer;
import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.item.ItemType;
import mudmk3.domain.item.SpiritStoneGrade;
import mudmk3.domain.mob.DropLine;
import mudmk3.domain.mob.DropRoll;
import mudmk3.domain.mob.MobFixedDrop;
import mudmk3.domain.mob.MobTierDrop;
import mudmk3.domain.mob.MobType;
import mudmk3.domain.shared.Josa;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * 시체와 {@code 수습}(D87, command.md {@code ### 수습}): 몹이 쓰러질 때 드롭을 굴려 시체를 남기고, 수습하면 들어가는 만큼
 * 저물대에 넣는다. 3분이 된 시체를 지운다. {@link CombatEngine}·{@link CombatCommandHandler} 가 게임 락 안에서 부른다
 */
@Component
@RequiredArgsConstructor
class CorpseHandler {

    private final CorpseRepository corpseRepository;
    private final PresenceRepository presenceRepository;
    private final MobTypeFinder mobTypeFinder;
    private final ItemTypeFinder itemTypeFinder;
    private final InventoryEditor inventoryEditor;
    private final CharacterRealms realms;
    private final RandomGenerator random;
    private final CombatTexts combatTexts;
    private final RoomRefresher roomRefresher;

    /** 시체 id. 서버가 돌아가는 동안 늘기만 한다 */
    private final AtomicLong corpseIds = new AtomicLong();

    /**
     * 쓰러진 몹의 시체를 그 방에 남긴다. 드롭은 쓰러지는 순간의 드롭 표로 지금 굴려 정한다(비어도 시체는 남는다).
     * 몹 유형이 이미 지워졌으면(배치·유형 삭제 뒤 다음 틱 전) 표가 없으므로 드롭 없는 시체다(L-D3)
     */
    void leaveCorpse(MobInstance mob, @Nullable Long killerCharacterId, Instant now) {
        List<DropLine> drops = mobTypeFinder.findOptional(mob.getProfile().mobTypeId())
                .map(this::rollDrops)
                .orElse(List.of());
        corpseRepository.save(new Corpse(corpseIds.incrementAndGet(), mob.getRoomId(), mob.getProfile().name(),
                killerCharacterId, now, drops));
    }

    private List<DropLine> rollDrops(MobType type) {
        List<Long> itemTypeIds = Stream.concat(
                        type.getFixedDrops().stream().map(MobFixedDrop::itemTypeId),
                        type.getTierDrops().stream().map(MobTierDrop::itemTypeId))
                .distinct()
                .toList();
        return DropRoll.roll(type, itemTypeFinder.findAllById(itemTypeIds),
                itemTypeFinder.spiritStone(SpiritStoneGrade.LOW), random);
    }

    /** 그 방의 시체(쓰러진 순) */
    List<Corpse> inRoom(Long roomId) {
        return corpseRepository.findAllInRoom(roomId);
    }

    /** 그 방의 시체를 남은 것과 함께 지운다(방 삭제, 알리지 않음) */
    void removeInRoom(Long roomId) {
        corpseRepository.deleteAllInRoom(roomId);
    }

    /** 쓰러진 지 3분이 된 시체를 남은 것과 함께 지운다(알리지 않음, 그 방을 표시해 조용한 방 상태만 보냄) */
    void removeExpired(Instant now) {
        for (Corpse corpse : corpseRepository.findAll()) {
            if (corpse.expired(now)) {
                corpseRepository.delete(corpse.getId());
                roomRefresher.mark(corpse.getRoomId());
            }
        }
    }

    /**
     * {@code 보기 <시체>}(command.md {@code ## 시체 보기}): 남은 줄을 지금 종류 이름으로 보인다(지워진 종류는 안 보이고 빼지도 않음).
     * 누구나 보고, 보는 사람이 아직 수습할 수 없으면 둘째 줄에 수습 거절 문구를 붙인다. 시체를 바꾸지 않는다
     */
    SystemMessage look(Corpse corpse, Long viewerCharacterId, Instant now) {
        Map<Long, ItemType> itemTypes = itemTypeFinder.findAllById(
                corpse.getRemaining().stream().map(DropLine::itemTypeId).toList());
        List<String> parts = corpse.getRemaining().stream()
                .filter(line -> itemTypes.containsKey(line.itemTypeId()))
                .map(line -> CombatTexts.dropPart(requireNonNull(itemTypes.get(line.itemTypeId())).getName(),
                        line.count()))
                .toList();
        String text = CombatTexts.lookCorpse(corpse.name(), parts);
        if (!corpse.collectibleBy(viewerCharacterId, now)) {
            text = text + "\n" + combatTexts.otherShare(corpse.lockedFor(now));
        }
        return SystemMessage.item(text);
    }

    /**
     * {@code 수습 <시체>}: 시체 이름으로 찾고, 처음 60초는 처치자만 거둔다. 줄마다 들어가는 만큼 넣고 넣은 만큼 시체에서 뺀다.
     * 지워진 물건 종류 줄은 조용히 뺀다. 남은 것이 없으면 시체가 사라진다. 교전 중 막힘은 부르는 쪽이 본다
     */
    void collect(OnlinePlayer player, @Nullable ItemQuery query, Instant now, BiConsumer<String, GameMessage> send) {
        String sessionId = player.getSessionId();
        if (query == null) {
            send.accept(sessionId, SystemMessage.error(CombatTexts.COLLECT_NO_ARGUMENT));
            return;
        }
        Optional<Corpse> found = ItemNames.select(inRoom(player.getRoomId()), Corpse::name, query);
        if (found.isEmpty()) {
            String input = ItemCommandHandler.inputOf(query);
            send.accept(sessionId, SystemMessage.error("이곳에 '" + input + "'" + Josa.eunNeun(input) + " 없습니다."));
            return;
        }
        Corpse corpse = found.get();
        Long characterId = player.getCharacterId();
        if (!corpse.collectibleBy(characterId, now)) {
            send.accept(sessionId, SystemMessage.error(combatTexts.otherShare(corpse.lockedFor(now))));
            return;
        }

        Map<Long, ItemType> itemTypes = itemTypeFinder.findAllById(
                corpse.getRemaining().stream().map(DropLine::itemTypeId).toList());
        boolean braceletOpen = realms.braceletOpen(characterId);
        List<String> collected = new ArrayList<>();
        int index = 0;
        while (index < corpse.getRemaining().size()) {
            DropLine line = corpse.getRemaining().get(index);
            ItemType itemType = itemTypes.get(line.itemTypeId());
            if (itemType == null) {
                // 드롭 표가 고쳐진 뒤 지워진 물건 종류: 조용히 뺀다
                corpse.takeFrom(index, line.count());
                continue;
            }
            int added = inventoryEditor.addUpTo(characterId, line.itemTypeId(), line.count(), braceletOpen);
            if (added > 0) {
                collected.add(CombatTexts.dropPart(itemType.getName(), added));
                corpse.takeFrom(index, added);
            }
            if (added < line.count()) {
                index++;
            }
        }
        List<String> left = corpse.getRemaining().stream()
                .map(line -> CombatTexts.dropPart(requireNonNull(itemTypes.get(line.itemTypeId())).getName(),
                        line.count()))
                .toList();

        if (corpse.isEmpty()) {
            corpseRepository.delete(corpse.getId());
            roomRefresher.mark(player.getRoomId());
        } else {
            corpseRepository.save(corpse);
        }
        if (collected.isEmpty() && left.isEmpty()) {
            send.accept(sessionId, SystemMessage.item(combatTexts.emptyCorpse(corpse.name())));
        } else {
            if (!collected.isEmpty()) {
                send.accept(sessionId, SystemMessage.item(combatTexts.collected(corpse.name(), collected)));
            }
            if (!left.isEmpty()) {
                send.accept(sessionId, SystemMessage.error(combatTexts.noRoomLeft(left)));
            }
            if (collected.isEmpty()) {
                return; // 하나도 거두지 못했으면 같은 방에 알리지 않는다
            }
        }
        NoticeMessage notice = NoticeMessage.item(combatTexts.collectNotice(player.getName(), corpse.name()),
                player.getName(), "collect");
        presenceRepository.findAllInRoom(player.getRoomId()).stream()
                .filter(other -> !other.getCharacterId().equals(characterId))
                .forEach(other -> send.accept(other.getSessionId(), notice));
    }
}
