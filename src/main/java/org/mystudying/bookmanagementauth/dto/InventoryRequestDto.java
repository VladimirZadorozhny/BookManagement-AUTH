package org.mystudying.bookmanagementauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change book inventory amount")
public record InventoryRequestDto(
        @NotNull(message = "Amount cannot be null")
        @Min(value = 1, message = "Amount must be at least 1")
        @Schema(example = "5")
        Integer amount
) {
}
