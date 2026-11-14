package mudmk3.application.inventory;

import lombok.RequiredArgsConstructor;
import mudmk3.application.inventory.provided.InventoryFinder;
import mudmk3.application.inventory.required.InventoryRepository;
import mudmk3.domain.inventory.Inventory;
import mudmk3.support.stereotype.ApplicationService;

@ApplicationService
@RequiredArgsConstructor
public class InventoryQueryService implements InventoryFinder {

    private final InventoryRepository inventoryRepository;

    @Override
    public Inventory find(Long characterId) {
        return inventoryRepository.findByCharacterId(characterId)
                .orElseGet(() -> Inventory.empty(characterId));
    }
}
