package com.example.foodmobochain;

import org.junit.Test;

import com.example.foodmobochain.model.CartLine;
import com.example.foodmobochain.model.FoodItem;

import static org.junit.Assert.*;

public class ExampleUnitTest {
    @Test
    public void cartLineCalculatesExpectedSubtotal() {
        FoodItem item = new FoodItem();
        item.id = "kacchi";
        item.name = "Kacchi Biryani";
        item.price = 320;

        CartLine line = new CartLine(item, 2);

        assertEquals("kacchi", line.foodId);
        assertEquals(2, line.quantity);
        assertEquals(640.0, line.unitPrice * line.quantity, 0.001);
    }
}
