package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import com.fiap.techchallenge.inventory.exceptions.PartNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StockServiceImplTest {

    @Autowired
    StockService stockService;

    @Autowired
    PartCatalogService partCatalogService;

    @Test
    void aZeroQuantityAdjustmentIsRejected() {
        PartInfo part = partCatalogService.create(new CreatePartCommand(
                "STOCK-ZERO-1", "Test Part", null, null, UnitOfMeasure.UNIT, BigDecimal.valueOf(50)));

        assertThatThrownBy(() -> stockService.adjust(part.id(), new AdjustStockCommand(BigDecimal.ZERO, "No-op")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("may not be zero");
    }

    @Test
    void adjustingAnUnknownPartFails() {
        assertThatThrownBy(() -> stockService.adjust(UUID.randomUUID(), new AdjustStockCommand(BigDecimal.TEN, "Seed")))
                .isInstanceOf(PartNotFoundException.class);
    }
}
