package com.example.heartbit.controller;

import com.example.heartbit.domain.Category;
import com.example.heartbit.repository.CategoryRepository;
import com.example.heartbit.service.OrderService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @AfterEach
    void tearDown() {
        categoryRepository.deleteAllInBatch();
    }

    @DisplayName("전체 종목 목록을 조회한다.")
    @Test
    @Transactional
    @WithMockUser
    void readCategory() throws Exception {
        // given
        categoryRepository.saveAll(List.of(
                new Category(null, "비트코인", "BTC"),
                new Category(null, "이더리움", "ETH")
        ));

        // when & then
        mockMvc.perform(get("/api/categories")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].categoryName").value("비트코인"))
                .andExpect(jsonPath("$[1].symbol").value("ETH"));
    }

    @DisplayName("단건의 종목을 조회한다.")
    @Test
    @WithMockUser
    void readOneCategory() throws Exception {
        // given
        Category category = Category.builder()
                .categoryName("비트코인")
                .symbol("BTC")
                .build();

        Category savedCategory = categoryRepository.save(category);
        Long generatedId = savedCategory.getCategoryId();

        // when & then
        mockMvc.perform(
                        get("/api/category")
                                .param("categoryId", String.valueOf(generatedId)) // 생성된 ID 사용
                )
                .andExpect(status().isOk());

    }

}