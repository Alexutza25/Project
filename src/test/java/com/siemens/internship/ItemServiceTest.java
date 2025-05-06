package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ItemServiceTest {

    @InjectMocks
    private ItemService itemService;

    // using @Mock here to simulate the behavior of ItemRepository
    // this lets me test the ItemService in isolation without touching the actual database
    // it helps verify logic inside the service without worrying about external dependencies
    @Mock
    private ItemRepository itemRepository;

    private Item sampleItem;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        sampleItem = new Item();
        sampleItem.setId(1L);
        sampleItem.setName("Sample");
        sampleItem.setDescription("Description");
        sampleItem.setEmail("test@example.com");
        sampleItem.setStatus("NEW");
    }

    @Test
    public void testFindAll() {
        // test if findAll returns the list from repository
        List<Item> items = List.of(sampleItem);
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();
        assertEquals(1, result.size());
        assertEquals("Sample", result.get(0).getName());
    }

    @Test
    public void testFindById_found() {
        // test that findById returns an item if it exists
        when(itemRepository.findById(1L)).thenReturn(Optional.of(sampleItem));

        Optional<Item> result = itemService.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("Sample", result.get().getName());
    }

    @Test
    public void testFindById_notFound() {
        // test that findById returns empty optional if item doesn't exist
        when(itemRepository.findById(2L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(2L);
        assertFalse(result.isPresent());
    }

    @Test
    public void testSave() {
        // test that save passes the item to repository and returns it
        when(itemRepository.save(sampleItem)).thenReturn(sampleItem);

        Item saved = itemService.save(sampleItem);
        assertEquals("Sample", saved.getName());
    }

    @Test
    public void testDeleteById() {
        // test that deleteById calls the repository method
        itemService.deleteById(1L);
        verify(itemRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testProcessItemsAsync_success() throws ExecutionException, InterruptedException {
        // simulate 2 item IDs
        // test processing of items when all are found and valid
        List<Long> ids = List.of(1L, 2L);
        Item item1 = new Item(1L, "Item1", "desc1", "NEW", "a@b.com");
        Item item2 = new Item(2L, "Item2", "desc2", "NEW", "b@c.com");

        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(i -> i.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get(); // block for test

        assertEquals(2, result.size());
        assertEquals("PROCESSED", result.get(0).getStatus());
        assertEquals("PROCESSED", result.get(1).getStatus());
    }

    @Test
    public void testProcessItemsAsync_withMissingItem() throws Exception {
        // test processing when one of the items is not found
        List<Long> ids = List.of(1L);
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get(); // block for test

        assertTrue(result.isEmpty());
    }
}
