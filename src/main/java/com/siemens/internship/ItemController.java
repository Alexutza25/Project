package com.siemens.internship;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.findAll());
    }

    /**
     * Fixed: Validation errors now return 400 Bad Request(was incorrectly returning 201 Created)
     * On success, returns 201 Created with the saved item
     */
    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation failed");
        }
        Item savedItem = itemService.save(item);
        return new ResponseEntity<>(savedItem, HttpStatus.CREATED);
    }

    /**
     * Kept mostly as-is, but improved readability using ResponseEntity::ok
     * Returns 204 No content if item is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * Fixed: Added @Valid and BindingResult to validate input
     * Returns 400 Bad Request on validation errors
     * Returns 200 OK on successful update
     * Returns 404 Not found if item does not exist
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation failed");
        }

        Optional<Item> existingItem = itemService.findById(id);
        if (existingItem.isPresent()) {
            item.setId(id);
            return ResponseEntity.ok(itemService.save(item));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Fixed: Now returns 204 No Content on successful deletion (was incorrectly returning 409 Conflict)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteById(id);
        return ResponseEntity.noContent().build(); // 204 No Content for successful deletion
    }

    /**
     * Method now returns CompletableFuture<ResponseEntity<List<Item>>> (compatible with Service)
     * Uses thenApply to return response only after processing is complete
     */
    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        // This will return the response when processing is done
        return itemService.processItemsAsync()
                .thenApply(ResponseEntity::ok);
    }
}
