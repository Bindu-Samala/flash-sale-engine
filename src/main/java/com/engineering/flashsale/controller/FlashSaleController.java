package com.engineering.flashsale.controller;

import com.engineering.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/flashsale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @PostMapping("/reserve")
    public ResponseEntity<String> reserve(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam(defaultValue = "1") int quantity) {

        String status = flashSaleService.reserveProduct(userId, productId, quantity);

        if ("OUT_OF_STOCK".equals(status)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Inventory exhausted.");
        }
        return ResponseEntity.ok("Item reserved successfully. Processing confirmation.");
    }
}