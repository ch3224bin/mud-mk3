package mudmk3.adapter.webapi;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mudmk3.adapter.webapi.dto.ItemTypeResponse;
import mudmk3.application.item.provided.ItemTypeEditor;
import mudmk3.application.item.provided.ItemTypeFinder;
import mudmk3.application.item.provided.ItemTypeSaveRequest;
import mudmk3.support.stereotype.WebApiAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

/** 관리: 아이템 종류 */
@WebApiAdapter
@RequestMapping("/api/admin/item-types")
@RequiredArgsConstructor
public class ItemTypeApi {

    private final ItemTypeFinder itemTypeFinder;
    private final ItemTypeEditor itemTypeEditor;

    @GetMapping
    public List<ItemTypeResponse> list() {
        return itemTypeFinder.findAll().stream().map(ItemTypeResponse::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemTypeResponse create(@RequestBody @Valid ItemTypeSaveRequest request) {
        return ItemTypeResponse.of(itemTypeEditor.create(request));
    }

    @GetMapping("/{itemTypeId}")
    public ItemTypeResponse find(@PathVariable Long itemTypeId) {
        return ItemTypeResponse.of(itemTypeFinder.find(itemTypeId));
    }

    @PutMapping("/{itemTypeId}")
    public ItemTypeResponse update(@PathVariable Long itemTypeId, @RequestBody @Valid ItemTypeSaveRequest request) {
        return ItemTypeResponse.of(itemTypeEditor.update(itemTypeId, request));
    }

    @DeleteMapping("/{itemTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long itemTypeId) {
        itemTypeEditor.delete(itemTypeId);
    }
}
