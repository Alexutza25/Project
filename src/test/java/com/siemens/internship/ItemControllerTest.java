package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//this test only loads the web layer (controllers), without full Spring context
@WebMvcTest(ItemController.class)
public class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Autowired
    private ObjectMapper objectMapper;

    //testing GET /api/items
    @Test
    public void testGetAllItems_returnsList() throws Exception {
        List<Item> items = List.of(new Item(1L, "name", "desc", "NEW", "a@b.com"));
        when(itemService.findAll()).thenReturn(items);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("name"));
    }

    // tsting GET /api/items/{id} with found item
    @Test
    public void testGetItemById_found() throws Exception {
        Item item = new Item(1L, "nume", "desc", "NEW", "email@test.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("nume"));
    }

    // testing GET /api/items/{id} when item not found
    @Test
    public void testGetItemById_notFound() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/1"))
                .andExpect(status().isNoContent());
    }

    // testing POST /api/items with valid item
    @Test
    public void testCreateItem_valid() throws Exception {
        Item item = new Item(null, "x", "desc", "NEW", "test@email.com");
        Item saved = new Item(1L, "x", "desc", "NEW", "test@email.com");

        when(itemService.save(any(Item.class))).thenReturn(saved);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    //testing POST /api/items with invalid email
    @Test
    public void testCreateItem_invalidEmail_returnsBadRequest() throws Exception {
        Item item = new Item(null, "x", "desc", "NEW", "bad@em..ail.com");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andDo(print()) // <--- helps you see why it fails
                .andExpect(status().isBadRequest());
    }

    //testing PUT /api/items/{id} when item exists
    @Test
    public void testUpdateItem_found() throws Exception {
        Item item = new Item(null, "updated", "desc", "PROCESSED", "a@b.com");
        when(itemService.findById(1L)).thenReturn(Optional.of(item));
        when(itemService.save(any(Item.class))).thenReturn(item);

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));
    }

    //testing PUT /api/items/{id} when not found
    @Test
    public void testUpdateItem_notFound() throws Exception {
        Item item = new Item(null, "updated", "desc", "PROCESSED", "a@b.com");
        when(itemService.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isNotFound());
    }

    // testing DELETE /api/items/{id}
    @Test
    public void testDeleteItem_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
        verify(itemService).deleteById(1L);
    }

    // testing GET /api/items/process
    @Test
    public void testProcessItems_returnsProcessedList() throws Exception {
        List<Item> processed = List.of(
                new Item(1L, "a", "desc", "PROCESSED", "x@y.com")
        );

        when(itemService.processItemsAsync())
                .thenReturn(CompletableFuture.completedFuture(processed));

        // perform the async request
        MvcResult result = mockMvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted()) // check that it is async
                .andReturn();

        //wait for async to complete
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }
}
