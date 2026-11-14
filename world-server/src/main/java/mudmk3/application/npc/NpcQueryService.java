package mudmk3.application.npc;

import lombok.RequiredArgsConstructor;
import mudmk3.application.npc.provided.NpcFinder;
import mudmk3.application.npc.required.NpcRepository;
import mudmk3.domain.npc.Npc;
import mudmk3.support.stereotype.ApplicationService;

import java.util.List;

@ApplicationService
@RequiredArgsConstructor
public class NpcQueryService implements NpcFinder {

    private final NpcRepository npcRepository;

    @Override
    public Npc find(Long npcId) {
        return npcRepository.findById(npcId)
                .orElseThrow(() -> new IllegalArgumentException("NPC를 찾을 수 없습니다. id: " + npcId));
    }

    @Override
    public List<Npc> findAll() {
        return npcRepository.findAllByOrderByNameAsc();
    }

    @Override
    public List<Npc> findAllInRoom(Long roomId) {
        return npcRepository.findAllByRoomIdOrderByIdAsc(roomId);
    }

    @Override
    public boolean existsByName(String name) {
        return npcRepository.existsByNameIgnoreCase(name);
    }
}
