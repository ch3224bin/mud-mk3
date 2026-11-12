package mudmk3.adapter.webapi.dto;

import jakarta.validation.constraints.NotNull;

public record StartRoomRequest(@NotNull Long roomId) {
}
