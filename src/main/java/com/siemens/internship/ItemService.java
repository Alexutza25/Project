package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;

    //thread-safe
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);
    private List<Item> processedItems = new ArrayList<>();
    private int processedCount = 0;


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    private Item processItem(Long id) {
        try {
            // Simulate processing time
            Thread.sleep(100);

            Optional<Item> optionalItem = itemRepository.findById(id);
            if (optionalItem.isEmpty()) {
                return null;
            }

            Item item = optionalItem.get();
            item.setStatus("PROCESSED");
            return itemRepository.save(item);

        } catch (Exception e) {
            // Log error properly instead of just printing (production-ready logging can be used)
            System.err.println("Error processing item with ID " + id + ": " + e.getMessage());
            return null;
        }
    }
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {

        List<Long> itemIds = itemRepository.findAllIds();

        //create a list of CompletableFutures, one for each item
        List<CompletableFuture<Item>> futures = itemIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> processItem(id), executor))
                .collect(Collectors.toList());
        //wait for all futures to complete
        CompletableFuture<Void> allDone = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        //compose the final result: collect processed items when all tasks are done
        return allDone.thenApply(v -> futures.stream()
                .map(CompletableFuture::join) // Safe because all futures are completed
                .filter(item -> item != null)
                .collect(Collectors.toList())
        );
    }

}

