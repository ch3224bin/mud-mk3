package mudmk3.adapter.webapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mudmk3.application.world.provided.StartingItemRequest;

import java.util.List;

/** 시작 소지품을 통째로 바꾸는 요청(입력 순서, 30줄까지) */
public record StartingItemsSaveRequest(@NotNull @Size(max = 30) List<@NotNull @Valid StartingItemRequest> items) {
}
