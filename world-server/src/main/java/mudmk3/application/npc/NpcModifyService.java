package mudmk3.application.npc;

import lombok.RequiredArgsConstructor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.npc.provided.NpcEditor;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.npc.provided.NpcSaveRequest;
import mudmk3.application.npc.provided.ShopSaveRequest;
import mudmk3.application.npc.required.NpcRepository;
import mudmk3.application.room.provided.RoomDeletedEvent;
import mudmk3.application.room.provided.RoomFinder;
import mudmk3.domain.npc.DuplicateNpcNameException;
import mudmk3.domain.npc.Npc;
import mudmk3.domain.npc.NpcSaveInfo;
import mudmk3.domain.npc.ShopOffer;
import mudmk3.support.stereotype.ValidatedApplicationService;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;

import java.util.List;

@ValidatedApplicationService
@RequiredArgsConstructor
public class NpcModifyService implements NpcEditor {

    private final NpcFinder npcFinder;
    private final NpcRepository npcRepository;
    private final RoomFinder roomFinder;
    private final ItemTypeFinder itemTypeFinder;

    @Override
    public Npc create(NpcSaveRequest request) {
        NpcSaveInfo info = request.toInfo();
        if (npcRepository.existsByNameIgnoreCase(info.name())) {
            throw duplicateName(info.name());
        }
        Npc npc = Npc.create(info);
        place(npc, request.roomId());
        return npcRepository.save(npc);
    }

    @Override
    public Npc update(Long npcId, NpcSaveRequest request) {
        Npc npc = npcFinder.find(npcId);
        NpcSaveInfo info = request.toInfo();
        if (npcRepository.existsByNameIgnoreCaseAndIdNot(info.name(), npcId)) {
            throw duplicateName(info.name());
        }
        npc.update(info);
        place(npc, request.roomId());
        return npcRepository.save(npc);
    }

    @Override
    public void delete(Long npcId) {
        npcRepository.delete(npcFinder.find(npcId));
    }

    @Override
    public Npc openShop(Long npcId, ShopSaveRequest request) {
        Npc npc = npcFinder.find(npcId);
        List<ShopOffer> offers = request.items().stream()
                .map(item -> new ShopOffer(itemTypeFinder.find(item.itemTypeId()), item.price()))
                .toList();
        npc.openShop(request.kind(), offers);
        return npcRepository.save(npc);
    }

    @Override
    public void closeShop(Long npcId) {
        Npc npc = npcFinder.find(npcId);
        npc.closeShop();
        npcRepository.save(npc);
    }

    /** 방이 지워지면 그 방의 NPC 배치를 푼다(방을 지우는 트랜잭션 안에서) */
    @EventListener
    public void onRoomDeleted(RoomDeletedEvent event) {
        npcRepository.findAllByRoomIdOrderByIdAsc(event.roomId()).forEach(Npc::unplace);
    }

    private void place(Npc npc, @Nullable Long roomId) {
        if (roomId == null) {
            npc.unplace();
            return;
        }
        roomFinder.find(roomId);
        npc.placeIn(roomId);
    }

    private static DuplicateNpcNameException duplicateName(String name) {
        return new DuplicateNpcNameException("이미 있는 NPC 이름입니다: " + name);
    }
}
