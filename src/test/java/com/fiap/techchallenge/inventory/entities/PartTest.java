package com.fiap.techchallenge.inventory.entities;

import com.fiap.techchallenge.inventory.exceptions.InvalidStockAdjustmentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartTest {

    @Test
    void applyAdjustmentRejectsDroppingOnHandBelowWhatIsAlreadyReserved() {
        Part part = partWith(BigDecimal.TEN, BigDecimal.valueOf(6));

        // 10 - 5 = 5, which is below the 6 already reserved: this claim would break a promise.
        assertThatThrownBy(() -> part.applyAdjustment(BigDecimal.valueOf(-5)))
                .isInstanceOf(InvalidStockAdjustmentException.class)
                .hasMessageContaining("already reserved");

        // Rejected atomically: on-hand is untouched.
        assertThat(part.getQuantityOnHand()).isEqualByComparingTo("10");
    }

    @Test
    void applyAdjustmentAllowsDroppingOnHandDownToExactlyWhatIsReserved() {
        Part part = partWith(BigDecimal.TEN, BigDecimal.valueOf(6));

        part.applyAdjustment(BigDecimal.valueOf(-4));

        assertThat(part.getQuantityOnHand()).isEqualByComparingTo("6");
    }

    @Test
    void releaseReservationRejectsReleasingMoreThanIsReserved() {
        Part part = partWith(BigDecimal.TEN, BigDecimal.valueOf(3));

        assertThatThrownBy(() -> part.releaseReservation(BigDecimal.valueOf(4)))
                .isInstanceOf(InvalidStockAdjustmentException.class)
                .hasMessageContaining("Cannot release more than what is reserved");

        assertThat(part.getQuantityReserved()).isEqualByComparingTo("3");
    }

    @Test
    void receivingIntoAPartWithNothingOnHandAndNothingReceivedLeavesAverageCostAtZero() {
        Part part = partWith(BigDecimal.ZERO, BigDecimal.ZERO);

        // Zero on hand + zero received = zero total: the division-by-zero guard, not the weighted-average path.
        part.receive(BigDecimal.ZERO, BigDecimal.valueOf(12));

        assertThat(part.getAverageCost()).isEqualByComparingTo("0");
        assertThat(part.getQuantityOnHand()).isEqualByComparingTo("0");
    }

    @Test
    void receivingStockRollsItIntoAWeightedAverageCost() {
        Part part = partWith(BigDecimal.TEN, BigDecimal.ZERO);
        part.setAverageCost(BigDecimal.valueOf(4));

        // (10 * 4 + 10 * 8) / 20 = 6
        part.receive(BigDecimal.TEN, BigDecimal.valueOf(8));

        assertThat(part.getAverageCost()).isEqualByComparingTo("6.00");
        assertThat(part.getQuantityOnHand()).isEqualByComparingTo("20");
    }

    private Part partWith(BigDecimal onHand, BigDecimal reserved) {
        Part part = new Part();
        part.setId(UUID.randomUUID());
        part.setSku("PART-" + UUID.randomUUID());
        part.setName("Test Part");
        part.setSalePrice(BigDecimal.valueOf(50));
        part.setQuantityOnHand(onHand);
        part.setQuantityReserved(reserved);

        return part;
    }
}
